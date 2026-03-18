"""
智能助手请求/响应模型
"""
from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    """聊天请求体"""
    conversation_id: int | None = None
    message: str = Field(..., min_length=1, max_length=8000, description="用户消息")
    context_data: dict | None = Field(default=None, description="SpringBoot 预取的业务数据")


class ChatResponse(BaseModel):
    """聊天响应体"""
    content: str = Field(..., description="助手回复内容")
    sources: list[str] = Field(default_factory=list, description="参考文档来源列表")
