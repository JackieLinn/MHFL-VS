import sys
from pathlib import Path

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


@pytest.fixture
def api_app() -> FastAPI:
    from routers import health, resource, train

    app = FastAPI()
    app.include_router(health.router)
    app.include_router(resource.router)
    app.include_router(train.router)
    return app


@pytest.fixture
def client(api_app: FastAPI) -> TestClient:
    return TestClient(api_app)


@pytest.fixture(autouse=True)
def clear_running_trainers():
    from routers import train

    with train.running_trainers_lock:
        train.running_trainers.clear()
    yield
    with train.running_trainers_lock:
        train.running_trainers.clear()

