from types import SimpleNamespace

import utils.resource_checker as rc


def test_check_gpu_should_return_gpu_dict_when_nvml_ok(monkeypatch):
    fake_mem = SimpleNamespace(
        total=8 * 1024**3,
        used=2 * 1024**3,
        free=6 * 1024**3,
    )

    monkeypatch.setattr(rc.pynvml, "nvmlInit", lambda: None)
    monkeypatch.setattr(rc.pynvml, "nvmlDeviceGetHandleByIndex", lambda idx: object())
    monkeypatch.setattr(rc.pynvml, "nvmlDeviceGetMemoryInfo", lambda handle: fake_mem)
    monkeypatch.setattr(rc.pynvml, "nvmlDeviceGetName", lambda handle: b"FakeGPU")
    monkeypatch.setattr(rc.pynvml, "nvmlShutdown", lambda: None)

    result = rc.check_gpu(0)

    assert result == {
        "total": 8.0,
        "used": 2.0,
        "free": 6.0,
        "usage_percent": 25.0,
    }


def test_check_gpu_should_return_none_when_nvml_error(monkeypatch):
    class FakeNvmlError(Exception):
        pass

    monkeypatch.setattr(rc.pynvml, "NVMLError", FakeNvmlError)
    monkeypatch.setattr(rc.pynvml, "nvmlInit", lambda: (_ for _ in ()).throw(FakeNvmlError("boom")))
    monkeypatch.setattr(rc.pynvml, "nvmlShutdown", lambda: None)

    result = rc.check_gpu(0)

    assert result is None


def test_check_cpu_should_return_cpu_info(monkeypatch):
    monkeypatch.setattr(rc.psutil, "cpu_percent", lambda interval=1: 12.3456)
    monkeypatch.setattr(rc.psutil, "cpu_count", lambda logical=False: 4 if not logical else 8)

    result = rc.check_cpu()

    assert result == {"usage_percent": 12.35, "cores": 4, "cores_logical": 8}


def test_check_memory_should_return_memory_info(monkeypatch):
    fake_mem = SimpleNamespace(
        total=16 * 1024**3,
        used=5 * 1024**3,
        available=11 * 1024**3,
        percent=31.234,
    )
    monkeypatch.setattr(rc.psutil, "virtual_memory", lambda: fake_mem)

    result = rc.check_memory()

    assert result == {
        "total": 16.0,
        "used": 5.0,
        "free": 11.0,
        "usage_percent": 31.23,
    }


def test_check_resources_should_aggregate_all_parts(monkeypatch):
    monkeypatch.setattr(rc, "check_cpu", lambda: {"usage_percent": 10.0, "cores": 4, "cores_logical": 8})
    monkeypatch.setattr(rc, "check_memory", lambda: {"total": 16.0, "used": 4.0, "free": 12.0, "usage_percent": 25.0})
    monkeypatch.setattr(rc, "check_gpu", lambda idx=0: {"total": 8.0, "used": 2.0, "free": 6.0, "usage_percent": 25.0})

    result = rc.check_resources(0)

    assert result["cpu"]["usage_percent"] == 10.0
    assert result["memory"]["free"] == 12.0
    assert result["gpu"]["free"] == 6.0

