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


def build_rag_prompt(query: str, docs: list, context_data: dict | None = None) -> tuple[str, str]:
    """拼装 RAG 的 system 与 user，返回 (system, user)。context_data 为 SpringBoot 预取的业务数据。"""
    parts: list[str] = []
    if docs:
        context = "\n\n".join([
            f"[文档{i + 1}]\n来源: {d.metadata.get('source', '')}\n内容:\n{d.page_content}"
            for i, d in enumerate(docs)
        ])
        parts.append(context)
    if context_data:
        parts.append(_BUSINESS_DATA_DESC)
        parts.append("[业务数据]\n" + json.dumps(context_data, ensure_ascii=False, indent=2))
    full_ctx = "\n\n".join(parts) if parts else "（无检索文档与业务数据）"
    system = """你是 MHFL-VS 平台的智能助手。请仅依据下方检索到的上下文和业务数据回答，不知道则明确说明。
不要编造项目中不存在的接口或字段。回答末尾请列出参考文档。"""
    user = f"上下文：\n{full_ctx}\n\n用户问题：{query}"
    return system, user
