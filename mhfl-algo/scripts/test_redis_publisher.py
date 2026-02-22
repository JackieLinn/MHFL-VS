import sys
from pathlib import Path

# 将 mhfl-algo 根目录加入 path，保证可导入 config、utils
_root = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(_root))

from utils.redis_publisher import RedisPublisher


def main():
    print("Redis 发布工具类集成测试")
    print(
        "请先在另一终端执行: redis-cli SUBSCRIBE task:experiment:round:999 task:experiment:client:999 task:experiment:status:999")
    print()

    pub = RedisPublisher()
    task_id = 999

    # 1. 发布 Round 消息
    ok1 = pub.publish_round(task_id, 1, {
        "loss": 0.52,
        "accuracy": 0.78,
        "precision": 0.76,
        "recall": 0.75,
        "f1_score": 0.755,
    })
    print(f"publish_round(999, 1, metrics): {ok1}")

    # 2. 发布 Client 消息
    ok2 = pub.publish_client(task_id, 1, 0, {
        "loss": 0.48,
        "accuracy": 0.82,
        "precision": 0.80,
        "recall": 0.79,
        "f1_score": 0.795,
    })
    print(f"publish_client(999, 1, 0, metrics): {ok2}")

    # 3. 发布状态消息
    ok3 = pub.publish_status(task_id, "IN_PROGRESS", "训练已启动")
    print(f"publish_status(999, IN_PROGRESS): {ok3}")

    if ok1 and ok2 and ok3:
        print("\n全部发布成功。请在订阅端查看是否收到三条消息。")
    else:
        print("\n存在发布失败，请检查 Redis 连接与配置。")
        sys.exit(1)


if __name__ == "__main__":
    main()
