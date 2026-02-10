import pynvml
import psutil
from typing import Dict, Optional


def check_gpu(device_index: int = 0) -> Optional[Dict[str, float]]:
    """
    检查GPU显存信息
    
    Args:
        device_index: GPU设备索引，默认为0
        
    Returns:
        包含显存信息的字典，如果失败返回None
        {
            'total': 总显存(GB),
            'used': 已用显存(GB),
            'free': 剩余显存(GB),
            'usage_percent': 显存使用率(%)
        }
    """
    try:
        # 初始化 NVML 库
        pynvml.nvmlInit()

        # 获取指定 GPU 的句柄
        handle = pynvml.nvmlDeviceGetHandleByIndex(device_index)

        # 获取显存信息
        mem_info = pynvml.nvmlDeviceGetMemoryInfo(handle)

        # 获取显卡名称
        gpu_name = pynvml.nvmlDeviceGetName(handle)

        # 转换单位为 GB
        total_gb = mem_info.total / 1024 / 1024 / 1024
        used_gb = mem_info.used / 1024 / 1024 / 1024
        free_gb = mem_info.free / 1024 / 1024 / 1024
        usage_percent = (mem_info.used / mem_info.total) * 100

        print(f"=== GPU {device_index}: {gpu_name.decode('utf-8') if isinstance(gpu_name, bytes) else gpu_name} ===")
        print(f"总显存: {total_gb:.2f} GB")
        print(f"已用:   {used_gb:.2f} GB")
        print(f"剩余:   {free_gb:.2f} GB")
        print(f"使用率: {usage_percent:.2f}%")

        return {
            'total': round(total_gb, 2),
            'used': round(used_gb, 2),
            'free': round(free_gb, 2),
            'usage_percent': round(usage_percent, 2)
        }

    except pynvml.NVMLError as e:
        print(f"读取 GPU 信息失败: {e}")
        return None
    finally:
        # 关闭库
        try:
            pynvml.nvmlShutdown()
        except:
            pass


def check_cpu() -> Dict[str, float]:
    """
    检查CPU使用率
    
    Returns:
        包含CPU信息的字典
        {
            'usage_percent': CPU使用率(%),
            'cores': CPU核心数
        }
    """
    # 获取CPU使用率（1秒间隔）
    cpu_percent = psutil.cpu_percent(interval=1)
    cpu_count = psutil.cpu_count(logical=False)  # 物理核心数
    cpu_count_logical = psutil.cpu_count(logical=True)  # 逻辑核心数

    print(f"=== CPU ===")
    print(f"使用率: {cpu_percent:.2f}%")
    print(f"物理核心数: {cpu_count}")
    print(f"逻辑核心数: {cpu_count_logical}")

    return {
        'usage_percent': round(cpu_percent, 2),
        'cores': cpu_count,
        'cores_logical': cpu_count_logical
    }


def check_memory() -> Dict[str, float]:
    """
    检查内存信息
    
    Returns:
        包含内存信息的字典
        {
            'total': 总内存(GB),
            'used': 已用内存(GB),
            'free': 剩余内存(GB),
            'usage_percent': 内存使用率(%)
        }
    """
    # 获取内存信息
    mem = psutil.virtual_memory()

    # 转换单位为 GB
    total_gb = mem.total / 1024 / 1024 / 1024
    used_gb = mem.used / 1024 / 1024 / 1024
    free_gb = mem.available / 1024 / 1024 / 1024
    usage_percent = mem.percent

    print(f"=== 内存 ===")
    print(f"总内存: {total_gb:.2f} GB")
    print(f"已用:   {used_gb:.2f} GB")
    print(f"剩余:   {free_gb:.2f} GB")
    print(f"使用率: {usage_percent:.2f}%")

    return {
        'total': round(total_gb, 2),
        'used': round(used_gb, 2),
        'free': round(free_gb, 2),
        'usage_percent': round(usage_percent, 2)
    }


def check_resources(device_index: int = 0) -> Dict:
    """
    检查所有系统资源（CPU、内存、GPU）
    
    Args:
        device_index: GPU设备索引，默认为0
        
    Returns:
        包含所有资源信息的字典
        {
            'cpu': {...},
            'memory': {...},
            'gpu': {...} 或 None
        }
    """
    print("\n" + "=" * 50)
    print("系统资源检查")
    print("=" * 50 + "\n")

    cpu_info = check_cpu()
    print()

    memory_info = check_memory()
    print()

    gpu_info = check_gpu(device_index)
    print()

    print("=" * 50)

    return {
        'cpu': cpu_info,
        'memory': memory_info,
        'gpu': gpu_info
    }


if __name__ == "__main__":
    check_resources(0)
