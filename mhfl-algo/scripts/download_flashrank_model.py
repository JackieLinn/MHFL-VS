"""
预下载 FlashRank 模型到 mhfl-algo/cache 目录。
用于步骤 11 重排序，避免首次请求时下载超时。

模型：ms-marco-MultiBERT-L-12（多语言，约 150MB）

用法：在 mhfl-algo 根目录执行
  python scripts/download_flashrank_model.py

国内网络可设置镜像加速：
  # PowerShell:
  $env:HF_ENDPOINT="https://hf-mirror.com"
  python scripts/download_flashrank_model.py
"""
import sys
from pathlib import Path

# 确保能导入项目模块
_BASE = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(_BASE))

CACHE_DIR = _BASE / "cache"
MODEL_NAME = "ms-marco-MultiBERT-L-12"


def main() -> int:
    print(f">>> 预下载 FlashRank 模型 {MODEL_NAME} 到 mhfl-algo/cache ...", flush=True)
    print(f"缓存目录: {CACHE_DIR}", flush=True)

    try:
        from flashrank import Ranker
    except ImportError as e:
        print(f"请先安装 FlashRank: pip install flashrank", flush=True)
        print(f"错误: {e}", flush=True)
        return 1

    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    model_dir = CACHE_DIR / MODEL_NAME

    if model_dir.exists():
        onnx_files = list(model_dir.glob("*.onnx"))
        if onnx_files:
            print(f"模型已存在: {model_dir}", flush=True)
            print(f"若需重新下载，请删除该目录后重试。", flush=True)
            return 0

    print(f"正在下载（约 150MB，请耐心等待）...", flush=True)
    ranker = Ranker(model_name=MODEL_NAME, cache_dir=str(CACHE_DIR))

    # 跑一次推理，确保模型完整加载
    from flashrank import RerankRequest
    ranker.rerank(RerankRequest(query="test", passages=[{"text": "test"}]))

    print(f"成功: 模型已缓存到 {CACHE_DIR / MODEL_NAME}", flush=True)
    print("步骤 11 的 rerank 将使用此缓存，无需启动时下载。", flush=True)
    return 0


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(line_buffering=True)
    try:
        sys.exit(main())
    except Exception as e:
        print(f"错误: {e}", flush=True)
        import traceback

        traceback.print_exc()
        sys.exit(1)
