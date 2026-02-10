"""
训练相关接口路由
"""
from fastapi import APIRouter
from utils.schemas import ApiResponse, TrainStartRequest, TrainStopRequest

router = APIRouter(prefix="/api/train", tags=["训练管理"])


@router.post("/start", response_model=ApiResponse)
async def start_training(request: TrainStartRequest):
    """
    启动训练任务
    
    接收训练参数，检查GPU资源，启动联邦学习训练
    """
    # TODO: 实现训练启动逻辑
    pass


@router.post("/stop/{task_id}", response_model=ApiResponse)
async def stop_training(task_id: int):
    """
    停止训练任务
    
    根据task_id停止正在运行的训练任务
    """
    # TODO: 实现训练停止逻辑
    pass
