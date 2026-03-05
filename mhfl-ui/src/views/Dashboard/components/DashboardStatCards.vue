<script setup lang="ts">
import {ref, onMounted, onBeforeUnmount, type Ref} from 'vue'
import {Document, VideoPlay, List, CircleCheck} from '@element-plus/icons-vue'

const stats = {
  total: 12,
  running: 1,
  success: 8,
  today: 2
}

const animatedTotal = ref(0)
const animatedRunning = ref(0)
const animatedSuccess = ref(0)
const animatedToday = ref(0)

const numberAnimationFrameMap = new Map<string, number>()

const animateNumber = (key: string, targetRef: Ref<number>, to: number, duration = 560) => {
  const from = Number(targetRef.value) || 0
  const target = Number(to) || 0
  if (Math.abs(from - target) < 0.05) {
    targetRef.value = target
    return
  }
  const previousFrame = numberAnimationFrameMap.get(key)
  if (previousFrame) window.cancelAnimationFrame(previousFrame)
  const start = performance.now()
  const step = (now: number) => {
    const progress = Math.min((now - start) / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    targetRef.value = from + (target - from) * eased
    if (progress < 1) {
      const frame = window.requestAnimationFrame(step)
      numberAnimationFrameMap.set(key, frame)
    } else {
      targetRef.value = target
      numberAnimationFrameMap.delete(key)
    }
  }
  const frame = window.requestAnimationFrame(step)
  numberAnimationFrameMap.set(key, frame)
}

onMounted(() => {
  animateNumber('total', animatedTotal, stats.total, 800)
  animateNumber('running', animatedRunning, stats.running, 880)
  animateNumber('success', animatedSuccess, stats.success, 940)
  animateNumber('today', animatedToday, stats.today, 980)
})

onBeforeUnmount(() => {
  numberAnimationFrameMap.forEach((frame) => window.cancelAnimationFrame(frame))
  numberAnimationFrameMap.clear()
})
</script>

<template>
  <section class="mb-6 enter-rise" style="--enter-delay: 0.16s">
    <div class="flex gap-4 flex-wrap">
      <div class="stat-card-tech stat-card-glow flex items-center gap-3 p-4 min-w-[140px] flex-1 rounded-xl">
        <div class="stat-icon-tech stat-icon-indigo">
          <el-icon :size="22">
            <List/>
          </el-icon>
        </div>
        <div class="flex flex-col gap-0.5">
          <span class="stat-value-tech text-[var(--home-text-primary)]">{{ Math.round(animatedTotal) }}</span>
          <span class="stat-label-tech text-[var(--home-text-muted)]">{{ $t('pages.dashboard.statTotal') }}</span>
        </div>
      </div>
      <div class="stat-card-tech stat-card-glow flex items-center gap-3 p-4 min-w-[140px] flex-1 rounded-xl">
        <div class="stat-icon-tech stat-icon-green">
          <el-icon :size="22">
            <VideoPlay/>
          </el-icon>
        </div>
        <div class="flex flex-col gap-0.5">
          <span class="stat-value-tech text-[var(--home-text-primary)]">{{ Math.round(animatedRunning) }}</span>
          <span class="stat-label-tech text-[var(--home-text-muted)]">{{ $t('pages.dashboard.statRunning') }}</span>
        </div>
      </div>
      <div class="stat-card-tech stat-card-glow flex items-center gap-3 p-4 min-w-[140px] flex-1 rounded-xl">
        <div class="stat-icon-tech stat-icon-indigo">
          <el-icon :size="22">
            <CircleCheck/>
          </el-icon>
        </div>
        <div class="flex flex-col gap-0.5">
          <span class="stat-value-tech text-[var(--home-text-primary)]">{{ Math.round(animatedSuccess) }}</span>
          <span class="stat-label-tech text-[var(--home-text-muted)]">{{ $t('pages.dashboard.statSuccess') }}</span>
        </div>
      </div>
      <div class="stat-card-tech stat-card-glow flex items-center gap-3 p-4 min-w-[140px] flex-1 rounded-xl">
        <div class="stat-icon-tech stat-icon-amber">
          <el-icon :size="22">
            <Document/>
          </el-icon>
        </div>
        <div class="flex flex-col gap-0.5">
          <span class="stat-value-tech text-[var(--home-text-primary)]">{{ Math.round(animatedToday) }}</span>
          <span class="stat-label-tech text-[var(--home-text-muted)]">{{ $t('pages.dashboard.statToday') }}</span>
        </div>
      </div>
    </div>
  </section>
</template>
