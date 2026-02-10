"""
资源检查接口路由
"""
from fastapi import APIRouter
from utils.schemas import ApiResponse, SystemResources, GPUInfo

router = APIRouter(prefix="/api/resource", tags=["资源管理"])


@router.get("/gpu/check", response_model=ApiResponse[GPUInfo])
async def check_gpu():
    """
    检查GPU资源
    
    返回GPU显存信息（总量、已用、剩余、使用率）
    """
    # TODO: 实现GPU检查逻辑
    pass


@router.get("/system/resources", response_model=ApiResponse[SystemResources])
async def get_system_resources():
    """
    获取系统资源信息
    
    返回CPU、内存、GPU的实时使用情况
    用于前端实时监控展示
    """
    # TODO: 实现系统资源检查逻辑
    pass
