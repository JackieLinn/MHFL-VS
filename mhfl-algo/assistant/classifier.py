"""
意图分类（步骤 13）
根据用户问题判断需要预取哪些业务数据：任务、算法、数据集
"""
import json
import re

from langchain_openai import ChatOpenAI

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


_CLASSIFY_PROMPT = """你是一个意图分类器。根据用户问题，判断是否需要以下业务数据（用于从数据库预取）：
- needs_tasks: 用户问「我的任务」「最近任务」「任务总量」「有多少任务」「任务情况」「任务列表」等
- needs_algorithms: 用户问「有哪些算法」「平台算法」「算法列表」「算法总量」等
- needs_datasets: 用户问「有哪些数据集」「数据集列表」「数据集总量」等

仅输出 JSON，不要其他内容：{"needs_tasks": true或false, "needs_algorithms": true或false, "needs_datasets": true或false}
用户问题："""


def classify_intent(message: str) -> dict[str, bool]:
    """
    根据用户问题判断需要预取哪些业务数据。
    返回 {"needs_tasks": bool, "needs_algorithms": bool, "needs_datasets": bool}
    """
    if not message or not str(message).strip():
        return {"needs_tasks": False, "needs_algorithms": False, "needs_datasets": False}

    try:
        llm = _get_classify_llm()
        resp = llm.invoke(_CLASSIFY_PROMPT + message.strip())
        content = (resp.content or "").strip()
        # 提取 JSON（可能被 markdown 包裹）
        match = re.search(r"\{[^{}]*\}", content, re.DOTALL)
        if match:
            obj = json.loads(match.group())
            return {
                "needs_tasks": bool(obj.get("needs_tasks", False)),
                "needs_algorithms": bool(obj.get("needs_algorithms", False)),
                "needs_datasets": bool(obj.get("needs_datasets", False)),
            }
    except Exception:
        pass
    return {"needs_tasks": False, "needs_algorithms": False, "needs_datasets": False}
