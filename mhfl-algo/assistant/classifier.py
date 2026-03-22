"""
意图分类（步骤 13 / 14）
根据用户问题判断：1) 需要预取哪些业务数据；2) 是否需要知识库检索
"""
import json
import re

from langchain_openai import ChatOpenAI

from assistant.prompt import get_classify_prompt
from config.settings import settings


def _get_classify_llm() -> ChatOpenAI:
    llm_kwargs: dict = {
        "model": settings.ASSISTANT_MODEL,
        "temperature": 0,
        "api_key": settings.OPENAI_API_KEY,
    }
    if settings.OPENAI_API_BASE:
        llm_kwargs["base_url"] = settings.OPENAI_API_BASE
    return ChatOpenAI(**llm_kwargs)


async def classify_intent(message: str) -> dict[str, bool]:
    """
    根据用户问题判断：1) 需要预取哪些业务数据；2) 是否需要知识库检索。
    返回 {"needs_tasks", "needs_algorithms", "needs_datasets", "needs_kb"}
    """
    default = {
        "needs_tasks": False,
        "needs_algorithms": False,
        "needs_datasets": False,
        "needs_kb": True,
    }
    if not message or not str(message).strip():
        return default

    try:
        llm = _get_classify_llm()
        # 分类提示词从 assistant/knowledge/system/classify_prompt.md 读取
        resp = await llm.ainvoke(get_classify_prompt(message))
        content = (resp.content or "").strip()
        # 提取 JSON（可能被 markdown 包裹）
        match = re.search(r"\{[^{}]*\}", content, re.DOTALL)
        if match:
            obj = json.loads(match.group())
            return {
                "needs_tasks": bool(obj.get("needs_tasks", False)),
                "needs_algorithms": bool(obj.get("needs_algorithms", False)),
                "needs_datasets": bool(obj.get("needs_datasets", False)),
                "needs_kb": bool(obj.get("needs_kb", True)),
            }
    except Exception:
        pass
    return default
