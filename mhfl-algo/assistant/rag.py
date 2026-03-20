"""
RAG 问答链
- get_docs_for_query(): 统一检索入口，支持多查询改写（步骤 12）
- answer(): 根据 needs_kb 编排：kb_only / data_only / both（步骤 6 / 11 / 14）
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


def answer(
        query: str,
        context_data: dict | None = None,
        needs_kb: bool = True,
) -> tuple[str, list[str]]:
    """
    根据 needs_kb 编排回答（步骤 14）：
    - needs_kb=True: 做 RAG 检索，context_data 有则一并注入
    - needs_kb=False: 跳过 RAG；有 context_data 则仅用业务数据；无则返回兜底
    """
    has_context_data = bool(context_data)
    do_rag = needs_kb

    if do_rag:
        docs = get_docs_for_query(query)
    else:
        docs = []

    if not do_rag and not has_context_data:
        return "未获取到相关业务数据，请尝试提问知识库类问题（如「FedAvg 是什么」）。", []

    use_context_data = context_data if has_context_data else None
    system, user = build_rag_prompt(query, docs, use_context_data)
    llm = _get_llm()
    resp = llm.invoke([{"role": "system", "content": system}, {"role": "user", "content": user}])
    sources = list({d.metadata.get("source", "") for d in docs}) if docs else []
    return resp.content, sources
