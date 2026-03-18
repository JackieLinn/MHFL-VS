"""
Prompt 拼装
- build_rag_prompt(): 知识库 RAG 的 system + user（步骤 6）
- build_prompt_with_context(): 含 context_data 的 prompt（步骤 13）
"""


def build_rag_prompt(query: str, docs: list) -> tuple[str, str]:
    """拼装 RAG 的 system 与 user，返回 (system, user)"""
    context = "\n\n".join([
        f"[文档{i + 1}]\n来源: {d.metadata.get('source', '')}\n内容:\n{d.page_content}"
        for i, d in enumerate(docs)
    ])
    system = """你是 MHFL-VS 平台的智能助手。请仅依据下方检索到的上下文回答，不知道则明确说明。
不要编造项目中不存在的接口或字段。回答末尾请列出参考文档。"""
    user = f"上下文：\n{context}\n\n用户问题：{query}"
    return system, user
