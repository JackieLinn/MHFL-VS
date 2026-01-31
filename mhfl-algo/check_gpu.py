import pynvml


def check_gpu_memory(device_index=0):
    try:
        # 1. 初始化 NVML 库
        pynvml.nvmlInit()

        # 2. 获取指定 GPU 的句柄 (这里是 GPU 0)
        handle = pynvml.nvmlDeviceGetHandleByIndex(device_index)

        # 3. 获取内存信息
        mem_info = pynvml.nvmlDeviceGetMemoryInfo(handle)

        # 4. 获取显卡名称（可选，方便确认）
        gpu_name = pynvml.nvmlDeviceGetName(handle)

        # 5. 转换单位为 MB
        total_mb = mem_info.total / 1024 / 1024
        used_mb = mem_info.used / 1024 / 1024
        free_mb = mem_info.free / 1024 / 1024

        print(f"=== GPU {device_index}: {gpu_name} ===")
        print(f"总显存: {total_mb:.2f} MB")
        print(f"已用:   {used_mb:.2f} MB")
        print(f"剩余:   {free_mb:.2f} MB")

        return free_mb

    except pynvml.NVMLError as e:
        print(f"读取 GPU 信息失败: {e}")
    finally:
        # 6. 关闭库
        pynvml.nvmlShutdown()


if __name__ == "__main__":
    check_gpu_memory(0)
