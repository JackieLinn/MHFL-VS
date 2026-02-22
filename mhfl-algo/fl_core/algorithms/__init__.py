from typing import Optional, Type, TYPE_CHECKING

from fl_core.algorithms.standalone import StandaloneTrainer
from fl_core.algorithms.lg_fedavg import LGFedAvgTrainer
from fl_core.algorithms.fedral import FedRALTrainer
from fl_core.algorithms.fedssa import FedSSATrainer
from fl_core.algorithms.fedproto import FedProtoTrainer

if TYPE_CHECKING:
    from fl_core.trainer import BaseTrainer

# 数据库/前端传入的算法名称与训练器类的映射（与数据库 algorithm 表命名一致）
ALGORITHM_NAME_TO_TRAINER = {
    "Standalone": StandaloneTrainer,
    "LG-FedAvg": LGFedAvgTrainer,
    "FedSSA": FedSSATrainer,
    "FedProto": FedProtoTrainer,
    "FedRAL": FedRALTrainer,
}

__all__ = [
    'StandaloneTrainer', 'LGFedAvgTrainer', 'FedRALTrainer', 'FedSSATrainer', 'FedProtoTrainer',
    'get_trainer_class', 'ALGORITHM_NAME_TO_TRAINER',
]


def get_trainer_class(algorithm_name: str) -> Optional["Type[BaseTrainer]"]:
    """
    根据算法名称返回对应的训练器类。
    算法名称与数据库中一致：Standalone、LG-FedAvg、FedSSA、FedProto、FedRAL。

    Args:
        algorithm_name: 算法名称（与数据库 algorithm 表命名一致）

    Returns:
        训练器类，未匹配时返回 None
    """
    return ALGORITHM_NAME_TO_TRAINER.get(algorithm_name)
