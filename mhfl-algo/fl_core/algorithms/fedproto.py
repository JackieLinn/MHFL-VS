import copy
from collections import defaultdict
from typing import Dict, List, Optional

import torch
import torch.nn as nn
import torch.utils.data

from fl_core.trainer import BaseTrainer
from fl_core.models.cnn_fedproto import (
    CNN_1_Proto, CNN_2_Proto, CNN_3_Proto, CNN_4_Proto, CNN_5_Proto
)


class FedProtoTrainer(BaseTrainer):
    """FedProto 训练器：Federated Learning with Prototype-based Aggregation"""

    def __init__(self, *args, **kwargs):
        """
        初始化 FedProto 训练器
        
        Args:
            *args: 传递给基类的参数
            **kwargs: 传递给基类的其他参数
        """
        super().__init__(*args, **kwargs)

    def _init_models(self) -> List[nn.Module]:
        """初始化 FedProto 模型集合"""
        net_set = [
            CNN_1_Proto(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_2_Proto(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_3_Proto(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_4_Proto(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_5_Proto(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
        ]
        return net_set

    def _init_client_models(self):
        """初始化客户端模型状态和原型相关数据结构"""
        # 初始化每个客户端的模型参数
        self.client_models = defaultdict(dict)
        for i in range(self.num_nodes):
            self.client_models[i] = copy.deepcopy(self.net_set[i % 5].state_dict())

        # 初始化全局原型（按类别存储）
        self.global_proto = defaultdict()

        # 初始化每个客户端的数据分布统计（用于聚合权重计算）
        self.data_distributions = defaultdict(lambda: defaultdict(int))

        # 初始化跨类别分布统计（每个类别在所有客户端中的总样本数）
        self.distribution_cross_class = defaultdict(int)

    def _load_client_model(self, net: nn.Module, client_id: int, step: int):
        """加载客户端模型状态"""
        net.load_state_dict(self.client_models[client_id])

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
        optimizer = torch.optim.SGD(
            params=net.parameters(),
            lr=self.lr,
            momentum=0.9,
            weight_decay=self.wd
        )

        # 本地训练
        net.train()
        for epoch in range(self.epochs):
            for batch in train_loader:
                img, label = tuple(t.to(self.device) for t in batch)

                optimizer.zero_grad()

                # FedProto 模型返回 (pred, rep)
                pred, rep = net(img)

                # 硬损失（分类损失）
                hard_loss = self.criteria(pred, label)

                # 统计数据分布（只在第一个 epoch 的第一个 batch 统计）
                if epoch == 0:
                    for k in range(len(label)):
                        self.data_distributions[client_id][label[k].cpu().item()] += 1

                # 全局蒸馏损失（只在 step > 0 时计算）
                if step > 0:
                    owned_classes = label.unique().detach().cpu().numpy()
                    soft_loss = 0.0
                    soft_loss_count = 0

                    for cls in owned_classes:
                        if cls in self.global_proto.keys():
                            # 过滤出当前类别对应的 representation
                            filted_reps = [
                                rep[i] for i in range(len(label))
                                if label[i].cpu().item() == cls
                            ]

                            # 计算每个 representation 与全局原型的 L2 距离
                            for f in filted_reps:
                                soft_loss = soft_loss + torch.norm(f - self.global_proto[cls], p=2)
                                soft_loss_count += 1

                    if soft_loss_count > 0:
                        soft_loss = soft_loss / soft_loss_count
                        loss = hard_loss + 0.5 * soft_loss
                    else:
                        loss = hard_loss
                else:
                    loss = hard_loss

                loss.backward()
                optimizer.step()

        # 更新客户端模型参数
        self.client_models[client_id] = copy.deepcopy(net.state_dict())

        # 训练完成后，收集本地原型（存储到实例变量中，供 _aggregate 使用）
        # 初始化当前轮次的原型字典（如果不存在）
        if not hasattr(self, 'current_step_prototypes'):
            self.current_step_prototypes = defaultdict(dict)

        # 收集该客户端的本地原型
        self.current_step_prototypes[client_id] = self._collect_local_prototypes(
            client_id, net, train_loader
        )

    def _collect_local_prototypes(self, client_id: int, net: nn.Module, train_loader: torch.utils.data.DataLoader) -> \
            Dict[int, torch.Tensor]:
        """
        收集客户端的本地原型（按类别计算 representation 的平均值）
        
        Args:
            client_id: 客户端ID
            net: 客户端模型
            train_loader: 训练数据加载器
            
        Returns:
            按类别存储的原型字典 {class_id: prototype_tensor}
        """
        net.eval()
        proto_mean = defaultdict(list)

        with torch.no_grad():
            for batch in train_loader:
                img, label = tuple(t.to(self.device) for t in batch)

                # 获取 representation
                _, rep = net(img)

                owned_classes = label.unique().detach().cpu().numpy()

                for cls in owned_classes:
                    # 过滤出当前类别对应的 representation
                    filted_reps = [
                        rep[i] for i in range(len(label))
                        if label[i].cpu().item() == cls
                    ]

                    if len(filted_reps) > 0:
                        # 计算当前 batch 中该类别的平均 representation
                        sum_filted_reps = filted_reps[0].detach()
                        for f in range(1, len(filted_reps)):
                            sum_filted_reps = sum_filted_reps + filted_reps[f].detach()

                        mean_filted_reps = sum_filted_reps / len(filted_reps)
                        proto_mean[cls].append(mean_filted_reps)

        # 对每个类别，计算所有 batch 的平均值
        client_prototypes = {}
        for cls, protos in proto_mean.items():
            if len(protos) > 0:
                sum_proto = protos[0]
                for m in range(1, len(protos)):
                    sum_proto = sum_proto + protos[m]
                mean_proto = sum_proto / len(protos)
                client_prototypes[cls] = mean_proto

        return client_prototypes

    def _aggregate(
            self,
            step: int,
            selected_clients: List[int],
            client_models: Dict[int, Dict]
    ) -> Optional[Dict]:
        """
        聚合客户端原型（按类别加权平均）
        
        Args:
            step: 当前训练轮次
            selected_clients: 被选中的客户端ID列表
            client_models: 客户端模型参数字典（这里不使用，使用训练后收集的原型）
            
        Returns:
            聚合后的全局原型字典
        """
        # 获取当前轮次收集的原型（由 _train_client 方法收集并存储）
        if not hasattr(self, 'current_step_prototypes'):
            return None

        protos_mean = self.current_step_prototypes

        # 原版代码逻辑（严格遵循）：
        # 1. 在 step == 0 时，计算跨类别分布统计
        #    - classes = list(Global_Proto.keys())，第一次 Global_Proto 是空的，所以 classes 是空的
        #    - 因此 distribution_cross_class 不会被填充（保留原版的 bug）
        # 2. 从 Global_Proto.keys() 获取类别列表（第一次是空的，所以第一次不会聚合）
        # 3. 对每个类别进行加权平均聚合

        # 在 step == 0 时，计算跨类别分布统计（严格按照原版逻辑）
        if step == 0:
            # 原版代码：classes = list(Global_Proto.keys())
            # 第一次 Global_Proto 是空的，所以 classes 是空列表
            classes = list(self.global_proto.keys())

            # 原版代码：distribution_cross_class = defaultdict()
            # 只在 step == 0 时计算，后续轮次不复用（原版代码的写法）
            self.distribution_cross_class = defaultdict(int)
            for cls in classes:
                self.distribution_cross_class[cls] = 0
                for client_id, disbs in self.data_distributions.items():
                    self.distribution_cross_class[cls] += disbs[cls]

        # 原版代码：for cls in classes:（classes 从 Global_Proto.keys() 获取）
        # 第一次 classes 是空的，所以不会聚合（保留原版的逻辑）
        classes = list(self.global_proto.keys())

        # 对每个类别进行加权平均聚合（严格按照原版逻辑）
        # 原版代码逻辑：
        # 1. 遍历 classes（从 Global_Proto.keys() 获取，第一次是空的）
        # 2. 收集所有选中客户端中拥有该类别的原型（原版遍历 Protos_Mean.items()，即所有选中的客户端）
        # 3. 使用权重 (Data_Distirbutions[id][cls]/distribution_cross_class[cls]) 加权
        # 4. 对所有加权原型求和，然后除以参与聚合的客户端数量（保留原版的"二次除法"）
        for cls in classes:
            cls_collect = []
            # 原版代码：for id, protos in Protos_Mean.items()
            # Protos_Mean 只包含本轮选中的客户端
            for client_id, protos in protos_mean.items():
                if cls in protos.keys():
                    # 原版代码：cls_collect.append((Data_Distirbutions[id][cls]/distribution_cross_class[cls]) * protos[cls])
                    if cls in self.distribution_cross_class and self.distribution_cross_class[cls] > 0:
                        weighted_proto = (self.data_distributions[client_id][cls] / self.distribution_cross_class[
                            cls]) * protos[cls]
                        cls_collect.append(weighted_proto)

            # 原版代码：if cls_collect != []
            if cls_collect:
                # 原版代码：对所有加权原型求和
                cls_sum = cls_collect[0]
                for i in range(1, len(cls_collect)):
                    cls_sum = cls_sum + cls_collect[i]
                # 原版代码：Global_Proto[cls] = cls_sum/len(cls_collect)
                # 保留原版的"二次除法"操作
                self.global_proto[cls] = cls_sum / len(cls_collect)

        # 清空当前轮次的原型，准备下一轮
        self.current_step_prototypes = defaultdict(dict)

        return self.global_proto

    def _apply_aggregated_model(self, aggregated_model: Dict, selected_clients: List[int]):
        """
        应用聚合后的全局原型
        
        Args:
            aggregated_model: 聚合后的全局原型字典
            selected_clients: 被选中的客户端列表
        """
        # 全局原型已经在 _aggregate 中更新了
        # 这里不需要额外操作，因为训练时会直接使用 self.global_proto
        pass
