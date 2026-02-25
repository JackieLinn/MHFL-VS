from abc import ABC, abstractmethod
from typing import Dict, List, Callable, Optional, Any, Protocol, runtime_checkable
from collections import defaultdict
import random
import logging
from pathlib import Path
from datetime import datetime
import csv

import numpy as np
import torch
import torch.nn as nn
import torch.utils.data
from sklearn.metrics import precision_score, recall_score, f1_score
from tqdm import tqdm

from fl_core.utils.node import BaseNodes
from fl_core.utils.tools import get_device, set_seed


@runtime_checkable
class StopEventLike(Protocol):
    """threading.Event 或 multiprocessing.Manager().Event() 代理等，仅需 is_set()。"""

    def is_set(self) -> bool: ...


def get_data_path() -> Path:
    """
    获取数据路径，确保无论从哪里运行都能找到 mhfl-algo/data
    
    路径解析：
    - __file__ = mhfl-algo/fl_core/trainer.py
    - parent = mhfl-algo/fl_core/
    - parent.parent = mhfl-algo/
    """
    current_file = Path(__file__).resolve()
    project_root = current_file.parent.parent
    data_path = project_root / "data"
    return data_path


def get_results_path() -> Path:
    """
    获取结果目录路径
    
    路径解析：
    - __file__ = mhfl-algo/fl_core/trainer.py
    - parent = mhfl-algo/fl_core/
    - parent.parent = mhfl-algo/
    """
    current_file = Path(__file__).resolve()
    project_root = current_file.parent.parent
    results_path = project_root / "results"
    results_path.mkdir(parents=True, exist_ok=True)
    return results_path


class BaseTrainer(ABC):
    """
    联邦学习训练器基类
    
    参数说明:
    - data_name: 数据集名称 (String)
    - algorithm_name: 算法名称 (String, 用于判断使用哪个算法)
    - num_nodes: 客户端数量
    - fraction: 每轮选择的客户端比例
    - classes_per_node: 每个客户端的类别数
    - low_prob: Non-IID 数据分布的低概率阈值
    - num_steps: 训练总轮次
    - epochs: 每个客户端每轮的训练轮次
    """

    def __init__(
            self,
            tid: int,
            # 配置参数
            data_name: str,
            algorithm_name: str,
            num_nodes: int,
            fraction: float,
            classes_per_node: int,
            low_prob: float,
            num_steps: int,
            epochs: int,
            # 默认参数
            data_path: Optional[str] = None,
            batch_size: int = 512,
            lr: float = 1e-2,
            wd: float = 1e-3,
            optim: str = 'sgd',
            n_kernels: int = 16,
            seed: int = 42,
            gpu: int = 0,
            # 回调函数用于实时数据传输
            step_callback: Optional[Callable[[int, int, Dict[str, Any]], None]] = None,  # (tid, step, metrics)
            client_callback: Optional[Callable[[int, int, int, Dict[str, Any]], None]] = None,
            # (tid, step, client_id, metrics)
            # 停止信号：由外部设置后，训练循环会在下一轮或下一客户端前退出（支持 threading.Event 或 Manager().Event()）
            stop_event: Optional[StopEventLike] = None,
    ):
        """
        初始化训练器
        
        Args:
            tid: Task ID，用于区分不同的训练任务
            data_name: 数据集名称 (cifar100, tiny-imagenet)
            algorithm_name: 算法名称
            num_nodes: 客户端数量
            fraction: 每轮选择的客户端比例
            classes_per_node: 每个客户端的类别数
            low_prob: Non-IID 数据分布的低概率阈值
            num_steps: 训练总轮次
            epochs: 每个客户端每轮的训练轮次
            data_path: 数据路径，如果为 None 则自动查找 mhfl-algo/data
            batch_size: 批次大小
            lr: 学习率
            wd: 权重衰减
            optim: 优化器类型 (sgd/adam)
            n_kernels: 卷积核数量
            seed: 随机种子
            gpu: GPU 设备编号
            step_callback: 每轮训练完成后的回调函数 (tid, step, metrics)
            client_callback: 每个客户端训练完成后的回调函数 (tid, step, client_id, metrics)
            stop_event: 若设置则训练循环会定期检查，is_set() 时退出
        """
        # Task ID
        self.tid = tid
        self.stop_event = stop_event

        # 基础参数
        self.data_name = data_name
        self.algorithm_name = algorithm_name
        self.num_nodes = num_nodes
        self.fraction = fraction
        self.classes_per_node = classes_per_node
        self.low_prob = low_prob
        self.num_steps = num_steps
        self.epochs = epochs

        # 默认参数 - 处理数据路径
        if data_path is None:
            data_path = str(get_data_path())
        self.data_path = data_path
        self.batch_size = batch_size
        self.lr = lr
        self.wd = wd
        self.optim = optim
        self.n_kernels = n_kernels
        self.seed = seed
        self.gpu = gpu

        # 回调函数
        self.step_callback = step_callback
        self.client_callback = client_callback

        # 设置随机种子
        set_seed(self.seed)

        # 设备
        self.device = get_device(gpus=self.gpu)

        # 数据集相关配置
        self._init_dataset_config()

        # 初始化节点
        self.nodes = BaseNodes(
            data_name=self.data_name,
            data_path=self.data_path,
            n_nodes=self.num_nodes,
            batch_size=self.batch_size,
            classes_per_node=self.classes_per_node,
            LowProb=self.low_prob
        )

        # 初始化模型（子类实现）
        self.net_set = self._init_models()

        # 初始化优化器和损失函数
        self.criteria = nn.CrossEntropyLoss()
        self.optimizers = self._init_optimizers()

        # 客户端指标记录
        self.client_acc = defaultdict(float)
        self.client_metrics_all = defaultdict(lambda: {
            'loss': 0.0,
            'accuracy': 0.0,
            'precision': 0.0,
            'recall': 0.0,
            'f1_score': 0.0
        })  # 存储所有客户端的最新指标

        # 初始化CSV日志文件
        self.csv_file_path = self._init_csv_log()

        # 打印基础参数（只打印用户传入的参数，不包括默认参数）
        self._log_info(
            f"Trainer initialized: "
            f"tid={self.tid}, "
            f"data_name={self.data_name}, "
            f"algorithm_name={self.algorithm_name}, "
            f"num_nodes={self.num_nodes}, "
            f"fraction={self.fraction}, "
            f"classes_per_node={self.classes_per_node}, "
            f"low_prob={self.low_prob}, "
            f"num_steps={self.num_steps}, "
            f"epochs={self.epochs}"
        )

    def _log_info(self, message: str):
        """
        带 Task ID 的日志输出
        
        Args:
            message: 日志消息
        """
        logging.info(f"[Task-{self.tid}] {message}")

    def _log_warning(self, message: str):
        """
        带 Task ID 的警告日志输出
        
        Args:
            message: 日志消息
        """
        logging.warning(f"[Task-{self.tid}] {message}")

    def _log_error(self, message: str):
        """
        带 Task ID 的错误日志输出
        
        Args:
            message: 日志消息
        """
        logging.error(f"[Task-{self.tid}] {message}")

    def _init_csv_log(self) -> Path:
        """
        初始化CSV日志文件
        
        Returns:
            CSV文件路径
        """
        # 生成文件名（时间戳格式：年月日时分秒，如20260205143025）
        timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
        filename = (
            f"{timestamp}_T{self.tid}_{self.algorithm_name}_{self.data_name}_"
            f"{self.classes_per_node}_N{self.num_nodes}_C{self.fraction}_"
            f"R{self.num_steps}_E{self.epochs}_low{self.low_prob}.csv"
        )

        results_path = get_results_path()
        csv_file_path = results_path / filename

        # 创建CSV文件并写入表头
        with open(csv_file_path, 'w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)

            # 构建表头
            headers = [
                'mean_loss', 'mean_accuracy', 'mean_precision', 'mean_recall', 'mean_f1_score'
            ]

            # 添加每个客户端的5个指标
            for client_id in range(self.num_nodes):
                headers.extend([
                    f'{client_id}_loss',
                    f'{client_id}_accuracy',
                    f'{client_id}_precision',
                    f'{client_id}_recall',
                    f'{client_id}_f1_score'
                ])

            writer.writerow(headers)

        self._log_info(f"CSV log file created: {csv_file_path}")
        return csv_file_path

    def _write_csv_log(self, step: int, mean_metrics: Dict[str, float], client_metrics: Dict[int, Dict[str, float]]):
        """
        写入CSV日志
        
        Args:
            step: 当前轮次
            mean_metrics: 平均指标字典
            client_metrics: 客户端指标字典 {client_id: {loss, accuracy, ...}}
        """
        # 更新所有客户端的指标（只更新有数据的客户端）
        for client_id, metrics in client_metrics.items():
            self.client_metrics_all[client_id] = {
                'loss': metrics.get('loss', 0.0),
                'accuracy': metrics.get('accuracy', 0.0),
                'precision': metrics.get('precision', 0.0),
                'recall': metrics.get('recall', 0.0),
                'f1_score': metrics.get('f1_score', 0.0)
            }

        # 写入CSV文件
        with open(self.csv_file_path, 'a', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)

            # 构建行数据
            row = [
                mean_metrics.get('loss', 0.0),
                mean_metrics.get('accuracy', 0.0),
                mean_metrics.get('precision', 0.0),
                mean_metrics.get('recall', 0.0),
                mean_metrics.get('f1_score', 0.0)
            ]

            # 添加每个客户端的5个指标
            for client_id in range(self.num_nodes):
                client_metric = self.client_metrics_all[client_id]
                row.extend([
                    client_metric['loss'],
                    client_metric['accuracy'],
                    client_metric['precision'],
                    client_metric['recall'],
                    client_metric['f1_score']
                ])

            writer.writerow(row)

        # 刷新文件，确保数据及时写入
        self.csv_file_path.stat()  # 触发文件系统同步

    def _rename_csv_log_completed(self):
        """
        训练完成后重命名CSV文件（添加Done前缀）
        """
        if not self.csv_file_path.exists():
            return

        # 获取原文件名
        old_name = self.csv_file_path.name

        # 检查是否已经包含Done
        if old_name.startswith('Done_'):
            return

        # 构建新文件名（在时间戳后添加Done_）
        parts = old_name.split('_', 1)
        if len(parts) == 2:
            timestamp = parts[0]
            rest = parts[1]
            new_name = f"{timestamp}_Done_{rest}"
        else:
            new_name = f"Done_{old_name}"

        new_path = self.csv_file_path.parent / new_name

        try:
            self.csv_file_path.rename(new_path)
            self._log_info(f"CSV log file renamed to: {new_path}")
        except Exception as e:
            self._log_error(f"Failed to rename CSV log file: {e}")

    def _init_dataset_config(self):
        if self.data_name == "cifar100":
            self.out_dim = 100
            self.input_size = 32
        elif self.data_name == "tiny-imagenet":
            self.out_dim = 200
            self.input_size = 64
        else:
            raise ValueError(
                f"Unsupported dataset: {self.data_name}. Choose from ['cifar100', 'tiny-imagenet']")

    @abstractmethod
    def _init_models(self) -> List[nn.Module]:
        """
        初始化模型集合（子类实现）

        Returns:
            模型列表，通常包含5个不同结构的模型
        """
        pass

    def _init_optimizers(self) -> Dict[str, torch.optim.Optimizer]:
        """初始化优化器字典"""
        if len(self.net_set) == 0:
            return {}

        net = self.net_set[0]
        optimizers = {
            'sgd': torch.optim.SGD(params=net.parameters(), lr=self.lr, momentum=0.9, weight_decay=self.wd),
            'adam': torch.optim.Adam(params=net.parameters(), lr=self.lr)
        }
        return optimizers

    def test_metrics(self, net: nn.Module, testloader: torch.utils.data.DataLoader) -> Dict[str, float]:
        """
        测试模型指标：损失、准确率、精确率、召回率、F1分数

        Args:
            net: 模型
            testloader: 测试数据加载器

        Returns:
            包含所有指标的字典: {
                'loss': 平均损失,
                'accuracy': 准确率,
                'precision': 精确率 (macro average),
                'recall': 召回率 (macro average),
                'f1_score': F1分数 (macro average)
            }
        """
        net.eval()
        total_loss = 0.0
        n_batch = 0

        all_preds = []
        all_labels = []

        with torch.no_grad():
            for img, label in testloader:
                n_batch += 1
                img, label = img.to(self.device), label.to(self.device)
                logits = self._forward(net, img)
                loss = self.criteria(logits, label)
                total_loss += float(loss.item())

                # 获取预测结果
                preds = logits.argmax(1)
                all_preds.append(preds.cpu().numpy())
                all_labels.append(label.cpu().numpy())

        if n_batch == 0:
            return {
                'loss': 0.0,
                'accuracy': 0.0,
                'precision': 0.0,
                'recall': 0.0,
                'f1_score': 0.0
            }

        # 合并所有批次的结果
        all_preds = np.concatenate(all_preds)
        all_labels = np.concatenate(all_labels)

        # 计算平均损失和准确率
        avg_loss = total_loss / n_batch
        accuracy = float((all_preds == all_labels).mean())

        # 计算精确率、召回率、F1分数
        precision = float(precision_score(all_labels, all_preds, average='macro', zero_division=0))
        recall = float(recall_score(all_labels, all_preds, average='macro', zero_division=0))
        f1 = float(f1_score(all_labels, all_preds, average='macro', zero_division=0))

        return {
            'loss': avg_loss,
            'accuracy': accuracy,
            'precision': precision,
            'recall': recall,
            'f1_score': f1
        }

    def _forward(self, net: nn.Module, img: torch.Tensor) -> torch.Tensor:
        """
        模型前向传播（处理不同模型的输出格式）

        Args:
            net: 模型
            img: 输入图像

        Returns:
            模型输出 logits
        """
        output = net(img)
        # 如果模型返回 (logits, representation)，只取 logits
        if isinstance(output, tuple):
            return output[0]
        return output

    @abstractmethod
    def _train_client(
            self,
            step: int,
            client_id: int,
            net: nn.Module,
            train_loader: torch.utils.data.DataLoader
    ) -> None:
        """
        训练单个客户端（子类实现）

        Args:
            step: 当前训练轮次
            client_id: 客户端ID
            net: 客户端模型
            train_loader: 训练数据加载器
        """
        pass

    @abstractmethod
    def _aggregate(self, step: int, selected_clients: List[int], client_models: Dict[int, Dict]) -> Optional[Dict]:
        """
        聚合客户端模型（子类实现）

        Args:
            step: 当前训练轮次
            selected_clients: 被选中的客户端ID列表
            client_models: 客户端模型参数字典 {client_id: model_state_dict}

        Returns:
            聚合后的全局模型参数字典，如果不需要聚合则返回 None
        """
        pass

    def train(self):
        """
        主训练循环
        """
        self._log_info(f"Starting training: {self.algorithm_name} on {self.data_name}")

        # 初始化客户端模型状态（子类可以重写）
        self._init_client_models()

        # 训练循环（使用 tqdm 显示进度）
        pbar = tqdm(
            range(self.num_steps),
            desc=f"[Task-{self.tid}] Training",
            unit="round",
            ncols=100,
            bar_format='{l_bar}{bar}| {n_fmt}/{total_fmt} [{elapsed}<{remaining}, {rate_fmt}]'
        )

        for step in pbar:
            if self.stop_event is not None and self.stop_event.is_set():
                self._log_info("Training stopped by user")
                break
            # 随机选择客户端
            num_selected = int(self.fraction * self.num_nodes)
            selected_clients = random.sample(range(self.num_nodes), num_selected)

            self._log_info(f'#-----------Round: {step}-------------#')
            self._log_info(f'Selected clients: {selected_clients}')

            # 存储本轮训练的指标
            all_client_losses = []
            all_client_accs = []
            client_metrics = {}  # {client_id: {loss, accuracy, ...}}
            client_models = {}  # {client_id: model_state_dict}

            # 训练每个选中的客户端
            for client_id in selected_clients:
                if self.stop_event is not None and self.stop_event.is_set():
                    self._log_info("Training stopped by user")
                    break
                # 获取客户端模型
                net = self.net_set[client_id % len(self.net_set)]
                net = net.to(self.device)

                # 加载客户端模型状态（子类实现）
                self._load_client_model(net, client_id, step)

                # 训练客户端
                train_loader = self.nodes.train_loaders[client_id]
                self._train_client(step, client_id, net, train_loader)

                # 测试客户端模型，获取所有指标
                test_metrics = self.test_metrics(net, self.nodes.test_loaders[client_id])

                # 构建客户端指标（对应 Client 实体）
                metrics = {
                    'client_index': client_id,
                    'loss': test_metrics['loss'],
                    'accuracy': test_metrics['accuracy'],
                    'precision': test_metrics['precision'],
                    'recall': test_metrics['recall'],
                    'f1_score': test_metrics['f1_score'],
                    'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
                }

                all_client_losses.append(test_metrics['loss'])
                all_client_accs.append(test_metrics['accuracy'])
                client_metrics[client_id] = metrics
                self.client_acc[client_id] = test_metrics['accuracy']

                # 保存客户端模型状态
                client_models[client_id] = {k: v.cpu().clone() for k, v in net.state_dict().items()}

                self._log_info(
                    f'Round {step} | Client {client_id} | Loss: {test_metrics["loss"]:.4f} | '
                    f'Acc: {test_metrics["accuracy"]:.4f} | Precision: {test_metrics["precision"]:.4f} | '
                    f'Recall: {test_metrics["recall"]:.4f} | F1: {test_metrics["f1_score"]:.4f}')

                # 客户端训练完成回调
                if self.client_callback:
                    self.client_callback(self.tid, step, client_id, metrics)

            if self.stop_event is not None and self.stop_event.is_set():
                break
            # 计算平均指标（所有选中客户端的平均值）
            mean_loss = round(float(np.mean(all_client_losses)), 4) if all_client_losses else 0.0
            mean_acc = round(float(np.mean(all_client_accs)), 4) if all_client_accs else 0.0

            # 计算平均精确率、召回率、F1分数
            all_precisions = [client_metrics[cid]['precision'] for cid in selected_clients if cid in client_metrics]
            all_recalls = [client_metrics[cid]['recall'] for cid in selected_clients if cid in client_metrics]
            all_f1_scores = [client_metrics[cid]['f1_score'] for cid in selected_clients if cid in client_metrics]

            mean_precision = round(float(np.mean(all_precisions)), 4) if all_precisions else 0.0
            mean_recall = round(float(np.mean(all_recalls)), 4) if all_recalls else 0.0
            mean_f1 = round(float(np.mean(all_f1_scores)), 4) if all_f1_scores else 0.0

            # 聚合模型（如果有聚合逻辑）
            aggregated_model = self._aggregate(step, selected_clients, client_models)
            if aggregated_model:
                self._apply_aggregated_model(aggregated_model, selected_clients)

            # 构建本轮指标（对应 Round 实体）
            step_metrics = {
                'round_num': step,
                'loss': mean_loss,
                'accuracy': mean_acc,
                'precision': mean_precision,
                'recall': mean_recall,
                'f1_score': mean_f1
            }

            self._log_info(
                f'Round: {step} | Mean Loss: {mean_loss} | Mean Acc: {mean_acc} | '
                f'Mean Precision: {mean_precision} | Mean Recall: {mean_recall} | Mean F1: {mean_f1}')

            # 写入CSV日志
            mean_metrics = {
                'loss': mean_loss,
                'accuracy': mean_acc,
                'precision': mean_precision,
                'recall': mean_recall,
                'f1_score': mean_f1
            }
            self._write_csv_log(step, mean_metrics, client_metrics)

            # 更新进度条描述，显示当前轮次的指标
            pbar.set_postfix({
                'Loss': f'{mean_loss:.4f}',
                'Acc': f'{mean_acc:.4f}'
            })

            # 轮次完成回调（在所有客户端训练完成、平均指标计算完成、日志输出完成后调用）
            if self.step_callback:
                self.step_callback(self.tid, step, step_metrics)

        # 关闭进度条
        pbar.close()

        # 训练完成后重命名CSV文件
        self._rename_csv_log_completed()
        self._log_info(f'Training completed: {self.algorithm_name} on {self.data_name}')

    def _init_client_models(self):
        """
        初始化客户端模型状态（子类可以重写）
        默认实现：每个客户端使用对应的模型初始状态
        """
        pass

    def _load_client_model(self, net: nn.Module, client_id: int, step: int):
        """
        加载客户端模型状态（子类可以重写）

        Args:
            net: 模型
            client_id: 客户端ID
            step: 当前轮次
        """
        # 默认实现：在 step 0 时使用初始状态，其他情况由子类实现
        if step == 0:
            net.load_state_dict(self.net_set[client_id % len(self.net_set)].state_dict())

    def _apply_aggregated_model(self, aggregated_model: Dict, selected_clients: List[int]):
        """
        应用聚合后的模型到客户端（子类可以重写）

        注意：此方法只在 _aggregate 返回非 None 时被调用

        Args:
            aggregated_model: 聚合后的模型参数字典
            selected_clients: 被选中的客户端列表
        """
        # 默认实现：子类需要根据具体算法实现如何应用聚合模型
        # 例如：LG-FedAvg 需要将聚合的共享参数与客户端的私有参数合并
        # Standalone 算法不需要聚合，所以不会调用此方法
        pass
