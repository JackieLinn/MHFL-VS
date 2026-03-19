"""
向量检索、BM25、混合检索
- retrieve(): 纯向量检索（步骤 5）
- bm25_retrieve(): BM25 检索（步骤 10）
- hybrid_retrieve(): 混合检索（步骤 10）
"""
from pathlib import Path

from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_openai import OpenAIEmbeddings

from config.settings import settings

# 与 build_kb 一致，路径相对项目根
_BASE_DIR = Path(__file__).resolve().parent.parent


def _get_embeddings() -> OpenAIEmbeddings:
    embed_kwargs: dict = {"model": settings.ASSISTANT_EMBEDDING_MODEL, "api_key": settings.OPENAI_API_KEY}
    if settings.OPENAI_API_BASE:
        embed_kwargs["base_url"] = settings.OPENAI_API_BASE
    return OpenAIEmbeddings(**embed_kwargs)


def get_retriever(k: int | None = None):
    """创建 Chroma retriever，k 为召回数量"""
    chroma_dir = _BASE_DIR / settings.ASSISTANT_CHROMA_DIR
    embeddings = _get_embeddings()
    vectorstore = Chroma(
        collection_name="mhfl_kb",
        embedding_function=embeddings,
        persist_directory=str(chroma_dir),
    )
    k = k or settings.ASSISTANT_TOP_K
    return vectorstore.as_retriever(search_kwargs={"k": k})


def retrieve(query: str, k: int | None = None) -> list[Document]:
    """向量检索，返回带 metadata 的 Document 列表"""
    retriever = get_retriever(k=k)
    return retriever.invoke(query)
