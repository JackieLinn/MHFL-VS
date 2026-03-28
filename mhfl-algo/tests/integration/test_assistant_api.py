import json
from types import SimpleNamespace

from fastapi import FastAPI
from fastapi.testclient import TestClient

from routers import assistant


class _AsyncStream:
    def __init__(self, events):
        self._events = iter(events)

    def __aiter__(self):
        return self

    async def __anext__(self):
        try:
            return next(self._events)
        except StopIteration as e:
            raise StopAsyncIteration from e


def _assistant_client() -> TestClient:
    app = FastAPI()
    app.include_router(assistant.router)
    return TestClient(app)


def _sse_lines(text: str):
    lines = []
    for line in text.splitlines():
        if line.startswith("data: "):
            lines.append(json.loads(line[6:]))
    return lines


def test_classify_should_return_data_when_service_ok(monkeypatch):
    async def _fake_classify(message):
        return {"needs_tasks": True, "needs_algorithms": False, "needs_datasets": True, "needs_kb": False}

    monkeypatch.setattr(assistant, "classify_intent", _fake_classify)
    client = _assistant_client()

    resp = client.post("/api/assistant/classify", json={"message": "q"})
    body = resp.json()

    assert resp.status_code == 200
    assert body["code"] == 200
    assert body["data"]["needs_tasks"] is True
    assert body["data"]["needs_kb"] is False


def test_classify_should_fallback_when_service_raises(monkeypatch):
    async def _fake_classify(message):
        raise RuntimeError("boom")

    monkeypatch.setattr(assistant, "classify_intent", _fake_classify)
    client = _assistant_client()

    resp = client.post("/api/assistant/classify", json={"message": "q"})
    body = resp.json()

    assert body["code"] == 200
    assert body["data"]["needs_tasks"] is False
    assert body["data"]["needs_kb"] is True


def test_summarize_should_return_summary(monkeypatch):
    async def _fake_summary(prev, msgs):
        return "new-summary"

    monkeypatch.setattr(assistant, "generate_summary", _fake_summary)
    client = _assistant_client()

    resp = client.post("/api/assistant/summarize", json={"prev_summary": "old", "messages": [{"role": "user", "content": "hi"}]})
    body = resp.json()

    assert body["code"] == 200
    assert body["data"]["summary"] == "new-summary"


def test_summarize_should_fallback_when_error(monkeypatch):
    async def _fake_summary(prev, msgs):
        raise RuntimeError("boom")

    monkeypatch.setattr(assistant, "generate_summary", _fake_summary)
    client = _assistant_client()

    resp = client.post("/api/assistant/summarize", json={"prev_summary": "old", "messages": [{"role": "user", "content": "hi"}]})
    body = resp.json()

    assert body["code"] == 200
    assert body["data"]["summary"] == "old"


def test_chat_should_return_success(monkeypatch):
    async def _fake_chat(message, context_data=None, needs_kb=True, memory_context=None):
        return "answer", ["S1"]

    monkeypatch.setattr(assistant, "assistant_chat", _fake_chat)
    client = _assistant_client()

    resp = client.post("/api/assistant/chat", json={"message": "q", "needs_kb": None})
    body = resp.json()

    assert body["code"] == 200
    assert body["data"]["content"] == "answer"
    assert body["data"]["sources"] == ["S1"]


def test_chat_should_return_failure_when_exception(monkeypatch):
    async def _fake_chat(message, context_data=None, needs_kb=True, memory_context=None):
        raise RuntimeError("rate limit reached")

    monkeypatch.setattr(assistant, "assistant_chat", _fake_chat)
    client = _assistant_client()

    resp = client.post("/api/assistant/chat", json={"message": "q"})
    body = resp.json()

    assert body["code"] == 429


def test_classify_error_should_cover_all_branches():
    assert assistant._classify_error("API key invalid")[0] == 401
    assert assistant._classify_error("rate limit")[0] == 429
    assert assistant._classify_error("timed out")[0] == 504
    assert assistant._classify_error("connection reset")[0] == 503
    assert assistant._classify_error("other")[0] == 500


def test_get_openai_client_should_include_base_url_when_configured(monkeypatch):
    captured = {}

    class _FakeClient:
        def __init__(self, **kwargs):
            captured.update(kwargs)

    monkeypatch.setattr(assistant, "AsyncOpenAI", _FakeClient)
    monkeypatch.setattr(assistant.settings, "OPENAI_API_KEY", "k")
    monkeypatch.setattr(assistant.settings, "ASSISTANT_STREAM_TIMEOUT_SECONDS", 10)
    monkeypatch.setattr(assistant.settings, "OPENAI_API_BASE", "http://x")

    assistant._get_openai_client()

    assert captured["api_key"] == "k"
    assert captured["timeout"] == 10
    assert captured["base_url"] == "http://x"


def test_chat_stream_should_return_fallback_done_when_no_kb_no_context_no_memory():
    client = _assistant_client()

    resp = client.post("/api/assistant/chat/stream", json={"message": "q", "needs_kb": False})
    assert resp.status_code == 200
    events = _sse_lines(resp.text)
    assert events[0]["type"] == "start"
    assert events[1]["type"] == "done"


def test_chat_stream_should_return_delta_and_done(monkeypatch):
    class _FakeCompletions:
        @staticmethod
        async def create(**kwargs):
            e1 = SimpleNamespace(choices=[SimpleNamespace(delta=SimpleNamespace(content="A"))])
            e2 = SimpleNamespace(choices=[])
            e3 = SimpleNamespace(choices=[SimpleNamespace(delta=SimpleNamespace(content=None))])
            e4 = SimpleNamespace(choices=[SimpleNamespace(delta=SimpleNamespace(content="B"))])
            return _AsyncStream([e1, e2, e3, e4])

    class _FakeClient:
        def __init__(self):
            self.chat = SimpleNamespace(completions=_FakeCompletions())

    class _Doc:
        def __init__(self, source):
            self.metadata = {"source": source}

    async def _fake_docs(query):
        return [_Doc("doc1.md"), _Doc("doc2.md")]

    monkeypatch.setattr(assistant, "get_docs_for_query", _fake_docs)
    monkeypatch.setattr(assistant, "build_rag_prompt", lambda q, d, c, m: ("sys", "usr"))
    monkeypatch.setattr(assistant, "_get_openai_client", lambda: _FakeClient())
    monkeypatch.setattr(assistant.settings, "ASSISTANT_MODEL", "m")

    client = _assistant_client()
    resp = client.post("/api/assistant/chat/stream", json={"message": "q", "needs_kb": True, "context_data": {"k": 1}})

    assert resp.status_code == 200
    events = _sse_lines(resp.text)
    assert events[0]["type"] == "start"
    assert events[1] == {"type": "delta", "content": "A"}
    assert events[2] == {"type": "delta", "content": "B"}
    assert events[3]["type"] == "done"
    assert events[3]["content"] == "AB"
    assert "BusinessData" in events[3]["sources"]


def test_chat_stream_should_return_error_event_when_exception(monkeypatch):
    async def _fake_docs(query):
        raise RuntimeError("connection error")

    monkeypatch.setattr(assistant, "get_docs_for_query", _fake_docs)
    client = _assistant_client()

    resp = client.post("/api/assistant/chat/stream", json={"message": "q", "needs_kb": True})
    events = _sse_lines(resp.text)

    assert events[0]["type"] == "error"
    assert events[0]["content"]

