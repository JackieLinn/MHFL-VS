<script setup lang="ts">
import {ref, onMounted, onBeforeUnmount} from 'vue'
import mysqlIcon from '@/assets/middleware/mysql.svg'
import redisIcon from '@/assets/middleware/redis.svg'
import rabbitmqIcon from '@/assets/middleware/rabbitmq.svg'
import fastapiIcon from '@/assets/middleware/fastapi.svg'
import {getSystemHealth, type DashboardSystemHealthVO} from '@/api/dashboard'

const systemHealthItems = [
  {key: 'mysql' as const, labelKey: 'pages.dashboard.healthMysql', icon: mysqlIcon},
  {key: 'redis' as const, labelKey: 'pages.dashboard.healthRedis', icon: redisIcon},
  {key: 'rabbitmq' as const, labelKey: 'pages.dashboard.healthRabbitMQ', icon: rabbitmqIcon},
  {key: 'fastapi' as const, labelKey: 'pages.dashboard.healthFastAPI', icon: fastapiIcon},
]

const health = ref<DashboardSystemHealthVO | null>(null)
const POLL_INTERVAL_MS = 10_000
let pollTimer: ReturnType<typeof setInterval> | null = null

const fetchHealth = () => {
  getSystemHealth((data) => {
    health.value = data
  }, () => {
    health.value = {mysql: false, redis: false, rabbitmq: false, fastapi: false}
  })
}

const healthyCount = () => {
  if (!health.value) return 0
  return [health.value.mysql, health.value.redis, health.value.rabbitmq, health.value.fastapi].filter(Boolean).length
}

const isHealthy = (key: keyof DashboardSystemHealthVO) => health.value?.[key] ?? false

onMounted(() => {
  fetchHealth()
  pollTimer = setInterval(fetchHealth, POLL_INTERVAL_MS)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <div class="resource-card tech-card flex flex-col min-h-0 py-3.5 px-4 rounded-xl">
    <div class="tech-card-glow"></div>
    <div class="flex items-center justify-between mb-2.5">
      <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
        {{ $t('pages.dashboard.systemHealth') }}</h3>
      <span class="health-summary-badge">{{ healthyCount() }}/{{ systemHealthItems.length }}</span>
    </div>
    <div class="grid grid-cols-2 gap-x-3.5 gap-y-2.5 flex-1 min-h-0 health-grid-inner">
      <div
          v-for="item in systemHealthItems"
          :key="item.key"
          class="health-item flex items-center justify-between py-3.5 px-4 rounded-xl transition-all duration-200 health-item-theme"
      >
        <div class="health-main flex items-center gap-3 min-w-0">
          <div
              class="health-icon-wrap w-14 h-14 rounded-[14px] flex items-center justify-center flex-shrink-0 p-2.5 box-border">
            <img :src="item.icon" class="w-full h-full object-contain" :alt="$t(item.labelKey)"/>
          </div>
          <span class="health-name text-[15px] font-semibold text-[var(--home-text-primary)]">{{
              $t(item.labelKey)
            }}</span>
        </div>
        <span class="health-status inline-flex items-center gap-1.5 flex-shrink-0 text-xs font-medium"
              :class="{ 'health-status-unhealthy': !isHealthy(item.key) }">
          <span
              class="health-status-badge inline-flex items-center justify-center w-[22px] h-[22px] rounded-full flex-shrink-0"
              :class="{ 'health-status-badge-unhealthy': !isHealthy(item.key) }">
            <span v-if="isHealthy(item.key)" class="i-mdi-check-circle-outline text-sm"></span>
            <span v-else class="i-mdi-close-circle-outline text-sm"></span>
          </span>
          <span class="text-[13px] font-semibold">{{
              isHealthy(item.key) ? $t('pages.dashboard.healthHealthy') : $t('pages.dashboard.healthUnhealthy')
            }}</span>
        </span>
      </div>
    </div>
  </div>
</template>
