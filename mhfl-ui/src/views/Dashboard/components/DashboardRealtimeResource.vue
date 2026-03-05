<script setup lang="ts">
import {ref, computed, watch, onMounted, onBeforeUnmount, nextTick, type Ref} from 'vue'
import {useSystemResourceStore} from '@/stores/systemResource'
import {useTheme} from '@/stores/theme'
import * as echarts from 'echarts'

const {cpuUsageHistory, memoryUsageHistory, gpuUsageHistory} = useSystemResourceStore()
const {actualTheme} = useTheme()

const chartRealtimeCpuRef = ref<HTMLElement | null>(null)
const chartRealtimeMemRef = ref<HTMLElement | null>(null)
const chartRealtimeGpuRef = ref<HTMLElement | null>(null)
let chartRealtimeCpu: echarts.ECharts | null = null
let chartRealtimeMem: echarts.ECharts | null = null
let chartRealtimeGpu: echarts.ECharts | null = null

const realtimeChartColor = {
  cpu: '#6366f1',
  memory: '#0ea5a4',
  gpu: '#f59e0b',
}

const numberAnimationFrameMap = new Map<string, number>()
const animatedCpuUsage = ref(0)
const animatedMemoryUsage = ref(0)
const animatedGpuUsage = ref(0)

const getChartColorVar = (name: string): string => {
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || (document.documentElement.classList.contains('dark') ? '#e2e8f0' : '#1e293b')
}

const chartTextColor = () => getChartColorVar('--home-text-primary')
const chartMutedColor = () => getChartColorVar('--home-text-muted')

const getLatestUsage = (history: number[]) => {
  return Math.round(history[history.length - 1] ?? 0)
}

const getUsageDelta = (history: number[]) => {
  if (history.length < 2) return 0
  const latest = history[history.length - 1] ?? 0
  const previous = history[history.length - 2] ?? latest
  return Number((latest - previous).toFixed(1))
}

const formatDelta = (delta: number) => {
  if (delta > 0) return `+${delta.toFixed(1)}%`
  return `${delta.toFixed(1)}%`
}

const getDeltaLevel = (delta: number) => {
  if (delta > 0.4) return 'up'
  if (delta < -0.4) return 'down'
  return 'flat'
}

const clampPercent = (value: number) => Math.min(100, Math.max(0, value))

const latestCpuUsage = computed(() => getLatestUsage(cpuUsageHistory.value))
const latestMemoryUsage = computed(() => getLatestUsage(memoryUsageHistory.value))
const latestGpuUsage = computed(() => getLatestUsage(gpuUsageHistory.value))

const cpuDelta = computed(() => getUsageDelta(cpuUsageHistory.value))
const memoryDelta = computed(() => getUsageDelta(memoryUsageHistory.value))
const gpuDelta = computed(() => getUsageDelta(gpuUsageHistory.value))

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

const makeRealtimeLineOption = (data: number[], color: string) => {
  const textColor = chartTextColor()
  const mutedColor = chartMutedColor()
  const isDark = document.documentElement.classList.contains('dark')
  const xData = data.map((_, i) => i)
  return {
    backgroundColor: 'transparent',
    grid: {left: 44, right: 12, top: 12, bottom: 24},
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xData,
      show: false,
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      splitNumber: 5,
      splitLine: {lineStyle: {color: mutedColor, type: 'dashed', opacity: 0.25}},
      axisLabel: {color: textColor, fontSize: 12, formatter: '{value}%', margin: 8},
    },
    series: [{
      type: 'line',
      data,
      smooth: true,
      symbol: 'none',
      lineStyle: {width: 2.5, color, shadowColor: color + '60', shadowBlur: isDark ? 10 : 4},
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {offset: 0, color: color + (isDark ? '50' : '35')},
          {offset: 1, color: color + '05'},
        ]),
      },
      animationDuration: 420,
      animationEasing: 'cubicOut',
      animationDurationUpdate: 280,
      animationEasingUpdate: 'linear'
    }],
  }
}

const initRealtimeCharts = () => {
  if (chartRealtimeCpuRef.value && !chartRealtimeCpu) {
    chartRealtimeCpu = echarts.init(chartRealtimeCpuRef.value)
    chartRealtimeCpu.setOption(makeRealtimeLineOption([...cpuUsageHistory.value], realtimeChartColor.cpu))
  }
  if (chartRealtimeMemRef.value && !chartRealtimeMem) {
    chartRealtimeMem = echarts.init(chartRealtimeMemRef.value)
    chartRealtimeMem.setOption(makeRealtimeLineOption([...memoryUsageHistory.value], realtimeChartColor.memory))
  }
  if (chartRealtimeGpuRef.value && !chartRealtimeGpu) {
    chartRealtimeGpu = echarts.init(chartRealtimeGpuRef.value)
    chartRealtimeGpu.setOption(makeRealtimeLineOption([...gpuUsageHistory.value], realtimeChartColor.gpu))
  }
}

const updateRealtimeCharts = () => {
  chartRealtimeCpu?.setOption(makeRealtimeLineOption([...cpuUsageHistory.value], realtimeChartColor.cpu))
  chartRealtimeMem?.setOption(makeRealtimeLineOption([...memoryUsageHistory.value], realtimeChartColor.memory))
  chartRealtimeGpu?.setOption(makeRealtimeLineOption([...gpuUsageHistory.value], realtimeChartColor.gpu))
}

const resizeCharts = () => {
  chartRealtimeCpu?.resize()
  chartRealtimeMem?.resize()
  chartRealtimeGpu?.resize()
}

watch([cpuUsageHistory, memoryUsageHistory, gpuUsageHistory], updateRealtimeCharts, {deep: true})
watch(actualTheme, () => updateRealtimeCharts())
watch(latestCpuUsage, (value) => animateNumber('cpu', animatedCpuUsage, value), {immediate: true})
watch(latestMemoryUsage, (value) => animateNumber('memory', animatedMemoryUsage, value), {immediate: true})
watch(latestGpuUsage, (value) => animateNumber('gpu', animatedGpuUsage, value), {immediate: true})

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  nextTick(() => {
    initRealtimeCharts()
    resizeObserver = new ResizeObserver(() => resizeCharts())
    const el = document.querySelector('.dashboard-page')
    if (el) resizeObserver?.observe(el)
    window.addEventListener('resize', resizeCharts)
  })
})

onBeforeUnmount(() => {
  numberAnimationFrameMap.forEach((frame) => window.cancelAnimationFrame(frame))
  numberAnimationFrameMap.clear()
  resizeObserver?.disconnect()
  window.removeEventListener('resize', resizeCharts)
  chartRealtimeCpu?.dispose()
  chartRealtimeMem?.dispose()
  chartRealtimeGpu?.dispose()
})
</script>

<template>
  <section class="mb-6 enter-rise" style="--enter-delay: 0.08s">
    <h3 class="section-title text-[15px] font-semibold mb-3 text-[var(--home-text-secondary)]">
      {{ $t('pages.dashboard.realtimeResourceTrend') }}
    </h3>
    <div class="realtime-resource-grid grid gap-4">
      <div class="tech-card resource-card-visual resource-cpu min-w-0 p-5 rounded-xl">
        <div class="tech-card-glow"></div>
        <div class="tech-card-scanline"></div>
        <div class="resource-head">
          <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
            {{ $t('pages.dashboard.cpu') }}</h3>
          <span class="resource-pill resource-pill-cpu">{{ Math.round(animatedCpuUsage) }}%</span>
        </div>
        <div class="resource-delta" :class="'trend-' + getDeltaLevel(cpuDelta)">
          {{ formatDelta(cpuDelta) }}
        </div>
        <div ref="chartRealtimeCpuRef" class="chart-wrap chart-realtime-h w-full mt-2"></div>
        <div class="resource-progress mt-1.5">
          <span class="resource-progress-cpu"
                :style="{ width: `${clampPercent(animatedCpuUsage).toFixed(1)}%` }"></span>
        </div>
      </div>
      <div class="tech-card resource-card-visual resource-memory min-w-0 p-5 rounded-xl">
        <div class="tech-card-glow"></div>
        <div class="tech-card-scanline"></div>
        <div class="resource-head">
          <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
            {{ $t('pages.dashboard.memory') }}</h3>
          <span class="resource-pill resource-pill-memory">{{ Math.round(animatedMemoryUsage) }}%</span>
        </div>
        <div class="resource-delta" :class="'trend-' + getDeltaLevel(memoryDelta)">
          {{ formatDelta(memoryDelta) }}
        </div>
        <div ref="chartRealtimeMemRef" class="chart-wrap chart-realtime-h w-full mt-2"></div>
        <div class="resource-progress mt-1.5">
          <span class="resource-progress-memory"
                :style="{ width: `${clampPercent(animatedMemoryUsage).toFixed(1)}%` }"></span>
        </div>
      </div>
      <div class="tech-card resource-card-visual resource-gpu min-w-0 p-5 rounded-xl">
        <div class="tech-card-glow"></div>
        <div class="tech-card-scanline"></div>
        <div class="resource-head">
          <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
            {{ $t('pages.dashboard.gpu') }}</h3>
          <span class="resource-pill resource-pill-gpu">{{ Math.round(animatedGpuUsage) }}%</span>
        </div>
        <div class="resource-delta" :class="'trend-' + getDeltaLevel(gpuDelta)">
          {{ formatDelta(gpuDelta) }}
        </div>
        <div ref="chartRealtimeGpuRef" class="chart-wrap chart-realtime-h w-full mt-2"></div>
        <div class="resource-progress mt-1.5">
          <span class="resource-progress-gpu"
                :style="{ width: `${clampPercent(animatedGpuUsage).toFixed(1)}%` }"></span>
        </div>
      </div>
    </div>
  </section>
</template>
