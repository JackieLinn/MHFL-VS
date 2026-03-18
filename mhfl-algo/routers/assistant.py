"""
智能助手 FastAPI 路由
- POST /api/assistant/chat: 非流式聊天
- POST /api/assistant/chat/stream: 流式聊天（后续步骤实现）
"""
from fastapi import APIRouter

router = APIRouter(prefix="/api/assistant", tags=["智能助手"])

# TODO: 步骤 7 实现 /chat
# @router.post("/chat")
# async def chat(req: ChatRequest): ...

# TODO: 步骤 9 实现 /chat/stream
# @router.post("/chat/stream")
# async def chat_stream(req: ChatRequest): ...
