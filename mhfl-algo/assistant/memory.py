"""
会话摘要记忆（步骤 15）
- generate_summary(): 增量摘要，prev_summary + 新 2 条消息 -> 新摘要，提示词引导 300-350 字，不截断
"""
from langchain_openai import ChatOpenAI

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

    if prev_summary and prev_summary.strip():
        prompt = f"""请将以下「已有摘要」与「新增对话」合并为一条新摘要。
要求：保留用户关注点、已讨论模块、关键结论、未解决问题；每条助手回复带有 [用户反馈：无反馈/点赞/点踩]，请在摘要中简要提及用户对哪些回答满意或不满；字数尽可能在 {SUMMARY_TARGET_CHARS} 字左右，不要超出太多。

已有摘要：
{prev_summary.strip()}

新增对话：
{hist}

新摘要："""
    else:
        prompt = f"""请将以下对话整理成简短摘要。
要求：保留用户关注点、已讨论模块、关键结论、未解决问题；每条助手回复带有 [用户反馈：无反馈/点赞/点踩]，请在摘要中简要提及用户对哪些回答满意或不满；字数尽可能在 {SUMMARY_TARGET_CHARS} 字左右，不要超出太多。

对话记录：
{hist}

摘要："""

    try:
        llm = _get_summary_llm()
        resp = await llm.ainvoke(prompt)
        summary = (resp.content or "").strip()
        return summary
    except Exception:
        return prev_summary or ""
