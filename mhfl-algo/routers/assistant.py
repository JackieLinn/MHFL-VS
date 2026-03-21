"""
Assistant FastAPI router.
- POST /api/assistant/chat: non-streaming chat
- POST /api/assistant/chat/stream: SSE streaming chat
"""

from __future__ import annotations

import json
import logging

from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from openai import AsyncOpenAI

from assistant.classifier import classify_intent
from assistant.memory import generate_summary
from assistant.prompt import build_rag_prompt
from assistant.rag import get_docs_for_query
from config.settings import settings
from services.assistant_service import chat as assistant_chat
from utils.schemas import (
    ApiResponse,
    ChatRequest,
    ChatResponse,
    ClassifyRequest,
    ClassifyResponse,
    SummarizeRequest,
    SummarizeResponse,
)

router = APIRouter(prefix="/api/assistant", tags=["智能助手"])
logger = logging.getLogger(__name__)


def _get_openai_client() -> AsyncOpenAI:
    kwargs: dict[str, str | int] = {
        "api_key": settings.OPENAI_API_KEY,
        "timeout": settings.ASSISTANT_STREAM_TIMEOUT_SECONDS,
    }
    if settings.OPENAI_API_BASE:
        kwargs["base_url"] = settings.OPENAI_API_BASE
    return AsyncOpenAI(**kwargs)


@router.post("/classify")
async def classify(req: ClassifyRequest):
    """根据用户问题判断需要预取哪些业务数据（任务、算法、数据集）"""
    try:
        result = await classify_intent(req.message)
        return ApiResponse.success(data=ClassifyResponse(**result))
    except Exception as e:
        logger.exception("Assistant classify failed: %s", e)
        return ApiResponse.success(data=ClassifyResponse())


def _classify_error(msg: str) -> tuple[int, str]:
    msg_lower = msg.lower()
    if "api key" in msg_lower or "authentication" in msg_lower or "invalid" in msg_lower:
        return 401, "API 密钥无效或未配置，请检查 OPENAI_API_KEY"
    if "rate limit" in msg_lower or "429" in msg_lower:
        return 429, "请求过于频繁，请稍后重试"
    if "timeout" in msg_lower or "timed out" in msg_lower:
        return 504, "服务响应超时，请稍后重试"
    if "connection" in msg_lower or "network" in msg_lower:
        return 503, "服务暂时不可用，请检查网络或 API 地址"
    return 500, "智能助手服务异常，请稍后重试"


@router.post("/summarize")
async def summarize(req: SummarizeRequest):
    """增量摘要：prev_summary + 新 2 条消息 -> 新摘要，限制 300 字"""
    try:
        summary = await generate_summary(req.prev_summary, req.messages)
        return ApiResponse.success(data=SummarizeResponse(summary=summary))
    except Exception as e:
        logger.exception("Assistant summarize failed: %s", e)
        return ApiResponse.success(data=SummarizeResponse(summary=req.prev_summary or ""))


@router.post("/chat")
async def chat(req: ChatRequest):
    try:
        needs_kb = req.needs_kb if req.needs_kb is not None else True
        content, sources = await assistant_chat(
            req.message,
            context_data=req.context_data,
            needs_kb=needs_kb,
            memory_context=req.memory_context,
        )
        return ApiResponse.success(data=ChatResponse(content=content, sources=sources))
    except Exception as e:
        logger.exception("Assistant chat failed: %s", e)
        code, user_msg = _classify_error(str(e))
        return ApiResponse.failure(code=code, message=user_msg)


@router.post("/chat/stream")
async def chat_stream(req: ChatRequest):
    """
    True backend streaming with SSE events:
    - delta: incremental token text
    - done: full content + sources
    - error: terminal error
    """

    async def gen():
        full_parts: list[str] = []
        try:
            needs_kb = req.needs_kb if req.needs_kb is not None else True
            has_context_data = bool(req.context_data)
            has_memory = bool(req.memory_context and str(req.memory_context or "").strip())

            if needs_kb:
                docs = await get_docs_for_query(req.message)
            else:
                docs = []

            if not needs_kb and not has_context_data and not has_memory:
                yield f"data: {json.dumps({'type': 'start'}, ensure_ascii=False)}\n\n"
                yield f"data: {json.dumps({'type': 'done', 'content': '未获取到相关业务数据，请尝试提问知识库类问题（如「FedAvg 是什么」）或先发起对话。', 'sources': []}, ensure_ascii=False)}\n\n"
                return

            use_context_data = req.context_data if has_context_data else None
            memory_ctx = req.memory_context if req.memory_context else None
            system, user = build_rag_prompt(req.message, docs, use_context_data, memory_ctx)
            sources = list({d.metadata.get("source", "") for d in docs if d.metadata.get("source", "")})
            client = _get_openai_client()

            # Start event helps frontend mark stream as connected quickly.
            yield f"data: {json.dumps({'type': 'start'}, ensure_ascii=False)}\n\n"

            stream = await client.chat.completions.create(
                model=settings.ASSISTANT_MODEL,
                temperature=0.3,
                stream=True,
                messages=[
                    {"role": "system", "content": system},
                    {"role": "user", "content": user},
                ],
            )

            async for event in stream:
                if not event.choices:
                    continue
                delta = event.choices[0].delta.content
                if not delta:
                    continue
                full_parts.append(delta)
                yield f"data: {json.dumps({'type': 'delta', 'content': delta}, ensure_ascii=False)}\n\n"

            full = "".join(full_parts)
            yield (
                f"data: {json.dumps({'type': 'done', 'content': full, 'sources': sources}, ensure_ascii=False)}\n\n"
            )
        except Exception as e:
            logger.exception("Assistant chat_stream failed: %s", e)
            err_msg = _classify_error(str(e))[1]
            yield f"data: {json.dumps({'type': 'error', 'content': err_msg}, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        gen(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )
