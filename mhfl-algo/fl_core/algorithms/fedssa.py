import copy
from collections import defaultdict
from typing import Dict, List, Optional

import numpy as np
import torch
import torch.nn as nn
import torch.utils.data

from fl_core.trainer import BaseTrainer
from fl_core.models.cnn_fedssa import (
    CNN_1_SSA, CNN_2_SSA, CNN_3_SSA, CNN_4_SSA, CNN_5_SSA
)


class FedSSATrainer(BaseTrainer):
    """FedSSA 训练器：Federated Learning with Semantic-aware Aggregation"""

    def __init__(self, *args, total_classes: int = 100, inner_lr: float = 5e-3,
                 inner_wd: float = 5e-5, decay_rounds: int = 2, miu_0: float = 0.5, **kwargs):
        """
        初始化 FedSSA 训练器
        
        Args:
            *args: 传递给基类的参数
            total_classes: 数据集总类别数
            inner_lr: 内部学习率（用于客户端训练）
            inner_wd: 内部权重衰减（用于客户端训练）
            decay_rounds: 衰减轮次数
            miu_0: 初始混合系数
            **kwargs: 传递给基类的其他参数
        """
        self.total_classes = total_classes
        self.inner_lr = inner_lr
        self.inner_wd = inner_wd
        self.decay_rounds = decay_rounds
        self.miu_0 = miu_0
        super().__init__(*args, **kwargs)

    def _init_models(self) -> List[nn.Module]:
        """初始化 FedSSA 模型集合"""
        net_set = [
            CNN_1_SSA(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_2_SSA(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_3_SSA(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_4_SSA(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_5_SSA(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
        ]
        return net_set

    def _init_client_models(self):
        """初始化客户端模型状态和参数分离"""
        # 获取初始模型参数，用于分离共享参数和私有参数
        init_params = copy.deepcopy(self.net_set[0].state_dict())

        # 分离共享参数（header）和私有参数（extractor）
        # 共享参数：最后两层（fc3的 weight 和 bias，共2个 key）
        # 私有参数：其他层（conv1, conv2, fc1, fc2 的 weight 和 bias）
        self.share_keys = []
        self.private_keys = []

        count = 0
        for k, v in init_params.items():
            if count < len(init_params.keys()) - 2:
                self.private_keys.append(k)
                count += 1
            else:
                self.share_keys.append(k)

        # 初始化全局类别 header（每个类别一个 header）
        self.global_class_headers = defaultdict()
        header = dict([(k, init_params[k]) for k in self.share_keys])
        for key, paras in header.items():
            self.global_class_headers[key] = defaultdict()
            for cls in range(self.total_classes):
                self.global_class_headers[key][cls] = header[key][cls].clone()

        # 初始化每个客户端的模型参数
        self.client_models = defaultdict(dict)
        for i in range(self.num_nodes):
            self.client_models[i] = copy.deepcopy(self.net_set[i % 5].state_dict())

        # 初始化每个客户端的本地 header（按类别存储）
        self.local_headers = defaultdict()
        for i in range(self.num_nodes):
            self.local_headers[i] = defaultdict()
            for key in self.share_keys:
                self.local_headers[i][key] = defaultdict()

        # 获取每个客户端拥有的类别
        self.owned_all_classes = defaultdict(list)
        for i in range(self.num_nodes):
            owned_classes = []
            for batch in self.nodes.train_loaders[i]:
                img, label = tuple(t.to(self.device) for t in batch)
                classes = label.unique().detach().cpu().numpy()
                for cls in classes:
                    if cls not in owned_classes:
                        owned_classes.append(int(cls))
            self.owned_all_classes[i] = owned_classes

            # 初始化客户端拥有的类别的 header
            for key in self.share_keys:
                for cls in self.owned_all_classes[i]:
                    self.local_headers[i][key][cls] = header[key][cls].clone()

    def _load_client_model(self, net: nn.Module, client_id: int, step: int):
        """加载客户端模型状态，混合本地和全局 header"""
        # 获取客户端模型参数
        model_dict = copy.deepcopy(self.client_models[client_id])

        # 混合本地和全局 header（带衰减）
        for key in self.share_keys:
            for cls in self.owned_all_classes[client_id]:
                if step <= self.decay_rounds:
                    alpha = self.miu_0 * np.cos(step * np.pi / (self.decay_rounds * 2))
                else:
                    alpha = 0

                # 混合本地 header 和全局 header
                # header[key] 是 [out_dim, ...] 形状，header[key][cls] 是第 cls 个类别的参数
                model_dict[key][cls] = (
                        self.client_models[client_id][key][cls] * alpha +
                        self.global_class_headers[key][cls]
                )

        # 加载混合后的模型参数
        net.load_state_dict(model_dict)

    def _train_client(
            self,
            step: int,
            client_id: int,
            net: nn.Module,
            train_loader: torch.utils.data.DataLoader
    ) -> None:
        """
        训练单个客户端
        
        Args:
            step: 当前训练轮次
            client_id: 客户端ID
            net: 客户端模型
            train_loader: 训练数据加载器
        """
        inner_optim = torch.optim.SGD(
            net.parameters(),
            lr=self.inner_lr,
            momentum=0.9,
            weight_decay=self.inner_wd
        )

        # 本地训练
        net.train()
        for epoch in range(self.epochs):
            for batch in train_loader:
                img, label = tuple(t.to(self.device) for t in batch)

                inner_optim.zero_grad()
                logits, _ = net(img)  # FedSSA 模型返回 (logits, representation)
                loss = self.criteria(logits, label)
                loss.backward()
                torch.nn.utils.clip_grad_norm_(net.parameters(), 50)
                inner_optim.step()

        # 更新客户端模型参数
        local_model_dict = copy.deepcopy(net.state_dict())
        self.client_models[client_id] = local_model_dict

        # 提取并更新本地 header（只更新客户端拥有的类别）
        local_header = dict([(k, local_model_dict[k]) for k in self.share_keys])
        for key in self.share_keys:
            for cls in self.owned_all_classes[client_id]:
                self.local_headers[client_id][key][cls] = local_header[key][cls].clone()

    def _aggregate(
            self,
            step: int,
            selected_clients: List[int],
            client_models: Dict[int, Dict]
    ) -> Optional[Dict]:
        """
        聚合客户端 header（按类别聚合，语义感知聚合）
        
        Args:
            step: 当前训练轮次
            selected_clients: 被选中的客户端ID列表
            client_models: 客户端模型参数字典（这里不使用，使用 self.local_headers）
            
        Returns:
            聚合后的全局类别 header 字典
        """
        # 对每个类别，聚合所有拥有该类别的客户端的 header
        for key in self.share_keys:
            for cls in range(self.total_classes):
                # 收集所有拥有该类别的客户端的 header（遍历所有客户端，不仅仅是选中的）
                cls_collect = []
                for client_id, _ in self.local_headers.items():
                    if cls in self.local_headers[client_id][key]:
                        cls_collect.append(self.local_headers[client_id][key][cls])

                # 如果有客户端拥有该类别，则进行平均聚合
                if cls_collect:
                    new_paras = self.global_class_headers[key][cls] * 0
                    for paras in cls_collect:
                        new_paras += paras
                    self.global_class_headers[key][cls] = new_paras / len(cls_collect)

        # 返回全局类别 header（用于 _apply_aggregated_model）
        return self.global_class_headers

    def _apply_aggregated_model(self, aggregated_model: Dict, selected_clients: List[int]):
        """
        应用聚合后的全局类别 header
        
        Args:
            aggregated_model: 聚合后的全局类别 header 字典
            selected_clients: 被选中的客户端列表
        """
        # 全局类别 header 已经在 _aggregate 中更新了
        # 这里不需要额外操作，因为 _load_client_model 会使用 self.global_class_headers
        pass
