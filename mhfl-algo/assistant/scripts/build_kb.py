"""
知识库构建脚本（步骤 3）
将 assistant/knowledge/raw/ 下 .md 分块并写入 Chroma
执行：python assistant/scripts/build_kb.py（在 mhfl-algo 根目录）
"""
import sys
import shutil
from pathlib import Path

# 确保能导入项目模块
BASE_DIR = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(BASE_DIR))

from tqdm import tqdm
from langchain_text_splitters import MarkdownHeaderTextSplitter, RecursiveCharacterTextSplitter
from langchain_chroma import Chroma
from langchain_openai import OpenAIEmbeddings
from config.settings import settings


def build_kb(clear_first: bool = True) -> tuple[int, int, list[Path]]:
    """
    构建知识库：递归扫描 raw 下 .md -> 分块 -> 写入 Chroma
    :return: (成功文件数, chunk 总数, 失败文件列表)
    """
    kb_dir = BASE_DIR / settings.ASSISTANT_KB_DIR
    chroma_dir = BASE_DIR / settings.ASSISTANT_CHROMA_DIR
    kb_dir.mkdir(parents=True, exist_ok=True)
    chroma_dir.mkdir(parents=True, exist_ok=True)

    if clear_first and chroma_dir.exists():
        shutil.rmtree(chroma_dir)
        chroma_dir.mkdir(parents=True, exist_ok=True)

    # 显式传入 api_key，避免依赖 os.environ
    embed_kwargs: dict = {"model": settings.ASSISTANT_EMBEDDING_MODEL, "api_key": settings.OPENAI_API_KEY}
    if settings.OPENAI_API_BASE:
        embed_kwargs["base_url"] = settings.OPENAI_API_BASE
    embeddings = OpenAIEmbeddings(**embed_kwargs)

    splitter = RecursiveCharacterTextSplitter(
        chunk_size=getattr(settings, "ASSISTANT_CHUNK_SIZE", 400),
        chunk_overlap=getattr(settings, "ASSISTANT_CHUNK_OVERLAP", 50),
    )
    md_splitter = MarkdownHeaderTextSplitter(
        headers_to_split_on=[("#", "h1"), ("##", "h2"), ("###", "h3")]
    )

    vectorstore = Chroma(
        collection_name="mhfl_kb",
        embedding_function=embeddings,
        persist_directory=str(chroma_dir),
    )

    total_chunks = 0
    failed_files: list[Path] = []
    md_files = list(kb_dir.rglob("*.md"))

    for md_path in tqdm(md_files, desc="Building KB", unit="file"):
        try:
            text = md_path.read_text(encoding="utf-8")
            splits = md_splitter.split_text(text)
            docs, ids, metadatas = [], [], []
            for s in splits:
                sub = splitter.split_documents([s])
                for d in sub:
                    doc_id = f"{md_path.stem}_{len(ids):04d}"
                    d.metadata.update({
                        "source": md_path.name,
                        "file_path": str(md_path.relative_to(kb_dir)),
                        "doc_id": md_path.stem,
                        "chunk_id": doc_id,
                    })
                    docs.append(d)
                    ids.append(doc_id)
                    metadatas.append(d.metadata)
            if docs:
                vectorstore.add_documents(docs, ids=ids)
                total_chunks += len(docs)
        except Exception as e:
            tqdm.write(f"Failed: {md_path}: {e}")
            failed_files.append(md_path)

    success_count = len(md_files) - len(failed_files)
    print(f"Built: {total_chunks} chunks from {success_count} files")
    if failed_files:
        print(f"Failed: {[str(p.relative_to(kb_dir)) for p in failed_files]}")

    return success_count, total_chunks, failed_files


if __name__ == "__main__":
    build_kb(clear_first=True)
