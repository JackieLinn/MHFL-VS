import copy
from collections import OrderedDict, defaultdict
from typing import Dict, List, Optional

import torch
import torch.nn as nn
import torch.utils.data

from fl_core.trainer import BaseTrainer
from fl_core.models.cnn_lgfedavg import (
    CNN_1_LG, CNN_2_LG, CNN_3_LG, CNN_4_LG, CNN_5_LG
)


class LGFedAvgTrainer(BaseTrainer):
    """LG-FedAvg 训练器：Local-Global Federated Averaging"""

    def _init_models(self) -> List[nn.Module]:
        """初始化 LG-FedAvg 模型集合"""
        net_set = [
            CNN_1_LG(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_2_LG(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_3_LG(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_4_LG(
                n_kernels=self.n_kernels,
                out_dim=self.out_dim,
                input_size=self.input_size
            ).to(self.device),
            CNN_5_LG(
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

        # 分离共享参数和私有参数
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

        # 初始化每个客户端的私有参数
        self.client_private_params = defaultdict(dict)
        for i in range(self.num_nodes):
            self.client_private_params[i] = dict([
                (k, self.net_set[i % 5].state_dict()[k])
                for k in self.private_keys
            ])

        # 全局共享参数（初始为 None，第一轮后更新）
        self.global_share_params = None

        # 存储每轮训练后的共享参数（用于聚合）
        self.current_step_share_params = {}

        # 计算聚合权重（基于客户端样本数量）
        train_sample_count = self.nodes.train_sample_count
        eval_sample_count = self.nodes.eval_sample_count
        test_sample_count = self.nodes.test_sample_count

        self.client_sample_count = [
            train_sample_count[i] + eval_sample_count[i] + test_sample_count[i]
            for i in range(len(train_sample_count))
        ]

    def _load_client_model(self, net: nn.Module, client_id: int, step: int):
        """加载客户端模型状态"""
        if step == 0:
            # 第一轮：使用初始模型状态
            net.load_state_dict(self.net_set[client_id % 5].state_dict())
        else:
            # 后续轮次：合并私有参数和全局共享参数
            if self.global_share_params is not None:
                net_params = dict(self.client_private_params[client_id], **self.global_share_params)
                net.load_state_dict(net_params)

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
            lr=self.lr,
            momentum=0.9,
            weight_decay=self.wd
        )

        # 内部更新 -> 获得 theta_delta
        net.train()

        for epoch in range(self.epochs):
            for batch in train_loader:
                inner_optim.zero_grad()

                img, label = tuple(t.to(self.device) for t in batch)
                logits = net(img)
                loss = self.criteria(logits, label)
                loss.backward()
                torch.nn.utils.clip_grad_norm_(net.parameters(), 50)
                inner_optim.step()

        # 分离并保存客户端的共享参数和私有参数
        local_model_dict = copy.deepcopy(net.state_dict())
        share_dict = dict([(k, local_model_dict[k]) for k in self.share_keys])
        private_dict = dict([(k, local_model_dict[k]) for k in self.private_keys])

        # 更新客户端的私有参数
        self.client_private_params[client_id] = private_dict

        # 保存共享参数，用于本轮聚合
        # 共享参数是最后两层（fc3的weight和bias，共2个key）
        self.current_step_share_params[client_id] = share_dict

    def _aggregate(
            self,
            step: int,
            selected_clients: List[int],
            client_models: Dict[int, Dict]
    ) -> Optional[Dict]:
        """
        聚合客户端模型的共享参数
        
        Args:
            step: 当前训练轮次
            selected_clients: 被选中的客户端ID列表
            client_models: 客户端模型参数字典（这里不使用，使用 self.current_step_share_params）
            
        Returns:
            聚合后的全局共享参数字典
        """
        # 计算聚合权重（基于客户端样本数量）
        selected_sample_count = OrderedDict()
        for client_id in selected_clients:
            selected_sample_count[client_id] = self.client_sample_count[client_id]

        total_samples = sum(selected_sample_count.values())
        client_agg_weights = OrderedDict()
        for client_id in selected_clients:
            client_agg_weights[client_id] = selected_sample_count[client_id] / total_samples

        # 使用训练时保存的共享参数
        share_params_dict = self.current_step_share_params

        # 聚合共享参数
        global_share_params = OrderedDict()
        for key in self.share_keys:
            key_sum = 0
            for client_id in selected_clients:
                if client_id in share_params_dict:
                    key_sum += client_agg_weights[client_id] * share_params_dict[client_id][key]
            global_share_params[key] = key_sum

        # 清空本轮共享参数，准备下一轮
        self.current_step_share_params = {}

        return global_share_params

    def _apply_aggregated_model(self, aggregated_model: Dict, selected_clients: List[int]):
        """
        应用聚合后的模型到客户端
        
        Args:
            aggregated_model: 聚合后的全局共享参数字典
            selected_clients: 被选中的客户端列表
        """
        # 更新全局共享参数
        self.global_share_params = aggregated_model
        # 注意：客户端的私有参数已经在 _train_client 中更新了
        # 这里只需要保存全局共享参数，下次 _load_client_model 时会自动合并
