"""
下载 tiktoken BPE 文件到 mhfl-algo/cache 目录。
用于解决网络不稳定时 IncompleteRead 导致的 embedding 失败。

用法：在 mhfl-algo 根目录执行
  python scripts/download_tiktoken_bpe.py

若 PowerShell 无输出，可加 -u 禁用缓冲：
  python -u scripts/download_tiktoken_bpe.py

或使用代理加速（国内推荐）：
  # PowerShell:
  $env:HTTPS_PROXY="http://127.0.0.1:7890"
  python scripts/download_tiktoken_bpe.py
"""
import hashlib
import sys
from pathlib import Path

# 确保能导入项目模块
_BASE = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(_BASE))

CACHE_DIR = _BASE / "cache"
BPE_URL = "https://openaipublic.blob.core.windows.net/encodings/cl100k_base.tiktoken"
# tiktoken 缓存文件名 = sha1(URL)
CACHE_FILENAME = hashlib.sha1(BPE_URL.encode()).hexdigest()
CACHE_PATH = CACHE_DIR / CACHE_FILENAME

# 镜像列表：国内 hf-mirror 优先，再试官方 Azure
# Microsoft Phi 等模型仓库内含同款 cl100k_base.tiktoken，国内访问更快
MIRROR_URLS = [
    "https://hf-mirror.com/microsoft/Phi-3-small-128k-instruct/resolve/main/cl100k_base.tiktoken",
    "https://hf-mirror.com/microsoft/phi-4/resolve/main/cl100k_base.tiktoken",
    BPE_URL,
]


def download_with_retries(url: str, path: Path, max_retries: int = 5) -> bool:
    """带重试的分块下载，先写临时文件，成功后再重命名"""
    import time

    try:
        import requests
    except ImportError:
        print("请先安装 requests: pip install requests", flush=True)
        return False

    path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = path.with_suffix(path.suffix + ".tmp")

    for attempt in range(max_retries):
        try:
            print(f"尝试 {attempt + 1}/{max_retries}: {url[:60]}...", flush=True)
            # 超时 600 秒，适应慢速网络；国内优先用 hf-mirror
            resp = requests.get(url, stream=True, timeout=(10, 600))
            resp.raise_for_status()
            total = int(resp.headers.get("content-length", 0))
            downloaded = 0
            with open(tmp_path, "wb") as f:
                for chunk in resp.iter_content(chunk_size=256 * 1024):  # 256KB 大块，减少往返
                    if chunk:
                        f.write(chunk)
                        downloaded += len(chunk)
                        if total and downloaded % (256 * 1024) < len(chunk):
                            pct = 100 * downloaded / total if total else 0
                            print(f"\r  已下载: {downloaded / 1024 / 1024:.2f} MB ({pct:.1f}%)", end="", flush=True)
            print(flush=True)
            if total and downloaded != total:
                print(f"  警告: 期望 {total} 字节，实际 {downloaded} 字节", flush=True)
                tmp_path.unlink(missing_ok=True)
                if attempt < max_retries - 1:
                    time.sleep(3 ** attempt)
                    continue
            path.unlink(missing_ok=True)  # Windows: rename 前需删除已存在的目标
            tmp_path.rename(path)
            return True
        except Exception as e:
            print(f"  失败: {e}", flush=True)
            tmp_path.unlink(missing_ok=True)
            if attempt < max_retries - 1:
                wait = 3 ** attempt
                print(f"  {wait} 秒后重试...", flush=True)
                time.sleep(wait)
    return False


def main():
    print(f"缓存路径: {CACHE_PATH}", flush=True)
    if CACHE_PATH.exists():
        size = CACHE_PATH.stat().st_size
        # cl100k_base 约 1.68 MB
        if size > 1_000_000:
            print(f"缓存已存在: {CACHE_PATH}", flush=True)
            print(f"大小: {size / 1024 / 1024:.2f} MB", flush=True)
            print("若需重新下载，请先删除该文件。", flush=True)
            return 0
        print(f"缓存文件不完整 ({size} bytes)，将重新下载", flush=True)
        CACHE_PATH.unlink(missing_ok=True)

    for url in MIRROR_URLS:
        if download_with_retries(url, CACHE_PATH):
            print(f"成功: {CACHE_PATH}", flush=True)
            print(f"main.py 已配置 TIKTOKEN_CACHE_DIR，无需额外设置。", flush=True)
            return 0
        if url != MIRROR_URLS[-1]:
            print("尝试下一个镜像...", flush=True)

    print("所有下载尝试均失败。可尝试：", flush=True)
    print("  1. 使用代理: set HTTPS_PROXY=http://127.0.0.1:7890", flush=True)
    print("  2. 手动下载并放入 cache 目录:", flush=True)
    print(f"     URL: {BPE_URL}", flush=True)
    print(f"     保存为: {CACHE_PATH}", flush=True)
    return 1


if __name__ == "__main__":
    # 强制无缓冲输出，避免 PowerShell 下无显示
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(line_buffering=True)
    print(">>> 开始下载 tiktoken BPE 到 mhfl-algo/cache ...", flush=True)
    try:
        sys.exit(main())
    except Exception as e:
        print(f"错误: {e}", flush=True)
        import traceback

        traceback.print_exc()
        sys.exit(1)
