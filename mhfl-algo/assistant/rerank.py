"""
FlashRank 重排序（步骤 11）
对混合召回结果做 rerank，取 top-n 作为最终上下文
"""
from pathlib import Path

from langchain_core.documents import Document

from config.settings import settings

# 模型缓存目录，与 download_flashrank_model.py 一致
_CACHE_DIR = Path(__file__).resolve().parent.parent / "cache"
_MODEL_NAME = "ms-marco-MultiBERT-L-12"

_ranker = None


def _get_ranker():
    global _ranker
    if _ranker is None:
        from flashrank import Ranker
        _ranker = Ranker(
            model_name=_MODEL_NAME,
            cache_dir=str(_CACHE_DIR),
        )
    return _ranker


def rerank(query: str, docs: list[Document], top_n: int | None = None) -> list[Document]:
    """
    对检索结果做 FlashRank 重排序，取 top-n 条。
    :param query: 用户问题
    :param docs: 检索到的 Document 列表
    :param top_n: 返回条数，默认 ASSISTANT_RERANK_TOP_N
    :return: 重排后的 Document 列表
    """
    if not docs:
        return []
    top_n = top_n or settings.ASSISTANT_RERANK_TOP_N
    top_n = min(top_n, len(docs))

    passages = [{"id": i, "text": d.page_content or ""} for i, d in enumerate(docs)]
    from flashrank import RerankRequest
    results = _get_ranker().rerank(RerankRequest(query=query, passages=passages))

    # results 已按 score 降序，取前 top_n，按 id 映射回原 Document
    return [docs[r["id"]] for r in results[:top_n]]
