"""
向量检索、BM25、混合检索
- retrieve(): 主入口，根据配置走纯向量或混合检索（步骤 5 / 10）
- bm25_retrieve(): BM25 检索（步骤 10）
- hybrid_retrieve(): 向量 + BM25 混合检索，RRF 融合（步骤 10）
"""
from pathlib import Path
from typing import Any

import jieba
import numpy as np
from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_openai import OpenAIEmbeddings
from rank_bm25 import BM25Okapi

from config.settings import settings

# 与 build_kb 一致，路径相对项目根
_BASE_DIR = Path(__file__).resolve().parent.parent

# BM25 索引懒加载缓存：(BM25Okapi, documents, metadatas, ids)
_bm25_cache: tuple[BM25Okapi, list[str], list[dict[str, Any]], list[str]] | None = None

# RRF 融合常数 k，常用 60
_RRF_K = 60


def _get_embeddings() -> OpenAIEmbeddings:
    embed_kwargs: dict = {"model": settings.ASSISTANT_EMBEDDING_MODEL, "api_key": settings.OPENAI_API_KEY}
    if settings.OPENAI_API_BASE:
        embed_kwargs["base_url"] = settings.OPENAI_API_BASE
    return OpenAIEmbeddings(**embed_kwargs)


def _get_vectorstore() -> Chroma:
    """获取 Chroma 向量库实例，供向量检索和 BM25 索引构建共用"""
    chroma_dir = _BASE_DIR / settings.ASSISTANT_CHROMA_DIR
    embeddings = _get_embeddings()
    return Chroma(
        collection_name="mhfl_kb",
        embedding_function=embeddings,
        persist_directory=str(chroma_dir),
    )


def _tokenize(text: str) -> list[str]:
    """BM25 用：jieba 分词，过滤空 token，英文统一小写"""
    if not text or not str(text).strip():
        return []
    tokens = jieba.lcut(str(text).strip())
    return [t.lower() for t in tokens if t and t.strip()]


def _get_bm25_index() -> tuple[BM25Okapi, list[str], list[dict[str, Any]], list[str]]:
    """懒加载 BM25 索引：从 Chroma 拉全量文档，构建 BM25Okapi"""
    global _bm25_cache
    if _bm25_cache is not None:
        return _bm25_cache

    vectorstore = _get_vectorstore()
    data = vectorstore._collection.get(include=["documents", "metadatas"])
    documents = data.get("documents") or []
    metadatas = data.get("metadatas") or []
    ids = data.get("ids") or []

    if not documents:
        # 空知识库：构造空 BM25，避免后续报错
        corpus: list[list[str]] = [[]]
        bm25 = BM25Okapi(corpus)
        _bm25_cache = (bm25, [], [], [])
        return _bm25_cache

    corpus = [_tokenize(doc) for doc in documents]
    # 空文档会导致 BM25 报错，用占位 token
    corpus = [tokens if tokens else ["__empty__"] for tokens in corpus]
    bm25 = BM25Okapi(corpus)

    _bm25_cache = (bm25, documents, metadatas, ids)
    return _bm25_cache


def get_retriever(k: int | None = None):
    """创建 Chroma retriever，k 为召回数量"""
    vectorstore = _get_vectorstore()
    k = k or settings.ASSISTANT_TOP_K
    return vectorstore.as_retriever(search_kwargs={"k": k})


def _retrieve_vector(query: str, k: int | None = None) -> list[Document]:
    """纯向量检索，供 hybrid_retrieve 内部调用，避免递归"""
    retriever = get_retriever(k=k)
    return retriever.invoke(query)


def retrieve(query: str, k: int | None = None) -> list[Document]:
    """
    主检索入口：根据 ASSISTANT_ENABLE_HYBRID 选择纯向量或混合检索。
    供 rag、router 等调用。
    """
    if settings.ASSISTANT_ENABLE_HYBRID:
        return hybrid_retrieve(query, k)
    return _retrieve_vector(query, k)


def bm25_retrieve(query: str, k: int | None = None) -> list[Document]:
    """BM25 检索，基于 jieba 分词，返回带 metadata 的 Document 列表"""
    k = k or settings.ASSISTANT_BM25_K
    bm25, documents, metadatas, ids = _get_bm25_index()

    if not documents:
        return []

    q_tokens = _tokenize(query)
    if not q_tokens:
        return []

    scores = bm25.get_scores(q_tokens)
    top_indices = np.argsort(scores)[::-1][:k]

    result: list[Document] = []
    for i in top_indices:
        if scores[i] <= 0:
            continue
        doc_content = documents[i] if i < len(documents) else ""
        if doc_content is None:
            doc_content = ""
        meta = metadatas[i] if i < len(metadatas) else None
        meta_dict = dict(meta) if meta else {}
        result.append(Document(page_content=doc_content, metadata=meta_dict))
        if len(result) >= k:
            break

    return result


def hybrid_retrieve(query: str, k: int | None = None) -> list[Document]:
    """
    向量 + BM25 混合检索，RRF 融合后取 top-k。
    向量召回 ASSISTANT_BM25_K 条，BM25 召回 ASSISTANT_BM25_K 条，合并去重后按 RRF 分数排序取前 k 条。
    """
    k = k or settings.ASSISTANT_HYBRID_TOP_K
    each_k = settings.ASSISTANT_BM25_K

    vec_docs = _retrieve_vector(query, k=each_k)
    bm25_docs = bm25_retrieve(query, k=each_k)

    # RRF 融合：score(d) = Σ 1/(_RRF_K + rank)
    rrf_scores: dict[str, tuple[Document, float]] = {}

    for rank, d in enumerate(vec_docs):
        chunk_id = d.metadata.get("chunk_id") or d.metadata.get("doc_id") or id(d)
        key = str(chunk_id)
        rrf = 1.0 / (_RRF_K + rank + 1)
        if key in rrf_scores:
            old_doc, old_score = rrf_scores[key]
            rrf_scores[key] = (old_doc, old_score + rrf)
        else:
            rrf_scores[key] = (d, rrf)

    for rank, d in enumerate(bm25_docs):
        chunk_id = d.metadata.get("chunk_id") or d.metadata.get("doc_id") or id(d)
        key = str(chunk_id)
        rrf = 1.0 / (_RRF_K + rank + 1)
        if key in rrf_scores:
            old_doc, old_score = rrf_scores[key]
            rrf_scores[key] = (old_doc, old_score + rrf)
        else:
            rrf_scores[key] = (d, rrf)

    sorted_items = sorted(rrf_scores.values(), key=lambda x: -x[1])
    return [doc for doc, _ in sorted_items[:k]]
