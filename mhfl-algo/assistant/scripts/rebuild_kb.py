"""
知识库增量更新脚本（步骤 4）
支持 --all 全量重建、--file <path> 单文件重建
执行：python assistant/scripts/rebuild_kb.py --all
      python assistant/scripts/rebuild_kb.py --file assistant/knowledge/raw/faq.md
"""
import argparse
import sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(BASE_DIR))

from tqdm import tqdm
from langchain_text_splitters import MarkdownHeaderTextSplitter, RecursiveCharacterTextSplitter
from langchain_chroma import Chroma
from langchain_openai import OpenAIEmbeddings
from config.settings import settings

BATCH_SIZE = 10


def _get_embeddings() -> OpenAIEmbeddings:
    embed_kwargs: dict = {"model": settings.ASSISTANT_EMBEDDING_MODEL, "api_key": settings.OPENAI_API_KEY}
    if settings.OPENAI_API_BASE:
        embed_kwargs["base_url"] = settings.OPENAI_API_BASE
    return OpenAIEmbeddings(**embed_kwargs)


def _split_and_add(vectorstore: Chroma, md_path: Path, kb_dir: Path) -> int:
    """单文件分块并写入 Chroma，返回 chunk 数"""
    text = md_path.read_text(encoding="utf-8")
    md_splitter = MarkdownHeaderTextSplitter(
        headers_to_split_on=[("#", "h1"), ("##", "h2"), ("###", "h3")]
    )
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=getattr(settings, "ASSISTANT_CHUNK_SIZE", 400),
        chunk_overlap=getattr(settings, "ASSISTANT_CHUNK_OVERLAP", 50),
    )
    splits = md_splitter.split_text(text)
    docs, ids = [], []
    for s in splits:
        for d in splitter.split_documents([s]):
            doc_id = f"{md_path.stem}_{len(ids):04d}"
            d.metadata.update({
                "source": md_path.name,
                "file_path": str(md_path.relative_to(kb_dir)),
                "doc_id": md_path.stem,
                "chunk_id": doc_id,
            })
            docs.append(d)
            ids.append(doc_id)
    if docs:
        for i in tqdm(range(0, len(docs), BATCH_SIZE), desc="Embedding", unit="batch"):
            batch_docs = docs[i: i + BATCH_SIZE]
            batch_ids = ids[i: i + BATCH_SIZE]
            vectorstore.add_documents(batch_docs, ids=batch_ids)
    return len(docs)


def rebuild_file(file_path: str) -> None:
    """单文件重建：按 doc_id 删除旧 chunk 再写入"""
    chroma_dir = BASE_DIR / settings.ASSISTANT_CHROMA_DIR
    kb_dir = BASE_DIR / settings.ASSISTANT_KB_DIR
    md_path = Path(file_path)
    if not md_path.exists():
        md_path = kb_dir / file_path
    if not md_path.exists():
        print(f"File not found: {file_path}")
        return
    if not md_path.suffix == ".md":
        print(f"Not a markdown file: {md_path}")
        return

    doc_id = md_path.stem
    embeddings = _get_embeddings()
    vectorstore = Chroma(
        collection_name="mhfl_kb",
        embedding_function=embeddings,
        persist_directory=str(chroma_dir),
    )
    try:
        vectorstore._collection.delete(where={"doc_id": doc_id})
    except Exception:
        pass
    n = _split_and_add(vectorstore, md_path, kb_dir)
    print(f"Rebuilt {doc_id}: {n} chunks")


def build_all(clear_first: bool = True) -> None:
    """全量重建，复用 build_kb"""
    from assistant.scripts.build_kb import build_kb
    build_kb(clear_first=clear_first)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="知识库增量更新")
    parser.add_argument("--all", action="store_true", help="全量重建")
    parser.add_argument("--file", type=str, help="单文件重建，路径可为绝对或相对 raw 的路径")
    args = parser.parse_args()
    if args.all:
        build_all(clear_first=True)
    elif args.file:
        rebuild_file(args.file)
    else:
        print("Use --all or --file <path>")
