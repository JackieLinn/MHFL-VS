import copy
from collections import defaultdict
from typing import Dict, List, Optional

import torch
import torch.nn as nn
import torch.utils.data

from fl_core.trainer import BaseTrainer
from fl_core.models.cnn_standalone import (
    CNN_1_Standalone, CNN_2_Standalone, CNN_3_Standalone,
    CNN_4_Standalone, CNN_5_Standalone
)


class StandaloneTrainer(BaseTrainer):
    """Standalone 训练器：本地训练，无联邦聚合"""

    def _init_models(self) -> List[nn.Module]:
        """初始化 Standalone 模型集合"""
        net_set = [
            CNN_1_Standalone(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_2_Standalone(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_3_Standalone(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_4_Standalone(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_5_Standalone(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
        ]
        return net_set

    def _init_client_models(self):
        """初始化客户端模型状态"""
        self.client_models = defaultdict(dict)
        for i in range(self.num_nodes):
            self.client_models[i] = copy.deepcopy(self.net_set[i % 5].state_dict())

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
        # 创建优化器
        optimizer = torch.optim.SGD(
            params=net.parameters(),
            lr=self.lr,
            momentum=0.9,
            weight_decay=self.wd
        )

        # 本地训练（standalone，无联邦）
        net.train()
        total_loss = 0.0
        n_batches = 0

        for epoch in range(self.epochs):
            for batch in train_loader:
                img, label = tuple(t.to(self.device) for t in batch)

                optimizer.zero_grad()
                logits = net(img)
                loss = self.criteria(logits, label)
                loss.backward()
                torch.nn.utils.clip_grad_norm_(net.parameters(), 50)
                optimizer.step()

                total_loss += float(loss.item())
                n_batches += 1

        # 更新客户端模型状态
        self.client_models[client_id] = copy.deepcopy(net.state_dict())

    def _aggregate(
            self,
            step: int,
            selected_clients: List[int],
            client_models: Dict[int, Dict]
    ) -> Optional[Dict]:
        """
        聚合客户端模型
        
        Standalone 算法不需要聚合，直接返回 None
        """
        return None
