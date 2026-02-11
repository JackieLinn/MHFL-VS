"""
资源检查接口路由
"""
from fastapi import APIRouter
from utils.schemas import ApiResponse, SystemResources, GPUInfo, CPUInfo, MemoryInfo
from utils.resource_checker import check_gpu as check_gpu_func, check_cpu, check_memory

router = APIRouter(prefix="/api/resource", tags=["资源管理"])


@router.get("/gpu/check", response_model=ApiResponse[GPUInfo])
async def check_gpu():
    """
    检查GPU资源
    
    返回GPU显存信息（总量、已用、剩余、使用率）
    """
    gpu_dict = check_gpu_func(0)

    if gpu_dict is None:
        return ApiResponse.failure(
            code=503,
            message="GPU不可用或未检测到GPU"
        )

    gpu_info = GPUInfo(**gpu_dict)
    return ApiResponse.success(
        data=gpu_info,
        message="GPU信息获取成功"
    )


@router.get("/system/check", response_model=ApiResponse[SystemResources])
async def get_system_resources():
    """
    获取系统资源信息
    
    返回CPU、内存、GPU的实时使用情况
    用于前端实时监控展示
    """
    # 检查CPU
    cpu_dict = check_cpu()
    cpu_info = CPUInfo(**cpu_dict)

    # 检查内存
    memory_dict = check_memory()
    memory_info = MemoryInfo(**memory_dict)

    # 检查GPU（可选）
    gpu_dict = check_gpu_func(0)
    gpu_info = None
    if gpu_dict is not None:
        gpu_info = GPUInfo(**gpu_dict)

    system_resources = SystemResources(
        cpu=cpu_info,
        memory=memory_info,
        gpu=gpu_info
    )

    return ApiResponse.success(
        data=system_resources,
        message="系统资源信息获取成功"
    )
