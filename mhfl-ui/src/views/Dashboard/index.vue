<script setup lang="ts">
import {computed, ref, onMounted, onBeforeUnmount, nextTick, watch, type Ref} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {
  Document,
  VideoPlay,
  List,
  User,
  FolderOpened,
  CircleCheck,
  DataBoard,
  Setting,
  MagicStick,
  Star
} from '@element-plus/icons-vue'
import {getUserInfo} from '@/api/user'
import mysqlIcon from '@/assets/middleware/mysql.svg'
import redisIcon from '@/assets/middleware/redis.svg'
import rabbitmqIcon from '@/assets/middleware/rabbitmq.svg'
import fastapiIcon from '@/assets/middleware/fastapi.svg'
import {useSystemResourceStore} from '@/stores/systemResource'
import {useTheme} from '@/stores/theme'
import * as echarts from 'echarts'

const {t} = useI18n()
const router = useRouter()
const {actualTheme} = useTheme()

const isAdmin = computed(() => getUserInfo()?.role === 'admin')
const {cpuUsageHistory, memoryUsageHistory, gpuUsageHistory} = useSystemResourceStore()

const chartStatusRef = ref<HTMLElement | null>(null)
const chartTrendRef = ref<HTMLElement | null>(null)
const chartAlgorithmRef = ref<HTMLElement | null>(null)
const chartRealtimeCpuRef = ref<HTMLElement | null>(null)
const chartRealtimeMemRef = ref<HTMLElement | null>(null)
const chartRealtimeGpuRef = ref<HTMLElement | null>(null)
let chartStatus: echarts.ECharts | null = null
let chartTrend: echarts.ECharts | null = null
let chartAlgorithm: echarts.ECharts | null = null
let chartRealtimeCpu: echarts.ECharts | null = null
let chartRealtimeMem: echarts.ECharts | null = null
let chartRealtimeGpu: echarts.ECharts | null = null

/** 从 CSS 变量读取颜色，与「CPU/内存/GPU 标题」等区域一致，随深浅色主题切换 */
function getChartColorVar(name: string): string {
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || (document.documentElement.classList.contains('dark') ? '#e2e8f0' : '#1e293b')
}

const chartTextColor = () => getChartColorVar('--home-text-primary')
const chartMutedColor = () => getChartColorVar('--home-text-muted')
const chartTooltipBg = () => document.documentElement.classList.contains('dark')
    ? 'rgba(15, 23, 42, 0.92)'
    : 'rgba(255, 255, 255, 0.96)'
const chartTooltipBorder = () => getChartColorVar('--home-card-border')
const realtimeChartColor = {
  cpu: '#4f46e5',
  memory: '#0ea5a4',
  gpu: '#f59e0b',
}

const stats = {
  total: 12,
  running: 1,
  success: 8,
  today: 2
}

const taskStatusPieData = [
  {value: 3, name: '未开始', itemStyle: {color: '#94a3b8'}},
  {value: 1, name: '进行中', itemStyle: {color: '#22c55e'}},
  {value: 8, name: '已完成', itemStyle: {color: '#6366f1'}},
  {value: 0, name: '失败', itemStyle: {color: '#f87171'}}
]

const taskTrendDays = ['02-25', '02-26', '02-27', '02-28', '03-01', '03-02', '03-03']
const taskTrendValues = [2, 1, 3, 0, 2, 1, 2]

const algorithmBarData = [
  {name: 'FedAvg', value: 45},
  {name: 'FedProto', value: 38},
  {name: 'LG-FedAvg', value: 32},
  {name: 'FedSSA', value: 25},
  {name: 'Standalone', value: 16}
]

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

const platformStats = {
  totalUsers: 28,
  totalTasks: 156,
  totalDatasets: 6,
  totalAlgorithms: 5
}

const systemHealthItems = [
  {key: 'mysql', labelKey: 'pages.dashboard.healthMysql', icon: mysqlIcon},
  {key: 'redis', labelKey: 'pages.dashboard.healthRedis', icon: redisIcon},
  {key: 'rabbitmq', labelKey: 'pages.dashboard.healthRabbitMQ', icon: rabbitmqIcon},
  {key: 'fastapi', labelKey: 'pages.dashboard.healthFastAPI', icon: fastapiIcon},
]

const quickActionItemsAdmin = [
  {key: 'dashboard', labelKey: 'pages.dashboard.actionDashboard', icon: DataBoard},
  {key: 'taskManage', labelKey: 'pages.dashboard.actionTaskManage', icon: List},
  {key: 'systemAdmin', labelKey: 'pages.dashboard.actionSystemAdmin', icon: Setting},
  {key: 'smartAssistant', labelKey: 'pages.dashboard.actionSmartAssistant', icon: MagicStick},
]
const quickActionItemsUser = [
  {key: 'dashboard', labelKey: 'pages.dashboard.actionDashboard', icon: DataBoard},
  {key: 'myTasks', labelKey: 'pages.dashboard.actionMyTasks', icon: List},
  {key: 'recommendedShow', labelKey: 'pages.dashboard.actionRecommendedShow', icon: Star},
  {key: 'smartAssistant', labelKey: 'pages.dashboard.actionSmartAssistant', icon: MagicStick},
]
const quickActionItems = computed(() => isAdmin.value ? quickActionItemsAdmin : quickActionItemsUser)

const quickActionRouteMap: Record<string, string> = {
  dashboard: '/home/dashboard',
  taskManage: '/home/monitor',
  myTasks: '/home/monitor',
  recommendedShow: '/home/monitor?recommended=1',
  systemAdmin: '/home/admin',
  smartAssistant: '/home/monitor',
}

function handleQuickAction(key: string) {
  const targetRoute = quickActionRouteMap[key]
  if (targetRoute) {
    goTo(targetRoute)
  }
}

function getLatestUsage(history: number[]) {
  return Math.round(history[history.length - 1] ?? 0)
}

function getUsageDelta(history: number[]) {
  if (history.length < 2) {
    return 0
  }
  const latest = history[history.length - 1] ?? 0
  const previous = history[history.length - 2] ?? latest
  return Number((latest - previous).toFixed(1))
}

function formatDelta(delta: number) {
  if (delta > 0) {
    return `+${delta.toFixed(1)}%`
  }
  return `${delta.toFixed(1)}%`
}

function getDeltaLevel(delta: number) {
  if (delta > 0.4) {
    return 'up'
  }
  if (delta < -0.4) {
    return 'down'
  }
  return 'flat'
}

const latestCpuUsage = computed(() => getLatestUsage(cpuUsageHistory.value))
const latestMemoryUsage = computed(() => getLatestUsage(memoryUsageHistory.value))
const latestGpuUsage = computed(() => getLatestUsage(gpuUsageHistory.value))

const cpuDelta = computed(() => getUsageDelta(cpuUsageHistory.value))
const memoryDelta = computed(() => getUsageDelta(memoryUsageHistory.value))
const gpuDelta = computed(() => getUsageDelta(gpuUsageHistory.value))

const animatedCpuUsage = ref(0)
const animatedMemoryUsage = ref(0)
const animatedGpuUsage = ref(0)
const animatedTotal = ref(0)
const animatedRunning = ref(0)
const animatedSuccess = ref(0)
const animatedToday = ref(0)

const liveClock = ref('')
const liveDate = ref('')
let liveClockTimer: ReturnType<typeof window.setInterval> | null = null
const numberAnimationFrameMap = new Map<string, number>()

function animateNumber(key: string, targetRef: Ref<number>, to: number, duration = 560) {
  const from = Number(targetRef.value) || 0
  const target = Number(to) || 0

  if (Math.abs(from - target) < 0.05) {
    targetRef.value = target
    return
  }

  const previousFrame = numberAnimationFrameMap.get(key)
  if (previousFrame) {
    window.cancelAnimationFrame(previousFrame)
  }

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

function clampPercent(value: number) {
  return Math.min(100, Math.max(0, value))
}

function updateLiveClock() {
  const now = new Date()
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  const ss = String(now.getSeconds()).padStart(2, '0')
  const yyyy = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const dd = String(now.getDate()).padStart(2, '0')
  liveClock.value = `${hh}:${mm}:${ss}`
  liveDate.value = `${yyyy}-${month}-${dd}`
}

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

function getStatusPieOption() {
  const textColor = chartTextColor()
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      textStyle: {color: textColor},
      backgroundColor: chartTooltipBg(),
      borderColor: chartTooltipBorder(),
      borderWidth: 1
    },
    legend: {
      show: true,
      bottom: 0,
      left: 'center',
      textStyle: {color: textColor, fontSize: 12},
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 8,
      padding: [20, 0, 0, 0]
    },
    series: [{
      type: 'pie',
      radius: ['48%', '78%'],
      center: ['50%', '42%'],
      label: {show: false},
      labelLine: {show: false},
      data: taskStatusPieData,
      emphasis: {
        scale: true,
        itemStyle: {shadowBlur: 12, shadowOffsetY: 2}
      },
      animationDuration: 700,
      animationEasing: 'cubicOut',
      animationDurationUpdate: 380,
      animationEasingUpdate: 'cubicInOut'
    }]
  }
}

function getTrendLineOption() {
  const textColor = chartTextColor()
  const mutedColor = chartMutedColor()
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      textStyle: {fontSize: 13, color: textColor},
      axisPointer: {
        type: 'line',
        lineStyle: {color: mutedColor, type: 'dashed'}
      },
      backgroundColor: chartTooltipBg(),
      borderColor: chartTooltipBorder(),
      borderWidth: 1
    },
    grid: {left: 48, right: 16, top: 8, bottom: 32},
    xAxis: {
      type: 'category',
      data: taskTrendDays,
      axisLine: {lineStyle: {color: mutedColor}},
      axisLabel: {color: textColor, fontSize: 14, margin: 14}
    },
    yAxis: {
      type: 'value',
      splitLine: {lineStyle: {color: mutedColor, type: 'dashed', opacity: 0.4}},
      axisLabel: {color: textColor, fontSize: 13}
    },
    series: [{
      type: 'line',
      data: taskTrendValues,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: {width: 2, color: '#6366f1'},
      itemStyle: {color: '#6366f1'},
      emphasis: {
        focus: 'series',
        lineStyle: {width: 3, color: '#4f46e5'},
        itemStyle: {borderWidth: 2, borderColor: '#ffffff'}
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {offset: 0, color: 'rgba(99,102,241,0.35)'},
          {offset: 1, color: 'rgba(99,102,241,0.02)'}
        ])
      },
      animationDuration: 850,
      animationEasing: 'cubicOut',
      animationDurationUpdate: 380,
      animationEasingUpdate: 'cubicInOut'
    }]
  }
}

function getAlgorithmBarOption() {
  const textColor = chartTextColor()
  const mutedColor = chartMutedColor()
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      textStyle: {fontSize: 13, color: textColor},
      axisPointer: {
        type: 'shadow',
        shadowStyle: {color: 'rgba(99,102,241,0.1)'}
      },
      backgroundColor: chartTooltipBg(),
      borderColor: chartTooltipBorder(),
      borderWidth: 1
    },
    grid: {left: 56, right: 12, top: 16, bottom: 28},
    xAxis: {
      type: 'category',
      data: algorithmBarData.map(d => d.name),
      axisLine: {lineStyle: {color: mutedColor}},
      axisLabel: {color: textColor, fontSize: 14, rotate: 0, margin: 14}
    },
    yAxis: {
      type: 'value',
      axisLine: {show: false},
      splitLine: {lineStyle: {color: mutedColor, type: 'dashed', opacity: 0.4}},
      axisLabel: {color: textColor, fontSize: 13}
    },
    series: [{
      type: 'bar',
      data: algorithmBarData.map(d => d.value),
      barWidth: '56%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {offset: 0, color: '#818cf8'},
          {offset: 1, color: '#6366f1'}
        ]),
        borderRadius: [4, 4, 0, 0]
      },
      emphasis: {
        focus: 'series',
        itemStyle: {
          shadowBlur: 16,
          shadowColor: 'rgba(99,102,241,0.35)',
          borderRadius: [4, 4, 0, 0]
        }
      },
      animationDuration: 760,
      animationEasing: 'cubicOut',
      animationDurationUpdate: 380,
      animationEasingUpdate: 'cubicInOut'
    }]
  }
}

function initCharts() {
  if (chartStatusRef.value) {
    chartStatus = echarts.init(chartStatusRef.value)
    chartStatus.setOption(getStatusPieOption())
  }
  if (chartTrendRef.value) {
    chartTrend = echarts.init(chartTrendRef.value)
    chartTrend.setOption(getTrendLineOption())
  }
  if (isAdmin.value && chartAlgorithmRef.value) {
    chartAlgorithm = echarts.init(chartAlgorithmRef.value)
    chartAlgorithm.setOption(getAlgorithmBarOption())
  }
  initRealtimeCharts()
}

/** 主题切换时重新应用所有图表颜色（与 CPU/内存/GPU 标题区域一致） */
function applyChartTheme() {
  chartStatus?.setOption(getStatusPieOption(), {notMerge: true})
  chartTrend?.setOption(getTrendLineOption(), {notMerge: true})
  chartAlgorithm?.setOption(getAlgorithmBarOption(), {notMerge: true})
  updateRealtimeCharts()
}

function makeRealtimeLineOption(data: number[], color: string) {
  const textColor = chartTextColor()
  const mutedColor = chartMutedColor()
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
      splitLine: {lineStyle: {color: mutedColor, type: 'dashed', opacity: 0.3}},
      axisLabel: {color: textColor, fontSize: 12, formatter: '{value}%', margin: 8},
    },
    series: [{
      type: 'line',
      data,
      smooth: true,
      symbol: 'none',
      lineStyle: {width: 2, color},
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {offset: 0, color: color + '40'},
          {offset: 1, color: color + '08'},
        ]),
      },
      animationDuration: 420,
      animationEasing: 'cubicOut',
      animationDurationUpdate: 280,
      animationEasingUpdate: 'linear'
    }],
  }
}

function initRealtimeCharts() {
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

function updateRealtimeCharts() {
  chartRealtimeCpu?.setOption(makeRealtimeLineOption([...cpuUsageHistory.value], realtimeChartColor.cpu))
  chartRealtimeMem?.setOption(makeRealtimeLineOption([...memoryUsageHistory.value], realtimeChartColor.memory))
  chartRealtimeGpu?.setOption(makeRealtimeLineOption([...gpuUsageHistory.value], realtimeChartColor.gpu))
}

function resizeCharts() {
  chartStatus?.resize()
  chartTrend?.resize()
  chartAlgorithm?.resize()
  chartRealtimeCpu?.resize()
  chartRealtimeMem?.resize()
  chartRealtimeGpu?.resize()
}

watch([cpuUsageHistory, memoryUsageHistory, gpuUsageHistory], updateRealtimeCharts, {deep: true})
watch(actualTheme, () => applyChartTheme())
watch(latestCpuUsage, (value) => animateNumber('cpu', animatedCpuUsage, value), {immediate: true})
watch(latestMemoryUsage, (value) => animateNumber('memory', animatedMemoryUsage, value), {immediate: true})
watch(latestGpuUsage, (value) => animateNumber('gpu', animatedGpuUsage, value), {immediate: true})

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  updateLiveClock()
  liveClockTimer = window.setInterval(updateLiveClock, 1000)
  animateNumber('total', animatedTotal, stats.total, 800)
  animateNumber('running', animatedRunning, stats.running, 880)
  animateNumber('success', animatedSuccess, stats.success, 940)
  animateNumber('today', animatedToday, stats.today, 980)
  nextTick(() => {
    initCharts()
    resizeObserver = new ResizeObserver(() => resizeCharts())
    const el = document.querySelector('.dashboard-page')
    if (el) resizeObserver?.observe(el)
    window.addEventListener('resize', resizeCharts)
  })
})

onBeforeUnmount(() => {
  if (liveClockTimer) {
    window.clearInterval(liveClockTimer)
    liveClockTimer = null
  }
  numberAnimationFrameMap.forEach((frame) => window.cancelAnimationFrame(frame))
  numberAnimationFrameMap.clear()
  resizeObserver?.disconnect()
  window.removeEventListener('resize', resizeCharts)
  chartStatus?.dispose()
  chartTrend?.dispose()
  chartAlgorithm?.dispose()
  chartRealtimeCpu?.dispose()
  chartRealtimeMem?.dispose()
  chartRealtimeGpu?.dispose()
})
</script>

<template>
  <div class="dashboard-page dashboard-shell p-8 h-full overflow-y-auto">
    <section class="hero-panel enter-rise mb-6" style="--enter-delay: 0.02s">
      <div class="hero-main">
        <span class="hero-brand">MHFL-VS</span>
        <h2 class="page-title text-2xl font-bold mb-2 text-[var(--home-text-primary)]">
          {{ $t('pages.dashboard.title') }}
        </h2>
        <p class="page-desc text-sm mb-0 text-[var(--home-text-muted)]">
          {{ $t('pages.dashboard.desc') }}
        </p>
      </div>
      <div class="hero-side">
        <div class="hero-clock">{{ liveClock }}</div>
        <div class="hero-date">{{ liveDate }}</div>
        <div class="hero-health-chip">
          <span class="hero-health-dot"></span>
          <span>{{ $t('pages.dashboard.healthHealthy') }}</span>
        </div>
      </div>
    </section>

    <section class="mb-6 enter-rise" style="--enter-delay: 0.08s">
      <h3 class="section-title text-[15px] font-semibold mb-3 text-[var(--home-text-secondary)]">
        {{ $t('pages.dashboard.realtimeResourceTrend') }}
      </h3>
      <div class="realtime-resource-grid grid gap-4">
        <div
            class="dashboard-card resource-card-visual resource-cpu min-w-0 p-5 rounded-xl border bg-[var(--home-card-bg)] border-[var(--home-card-border)]">
          <div class="resource-head">
            <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
              {{ $t('pages.dashboard.cpu') }}</h3>
            <span class="resource-pill">{{ Math.round(animatedCpuUsage) }}%</span>
          </div>
          <div class="resource-delta" :class="'trend-' + getDeltaLevel(cpuDelta)">
            {{ formatDelta(cpuDelta) }}
          </div>
          <div ref="chartRealtimeCpuRef" class="chart-wrap chart-realtime-h w-full mt-2"></div>
          <div class="resource-progress mt-1.5">
            <span :style="{ width: `${clampPercent(animatedCpuUsage).toFixed(1)}%` }"></span>
          </div>
        </div>
        <div
            class="dashboard-card resource-card-visual resource-memory min-w-0 p-5 rounded-xl border bg-[var(--home-card-bg)] border-[var(--home-card-border)]">
          <div class="resource-head">
            <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
              {{ $t('pages.dashboard.memory') }}</h3>
            <span class="resource-pill">{{ Math.round(animatedMemoryUsage) }}%</span>
          </div>
          <div class="resource-delta" :class="'trend-' + getDeltaLevel(memoryDelta)">
            {{ formatDelta(memoryDelta) }}
          </div>
          <div ref="chartRealtimeMemRef" class="chart-wrap chart-realtime-h w-full mt-2"></div>
          <div class="resource-progress mt-1.5">
            <span :style="{ width: `${clampPercent(animatedMemoryUsage).toFixed(1)}%` }"></span>
          </div>
        </div>
        <div
            class="dashboard-card resource-card-visual resource-gpu min-w-0 p-5 rounded-xl border bg-[var(--home-card-bg)] border-[var(--home-card-border)]">
          <div class="resource-head">
            <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
              {{ $t('pages.dashboard.gpu') }}</h3>
            <span class="resource-pill">{{ Math.round(animatedGpuUsage) }}%</span>
          </div>
          <div class="resource-delta" :class="'trend-' + getDeltaLevel(gpuDelta)">
            {{ formatDelta(gpuDelta) }}
          </div>
          <div ref="chartRealtimeGpuRef" class="chart-wrap chart-realtime-h w-full mt-2"></div>
          <div class="resource-progress mt-1.5">
            <span :style="{ width: `${clampPercent(animatedGpuUsage).toFixed(1)}%` }"></span>
          </div>
        </div>
      </div>
    </section>

    <section v-if="isAdmin" class="mb-5 enter-rise" style="--enter-delay: 0.12s">
      <h3 class="section-title text-[15px] font-semibold mb-3 text-[var(--home-text-secondary)]">
        {{ $t('pages.dashboard.platformOverview') }}
      </h3>
      <div class="grid gap-5 platform-layout-grid">
        <div class="flex flex-col gap-2.5 w-[220px] min-w-0 h-full platform-stats-col">
          <div
              class="stat-card flex items-center gap-3 p-4 flex-1 min-h-0 max-w-none rounded-xl stat-card-theme platform-card">
            <div
                class="stat-icon w-11 h-11 rounded-[10px] flex items-center justify-center bg-[rgba(139,92,246,0.12)] text-[var(--home-text-secondary)]">
              <el-icon :size="24">
                <User/>
              </el-icon>
            </div>
            <div class="flex flex-col gap-0.5">
              <span class="stat-value text-[22px] font-bold leading-tight text-[var(--home-text-primary)]">{{
                  platformStats.totalUsers
                }}</span>
              <span class="stat-label text-xs text-[var(--home-text-muted)]">{{
                  $t('pages.dashboard.totalUsers')
                }}</span>
            </div>
          </div>
          <div
              class="stat-card flex items-center gap-3 p-4 flex-1 min-h-0 max-w-none rounded-xl stat-card-theme platform-card">
            <div
                class="stat-icon w-11 h-11 rounded-[10px] flex items-center justify-center bg-[rgba(139,92,246,0.12)] text-[var(--home-text-secondary)]">
              <el-icon :size="24">
                <FolderOpened/>
              </el-icon>
            </div>
            <div class="flex flex-col gap-0.5">
              <span class="stat-value text-[22px] font-bold leading-tight text-[var(--home-text-primary)]">{{
                  platformStats.totalTasks
                }}</span>
              <span class="stat-label text-xs text-[var(--home-text-muted)]">{{
                  $t('pages.dashboard.totalTasks')
                }}</span>
            </div>
          </div>
          <div
              class="stat-card flex items-center gap-3 p-4 flex-1 min-h-0 max-w-none rounded-xl stat-card-theme platform-card">
            <div
                class="stat-icon w-11 h-11 rounded-[10px] flex items-center justify-center bg-[rgba(139,92,246,0.12)] text-[var(--home-text-secondary)]">
              <el-icon :size="24">
                <Document/>
              </el-icon>
            </div>
            <div class="flex flex-col gap-0.5">
              <span class="stat-value text-[22px] font-bold leading-tight text-[var(--home-text-primary)]">{{
                  platformStats.totalDatasets
                }}</span>
              <span class="stat-label text-xs text-[var(--home-text-muted)]">{{
                  $t('pages.dashboard.totalDatasets')
                }}</span>
            </div>
          </div>
          <div
              class="stat-card flex items-center gap-3 p-4 flex-1 min-h-0 max-w-none rounded-xl stat-card-theme platform-card">
            <div
                class="stat-icon w-11 h-11 rounded-[10px] flex items-center justify-center bg-[rgba(139,92,246,0.12)] text-[var(--home-text-secondary)]">
              <el-icon :size="24">
                <List/>
              </el-icon>
            </div>
            <div class="flex flex-col gap-0.5">
              <span class="stat-value text-[22px] font-bold leading-tight text-[var(--home-text-primary)]">{{
                  platformStats.totalAlgorithms
                }}</span>
              <span class="stat-label text-xs text-[var(--home-text-muted)]">{{
                  $t('pages.dashboard.totalAlgorithms')
                }}</span>
            </div>
          </div>
        </div>
        <div
            class="dashboard-card min-w-0 p-5 rounded-xl border bg-[var(--home-card-bg)] border-[var(--home-card-border)]">
          <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
            {{ $t('pages.dashboard.chartTasksByAlgorithm') }}</h3>
          <div ref="chartAlgorithmRef" class="chart-wrap chart-algo w-full h-[280px] mt-2"></div>
        </div>
      </div>
    </section>

    <section class="mb-6 enter-rise" style="--enter-delay: 0.16s">
      <div class="flex gap-4 flex-wrap">
        <div class="stat-card flex items-center gap-3 p-4 min-w-[140px] flex-1 rounded-xl stat-card-theme">
          <div
              class="stat-icon w-11 h-11 rounded-[10px] flex items-center justify-center bg-[rgba(99,102,241,0.12)] text-[var(--home-text-secondary)]">
            <el-icon :size="22">
              <List/>
            </el-icon>
          </div>
          <div class="flex flex-col gap-0.5">
            <span class="stat-value text-[22px] font-bold leading-tight text-[var(--home-text-primary)]">{{
                Math.round(animatedTotal)
              }}</span>
            <span class="stat-label text-xs text-[var(--home-text-muted)]">{{ $t('pages.dashboard.statTotal') }}</span>
          </div>
        </div>
        <div class="stat-card flex items-center gap-3 p-4 min-w-[140px] flex-1 rounded-xl stat-card-theme">
          <div
              class="stat-icon w-11 h-11 rounded-[10px] flex items-center justify-center bg-[rgba(34,197,94,0.15)] text-[#22c55e]">
            <el-icon :size="22">
              <VideoPlay/>
            </el-icon>
          </div>
          <div class="flex flex-col gap-0.5">
            <span class="stat-value text-[22px] font-bold leading-tight text-[var(--home-text-primary)]">{{
                Math.round(animatedRunning)
              }}</span>
            <span class="stat-label text-xs text-[var(--home-text-muted)]">{{
                $t('pages.dashboard.statRunning')
              }}</span>
          </div>
        </div>
        <div class="stat-card flex items-center gap-3 p-4 min-w-[140px] flex-1 rounded-xl stat-card-theme">
          <div
              class="stat-icon w-11 h-11 rounded-[10px] flex items-center justify-center bg-[rgba(99,102,241,0.15)] text-[var(--home-text-secondary)]">
            <el-icon :size="22">
              <Document/>
            </el-icon>
          </div>
          <div class="flex flex-col gap-0.5">
            <span class="stat-value text-[22px] font-bold leading-tight text-[var(--home-text-primary)]">{{
                Math.round(animatedSuccess)
              }}</span>
            <span class="stat-label text-xs text-[var(--home-text-muted)]">{{
                $t('pages.dashboard.statSuccess')
              }}</span>
          </div>
        </div>
        <div class="stat-card flex items-center gap-3 p-4 min-w-[140px] flex-1 rounded-xl stat-card-theme">
          <div
              class="stat-icon w-11 h-11 rounded-[10px] flex items-center justify-center bg-[rgba(99,102,241,0.12)] text-[var(--home-text-secondary)]">
            <el-icon :size="22">
              <Document/>
            </el-icon>
          </div>
          <div class="flex flex-col gap-0.5">
            <span class="stat-value text-[22px] font-bold leading-tight text-[var(--home-text-primary)]">{{
                Math.round(animatedToday)
              }}</span>
            <span class="stat-label text-xs text-[var(--home-text-muted)]">{{ $t('pages.dashboard.statToday') }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="flex gap-4 flex-wrap items-stretch mb-6 enter-rise" style="--enter-delay: 0.2s">
      <div
          class="dashboard-card w-[260px] flex-shrink-0 min-h-0 p-5 rounded-xl border bg-[var(--home-card-bg)] border-[var(--home-card-border)]">
        <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
          {{ $t('pages.dashboard.chartTaskStatus') }}</h3>
        <div ref="chartStatusRef" class="chart-wrap w-full h-[220px] mt-3"></div>
      </div>
      <div
          class="dashboard-card flex-1 min-w-[280px] min-h-0 p-5 rounded-xl border bg-[var(--home-card-bg)] border-[var(--home-card-border)]">
        <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
          {{ $t('pages.dashboard.chartTaskTrend') }}</h3>
        <div ref="chartTrendRef" class="chart-wrap w-full h-[240px] mt-2"></div>
      </div>
    </section>

    <div class="dashboard-grid grid gap-4 items-stretch enter-rise" style="--enter-delay: 0.24s">
      <div
          class="recent-card dashboard-card flex flex-col min-h-0 p-5 rounded-xl border bg-[var(--home-card-bg)] border-[var(--home-card-border)]">
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
              <span>{{ task.algorithmName }}</span>
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

      <div
          class="resource-card dashboard-card flex flex-col min-h-0 py-3.5 px-4 rounded-xl border bg-[var(--home-card-bg)] border-[var(--home-card-border)]">
        <div class="flex items-center justify-between mb-2.5">
          <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
            {{ $t('pages.dashboard.systemHealth') }}</h3>
          <span class="health-summary-badge">{{ systemHealthItems.length }}/{{ systemHealthItems.length }}</span>
        </div>
        <div class="grid grid-cols-2 gap-x-3.5 gap-y-2.5 flex-1 min-h-0 health-grid-inner">
          <div
              v-for="item in systemHealthItems"
              :key="item.key"
              class="health-item flex items-center justify-between py-3.5 px-4 rounded-xl border transition-all duration-200 health-item-theme"
          >
            <div class="health-main flex items-center gap-3 min-w-0">
              <div
                  class="health-icon-wrap w-14 h-14 rounded-[14px] flex items-center justify-center flex-shrink-0 p-2.5 box-border bg-[var(--home-hover-bg)]">
                <img :src="item.icon" class="w-full h-full object-contain" :alt="$t(item.labelKey)"/>
              </div>
              <span class="health-name text-[15px] font-semibold text-[var(--home-text-primary)]">{{
                  $t(item.labelKey)
                }}</span>
            </div>
            <span
                class="health-status inline-flex items-center gap-1.5 flex-shrink-0 text-xs font-medium text-[#16a34a]">
              <span
                  class="health-status-badge inline-flex items-center justify-center w-[22px] h-[22px] rounded-full bg-[rgba(22,163,74,0.15)] border border-[rgba(22,163,74,0.4)] flex-shrink-0">
                <el-icon class="text-sm text-[#16a34a]"><CircleCheck/></el-icon>
              </span>
              <span class="text-[13px] font-semibold text-[#16a34a]">{{ $t('pages.dashboard.healthHealthy') }}</span>
            </span>
          </div>
        </div>
      </div>

      <div
          class="actions-card dashboard-card flex flex-col min-h-0 py-3.5 px-4 rounded-xl border bg-[var(--home-card-bg)] border-[var(--home-card-border)]">
        <h3 class="card-title text-[15px] font-semibold mb-2 m-0 text-[var(--home-text-primary)]">
          {{ $t('pages.dashboard.quickActions') }}</h3>
        <div class="grid grid-cols-2 gap-2 mt-2">
          <div
              v-for="(item, index) in quickActionItems"
              :key="item.key"
              class="action-item flex items-center gap-2.5 py-2.5 px-3 rounded-[10px] border transition-all duration-200 action-item-theme"
              :class="index === 0 ? 'action-item-first' : ''"
              role="button"
              tabindex="0"
              @click="handleQuickAction(item.key)"
              @keyup.enter="handleQuickAction(item.key)"
          >
            <div
                class="action-icon-wrap w-10 h-10 rounded-[10px] flex items-center justify-center flex-shrink-0"
                :class="index === 0 ? 'bg-[rgba(99,102,241,0.15)] text-[#6366f1]' : 'bg-[rgba(99,102,241,0.08)] text-[var(--home-text-secondary)]'"
            >
              <el-icon class="text-xl">
                <component :is="item.icon"/>
              </el-icon>
            </div>
            <span class="action-label text-sm font-semibold text-[var(--home-text-primary)]">{{
                $t(item.labelKey)
              }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-shell {
  position: relative;
  isolation: isolate;
}

.dashboard-shell::before,
.dashboard-shell::after {
  content: '';
  position: absolute;
  border-radius: 999px;
  filter: blur(56px);
  pointer-events: none;
  z-index: -1;
  opacity: 0.5;
  animation: dashboardGlowFloat 12s ease-in-out infinite;
}

.dashboard-shell::before {
  width: 320px;
  height: 320px;
  top: -140px;
  right: 6%;
  background: radial-gradient(circle at center, rgba(99, 102, 241, 0.3), rgba(99, 102, 241, 0));
}

.dashboard-shell::after {
  width: 260px;
  height: 260px;
  left: 4%;
  top: 34%;
  background: radial-gradient(circle at center, rgba(14, 165, 164, 0.22), rgba(14, 165, 164, 0));
  animation-delay: -4s;
}

@keyframes dashboardGlowFloat {
  0%, 100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(0, -10px, 0) scale(1.04);
  }
}

.hero-panel {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--home-card-border);
  border-radius: 18px;
  padding: 22px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  box-shadow: 0 10px 24px var(--home-card-shadow);
  background: radial-gradient(circle at 12% 24%, rgba(99, 102, 241, 0.16), rgba(99, 102, 241, 0)),
  radial-gradient(circle at 88% 72%, rgba(14, 165, 164, 0.13), rgba(14, 165, 164, 0)),
  var(--home-card-bg);
}

.hero-panel::after {
  content: '';
  position: absolute;
  right: -54px;
  bottom: -58px;
  width: 200px;
  height: 200px;
  border-radius: 999px;
  background: radial-gradient(circle at center, rgba(99, 102, 241, 0.16), rgba(99, 102, 241, 0));
  pointer-events: none;
}

.hero-main {
  position: relative;
  z-index: 1;
  flex: 1 1 30%;
  min-width: 230px;
}

.hero-brand {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.6px;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.12);
  border: 1px solid rgba(99, 102, 241, 0.25);
  padding: 3px 10px;
  border-radius: 999px;
  margin-bottom: 10px;
}

.hero-side {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 10px;
  flex: 0 0 auto;
  min-width: 172px;
}

.hero-clock {
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
  color: var(--home-text-primary);
  letter-spacing: 1.6px;
  text-shadow: 0 8px 24px rgba(99, 102, 241, 0.16);
}

.hero-date {
  font-size: 13px;
  color: var(--home-text-muted);
  letter-spacing: 0.5px;
}

.hero-health-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #16a34a;
  background: rgba(22, 163, 74, 0.12);
  border: 1px solid rgba(22, 163, 74, 0.26);
}

.hero-health-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #22c55e;
  box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.42);
  animation: heroHealthPulse 1.9s ease-in-out infinite;
}

@keyframes heroHealthPulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.42);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(34, 197, 94, 0);
  }
}

.resource-card-visual {
  position: relative;
  overflow: hidden;
}

.resource-card-visual::after {
  content: '';
  position: absolute;
  right: -42px;
  top: -42px;
  width: 120px;
  height: 120px;
  border-radius: 999px;
  pointer-events: none;
  opacity: 0.6;
}

.resource-cpu::after {
  background: radial-gradient(circle at center, rgba(79, 70, 229, 0.18), rgba(79, 70, 229, 0));
}

.resource-memory::after {
  background: radial-gradient(circle at center, rgba(14, 165, 164, 0.18), rgba(14, 165, 164, 0));
}

.resource-gpu::after {
  background: radial-gradient(circle at center, rgba(245, 158, 11, 0.2), rgba(245, 158, 11, 0));
}

.resource-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.resource-pill {
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid transparent;
}

.resource-cpu .resource-pill {
  color: #4f46e5;
  background: rgba(79, 70, 229, 0.12);
  border-color: rgba(79, 70, 229, 0.25);
}

.resource-memory .resource-pill {
  color: #0d9488;
  background: rgba(13, 148, 136, 0.13);
  border-color: rgba(13, 148, 136, 0.28);
}

.resource-gpu .resource-pill {
  color: #d97706;
  background: rgba(217, 119, 6, 0.13);
  border-color: rgba(217, 119, 6, 0.28);
}

.resource-delta {
  display: inline-flex;
  align-items: center;
  margin-top: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.resource-delta.trend-up {
  color: #22c55e;
  background: rgba(34, 197, 94, 0.14);
}

.resource-delta.trend-down {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.12);
}

.resource-delta.trend-flat {
  color: var(--home-text-muted);
  background: rgba(148, 163, 184, 0.16);
}

.resource-progress {
  width: 100%;
  height: 4px;
  border-radius: 999px;
  background: var(--home-hover-bg);
  overflow: hidden;
}

.resource-progress > span {
  display: block;
  height: 100%;
  border-radius: inherit;
  transition: width 0.45s ease;
}

.resource-cpu .resource-progress > span {
  background: linear-gradient(90deg, #818cf8, #4f46e5);
}

.resource-memory .resource-progress > span {
  background: linear-gradient(90deg, #2dd4bf, #0d9488);
}

.resource-gpu .resource-progress > span {
  background: linear-gradient(90deg, #fbbf24, #d97706);
}

.health-summary-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #16a34a;
  background: rgba(22, 163, 74, 0.14);
  border: 1px solid rgba(22, 163, 74, 0.28);
}

.enter-rise {
  opacity: 0;
  animation: dashboardRise 0.55s cubic-bezier(0.22, 1, 0.36, 1) forwards;
  animation-delay: var(--enter-delay, 0s);
}

@keyframes dashboardRise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 主题相关：卡片阴影（所有 dashboard-card 统一） */
.dashboard-card {
  position: relative;
  overflow: hidden;
  box-shadow: 0 1px 3px var(--home-card-shadow);
  transition: transform 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease, background-color 0.22s ease;
  backdrop-filter: blur(8px);
}

.dashboard-card::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  padding: 1px;
  background: conic-gradient(
      from 140deg,
      rgba(99, 102, 241, 0.35),
      rgba(14, 165, 164, 0.12),
      rgba(99, 102, 241, 0.28)
  );
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  opacity: 0.2;
  pointer-events: none;
  animation: cardEdgeBreath 4.6s ease-in-out infinite;
}

.dashboard-card:hover {
  transform: translateY(-2px);
  border-color: rgba(99, 102, 241, 0.22);
  box-shadow: 0 10px 22px var(--home-card-shadow);
}

@keyframes cardEdgeBreath {
  0%, 100% {
    opacity: 0.16;
    filter: saturate(0.95);
  }
  50% {
    opacity: 0.38;
    filter: saturate(1.2);
  }
}

.card-title {
  letter-spacing: 0.2px;
}

.stat-card-theme {
  background: var(--home-card-bg);
  border: 1px solid var(--home-card-border);
  box-shadow: 0 1px 3px var(--home-card-shadow);
  transition: transform 0.2s, border-color 0.2s, box-shadow 0.2s;
}

.stat-card-theme:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.25);
  box-shadow: 0 8px 18px var(--home-card-shadow);
}

.recent-item-theme {
  --item-delay: 0s;
  background: var(--home-hover-bg);
  border: 1px solid transparent;
  transition: transform 0.2s, border-color 0.2s, background-color 0.2s;
}

.recent-item-theme:hover {
  transform: translateX(2px);
  border-color: var(--home-card-border);
  background: var(--home-card-bg);
}

.recent-list {
  overflow-x: hidden;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.recent-list::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

.health-item-theme {
  background: var(--home-hover-bg);
  border: 1px solid var(--home-border);
  transition: transform 0.2s, border-color 0.2s, background-color 0.2s;
}

.health-item {
  min-width: 0;
  gap: 10px;
}

.health-main {
  flex: 1;
}

.health-name {
  display: block;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.health-status {
  white-space: nowrap;
}

.health-item-theme:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.25);
  background: var(--home-card-bg);
}

.action-item-theme {
  background: var(--home-hover-bg);
  border: 1px solid var(--home-border);
  cursor: pointer;
  user-select: none;
  transition: transform 0.2s, border-color 0.2s, background-color 0.2s;
}

.action-item-theme:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.2);
  background: var(--home-card-bg);
}

.action-item-theme:active {
  transform: translateY(0);
}

.action-item-first {
  border-color: rgba(99, 102, 241, 0.28);
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.12), var(--home-hover-bg));
}

.health-status-badge {
  animation: statusBadgePulse 2.1s ease-in-out infinite;
}

@keyframes statusBadgePulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(22, 163, 74, 0.25);
  }
  50% {
    transform: scale(1.03);
    box-shadow: 0 0 0 4px rgba(22, 163, 74, 0);
  }
}

/* 任务状态标签（主题/语义色） */
.recent-status {
  border: 1px solid transparent;
  font-weight: 600;
}

.recent-status.status-in_progress {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
  border-color: rgba(34, 197, 94, 0.3);
  position: relative;
  padding-left: 16px;
}

.recent-status.status-in_progress::before {
  content: '';
  position: absolute;
  left: 6px;
  top: 50%;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #22c55e;
  transform: translateY(-50%);
  animation: runningDotPulse 1.6s ease-in-out infinite;
}

@keyframes runningDotPulse {
  0%, 100% {
    opacity: 0.55;
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.5);
  }
  50% {
    opacity: 1;
    box-shadow: 0 0 0 4px rgba(34, 197, 94, 0);
  }
}

.recent-status.status-success {
  background: rgba(99, 102, 241, 0.15);
  color: var(--home-text-secondary);
  border-color: rgba(99, 102, 241, 0.25);
}

.recent-status.status-not_started {
  background: rgba(148, 163, 184, 0.2);
  color: var(--home-text-muted);
  border-color: rgba(148, 163, 184, 0.3);
}

.recent-status.status-failed {
  background: rgba(248, 113, 113, 0.2);
  color: #f87171;
  border-color: rgba(248, 113, 113, 0.35);
}

.recent-status.status-recommended {
  background: rgba(139, 92, 246, 0.2);
  color: #a78bfa;
  border-color: rgba(139, 92, 246, 0.35);
}

.recent-list-fade-enter-active,
.recent-list-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
  transition-delay: var(--item-delay, 0s);
}

.recent-list-fade-enter-from,
.recent-list-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

/* 实时资源趋势图：增高容器，纵坐标刻度更疏 */
.chart-realtime-h {
  height: 200px;
}

/* 复杂布局：网格列/行与占位（UnoCSS 难以表达） */
.realtime-resource-grid {
  grid-template-columns: repeat(3, 1fr);
}

.platform-layout-grid {
  grid-template-columns: 220px 1fr;
  align-items: stretch;
}

.dashboard-grid {
  grid-template-columns: 1fr 1fr;
  grid-template-rows: auto auto;
}

.recent-card {
  grid-column: 1;
  grid-row: 1 / 3;
}

.resource-card {
  grid-column: 2;
  grid-row: 1;
}

.actions-card {
  grid-column: 2;
  grid-row: 2;
}

@media (prefers-reduced-motion: reduce) {
  .enter-rise {
    animation: none;
    opacity: 1;
    transform: none;
  }

  .dashboard-shell::before,
  .dashboard-shell::after,
  .dashboard-card::before,
  .dashboard-card,
  .stat-card-theme,
  .recent-item-theme,
  .health-item-theme,
  .action-item-theme,
  .health-status-badge,
  .hero-health-dot,
  .recent-status.status-in_progress::before {
    animation: none;
    transition: none;
    transform: none;
  }
}

@media (max-width: 900px) {
  .hero-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-side {
    width: 100%;
    align-items: flex-start;
  }

  .realtime-resource-grid {
    grid-template-columns: 1fr;
  }

  .platform-layout-grid {
    grid-template-columns: 1fr;
  }

  .platform-stats-col {
    width: auto;
    min-width: 0;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto auto;
  }

  .recent-card {
    grid-column: 1;
    grid-row: 1;
  }

  .resource-card {
    grid-column: 1;
    grid-row: 2;
  }

  .actions-card {
    grid-column: 1;
    grid-row: 3;
  }
}

</style>
