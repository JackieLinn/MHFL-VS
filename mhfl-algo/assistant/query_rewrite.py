"""
多查询改写（步骤 12）
对用户问题生成多个改写，分别检索后合并，提高召回覆盖率
"""
import re

from langchain_openai import ChatOpenAI

from config.settings import settings


def _get_rewrite_llm() -> ChatOpenAI:
    """用于改写的 LLM，temperature=0.3 以产生多样性"""
    llm_kwargs: dict = {
        "model": settings.ASSISTANT_MODEL,
        "temperature": 0.3,
        "api_key": settings.OPENAI_API_KEY,
    }
    if settings.OPENAI_API_BASE:
        llm_kwargs["base_url"] = settings.OPENAI_API_BASE
    return ChatOpenAI(**llm_kwargs)


def _strip_line_prefix(line: str) -> str:
    """去掉行首的序号，如 '1. '、'1) '、'- ' 等"""
    line = line.strip()
    return re.sub(r"^\d+[\.\)]\s*", "", line).strip()


def rewrite_queries(question: str, n: int = 3) -> list[str]:
    """
    对用户问题生成 n 个不同表述的改写，用于多路检索。
    返回 [原问题, 改写1, 改写2, ...]，最多 n+1 个。
    """
    if not question or not str(question).strip():
        return []

    prompt = f"""针对以下用户问题，生成 {n} 个不同表述的改写问题，用于检索相关文档。
要求：换说法、补全关键词、拆解子问题，覆盖同一意图的多种问法。
用户问题：{question}
仅输出 {n} 个问题，每行一个，不要其他内容。"""

    try:
        llm = _get_rewrite_llm()
        resp = llm.invoke(prompt)
        content = (resp.content or "").strip()
    except Exception:
        return [question]

    lines = [line.strip() for line in content.split("\n") if line.strip()]
    parsed = []
    for line in lines[:n]:
        cleaned = _strip_line_prefix(line)
        if cleaned and cleaned != question:
            parsed.append(cleaned)

    return [question] + parsed
