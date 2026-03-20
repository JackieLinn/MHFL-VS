"""
RAG 问答链
- get_docs_for_query(): 统一检索入口，支持多查询改写（步骤 12）
- answer(): 检索 -> [rerank] -> 拼 prompt -> 调 LLM -> 返回 content + sources（步骤 6 / 11）
"""
from langchain_core.documents import Document

from assistant.prompt import build_rag_prompt
from assistant.query_rewrite import rewrite_queries
from assistant.rerank import rerank
from assistant.retriever import hybrid_retrieve, retrieve
from langchain_openai import ChatOpenAI

from config.settings import settings


def get_docs_for_query(query: str) -> list[Document]:
    """
    统一检索入口：根据 ASSISTANT_ENABLE_MULTI_QUERY 选择单查询或多查询，
    再根据 ASSISTANT_ENABLE_RERANK 决定是否 rerank。
    """
    if settings.ASSISTANT_ENABLE_MULTI_QUERY:
        queries = rewrite_queries(query, n=3)
        all_docs: list[Document] = []
        for q in queries:
            all_docs.extend(hybrid_retrieve(q, k=5))
        seen: set[str] = set()
        deduped: list[Document] = []
        for d in all_docs:
            key = str(d.metadata.get("chunk_id") or d.metadata.get("doc_id") or id(d))
            if key not in seen:
                seen.add(key)
                deduped.append(d)
        docs = deduped[: settings.ASSISTANT_HYBRID_TOP_K]
    else:
        docs = retrieve(query)

    if settings.ASSISTANT_ENABLE_RERANK and docs:
        docs = rerank(query, docs)
    return docs


def _get_llm() -> ChatOpenAI:
    llm_kwargs: dict = {"model": settings.ASSISTANT_MODEL, "temperature": 0.3, "api_key": settings.OPENAI_API_KEY}
    if settings.OPENAI_API_BASE:
        llm_kwargs["base_url"] = settings.OPENAI_API_BASE
    return ChatOpenAI(**llm_kwargs)


def answer(query: str) -> tuple[str, list[str]]:
    """RAG 回答：检索 -> [rerank] -> 拼 prompt -> 调 LLM -> 返回 (content, sources)"""
    docs = get_docs_for_query(query)
    if not docs:
        return "未找到相关文档，请换一种问法或联系管理员。", []
    system, user = build_rag_prompt(query, docs)
    llm = _get_llm()
    resp = llm.invoke([{"role": "system", "content": system}, {"role": "user", "content": user}])
    sources = list({d.metadata.get("source", "") for d in docs})
    return resp.content, sources
