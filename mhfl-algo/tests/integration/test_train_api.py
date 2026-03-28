from types import SimpleNamespace

from routers import train


class _FakeProcess:
    def __init__(self, target=None, args=(), daemon=False):
        self.target = target
        self.args = args
        self.daemon = daemon
        self.started = False

    def start(self):
        self.started = True

    def join(self):
        return None


class _FakeThread:
    def __init__(self, target=None, args=(), daemon=False):
        self.target = target
        self.args = args
        self.daemon = daemon
        self.started = False

    def start(self):
        self.started = True


def _train_payload(task_id=1, data_name="CIFAR100"):
    return {
        "task_id": task_id,
        "data_name": data_name,
        "algorithm_name": "Standalone",
        "num_nodes": 100,
        "fraction": 0.1,
        "classes_per_node": 10,
        "low_prob": 0.4,
        "num_steps": 10,
        "epochs": 2,
    }


def test_start_training_should_return_400_when_task_already_running(client):
    with train.running_trainers_lock:
        train.running_trainers[1] = {"process": object(), "stop_event": SimpleNamespace(set=lambda: None)}

    resp = client.post("/api/train/start", json=_train_payload(task_id=1))
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 400


def test_start_training_should_return_503_when_gpu_unavailable(client, monkeypatch):
    monkeypatch.setattr(train, "check_gpu", lambda idx=0: None)

    resp = client.post("/api/train/start", json=_train_payload(task_id=2))
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 503


def test_start_training_should_return_400_for_unsupported_dataset(client, monkeypatch):
    monkeypatch.setattr(train, "check_gpu", lambda idx=0: {"free": 10.0})

    resp = client.post("/api/train/start", json=_train_payload(task_id=3, data_name="MNIST"))
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 400


def test_start_training_should_return_400_when_gpu_memory_not_enough(client, monkeypatch):
    monkeypatch.setattr(train, "check_gpu", lambda idx=0: {"free": 1.0})

    resp = client.post("/api/train/start", json=_train_payload(task_id=4, data_name="CIFAR100"))
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 400
    assert "GPU" in body["message"]


def test_start_training_should_return_success_and_register_running_task(client, monkeypatch):
    class _FakeEvent:
        def __init__(self):
            self.value = False

        def set(self):
            self.value = True

        def is_set(self):
            return self.value

    class _FakeManager:
        @staticmethod
        def Event():
            return _FakeEvent()

    monkeypatch.setattr(train, "check_gpu", lambda idx=0: {"free": 10.0})
    monkeypatch.setattr(train, "_get_manager", lambda: _FakeManager())
    monkeypatch.setattr(train.multiprocessing, "Process", _FakeProcess)
    monkeypatch.setattr(train, "_reap_process", lambda task_id, process: None)

    resp = client.post("/api/train/start", json=_train_payload(task_id=5, data_name="CIFAR100"))
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 200
    with train.running_trainers_lock:
        assert 5 in train.running_trainers
        assert train.running_trainers[5]["process"].started is True


def test_stop_training_should_return_404_when_task_not_running(client):
    resp = client.post("/api/train/stop/100")
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 404


def test_stop_training_should_set_event_and_remove_task(client):
    class _FakeEvent:
        def __init__(self):
            self.value = False

        def set(self):
            self.value = True

    fake_event = _FakeEvent()
    with train.running_trainers_lock:
        train.running_trainers[6] = {"process": object(), "stop_event": fake_event}

    resp = client.post("/api/train/stop/6")
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 200
    assert fake_event.value is True
    with train.running_trainers_lock:
        assert 6 not in train.running_trainers
