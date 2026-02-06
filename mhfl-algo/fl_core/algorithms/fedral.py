import copy
from collections import defaultdict
from typing import Dict, List, Optional

import torch
import torch.nn as nn
import torch.utils.data

from fl_core.trainer import BaseTrainer
from fl_core.models.cnn_fedral import (
    CNN_1_OFT, CNN_2_OFT, CNN_3_OFT, CNN_4_OFT, CNN_5_OFT
)


class FedRALTrainer(BaseTrainer):
    """FedRAL 训练器：Federated Learning with Orthogonal Fine-Tuning"""

    def __init__(self, *args, R_dim: int = 500, block_num: int = 500, **kwargs):
        """
        初始化 FedRAL 训练器
        
        Args:
            *args: 传递给基类的参数
            R_dim: OFT 表示维度
            block_num: OFT 压缩的块数量
            **kwargs: 传递给基类的其他参数
        """
        self.R_dim = R_dim
        self.block_num = block_num
        super().__init__(*args, **kwargs)

    def _init_models(self) -> List[nn.Module]:
        """初始化 FedRAL 模型集合"""
        net_set = [
            CNN_1_OFT(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                R_dim=self.R_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_2_OFT(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                R_dim=self.R_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_3_OFT(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                R_dim=self.R_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_4_OFT(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                R_dim=self.R_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_5_OFT(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                R_dim=self.R_dim,
                input_size=self.input_size
            ).to(self.device),
        ]
        return net_set

    def _init_client_models(self):
        """初始化客户端模型状态和 OFT 模块"""
        # 初始化每个客户端的模型参数
        self.client_models = defaultdict(dict)
        for i in range(self.num_nodes):
            self.client_models[i] = copy.deepcopy(self.net_set[i % 5].state_dict())

        # 初始化全局 OFT（使用第一个模型的 OFT 作为初始值）
        self.global_oft = copy.deepcopy(self.net_set[0].oft.state_dict())

        # 初始化每个客户端的本地 OFT
        self.local_ofts = defaultdict(dict)
        for i in range(self.num_nodes):
            self.local_ofts[i] = copy.deepcopy(self.net_set[0].oft.state_dict())

        # 初始化本地 mask（用于 OFT 压缩）
        self.local_mask = defaultdict()
        for i in range(self.num_nodes):
            self.local_mask[i] = torch.zeros_like(self.net_set[0].oft.R.data)

    def _load_client_model(self, net: nn.Module, client_id: int, step: int):
        """加载客户端模型状态和全局 OFT"""
        # 加载客户端模型参数
        net.load_state_dict(self.client_models[client_id])
        # 加载全局 OFT
        net.oft.load_state_dict(self.global_oft)

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
                logits = net(img)
                loss = self.criteria(logits, label)
                loss.backward()
                torch.nn.utils.clip_grad_norm_(net.parameters(), 50)
                optimizer.step()

        # 更新客户端模型参数
        self.client_models[client_id] = copy.deepcopy(net.state_dict())
        # 更新本地 OFT
        self.local_ofts[client_id] = copy.deepcopy(net.oft.state_dict())

        # OFT 压缩（如果 block_num > 1）
        if self.block_num > 1:
            pure_oft = copy.deepcopy(net.oft.R.data)
            masked_pure_oft = torch.zeros_like(pure_oft)
            mask = torch.zeros_like(pure_oft)

            block_size = int(self.R_dim / self.block_num)
            for i in range(self.block_num):
                start = i * block_size
                end = (i + 1) * block_size
                masked_pure_oft[start:end, start:end] = pure_oft[start:end, start:end]
                mask[start:end, start:end] = 1

            # 更新压缩后的 OFT
            net.oft.R.data = masked_pure_oft
            self.local_ofts[client_id] = copy.deepcopy(net.oft.state_dict())
            self.local_mask[client_id] = mask

    def _aggregate(
            self,
            step: int,
            selected_clients: List[int],
            client_models: Dict[int, Dict]
    ) -> Optional[Dict]:
        """
        聚合客户端 OFT 模块
        
        Args:
            step: 当前训练轮次
            selected_clients: 被选中的客户端ID列表
            client_models: 客户端模型参数字典（这里不使用，使用 self.local_ofts）
            
        Returns:
            聚合后的全局 OFT 参数字典
        """
        # 获取 OFT 的权重键
        weight_keys = list(self.net_set[0].oft.state_dict().keys())

        # 聚合 OFT：对所有选中客户端的 OFT 进行平均聚合
        global_oft = {}
        for key in weight_keys:
            key_sum = 0
            for client_id in selected_clients:
                if client_id in self.local_ofts:
                    key_sum += self.local_ofts[client_id][key] / len(selected_clients)
            global_oft[key] = key_sum

        return global_oft

    def _apply_aggregated_model(self, aggregated_model: Dict, selected_clients: List[int]):
        """
        应用聚合后的全局 OFT 到所有客户端
        
        Args:
            aggregated_model: 聚合后的全局 OFT 参数字典
            selected_clients: 被选中的客户端列表
        """
        # 更新全局 OFT
        self.global_oft = aggregated_model
