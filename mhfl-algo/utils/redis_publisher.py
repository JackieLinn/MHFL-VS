"""
Redis 发布工具类
用于将训练过程中的 Round 和 Client 数据发布到 Redis Pub/Sub
"""
import json
import logging
from datetime import datetime
from typing import Dict, Any

from config.redis_conn import get_redis_client

logger = logging.getLogger(__name__)


class RedisPublisher:
    """Redis 消息发布器"""

    def __init__(self, redis_client=None):
        """
        初始化 Redis 发布器

        Args:
            redis_client: Redis 客户端实例，如果为 None 则自动获取
        """
        self.redis_client = redis_client or get_redis_client()

    def publish_round(self, task_id: int, round_num: int, metrics: Dict[str, Any]) -> bool:
        """
        发布 Round 消息到 Redis

        Args:
            task_id: 任务 ID
            round_num: 轮次编号
            metrics: 指标字典，包含 loss, accuracy, precision, recall, f1_score

        Returns:
            是否发布成功
        """
        try:
            channel = f"task:experiment:round:{task_id}"
            message = {
                "taskId": task_id,
                "roundNum": round_num,
                "loss": metrics.get("loss"),
                "accuracy": metrics.get("accuracy"),
                "precision": metrics.get("precision"),
                "recall": metrics.get("recall"),
                "f1Score": metrics.get("f1_score"),
                "timestamp": datetime.now().isoformat()
            }
            self.redis_client.publish(channel, json.dumps(message))
            return True
        except Exception as e:
            logger.exception("Failed to publish round message: %s", e)
            return False

    def publish_client(self, task_id: int, round_num: int, client_index: int, metrics: Dict[str, Any]) -> bool:
        """
        发布 Client 消息到 Redis

        Args:
            task_id: 任务 ID
            round_num: 轮次编号
            client_index: 客户端索引
            metrics: 指标字典，包含 loss, accuracy, precision, recall, f1_score

        Returns:
            是否发布成功
        """
        try:
            channel = f"task:experiment:client:{task_id}"
            message = {
                "taskId": task_id,
                "roundNum": round_num,
                "clientIndex": client_index,
                "loss": metrics.get("loss"),
                "accuracy": metrics.get("accuracy"),
                "precision": metrics.get("precision"),
                "recall": metrics.get("recall"),
                "f1Score": metrics.get("f1_score"),
                "timestamp": datetime.now().isoformat()
            }
            self.redis_client.publish(channel, json.dumps(message))
            return True
        except Exception as e:
            logger.exception("Failed to publish client message: %s", e)
            return False

    def publish_status(self, task_id: int, status: str, message: str = "") -> bool:
        """
        发布训练状态消息到 Redis

        Args:
            task_id: 任务 ID
            status: 状态 ("IN_PROGRESS", "SUCCESS", "FAILED")
            message: 状态描述信息

        Returns:
            是否发布成功
        """
        try:
            channel = f"task:experiment:status:{task_id}"
            status_message = {
                "taskId": task_id,
                "status": status,
                "message": message,
                "timestamp": datetime.now().isoformat()
            }
            self.redis_client.publish(channel, json.dumps(status_message))
            return True
        except Exception as e:
            logger.exception("Failed to publish status message: %s", e)
            return False
