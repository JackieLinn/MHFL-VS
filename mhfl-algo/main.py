"""
FastAPI应用入口文件
"""
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import health, resource, train
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

if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="127.0.0.1",
        port=8000,
        reload=True,
        log_level="info"
    )
