"""
智能助手 FastAPI 路由
- POST /api/assistant/chat: 非流式聊天
- POST /api/assistant/chat/stream: 流式聊天（步骤 9 实现）
"""
import logging

from fastapi import APIRouter

from assistant.schemas import ChatRequest, ChatResponse
from services.assistant_service import chat as assistant_chat
from utils.schemas import ApiResponse

router = APIRouter(prefix="/api/assistant", tags=["智能助手"])
logger = logging.getLogger(__name__)


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
