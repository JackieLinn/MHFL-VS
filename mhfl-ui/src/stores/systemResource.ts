/**
 * 系统资源历史数据：供 Header 轮询写入，Dashboard 实时折线图消费。
 * 不发起请求，仅保存最近 N 个点的 CPU / 内存 / GPU 使用率。
 */
import {ref, type Ref} from 'vue'
import type {SystemResources} from '@/api/home'

const MAX_HISTORY = 60 // 3s 轮询 → 约 3 分钟

const cpuUsageHistory = ref<number[]>([])
const memoryUsageHistory = ref<number[]>([])
const gpuUsageHistory = ref<number[]>([])

const pushFromResponse = (data: SystemResources) => {
    const push = (arrRef: Ref<number[]>, value: number) => {
        arrRef.value = [...arrRef.value, value].slice(-MAX_HISTORY)
    }
    push(cpuUsageHistory, data.cpu.usagePercent ?? 0)
    push(memoryUsageHistory, data.memory.usagePercent ?? 0)
    push(gpuUsageHistory, data.gpu?.usagePercent ?? 0)
}

export const useSystemResourceStore = () => {
    return {
        cpuUsageHistory,
        memoryUsageHistory,
        gpuUsageHistory,
        pushFromResponse,
    }
}
