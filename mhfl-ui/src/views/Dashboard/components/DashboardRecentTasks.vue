<script setup lang="ts">
import {ref, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {getRecentTasks, type DashboardRecentTaskVO} from '@/api/dashboard'

const router = useRouter()
const {t} = useI18n()

const SLOT_COUNT = 8
const recentTasks = ref<DashboardRecentTaskVO[]>([])
const loading = ref(true)

/** 状态码 0-5 对应 i18n key */
const statusCodeToKey: Record<number, string> = {
  0: 'statusNotStarted',
  1: 'statusInProgress',
  2: 'statusSuccess',
  3: 'statusRecommended',
  4: 'statusFailed',
  5: 'statusCancelled'
}

/** 状态码 0-5 对应 CSS class 后缀 */
const statusCodeToClass: Record<number, string> = {
  0: 'not_started',
  1: 'in_progress',
  2: 'success',
  3: 'recommended',
  4: 'failed',
  5: 'cancelled'
}

const statusLabel = (code: number) => t(`pages.dashboard.${statusCodeToKey[code] ?? 'statusNotStarted'}`)
const statusClass = (code: number) => `status-${statusCodeToClass[code] ?? 'not_started'}`

/** 8 个槽位：有数据则填任务，无则 null */
const displaySlots = () => {
  const arr: (DashboardRecentTaskVO | null)[] = []
  for (let i = 0; i < SLOT_COUNT; i++) {
    arr.push(recentTasks.value[i] ?? null)
  }
  return arr
}

const goTo = (path: string) => router.push(path)

onMounted(() => {
  getRecentTasks((data) => {
    recentTasks.value = data
    loading.value = false
  }, () => {
    loading.value = false
  })
})
</script>

<template>
  <div class="recent-card tech-card flex flex-col min-h-0 p-5 rounded-xl">
    <div class="tech-card-glow"></div>
    <div class="flex items-center justify-between mb-2.5">
      <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
        {{ $t('pages.dashboard.recentTasks') }}</h3>
      <span
          class="card-link text-[13px] text-[var(--home-text-muted)] cursor-pointer transition-colors hover:text-[var(--home-text-secondary)]"
          @click="goTo('/home/task')">{{ $t('pages.dashboard.viewAll') }}</span>
    </div>
    <transition-group name="recent-list-fade" tag="ul"
                      class="recent-list flex flex-col gap-1.5 flex-1 overflow-y-auto list-none p-0 m-0" appear>
      <li
          v-for="(slot, index) in displaySlots()"
          :key="slot ? slot.id : `empty-${index}`"
          class="recent-item flex items-center justify-between gap-3 py-2.5 px-3 rounded-lg transition-all duration-200 recent-item-theme"
          :class="{ 'cursor-pointer': !!slot }"
          :style="{ '--item-delay': `${index * 0.03}s` }"
          @click="slot && goTo(`/home/task?taskId=${slot.id}`)"
      >
        <template v-if="slot">
          <div
              class="min-w-0 flex-1 overflow-hidden text-ellipsis whitespace-nowrap text-[13px] text-[var(--home-text-primary)]">
            <span class="font-medium">{{ slot.algorithmName }}</span>
            <span class="text-[var(--home-text-muted)]"> / {{ slot.dataName }}</span>
          </div>
          <span class="recent-status text-xs py-0.5 px-2 rounded-md flex-shrink-0"
                :class="statusClass(slot.status)">{{ statusLabel(slot.status) }}</span>
          <span class="text-[11px] text-[var(--home-text-muted)] flex-shrink-0">{{ slot.createTime }}</span>
        </template>
        <template v-else>
          <div class="flex-1 min-h-[20px]"></div>
        </template>
      </li>
    </transition-group>
    <p v-if="!loading && recentTasks.length === 0" class="text-[13px] text-[var(--home-text-muted)] mt-4 mb-0 p-0">
      {{ $t('pages.dashboard.noRecentTasks') }}</p>
    <p v-else-if="!loading && recentTasks.length > 0 && recentTasks.length < SLOT_COUNT"
       class="text-[12px] text-[var(--home-text-muted)] mt-2 mb-0 p-0">
      {{ $t('pages.dashboard.noMoreTasks') }}</p>
  </div>
</template>
