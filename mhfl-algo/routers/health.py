"""
健康检查接口
用于检查FastAPI服务是否正常运行，SpringBoot可以定期调用此接口来监控服务状态
"""
from fastapi import APIRouter
from utils.schemas import ApiResponse, HealthStatus

router = APIRouter(prefix="/api/health", tags=["健康检查"])


@router.get("", response_model=ApiResponse[HealthStatus])
async def health_check():
    """
    健康检查接口
    
    用途：
    1. SpringBoot可以定期调用此接口检查FastAPI服务是否正常运行
    2. 负载均衡器可以使用此接口判断服务是否可用
    3. 监控系统可以基于此接口进行服务健康监控
    
    返回：
    {
        "code": 200,
        "data": {
            "status": "healthy",
            "service": "mhfl-algo",
            "version": "1.0.0"
        },
        "message": "请求成功"
    }
    """
    return ApiResponse.success(
        data=HealthStatus(
            status="healthy",
            service="mhfl-algo",
            version="1.0.0"
        ),
        message="服务运行正常"
    )
