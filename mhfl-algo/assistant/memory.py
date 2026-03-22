"""
会话摘要记忆（步骤 15）
- generate_summary(): 增量摘要，prev_summary + 新 2 条消息 -> 新摘要，提示词引导 300-350 字，不截断
"""
from langchain_openai import ChatOpenAI

from assistant.prompt import get_summary_prompt
from config.settings import settings

SUMMARY_TARGET_CHARS = "300-350"


def _get_summary_llm() -> ChatOpenAI:
    llm_kwargs: dict = {
        "model": settings.ASSISTANT_MODEL,
        "temperature": 0.2,
        "api_key": settings.OPENAI_API_KEY,
    }
    if settings.OPENAI_API_BASE:
        llm_kwargs["base_url"] = settings.OPENAI_API_BASE
    return ChatOpenAI(**llm_kwargs)


async def generate_summary(prev_summary: str | None, new_messages: list[dict]) -> str:
    """
    生成或更新摘要。若已有摘要，则 prev_summary + new_messages（2 条）-> 新摘要；
    否则仅根据 new_messages 生成。提示词引导 300-350 字左右，不截断。
    """
    if not new_messages:
        return prev_summary or ""

    def _format_msg(m: dict) -> str:
        role = m.get('role', 'user')
        content = (m.get('content') or '')[:300]
        if role == 'assistant':
            feedback = m.get('feedback', '无反馈')
            return f"{role}: {content} [用户反馈：{feedback}]"
        return f"{role}: {content}"

    hist = "\n".join([_format_msg(m) for m in new_messages])
    # 摘要提示词统一从 assistant/knowledge/system 读取
    prompt = get_summary_prompt(prev_summary, hist, SUMMARY_TARGET_CHARS)

    try:
        llm = _get_summary_llm()
        resp = await llm.ainvoke(prompt)
        summary = (resp.content or "").strip()
        return summary
    except Exception:
        return prev_summary or ""
