import json

from utils.redis_publisher import RedisPublisher


class _FakeRedis:
    def __init__(self, should_fail=False):
        self.should_fail = should_fail
        self.calls = []

    def publish(self, channel, message):
        if self.should_fail:
            raise RuntimeError("publish failed")
        self.calls.append((channel, message))
        return 1


def test_publish_round_should_return_true_and_publish_message():
    fake = _FakeRedis()
    publisher = RedisPublisher(redis_client=fake)

    ok = publisher.publish_round(1, 2, {"loss": 0.1, "accuracy": 0.9, "precision": 0.8, "recall": 0.7, "f1_score": 0.75})

    assert ok is True
    assert len(fake.calls) == 1
    channel, payload = fake.calls[0]
    assert channel == "task:experiment:round:1"
    data = json.loads(payload)
    assert data["taskId"] == 1
    assert data["roundNum"] == 2
    assert data["f1Score"] == 0.75


def test_publish_round_should_return_false_when_exception():
    publisher = RedisPublisher(redis_client=_FakeRedis(should_fail=True))
    ok = publisher.publish_round(1, 1, {})
    assert ok is False


def test_publish_client_should_return_true_and_publish_message():
    fake = _FakeRedis()
    publisher = RedisPublisher(redis_client=fake)

    ok = publisher.publish_client(2, 3, 4, {"loss": 0.2, "accuracy": 0.6, "precision": 0.5, "recall": 0.4, "f1_score": 0.45})

    assert ok is True
    assert len(fake.calls) == 1
    channel, payload = fake.calls[0]
    assert channel == "task:experiment:client:2"
    data = json.loads(payload)
    assert data["clientIndex"] == 4
    assert data["f1Score"] == 0.45


def test_publish_client_should_return_false_when_exception():
    publisher = RedisPublisher(redis_client=_FakeRedis(should_fail=True))
    ok = publisher.publish_client(1, 1, 1, {})
    assert ok is False


def test_publish_status_should_return_true_and_publish_message():
    fake = _FakeRedis()
    publisher = RedisPublisher(redis_client=fake)

    ok = publisher.publish_status(3, "SUCCESS", "done")

    assert ok is True
    assert len(fake.calls) == 1
    channel, payload = fake.calls[0]
    assert channel == "task:experiment:status:3"
    data = json.loads(payload)
    assert data["status"] == "SUCCESS"
    assert data["message"] == "done"


def test_publish_status_should_return_false_when_exception():
    publisher = RedisPublisher(redis_client=_FakeRedis(should_fail=True))
    ok = publisher.publish_status(1, "FAILED", "err")
    assert ok is False

