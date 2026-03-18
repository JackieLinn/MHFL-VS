"""
智能助手对外入口
编排 assistant 包下的 retriever、rag、prompt 等模块，供 router 调用
"""
from config.settings import settings

from assistant.rag import answer as rag_answer


def chat(message: str, context_data: dict | None = None) -> tuple[str, list[str]]:
    """
    非流式聊天：检索 -> 拼 prompt -> 调 LLM -> 返回 (content, sources)
    context_data 为 SpringBoot 预取的业务数据，步骤 13 后使用
    """
    if not settings.OPENAI_API_KEY or not settings.OPENAI_API_KEY.strip():
        raise ValueError("API key invalid or not configured")
    return rag_answer(message)
