<script setup lang="ts">
import {CircleCheck} from '@element-plus/icons-vue'
import mysqlIcon from '@/assets/middleware/mysql.svg'
import redisIcon from '@/assets/middleware/redis.svg'
import rabbitmqIcon from '@/assets/middleware/rabbitmq.svg'
import fastapiIcon from '@/assets/middleware/fastapi.svg'

const systemHealthItems = [
  {key: 'mysql', labelKey: 'pages.dashboard.healthMysql', icon: mysqlIcon},
  {key: 'redis', labelKey: 'pages.dashboard.healthRedis', icon: redisIcon},
  {key: 'rabbitmq', labelKey: 'pages.dashboard.healthRabbitMQ', icon: rabbitmqIcon},
  {key: 'fastapi', labelKey: 'pages.dashboard.healthFastAPI', icon: fastapiIcon},
]
</script>

<template>
  <div class="resource-card tech-card flex flex-col min-h-0 py-3.5 px-4 rounded-xl">
    <div class="tech-card-glow"></div>
    <div class="flex items-center justify-between mb-2.5">
      <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
        {{ $t('pages.dashboard.systemHealth') }}</h3>
      <span class="health-summary-badge">{{ systemHealthItems.length }}/{{ systemHealthItems.length }}</span>
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
        <span class="health-status inline-flex items-center gap-1.5 flex-shrink-0 text-xs font-medium">
          <span
              class="health-status-badge inline-flex items-center justify-center w-[22px] h-[22px] rounded-full flex-shrink-0">
            <el-icon class="text-sm"><CircleCheck/></el-icon>
          </span>
          <span class="text-[13px] font-semibold">{{ $t('pages.dashboard.healthHealthy') }}</span>
        </span>
      </div>
    </div>
  </div>
</template>
