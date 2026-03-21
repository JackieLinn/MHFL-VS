import {computed, ref, type Ref} from 'vue'
import {getSystemHealth, type DashboardSystemHealthVO} from '@/api/dashboard'

const TOTAL = 4
const POLL_INTERVAL_MS = 10_000

const health: Ref<DashboardSystemHealthVO | null> = ref(null)
let pollTimer: ReturnType<typeof setInterval> | null = null

const fetchHealth = () => {
    getSystemHealth(
        (data) => {
            health.value = data
        },
        () => {
            health.value = {
                mysql: false,
                redis: false,
                rabbitmq: false,
                fastapi: false,
            }
        }
    )
}

const ensurePolling = () => {
    if (pollTimer) return
    fetchHealth()
    pollTimer = setInterval(fetchHealth, POLL_INTERVAL_MS)
}

/**
 * 系统健康状态（单例，与 Dashboard 系统健康卡片共享数据）。
 * 4/4 时表示全部健康，否则为异常。
 */
export const useSystemHealth = () => {
    const healthyCount = computed(() => {
        if (!health.value) return 0
        return [health.value.mysql, health.value.redis, health.value.rabbitmq, health.value.fastapi].filter(Boolean).length
    })

    const isAllHealthy = computed(() => healthyCount.value === TOTAL)

    const isHealthy = (key: keyof DashboardSystemHealthVO) => health.value?.[key] ?? false

    return {
        health,
        healthyCount,
        isAllHealthy,
        isHealthy,
        total: TOTAL,
        ensurePolling,
    }
}
