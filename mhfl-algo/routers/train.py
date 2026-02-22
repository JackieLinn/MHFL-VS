"""
训练相关接口路由
"""
import logging
import threading

from fastapi import APIRouter

from fl_core.algorithms import get_trainer_class
from utils.redis_publisher import RedisPublisher
from utils.schemas import ApiResponse, TrainStartRequest
from utils.resource_checker import check_gpu

router = APIRouter(prefix="/api/train", tags=["训练管理"])
logger = logging.getLogger(__name__)

# 存储正在运行的训练任务 {task_id: {"thread": Thread, "stop_event": Event}}
running_trainers: dict[int, dict] = {}


def _normalize_data_name(name: str) -> str:
    """
    将数据库/前端数据集名称规范为训练器内部使用的名称。
    数据库命名：CIFAR100、Tiny-Imagenet；训练器使用：cifar100、tiny-imagenet。
    """
    if not name or not name.strip():
        return name
    s = name.strip()
    # 数据库命名：CIFAR100、Tiny-Imagenet
    if s.upper() == "CIFAR100":
        return "cifar100"
    if s.lower() in ("tiny-imagenet", "tiny_imagenet", "tinyimagenet") or s == "Tiny-Imagenet":
        return "tiny-imagenet"
    return name


def _create_step_callback(task_id: int, publisher: RedisPublisher):
    """创建轮次回调函数"""

    def step_callback(tid: int, step: int, metrics: dict):
        publisher.publish_round(task_id, step, metrics)

    return step_callback


def _create_client_callback(task_id: int, publisher: RedisPublisher):
    """创建客户端回调函数"""

    def client_callback(tid: int, step: int, client_id: int, metrics: dict):
        publisher.publish_client(task_id, step, client_id, metrics)

    return client_callback


def _run_training(request: TrainStartRequest, stop_event: threading.Event) -> None:
    """在后台线程中运行训练；stop_event 被 set 时训练循环会尽快退出。"""
    publisher = RedisPublisher()
    data_name = _normalize_data_name(request.data_name)

    try:
        publisher.publish_status(request.task_id, "IN_PROGRESS", "训练已启动")

        step_callback = _create_step_callback(request.task_id, publisher)
        client_callback = _create_client_callback(request.task_id, publisher)

        trainer_class = get_trainer_class(request.algorithm_name)
        if not trainer_class:
            raise ValueError(f"未知算法: {request.algorithm_name}")

        trainer = trainer_class(
            tid=request.task_id,
            data_name=data_name,
            algorithm_name=request.algorithm_name,
            num_nodes=request.num_nodes,
            fraction=request.fraction,
            classes_per_node=request.classes_per_node,
            low_prob=request.low_prob,
            num_steps=request.num_steps,
            epochs=request.epochs,
            step_callback=step_callback,
            client_callback=client_callback,
            stop_event=stop_event,
        )

        trainer.train()

        if stop_event.is_set():
            publisher.publish_status(request.task_id, "CANCELLED", "用户停止训练")
        else:
            publisher.publish_status(request.task_id, "SUCCESS", "训练完成")

    except Exception as e:
        error_msg = str(e)
        publisher.publish_status(request.task_id, "FAILED", f"训练失败: {error_msg}")
        logger.exception("Training failed for task %s: %s", request.task_id, e)
    finally:
        if request.task_id in running_trainers:
            del running_trainers[request.task_id]


@router.post("/start", response_model=ApiResponse)
async def start_training(request: TrainStartRequest):
    """
    启动训练任务

    接收训练参数，检查 GPU 资源，在后台启动联邦学习训练；
    Round/Client 指标通过 Redis 发布，状态通过 task:experiment:status:{task_id} 发布。
    """
    if request.task_id in running_trainers:
        return ApiResponse.failure(400, f"任务 {request.task_id} 正在运行中")

    gpu_info = check_gpu(0)
    if gpu_info is None:
        return ApiResponse.failure(503, "GPU 不可用或未检测到 GPU")

    data_name = _normalize_data_name(request.data_name)
    threshold = 3.0 if data_name == "cifar100" else 5.5
    if data_name not in ("cifar100", "tiny-imagenet"):
        return ApiResponse.failure(400, "不支持的数据集，仅支持 CIFAR100 / Tiny-Imagenet")
    if gpu_info["free"] < threshold:
        return ApiResponse.failure(
            400,
            f"GPU 显存不足，需要至少 {threshold}GB，当前可用 {gpu_info['free']:.2f}GB",
        )

    stop_event = threading.Event()
    thread = threading.Thread(target=_run_training, args=(request, stop_event), daemon=True)
    thread.start()
    running_trainers[request.task_id] = {"thread": thread, "stop_event": stop_event}

    return ApiResponse.success(message=f"训练任务 {request.task_id} 已启动")


@router.post("/stop/{task_id}", response_model=ApiResponse)
async def stop_training(task_id: int):
    """
    停止训练任务：发送停止信号，训练循环会在当前轮或当前客户端结束后退出，
    并发布 CANCELLED 状态到 Redis。
    """
    if task_id not in running_trainers:
        return ApiResponse.failure(404, f"任务 {task_id} 未在运行")

    running_trainers[task_id]["stop_event"].set()
    del running_trainers[task_id]
    return ApiResponse.success(message=f"任务 {task_id} 已发送停止信号，训练将尽快结束")
