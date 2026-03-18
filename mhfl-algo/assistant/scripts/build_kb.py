"""
知识库构建脚本（步骤 3）
将 assistant/knowledge/raw/ 下 .md 分块并写入 Chroma
执行：python assistant/scripts/build_kb.py（在 mhfl-algo 根目录）
"""
# import sys
# from pathlib import Path
# sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent))
# from langchain_text_splitters import MarkdownHeaderTextSplitter, RecursiveCharacterTextSplitter
# from langchain_chroma import Chroma
# from langchain_openai import OpenAIEmbeddings
# from config.settings import settings

# TODO: 步骤 3 实现 build_kb(clear_first: bool = True):
# - 递归扫描 raw/ 下 .md
# - MarkdownHeaderTextSplitter + RecursiveCharacterTextSplitter 分块
# - 写入 Chroma 持久化目录
# - 输出文件数、chunk 数

if __name__ == "__main__":
    pass  # build_kb(clear_first=True)
