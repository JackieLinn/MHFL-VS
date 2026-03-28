from routers import resource


def test_gpu_check_should_return_success_when_gpu_exists(client, monkeypatch):
    monkeypatch.setattr(
        resource,
        "check_gpu_func",
        lambda idx=0: {"total": 8.0, "used": 2.0, "free": 6.0, "usage_percent": 25.0},
    )

    resp = client.get("/api/resource/gpu/check")
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 200
    assert body["data"]["free"] == 6.0


def test_gpu_check_should_return_503_when_gpu_missing(client, monkeypatch):
    monkeypatch.setattr(resource, "check_gpu_func", lambda idx=0: None)

    resp = client.get("/api/resource/gpu/check")
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 503
    assert body["data"] is None


def test_system_check_should_return_cpu_memory_gpu(client, monkeypatch):
    monkeypatch.setattr(resource, "check_cpu", lambda: {"usage_percent": 10.0, "cores": 4, "cores_logical": 8})
    monkeypatch.setattr(resource, "check_memory", lambda: {"total": 16.0, "used": 5.0, "free": 11.0, "usage_percent": 31.25})
    monkeypatch.setattr(resource, "check_gpu_func", lambda idx=0: {"total": 8.0, "used": 2.0, "free": 6.0, "usage_percent": 25.0})

    resp = client.get("/api/resource/system/check")
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 200
    assert body["data"]["cpu"]["cores"] == 4
    assert body["data"]["memory"]["free"] == 11.0
    assert body["data"]["gpu"]["free"] == 6.0


def test_system_check_should_allow_gpu_none(client, monkeypatch):
    monkeypatch.setattr(resource, "check_cpu", lambda: {"usage_percent": 10.0, "cores": 4, "cores_logical": 8})
    monkeypatch.setattr(resource, "check_memory", lambda: {"total": 16.0, "used": 5.0, "free": 11.0, "usage_percent": 31.25})
    monkeypatch.setattr(resource, "check_gpu_func", lambda idx=0: None)

    resp = client.get("/api/resource/system/check")
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 200
    assert body["data"]["gpu"] is None

