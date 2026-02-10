"""
健康检查接口
用于检查FastAPI服务是否正常运行，SpringBoot可以定期调用此接口来监控服务状态
"""
from fastapi import APIRouter
from utils.schemas import ApiResponse, HealthStatus
from config.redis_conn import get_redis_client
from utils.resource_checker import check_gpu
import logging

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/health", tags=["健康检查"])


def check_redis_connection() -> tuple[bool, str]:
    """
    检查Redis连接是否正常
    
    Returns:
        (是否正常, 错误信息)
    """
    try:
        redis_client = get_redis_client()
        redis_client.ping()
        return True, ""
    except Exception as e:
        logger.error(f"Redis连接检查失败: {e}")
        return False, f"Redis连接失败: {str(e)}"


def check_gpu_available() -> tuple[bool, str]:
    """
    检查GPU是否可用（可选检查，没有GPU不影响服务健康）
    
    Returns:
        (是否可用, 信息)
    """
    try:
        gpu_info = check_gpu(0)
        if gpu_info is None:
            return False, "GPU不可用或未检测到GPU"
        return True, "GPU可用"
    except Exception as e:
        logger.warning(f"GPU检查失败: {e}")
        return False, f"GPU检查失败: {str(e)}"


@router.get("", response_model=ApiResponse[HealthStatus])
async def health_check():
    """
    健康检查接口
    
    用途：
    1. SpringBoot可以定期调用此接口检查FastAPI服务是否正常运行
    2. 负载均衡器可以使用此接口判断服务是否可用
    3. 监控系统可以基于此接口进行服务健康监控
    
    检查项：
    - Redis连接（必需）
    - GPU可用性（可选，不影响健康状态）
    
    返回：
    健康时：
    {
        "code": 200,
        "data": {
            "status": "healthy",
            "service": "mhfl-algo",
            "version": "1.0.0"
        },
        "message": "服务运行正常"
    }
    
    不健康时：
    {
        "code": 503,
        "data": {
            "status": "unhealthy",
            "service": "mhfl-algo",
            "version": "1.0.0"
        },
        "message": "服务异常：Redis连接失败"
    }
    """
    # 检查Redis连接（必需）
    redis_ok, redis_error = check_redis_connection()
    if not redis_ok:
        return ApiResponse(
            code=503,
            data=HealthStatus(
                status="unhealthy",
                service="mhfl-algo",
                version="1.0.0"
            ),
            message=f"服务异常：{redis_error}"
        )

    # 检查GPU（可选，不影响健康状态）
    gpu_ok, gpu_info = check_gpu_available()

    # 如果Redis正常，即使GPU不可用也返回健康
    status = "healthy"
    message = "服务运行正常"
    if not gpu_ok:
        message = f"服务运行正常（{gpu_info}）"

    return ApiResponse.success(
        data=HealthStatus(
            status=status,
            service="mhfl-algo",
            version="1.0.0"
        ),
        message=message
    )
