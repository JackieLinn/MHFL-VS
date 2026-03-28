from routers import health


class _FakeRedisClient:
    def __init__(self, should_fail=False):
        self.should_fail = should_fail

    def ping(self):
        if self.should_fail:
            raise RuntimeError("ping failed")


def test_check_redis_connection_should_return_true_when_ping_ok(monkeypatch):
    monkeypatch.setattr(health, "get_redis_client", lambda: _FakeRedisClient(should_fail=False))

    ok, msg = health._check_redis_connection()

    assert ok is True
    assert msg == ""


def test_check_redis_connection_should_return_false_when_ping_fail(monkeypatch):
    monkeypatch.setattr(health, "get_redis_client", lambda: _FakeRedisClient(should_fail=True))

    ok, msg = health._check_redis_connection()

    assert ok is False
    assert "Redis" in msg


def test_check_gpu_available_should_return_true_when_gpu_present(monkeypatch):
    monkeypatch.setattr(health, "check_gpu", lambda idx=0: {"free": 1.0})

    ok, msg = health._check_gpu_available()

    assert ok is True
    assert "GPU" in msg


def test_check_gpu_available_should_return_false_when_gpu_absent(monkeypatch):
    monkeypatch.setattr(health, "check_gpu", lambda idx=0: None)

    ok, msg = health._check_gpu_available()

    assert ok is False
    assert "GPU" in msg


def test_check_gpu_available_should_return_false_when_exception(monkeypatch):
    monkeypatch.setattr(health, "check_gpu", lambda idx=0: (_ for _ in ()).throw(RuntimeError("gpu error")))

    ok, msg = health._check_gpu_available()

    assert ok is False
    assert "gpu error" in msg

