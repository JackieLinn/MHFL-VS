"""
智能助手 FastAPI 路由
- POST /api/assistant/chat: 非流式聊天
- POST /api/assistant/chat/stream: 流式聊天（步骤 9）
"""
import json
import logging

from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from langchain_openai import ChatOpenAI

from assistant.prompt import build_rag_prompt
from assistant.retriever import retrieve
from config.settings import settings
from services.assistant_service import chat as assistant_chat
from utils.schemas import ApiResponse, ChatRequest, ChatResponse

router = APIRouter(prefix="/api/assistant", tags=["智能助手"])
logger = logging.getLogger(__name__)


def _get_llm_streaming():
    """获取流式 LLM 实例"""
    llm_kwargs = {"model": settings.ASSISTANT_MODEL, "temperature": 0, "api_key": settings.OPENAI_API_KEY}
    if settings.OPENAI_API_BASE:
        llm_kwargs["base_url"] = settings.OPENAI_API_BASE
    return ChatOpenAI(**llm_kwargs, streaming=True)


def _classify_error(msg: str) -> tuple[int, str]:
    """根据异常信息返回 (code, user_message)"""
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


@router.post("/chat")
async def chat(req: ChatRequest):
    try:
        content, sources = assistant_chat(req.message, req.context_data)
        return ApiResponse.success(data=ChatResponse(content=content, sources=sources))
    except Exception as e:
        logger.exception("Assistant chat failed: %s", e)
        code, user_msg = _classify_error(str(e))
        return ApiResponse.failure(code=code, message=user_msg)


@router.post("/chat/stream")
async def chat_stream(req: ChatRequest):
    """流式聊天：每 chunk 发 data: {type:delta, content}，结束发 data: {type:done, content}"""

    async def gen():
        full = ""
        try:
            docs = retrieve(req.message)
            system, user = build_rag_prompt(req.message, docs)
            llm = _get_llm_streaming()
            async for chunk in llm.astream([{"role": "system", "content": system}, {"role": "user", "content": user}]):
                if chunk.content:
                    full += chunk.content
                    yield f"data: {json.dumps({'type': 'delta', 'content': chunk.content}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'type': 'done', 'content': full}, ensure_ascii=False)}\n\n"
        except Exception as e:
            logger.exception("Assistant chat_stream failed: %s", e)
            err_msg = _classify_error(str(e))[1]
            yield f"data: {json.dumps({'type': 'error', 'content': err_msg}, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        gen(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"}
    )
