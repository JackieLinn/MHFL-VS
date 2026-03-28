from types import SimpleNamespace

from routers import train
from utils.schemas import TrainStartRequest


class _FakePublisher:
    def __init__(self):
        self.status_calls = []
        self.round_calls = []
        self.client_calls = []

    def publish_status(self, task_id, status, message=""):
        self.status_calls.append((task_id, status, message))
        return True

    def publish_round(self, task_id, step, metrics):
        self.round_calls.append((task_id, step, metrics))
        return True

    def publish_client(self, task_id, step, client_id, metrics):
        self.client_calls.append((task_id, step, client_id, metrics))
        return True


class _FakeStopEvent:
    def __init__(self, is_set=False):
        self._is_set = is_set

    def is_set(self):
        return self._is_set

    def set(self):
        self._is_set = True


def _build_request(task_id=1, data_name="CIFAR100", algorithm_name="Standalone"):
    return TrainStartRequest(
        task_id=task_id,
        data_name=data_name,
        algorithm_name=algorithm_name,
        num_nodes=100,
        fraction=0.1,
        classes_per_node=10,
        low_prob=0.4,
        num_steps=5,
        epochs=2,
    )


def test_normalize_data_name_should_convert_known_values():
    assert train._normalize_data_name("CIFAR100") == "cifar100"
    assert train._normalize_data_name("Tiny-Imagenet") == "tiny-imagenet"
    assert train._normalize_data_name(" tiny_imagenet ") == "tiny-imagenet"
    assert train._normalize_data_name("other") == "other"


def test_callbacks_should_publish_round_and_client():
    publisher = _FakePublisher()
    step_cb = train._create_step_callback(11, publisher)
    client_cb = train._create_client_callback(11, publisher)

    step_cb(11, 3, {"accuracy": 0.9})
    client_cb(11, 3, 7, {"accuracy": 0.8})

    assert publisher.round_calls == [(11, 3, {"accuracy": 0.9})]
    assert publisher.client_calls == [(11, 3, 7, {"accuracy": 0.8})]


def test_run_training_should_publish_success(monkeypatch):
    publisher = _FakePublisher()
    request = _build_request(task_id=9, algorithm_name="Standalone")
    stop_event = _FakeStopEvent(is_set=False)

    class FakeTrainer:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

        def train(self):
            return None

    monkeypatch.setattr(train, "RedisPublisher", lambda: publisher)
    monkeypatch.setattr(train, "get_trainer_class", lambda name: FakeTrainer)

    train._run_training(request, stop_event)

    assert publisher.status_calls[0][1] == "IN_PROGRESS"
    assert publisher.status_calls[-1][1] == "SUCCESS"


def test_run_training_should_publish_cancelled_when_stop_set(monkeypatch):
    publisher = _FakePublisher()
    request = _build_request(task_id=10, algorithm_name="Standalone")
    stop_event = _FakeStopEvent(is_set=True)

    class FakeTrainer:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

        def train(self):
            return None

    monkeypatch.setattr(train, "RedisPublisher", lambda: publisher)
    monkeypatch.setattr(train, "get_trainer_class", lambda name: FakeTrainer)

    train._run_training(request, stop_event)

    assert publisher.status_calls[-1][1] == "CANCELLED"


def test_run_training_should_publish_failed_when_trainer_missing(monkeypatch):
    publisher = _FakePublisher()
    request = _build_request(task_id=11, algorithm_name="NotExists")
    stop_event = _FakeStopEvent(is_set=False)

    monkeypatch.setattr(train, "RedisPublisher", lambda: publisher)
    monkeypatch.setattr(train, "get_trainer_class", lambda name: None)

    train._run_training(request, stop_event)

    assert publisher.status_calls[0][1] == "IN_PROGRESS"
    assert publisher.status_calls[-1][1] == "FAILED"


def test_run_training_should_publish_failed_when_train_raises(monkeypatch):
    publisher = _FakePublisher()
    request = _build_request(task_id=12, algorithm_name="Standalone")
    stop_event = _FakeStopEvent(is_set=False)

    class FakeTrainer:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

        def train(self):
            raise RuntimeError("train failed")

    monkeypatch.setattr(train, "RedisPublisher", lambda: publisher)
    monkeypatch.setattr(train, "get_trainer_class", lambda name: FakeTrainer)

    train._run_training(request, stop_event)

    assert publisher.status_calls[-1][1] == "FAILED"


def test_reap_process_should_remove_task_from_registry():
    fake_process = SimpleNamespace(join=lambda: None)
    with train.running_trainers_lock:
        train.running_trainers[88] = {"process": fake_process, "stop_event": _FakeStopEvent()}

    train._reap_process(88, fake_process)

    with train.running_trainers_lock:
        assert 88 not in train.running_trainers


def test_get_manager_should_lazy_init_once(monkeypatch):
    class _Manager:
        pass

    created = {"count": 0}

    def _fake_manager():
        created["count"] += 1
        return _Manager()

    monkeypatch.setattr(train.multiprocessing, "Manager", _fake_manager)
    train._manager = None

    m1 = train._get_manager()
    m2 = train._get_manager()

    assert isinstance(m1, _Manager)
    assert m1 is m2
    assert created["count"] == 1


def test_run_training_should_treat_stop_event_check_error_as_cancelled(monkeypatch):
    publisher = _FakePublisher()
    request = _build_request(task_id=13, algorithm_name="Standalone")

    class _BadStopEvent:
        @staticmethod
        def is_set():
            raise RuntimeError("event broken")

    class FakeTrainer:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

        def train(self):
            return None

    monkeypatch.setattr(train, "RedisPublisher", lambda: publisher)
    monkeypatch.setattr(train, "get_trainer_class", lambda name: FakeTrainer)

    train._run_training(request, _BadStopEvent())

    assert publisher.status_calls[-1][1] == "CANCELLED"
