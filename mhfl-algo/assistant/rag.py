"""
RAG 问答链
- answer(): 检索 -> 拼 prompt -> 调 LLM -> 返回 content + sources（步骤 6）
"""
from assistant.prompt import build_rag_prompt
from assistant.retriever import retrieve
from langchain_openai import ChatOpenAI

from config.settings import settings


def _get_llm() -> ChatOpenAI:
    llm_kwargs: dict = {"model": settings.ASSISTANT_MODEL, "temperature": 0}
    if settings.OPENAI_API_BASE:
        llm_kwargs["base_url"] = settings.OPENAI_API_BASE
    return ChatOpenAI(**llm_kwargs)


def answer(query: str) -> tuple[str, list[str]]:
    """RAG 回答：检索 -> 拼 prompt -> 调 LLM -> 返回 (content, sources)"""
    docs = retrieve(query)
    if not docs:
        return "未找到相关文档，请换一种问法或联系管理员。", []
    system, user = build_rag_prompt(query, docs)
    llm = _get_llm()
    resp = llm.invoke([{"role": "system", "content": system}, {"role": "user", "content": user}])
    sources = list({d.metadata.get("source", "") for d in docs})
    return resp.content, sources
