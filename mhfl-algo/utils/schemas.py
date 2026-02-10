"""
FastAPI响应模型定义
与SpringBoot的ApiResponse格式保持一致，便于统一处理
"""
from typing import Generic, TypeVar, Optional
from pydantic import BaseModel

T = TypeVar('T')


class ApiResponse(BaseModel, Generic[T]):
    """
    统一API响应格式
    与SpringBoot的ApiResponse保持一致：
    {
        "code": 200,
        "data": {...},
        "message": "请求成功"
    }
    """
    code: int
    data: Optional[T] = None
    message: str

    @classmethod
    def success(cls, data: Optional[T] = None, message: str = "请求成功") -> "ApiResponse[T]":
        """成功响应"""
        return cls(code=200, data=data, message=message)

    @classmethod
    def failure(cls, code: int, message: str) -> "ApiResponse[None]":
        """失败响应"""
        return cls(code=code, data=None, message=message)

    @classmethod
    def unauthorized(cls, message: str = "未授权") -> "ApiResponse[None]":
        """未授权响应"""
        return cls.failure(401, message)

    @classmethod
    def forbidden(cls, message: str = "禁止访问") -> "ApiResponse[None]":
        """禁止访问响应"""
        return cls.failure(403, message)

    @classmethod
    def bad_request(cls, message: str = "请求参数有误") -> "ApiResponse[None]":
        """请求参数错误"""
        return cls.failure(400, message)

    @classmethod
    def internal_error(cls, message: str = "服务器内部错误") -> "ApiResponse[None]":
        """服务器内部错误"""
        return cls.failure(500, message)


# 训练相关模型
class TrainStartRequest(BaseModel):
    """训练任务启动请求"""
    task_id: int
    data_name: str
    algorithm_name: str
    num_nodes: int
    fraction: float
    classes_per_node: int
    low_prob: float
    num_steps: int
    epochs: int


class TrainStopRequest(BaseModel):
    """训练任务停止请求"""
    task_id: int


# 资源检查相关模型
class GPUInfo(BaseModel):
    """GPU信息"""
    total: float  # GB
    used: float  # GB
    free: float  # GB
    usage_percent: float  # %


class CPUInfo(BaseModel):
    """CPU信息"""
    usage_percent: float  # %
    cores: int
    cores_logical: int


class MemoryInfo(BaseModel):
    """内存信息"""
    total: float  # GB
    used: float  # GB
    free: float  # GB
    usage_percent: float  # %


class SystemResources(BaseModel):
    """系统资源信息"""
    cpu: CPUInfo
    memory: MemoryInfo
    gpu: Optional[GPUInfo] = None


# 健康检查模型
class HealthStatus(BaseModel):
    """健康检查状态"""
    status: str  # "healthy" | "unhealthy"
    service: str  # 服务名称
    version: Optional[str] = None
