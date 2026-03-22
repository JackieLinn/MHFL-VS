"""
Prompt 拼装与系统提示词管理
- 提示词模板统一放在 assistant/knowledge/system/*.md
- 本文件负责读取与渲染模板，供 RAG/分类/改写/摘要复用
"""
import json
from functools import lru_cache
from pathlib import Path

# 系统提示词目录（UTF-8）
_SYSTEM_PROMPT_DIR = Path(__file__).resolve().parent / "knowledge" / "system"


@lru_cache(maxsize=None)
def _read_system_prompt(filename: str) -> str:
    """读取 system 目录下的提示词模板，使用缓存避免重复 IO。"""
    path = _SYSTEM_PROMPT_DIR / filename
    return path.read_text(encoding="utf-8")


def _render_prompt(template: str, variables: dict[str, str | int]) -> str:
    """
    轻量模板渲染：仅替换 {key} 占位符，避免引入额外 loader 文件。
    注意：只会替换显式传入的 key，不影响 JSON 花括号内容。
    """
    rendered = template
    for key, value in variables.items():
        rendered = rendered.replace(f"{{{key}}}", str(value))
    return rendered


def get_business_data_desc_prompt() -> str:
    """业务数据说明提示词。"""
    return _read_system_prompt("business_data_desc.md")


def get_rag_system_prompt() -> str:
    """RAG system 提示词。"""
    return _read_system_prompt("rag_system.md")


def get_rag_user_prompt(full_ctx: str, query: str) -> str:
    """RAG user 提示词。"""
    template = _read_system_prompt("rag_user_prompt.md")
    return _render_prompt(template, {"full_ctx": full_ctx, "query": query})


def get_classify_prompt(message: str) -> str:
    """意图分类提示词。"""
    template = _read_system_prompt("classify_prompt.md")
    return template + str(message).strip()


def get_query_rewrite_prompt(question: str, n: int) -> str:
    """多查询改写提示词。"""
    template = _read_system_prompt("query_rewrite_prompt.md")
    return _render_prompt(template, {"n": n, "question": question})


def get_summary_prompt(prev_summary: str | None, hist: str, summary_target_chars: str) -> str:
    """摘要提示词（区分首轮摘要与增量摘要）。"""
    if prev_summary and prev_summary.strip():
        template = _read_system_prompt("summary_update_prompt.md")
        return _render_prompt(
            template,
            {
                "SUMMARY_TARGET_CHARS": summary_target_chars,
                "prev_summary": prev_summary.strip(),
                "hist": hist,
            },
        )
    template = _read_system_prompt("summary_init_prompt.md")
    return _render_prompt(
        template,
        {
            "SUMMARY_TARGET_CHARS": summary_target_chars,
            "hist": hist,
        },
    )


def build_rag_prompt(
        query: str,
        docs: list,
        context_data: dict | None = None,
        memory_context: str | None = None,
) -> tuple[str, str]:
    """拼装 RAG 的 system 与 user，返回 (system, user)。memory_context 为历史摘要+最近对话（步骤 15）。"""
    parts: list[str] = []
    if memory_context and memory_context.strip():
        parts.append(f"[历史记忆]\n{memory_context.strip()}\n")
    if docs:
        context = "\n\n".join([
            f"[文档{i + 1}]\n来源: {d.metadata.get('source', '')}\n内容:\n{d.page_content}"
            for i, d in enumerate(docs)
        ])
        parts.append(context)
    if context_data:
        parts.append(get_business_data_desc_prompt())
        parts.append("[业务数据]\n" + json.dumps(context_data, ensure_ascii=False, indent=2))
    full_ctx = "\n\n".join(parts) if parts else "（无历史记忆、检索文档与业务数据）"
    system = get_rag_system_prompt()
    user = get_rag_user_prompt(full_ctx, query)
    return system, user
