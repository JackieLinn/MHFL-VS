from routers import health


def test_health_check_should_return_healthy_when_redis_and_gpu_ok(client, monkeypatch):
    monkeypatch.setattr(health, "_check_redis_connection", lambda: (True, ""))
    monkeypatch.setattr(health, "_check_gpu_available", lambda: (True, "GPU OK"))

    resp = client.get("/api/health")
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 200
    assert body["data"]["status"] == "healthy"
    assert body["message"]


def test_health_check_should_return_healthy_with_gpu_warning_when_redis_ok(client, monkeypatch):
    monkeypatch.setattr(health, "_check_redis_connection", lambda: (True, ""))
    monkeypatch.setattr(health, "_check_gpu_available", lambda: (False, "GPU not available"))

    resp = client.get("/api/health")
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 200
    assert body["data"]["status"] == "healthy"
    assert "GPU" in body["message"]


def test_health_check_should_return_unhealthy_when_redis_down(client, monkeypatch):
    monkeypatch.setattr(health, "_check_redis_connection", lambda: (False, "Redis down"))

    resp = client.get("/api/health")
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 503
    assert body["data"]["status"] == "unhealthy"

