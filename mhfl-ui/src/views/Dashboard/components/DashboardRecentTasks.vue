<script setup lang="ts">
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'

const router = useRouter()
const {t} = useI18n()

const recentTasks = [
  {id: 101, algorithmName: 'FedAvg', dataName: 'CIFAR-100', status: 'IN_PROGRESS', createTime: '2026-03-01 10:20'},
  {id: 102, algorithmName: 'FedProto', dataName: 'Tiny-ImageNet', status: 'SUCCESS', createTime: '2026-03-01 09:15'},
  {id: 103, algorithmName: 'LG-FedAvg', dataName: 'CIFAR-100', status: 'NOT_STARTED', createTime: '2026-02-28 16:00'},
  {id: 104, algorithmName: 'FedSSA', dataName: 'CIFAR-100', status: 'SUCCESS', createTime: '2026-02-28 14:30'},
  {id: 105, algorithmName: 'Standalone', dataName: 'Tiny-ImageNet', status: 'SUCCESS', createTime: '2026-02-28 11:00'},
  {id: 106, algorithmName: 'FedAvg', dataName: 'Tiny-ImageNet', status: 'FAILED', createTime: '2026-02-27 17:45'},
  {id: 107, algorithmName: 'FedProto', dataName: 'CIFAR-100', status: 'SUCCESS', createTime: '2026-02-27 15:20'},
  {
    id: 108,
    algorithmName: 'LG-FedAvg',
    dataName: 'Tiny-ImageNet',
    status: 'NOT_STARTED',
    createTime: '2026-02-27 10:10'
  },
  {id: 109, algorithmName: 'FedSSA', dataName: 'Tiny-ImageNet', status: 'SUCCESS', createTime: '2026-02-26 16:00'},
  {id: 110, algorithmName: 'FedAvg', dataName: 'CIFAR-100', status: 'RECOMMENDED', createTime: '2026-02-26 09:00'}
].slice(0, 8)

const statusKey = (s: string) => {
  const map: Record<string, string> = {
    NOT_STARTED: 'statusNotStarted',
    IN_PROGRESS: 'statusInProgress',
    SUCCESS: 'statusSuccess',
    FAILED: 'statusFailed',
    CANCELLED: 'statusCancelled',
    RECOMMENDED: 'statusRecommended'
  }
  return map[s] || s
}

const statusLabel = (s: string) => t(`pages.dashboard.${statusKey(s)}`)

const goTo = (path: string) => router.push(path)
</script>

<template>
  <div class="recent-card tech-card flex flex-col min-h-0 p-5 rounded-xl">
    <div class="tech-card-glow"></div>
    <div class="flex items-center justify-between mb-2.5">
      <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
        {{ $t('pages.dashboard.recentTasks') }}</h3>
      <span
          class="card-link text-[13px] text-[var(--home-text-muted)] cursor-pointer transition-colors hover:text-[var(--home-text-secondary)]"
          @click="goTo('/home/monitor')">{{ $t('pages.dashboard.viewAll') }}</span>
    </div>
    <transition-group name="recent-list-fade" tag="ul"
                      class="recent-list flex flex-col gap-1.5 flex-1 overflow-y-auto list-none p-0 m-0" appear>
      <li
          v-for="(task, index) in recentTasks"
          :key="task.id"
          class="recent-item flex items-center justify-between gap-3 py-2.5 px-3 rounded-lg cursor-pointer transition-all duration-200 recent-item-theme"
          :style="{ '--item-delay': `${index * 0.03}s` }"
          @click="goTo(`/home/monitor?taskId=${task.id}`)"
      >
        <div
            class="min-w-0 flex-1 overflow-hidden text-ellipsis whitespace-nowrap text-[13px] text-[var(--home-text-primary)]">
          <span class="font-medium">{{ task.algorithmName }}</span>
          <span class="text-[var(--home-text-muted)]"> / {{ task.dataName }}</span>
        </div>
        <span class="recent-status text-xs py-0.5 px-2 rounded-md flex-shrink-0"
              :class="'status-' + task.status.toLowerCase()">{{ statusLabel(task.status) }}</span>
        <span class="text-[11px] text-[var(--home-text-muted)] flex-shrink-0">{{ task.createTime }}</span>
      </li>
    </transition-group>
    <p v-if="!recentTasks.length" class="text-[13px] text-[var(--home-text-muted)] mt-4 mb-0 p-0">
      {{ $t('pages.dashboard.noRecentTasks') }}</p>
  </div>
</template>
