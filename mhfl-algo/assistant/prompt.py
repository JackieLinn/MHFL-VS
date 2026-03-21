"""
Prompt 拼装
- build_rag_prompt(): 知识库 RAG 的 system + user，支持 context_data（步骤 6 / 13）
"""
import json

_BUSINESS_DATA_DESC = """[业务数据说明]
- 任务(task)：id、数据集名、算法名、状态、创建时间、训练参数等；可回答「我的任务」「最近任务」「任务总量」等
- 算法(algorithm)：id、算法名；可回答「有哪些算法」「平台算法」等
- 数据集(dataset)：id、数据集名；可回答「有哪些数据集」等
"""


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
        parts.append(_BUSINESS_DATA_DESC)
        parts.append("[业务数据]\n" + json.dumps(context_data, ensure_ascii=False, indent=2))
    full_ctx = "\n\n".join(parts) if parts else "（无历史记忆、检索文档与业务数据）"
    system = """你是 MHFL-VS 平台的智能助手。请仅依据下方检索到的上下文和业务数据回答，不知道则明确说明。
不要编造项目中不存在的接口或字段。
【重要】参考来源会在回答下方以标签形式单独展示。禁止在回答正文中写「参考文档」「参考来源」「来源：」等字样，也不要列出任何文档名。直接给出答案即可。
历史记忆中的助手回复带有 [用户反馈：无反馈/点赞/点踩]，请参考用户对以往回答的反馈来调整回答风格，用户点踩过的内容可避免重复。"""
    user = f"上下文：\n{full_ctx}\n\n用户问题：{query}"
    return system, user
