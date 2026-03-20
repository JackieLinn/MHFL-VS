"""
会话摘要记忆（步骤 15）
- generate_summary(): 增量摘要，prev_summary + 新 2 条消息 -> 新摘要，限制长度
"""
from langchain_openai import ChatOpenAI

from config.settings import settings

SUMMARY_MAX_CHARS = 300


def _get_summary_llm() -> ChatOpenAI:
    llm_kwargs: dict = {
        "model": settings.ASSISTANT_MODEL,
        "temperature": 0.2,
        "api_key": settings.OPENAI_API_KEY,
    }
    if settings.OPENAI_API_BASE:
        llm_kwargs["base_url"] = settings.OPENAI_API_BASE
    return ChatOpenAI(**llm_kwargs)


def generate_summary(prev_summary: str | None, new_messages: list[dict]) -> str:
    """
    生成或更新摘要。若已有摘要，则 prev_summary + new_messages（2 条）-> 新摘要；
    否则仅根据 new_messages 生成。输出限制在 SUMMARY_MAX_CHARS 字以内。
    """
    if not new_messages:
        return prev_summary or ""

    hist = "\n".join([
        f"{m.get('role', 'user')}: {(m.get('content') or '')[:300]}"
        for m in new_messages
    ])

    if prev_summary and prev_summary.strip():
        prompt = f"""请将以下「已有摘要」与「新增对话」合并为一条新摘要。
要求：保留用户关注点、已讨论模块、关键结论、未解决问题；严格控制在 {SUMMARY_MAX_CHARS} 字以内。

已有摘要：
{prev_summary.strip()}

新增对话：
{hist}

新摘要（{SUMMARY_MAX_CHARS} 字以内）："""
    else:
        prompt = f"""请将以下对话整理成简短摘要。
要求：保留用户关注点、已讨论模块、关键结论、未解决问题；严格控制在 {SUMMARY_MAX_CHARS} 字以内。

对话记录：
{hist}

摘要（{SUMMARY_MAX_CHARS} 字以内）："""

    try:
        llm = _get_summary_llm()
        resp = llm.invoke(prompt)
        summary = (resp.content or "").strip()
        if len(summary) > SUMMARY_MAX_CHARS:
            summary = summary[:SUMMARY_MAX_CHARS]
        return summary
    except Exception:
        return prev_summary or ""
