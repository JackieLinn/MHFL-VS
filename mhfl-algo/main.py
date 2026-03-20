"""
FastAPI应用入口文件
"""
import os
import warnings

warnings.filterwarnings("ignore", message="pkg_resources is deprecated", category=UserWarning)

# 设置 tiktoken 缓存目录为 mhfl-algo/cache，避免首次运行时下载 BPE 失败（IncompleteRead）
_cache_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cache")
os.makedirs(_cache_dir, exist_ok=True)
os.environ.setdefault("TIKTOKEN_CACHE_DIR", _cache_dir)

from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import health, resource, train, assistant
import logging

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    # 启动时的初始化操作
    logger.info("FastAPI服务启动中...")
    logger.info("服务地址: http://localhost:8000")
    logger.info("API文档: http://localhost:8000/api/docs")
    yield
    # 关闭时的清理操作
    logger.info("FastAPI服务正在关闭...")


# 创建FastAPI应用
app = FastAPI(
    title="MHFL-VS Algorithm Service",
    description="模型异构联邦学习算法服务",
    version="1.0.0",
    docs_url="/api/docs",
    redoc_url="/api/redoc",
    lifespan=lifespan
)

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(health.router)
app.include_router(resource.router)
app.include_router(train.router)
app.include_router(assistant.router)

if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="127.0.0.1",
        port=8000,
        reload=True,
        log_level="info"
    )
