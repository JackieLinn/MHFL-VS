"""
RAG 问答链
- answer(): 检索 -> 拼 prompt -> 调 LLM -> 返回 content + sources（步骤 6）
"""
# from assistant.retriever import retrieve
# from assistant.prompt import build_rag_prompt
# from langchain_openai import ChatOpenAI

# TODO: 步骤 6 实现 answer()
# def answer(query: str) -> tuple[str, list[str]]:
#     docs = retrieve(query)
#     if not docs:
#         return "未找到相关文档，请换一种问法或联系管理员。", []
#     system, user = build_rag_prompt(query, docs)
#     llm = ChatOpenAI(model=settings.ASSISTANT_MODEL, temperature=0)
#     resp = llm.invoke([{"role": "system", "content": system}, {"role": "user", "content": user}])
#     sources = list({d.metadata.get("source", "") for d in docs})
#     return resp.content, sources
