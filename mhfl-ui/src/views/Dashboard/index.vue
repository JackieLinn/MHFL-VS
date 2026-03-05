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
  cpu: '#6366f1',
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
  const isDark = document.documentElement.classList.contains('dark')
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      textStyle: {color: textColor, fontSize: 13},
      backgroundColor: chartTooltipBg(),
      borderColor: chartTooltipBorder(),
      borderWidth: 1,
      padding: [8, 12],
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
        scaleSize: 8,
        itemStyle: {
          shadowBlur: 20,
          shadowOffsetY: 4,
          shadowColor: isDark ? 'rgba(99, 102, 241, 0.4)' : 'rgba(99, 102, 241, 0.25)',
        }
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
  const isDark = document.documentElement.classList.contains('dark')
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      textStyle: {fontSize: 13, color: textColor},
      axisPointer: {
        type: 'line',
        lineStyle: {color: isDark ? 'rgba(99,102,241,0.5)' : 'rgba(99,102,241,0.3)', type: 'dashed'}
      },
      backgroundColor: chartTooltipBg(),
      borderColor: chartTooltipBorder(),
      borderWidth: 1,
      padding: [8, 12],
    },
    grid: {left: 48, right: 16, top: 12, bottom: 32},
    xAxis: {
      type: 'category',
      data: taskTrendDays,
      axisLine: {lineStyle: {color: mutedColor}},
      axisLabel: {color: textColor, fontSize: 13, margin: 14}
    },
    yAxis: {
      type: 'value',
      splitLine: {lineStyle: {color: mutedColor, type: 'dashed', opacity: 0.3}},
      axisLabel: {color: textColor, fontSize: 13}
    },
    series: [{
      type: 'line',
      data: taskTrendValues,
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      lineStyle: {width: 2.5, color: '#6366f1'},
      itemStyle: {color: '#6366f1', borderWidth: 2, borderColor: isDark ? '#1e1b4b' : '#fff'},
      emphasis: {
        focus: 'series',
        lineStyle: {width: 3.5, color: '#4f46e5'},
        itemStyle: {
          borderWidth: 3,
          borderColor: isDark ? '#1e1b4b' : '#ffffff',
          shadowBlur: 12,
          shadowColor: 'rgba(99,102,241,0.5)'
        }
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {offset: 0, color: isDark ? 'rgba(99,102,241,0.4)' : 'rgba(99,102,241,0.3)'},
          {offset: 1, color: 'rgba(99,102,241,0.01)'}
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
  const isDark = document.documentElement.classList.contains('dark')
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      textStyle: {fontSize: 13, color: textColor},
      axisPointer: {
        type: 'shadow',
        shadowStyle: {color: isDark ? 'rgba(99,102,241,0.15)' : 'rgba(99,102,241,0.08)'}
      },
      backgroundColor: chartTooltipBg(),
      borderColor: chartTooltipBorder(),
      borderWidth: 1,
      padding: [8, 12],
    },
    grid: {left: 56, right: 12, top: 16, bottom: 28},
    xAxis: {
      type: 'category',
      data: algorithmBarData.map(d => d.name),
      axisLine: {lineStyle: {color: mutedColor}},
      axisLabel: {color: textColor, fontSize: 13, rotate: 0, margin: 14}
    },
    yAxis: {
      type: 'value',
      axisLine: {show: false},
      splitLine: {lineStyle: {color: mutedColor, type: 'dashed', opacity: 0.3}},
      axisLabel: {color: textColor, fontSize: 13}
    },
    series: [{
      type: 'bar',
      data: algorithmBarData.map(d => d.value),
      barWidth: '52%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {offset: 0, color: isDark ? '#a5b4fc' : '#818cf8'},
          {offset: 1, color: '#6366f1'}
        ]),
        borderRadius: [6, 6, 0, 0]
      },
      emphasis: {
        focus: 'series',
        itemStyle: {
          shadowBlur: 20,
          shadowColor: 'rgba(99,102,241,0.4)',
          borderRadius: [6, 6, 0, 0]
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

function applyChartTheme() {
  chartStatus?.setOption(getStatusPieOption(), {notMerge: true})
  chartTrend?.setOption(getTrendLineOption(), {notMerge: true})
  chartAlgorithm?.setOption(getAlgorithmBarOption(), {notMerge: true})
  updateRealtimeCharts()
}

function makeRealtimeLineOption(data: number[], color: string) {
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
    <!-- 科技感背景网格 -->
    <div class="tech-grid-bg"></div>

    <!-- Hero Panel -->
    <section class="hero-panel enter-rise mb-6" style="--enter-delay: 0.02s">
      <div class="hero-scanline"></div>
      <div class="hero-main">
        <span class="hero-brand">
          <span class="hero-brand-dot"></span>
          MHFL-VS
        </span>
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

    <!-- 实时资源趋势 -->
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

    <!-- 平台概览 (admin) -->
    <section v-if="isAdmin" class="mb-5 enter-rise" style="--enter-delay: 0.12s">
      <h3 class="section-title text-[15px] font-semibold mb-3 text-[var(--home-text-secondary)]">
        {{ $t('pages.dashboard.platformOverview') }}
      </h3>
      <div class="grid gap-5 platform-layout-grid">
        <div class="flex flex-col gap-2.5 w-[220px] min-w-0 h-full platform-stats-col">
          <div class="stat-card-tech flex items-center gap-3 p-4 flex-1 min-h-0 max-w-none rounded-xl">
            <div class="stat-icon-tech stat-icon-purple">
              <el-icon :size="22">
                <User/>
              </el-icon>
            </div>
            <div class="flex flex-col gap-0.5">
              <span class="stat-value-tech text-[var(--home-text-primary)]">{{ platformStats.totalUsers }}</span>
              <span class="stat-label-tech text-[var(--home-text-muted)]">{{ $t('pages.dashboard.totalUsers') }}</span>
            </div>
          </div>
          <div class="stat-card-tech flex items-center gap-3 p-4 flex-1 min-h-0 max-w-none rounded-xl">
            <div class="stat-icon-tech stat-icon-purple">
              <el-icon :size="22">
                <FolderOpened/>
              </el-icon>
            </div>
            <div class="flex flex-col gap-0.5">
              <span class="stat-value-tech text-[var(--home-text-primary)]">{{ platformStats.totalTasks }}</span>
              <span class="stat-label-tech text-[var(--home-text-muted)]">{{ $t('pages.dashboard.totalTasks') }}</span>
            </div>
          </div>
          <div class="stat-card-tech flex items-center gap-3 p-4 flex-1 min-h-0 max-w-none rounded-xl">
            <div class="stat-icon-tech stat-icon-purple">
              <el-icon :size="22">
                <Document/>
              </el-icon>
            </div>
            <div class="flex flex-col gap-0.5">
              <span class="stat-value-tech text-[var(--home-text-primary)]">{{ platformStats.totalDatasets }}</span>
              <span class="stat-label-tech text-[var(--home-text-muted)]">{{
                  $t('pages.dashboard.totalDatasets')
                }}</span>
            </div>
          </div>
          <div class="stat-card-tech flex items-center gap-3 p-4 flex-1 min-h-0 max-w-none rounded-xl">
            <div class="stat-icon-tech stat-icon-purple">
              <el-icon :size="22">
                <List/>
              </el-icon>
            </div>
            <div class="flex flex-col gap-0.5">
              <span class="stat-value-tech text-[var(--home-text-primary)]">{{ platformStats.totalAlgorithms }}</span>
              <span class="stat-label-tech text-[var(--home-text-muted)]">{{
                  $t('pages.dashboard.totalAlgorithms')
                }}</span>
            </div>
          </div>
        </div>
        <div class="tech-card min-w-0 p-5 rounded-xl">
          <div class="tech-card-glow"></div>
          <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
            {{ $t('pages.dashboard.chartTasksByAlgorithm') }}</h3>
          <div ref="chartAlgorithmRef" class="chart-wrap chart-algo w-full h-[280px] mt-2"></div>
        </div>
      </div>
    </section>

    <!-- 统计卡片 -->
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

    <!-- 图表区域 -->
    <section class="flex gap-4 flex-wrap items-stretch mb-6 enter-rise" style="--enter-delay: 0.2s">
      <div class="tech-card w-[260px] flex-shrink-0 min-h-0 p-5 rounded-xl">
        <div class="tech-card-glow"></div>
        <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
          {{ $t('pages.dashboard.chartTaskStatus') }}</h3>
        <div ref="chartStatusRef" class="chart-wrap w-full h-[220px] mt-3"></div>
      </div>
      <div class="tech-card flex-1 min-w-[280px] min-h-0 p-5 rounded-xl">
        <div class="tech-card-glow"></div>
        <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
          {{ $t('pages.dashboard.chartTaskTrend') }}</h3>
        <div ref="chartTrendRef" class="chart-wrap w-full h-[240px] mt-2"></div>
      </div>
    </section>

    <!-- 底部网格 -->
    <div class="dashboard-grid grid gap-4 items-stretch enter-rise" style="--enter-delay: 0.24s">
      <!-- 最近任务 -->
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

      <!-- 系统健康 -->
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

      <!-- 快捷操作 -->
      <div class="actions-card tech-card flex flex-col min-h-0 py-3.5 px-4 rounded-xl">
        <div class="tech-card-glow"></div>
        <h3 class="card-title text-[15px] font-semibold mb-2 m-0 text-[var(--home-text-primary)]">
          {{ $t('pages.dashboard.quickActions') }}</h3>
        <div class="grid grid-cols-2 gap-2 mt-2">
          <div
              v-for="(item, index) in quickActionItems"
              :key="item.key"
              class="action-item flex items-center gap-2.5 py-2.5 px-3 rounded-[10px] transition-all duration-200 action-item-theme"
              :class="index === 0 ? 'action-item-first' : ''"
              role="button"
              tabindex="0"
              @click="handleQuickAction(item.key)"
              @keyup.enter="handleQuickAction(item.key)"
          >
            <div
                class="action-icon-wrap w-10 h-10 rounded-[10px] flex items-center justify-center flex-shrink-0"
                :class="index === 0 ? 'action-icon-primary' : 'action-icon-default'"
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
/* ========================================
   科技大屏风格 - Dashboard
   ======================================== */

/* --- 背景网格 --- */
.tech-grid-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: -2;
  background-image: linear-gradient(var(--dash-grid-color) 1px, transparent 1px),
  linear-gradient(90deg, var(--dash-grid-color) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse 70% 50% at 50% 30%, black 20%, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse 70% 50% at 50% 30%, black 20%, transparent 70%);
  --dash-grid-color: rgba(99, 102, 241, 0.04);
}

:global(html.dark) .tech-grid-bg {
  --dash-grid-color: rgba(99, 102, 241, 0.06);
}

/* --- Shell 光晕 --- */
.dashboard-shell {
  position: relative;
  isolation: isolate;
}

.dashboard-shell::before,
.dashboard-shell::after {
  content: '';
  position: absolute;
  border-radius: 999px;
  filter: blur(72px);
  pointer-events: none;
  z-index: -1;
  opacity: 0.45;
  animation: dashboardGlowFloat 14s ease-in-out infinite;
}

.dashboard-shell::before {
  width: 380px;
  height: 380px;
  top: -160px;
  right: 4%;
  background: radial-gradient(circle at center, rgba(99, 102, 241, 0.28), transparent 70%);
}

.dashboard-shell::after {
  width: 300px;
  height: 300px;
  left: 2%;
  top: 30%;
  background: radial-gradient(circle at center, rgba(14, 165, 164, 0.2), transparent 70%);
  animation-delay: -5s;
}

:global(html.dark) .dashboard-shell::before {
  opacity: 0.55;
  background: radial-gradient(circle at center, rgba(99, 102, 241, 0.35), transparent 70%);
}

:global(html.dark) .dashboard-shell::after {
  opacity: 0.5;
  background: radial-gradient(circle at center, rgba(14, 165, 164, 0.25), transparent 70%);
}

@keyframes dashboardGlowFloat {
  0%, 100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(0, -12px, 0) scale(1.05);
  }
}

/* ========================================
   科技卡片 - 统一基础样式
   ======================================== */
.tech-card {
  position: relative;
  overflow: hidden;
  background: var(--home-card-bg);
  border: 1px solid var(--home-card-border);
  box-shadow: 0 2px 8px var(--home-card-shadow);
  transition: border-color 0.32s ease, box-shadow 0.32s ease, background 0.32s ease;
}

.tech-card:hover {
  border-color: rgba(99, 102, 241, 0.35);
  box-shadow: 0 8px 30px rgba(99, 102, 241, 0.08),
  0 2px 8px var(--home-card-shadow);
}

:global(html.dark) .tech-card:hover {
  border-color: rgba(99, 102, 241, 0.5);
  box-shadow: 0 8px 36px rgba(99, 102, 241, 0.12),
  0 0 24px rgba(99, 102, 241, 0.06),
  0 2px 8px rgba(0, 0, 0, 0.3);
}

/* hover 时顶部发光条变亮、变粗 */
.tech-card:hover .tech-card-glow {
  opacity: 1;
  height: 2px;
  box-shadow: 0 0 12px rgba(99, 102, 241, 0.3);
}

:global(html.dark) .tech-card:hover .tech-card-glow {
  box-shadow: 0 0 20px rgba(99, 102, 241, 0.45);
}

/* hover 时扫描线加速 */
.tech-card:hover .tech-card-scanline {
  animation-duration: 3s;
}

/* 顶部发光条 */
.tech-card-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(99, 102, 241, 0.5) 30%,
      rgba(14, 165, 164, 0.4) 50%,
      rgba(99, 102, 241, 0.5) 70%,
      transparent 100%
  );
  opacity: 0.6;
  pointer-events: none;
  transition: opacity 0.32s ease, height 0.32s ease, box-shadow 0.32s ease;
}

:global(html.dark) .tech-card-glow {
  opacity: 0.8;
  background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(99, 102, 241, 0.7) 30%,
      rgba(14, 165, 164, 0.5) 50%,
      rgba(99, 102, 241, 0.7) 70%,
      transparent 100%
  );
}

/* 扫描线效果 */
.tech-card-scanline {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(99, 102, 241, 0.03) 45%,
      rgba(99, 102, 241, 0.06) 50%,
      rgba(99, 102, 241, 0.03) 55%,
      transparent 100%
  );
  pointer-events: none;
  animation: scanlineMove 8s ease-in-out infinite;
}

:global(html.dark) .tech-card-scanline {
  background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(99, 102, 241, 0.04) 45%,
      rgba(99, 102, 241, 0.1) 50%,
      rgba(99, 102, 241, 0.04) 55%,
      transparent 100%
  );
}

@keyframes scanlineMove {
  0% {
    left: -100%;
  }
  40% {
    left: 100%;
  }
  100% {
    left: 100%;
  }
}

/* ========================================
   Hero Panel
   ======================================== */
.hero-panel {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--home-card-border);
  border-radius: 18px;
  padding: 24px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: radial-gradient(ellipse 60% 80% at 10% 20%, rgba(99, 102, 241, 0.12), transparent),
  radial-gradient(ellipse 50% 60% at 90% 80%, rgba(14, 165, 164, 0.1), transparent),
  var(--home-card-bg);
  box-shadow: 0 8px 32px var(--home-card-shadow);
  transition: border-color 0.32s ease, box-shadow 0.32s ease;
}

.hero-panel:hover {
  border-color: rgba(99, 102, 241, 0.3);
  box-shadow: 0 10px 40px rgba(99, 102, 241, 0.08), 0 4px 16px var(--home-card-shadow);
}

:global(html.dark) .hero-panel:hover {
  border-color: rgba(99, 102, 241, 0.45);
  box-shadow: 0 10px 40px rgba(99, 102, 241, 0.1), 0 4px 16px rgba(0, 0, 0, 0.3);
}

:global(html.dark) .hero-panel {
  background: radial-gradient(ellipse 60% 80% at 10% 20%, rgba(99, 102, 241, 0.18), transparent),
  radial-gradient(ellipse 50% 60% at 90% 80%, rgba(14, 165, 164, 0.12), transparent),
  var(--home-card-bg);
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.3);
}

.hero-panel::after {
  content: '';
  position: absolute;
  right: -60px;
  bottom: -60px;
  width: 220px;
  height: 220px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.12), transparent 70%);
  pointer-events: none;
}

.hero-scanline {
  position: absolute;
  top: 0;
  left: -100%;
  width: 60%;
  height: 100%;
  background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(99, 102, 241, 0.04) 40%,
      rgba(99, 102, 241, 0.08) 50%,
      rgba(99, 102, 241, 0.04) 60%,
      transparent 100%
  );
  pointer-events: none;
  animation: heroScanlineMove 10s ease-in-out infinite;
  transition: animation-duration 0.3s ease;
}

.hero-panel:hover .hero-scanline {
  animation-duration: 4s;
}

:global(html.dark) .hero-scanline {
  background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(99, 102, 241, 0.06) 40%,
      rgba(99, 102, 241, 0.14) 50%,
      rgba(99, 102, 241, 0.06) 60%,
      transparent 100%
  );
}

@keyframes heroScanlineMove {
  0% {
    left: -60%;
  }
  50% {
    left: 100%;
  }
  100% {
    left: 100%;
  }
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
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.8px;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.25);
  padding: 4px 12px;
  border-radius: 999px;
  margin-bottom: 12px;
}

:global(html.dark) .hero-brand {
  color: #a5b4fc;
  background: rgba(99, 102, 241, 0.15);
  border-color: rgba(99, 102, 241, 0.35);
}

.hero-brand-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #6366f1;
  animation: brandDotPulse 2s ease-in-out infinite;
}

:global(html.dark) .hero-brand-dot {
  background: #a5b4fc;
  box-shadow: 0 0 8px rgba(165, 180, 252, 0.5);
}

@keyframes brandDotPulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(0.8);
  }
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
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
  color: var(--home-text-primary);
  letter-spacing: 2px;
  font-variant-numeric: tabular-nums;
}

:global(html.dark) .hero-clock {
  text-shadow: 0 0 30px rgba(99, 102, 241, 0.3);
}

.hero-date {
  font-size: 13px;
  color: var(--home-text-muted);
  letter-spacing: 0.5px;
  font-variant-numeric: tabular-nums;
}

.hero-health-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 26px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #16a34a;
  background: rgba(22, 163, 74, 0.1);
  border: 1px solid rgba(22, 163, 74, 0.22);
}

:global(html.dark) .hero-health-chip {
  color: #4ade80;
  background: rgba(22, 163, 74, 0.12);
  border-color: rgba(34, 197, 94, 0.3);
}

.hero-health-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #22c55e;
  animation: heroHealthPulse 2s ease-in-out infinite;
}

:global(html.dark) .hero-health-dot {
  box-shadow: 0 0 8px rgba(34, 197, 94, 0.6);
}

@keyframes heroHealthPulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.4);
  }
  50% {
    box-shadow: 0 0 0 5px rgba(34, 197, 94, 0);
  }
}

/* ========================================
   资源卡片
   ======================================== */
.resource-card-visual {
  position: relative;
  overflow: hidden;
}

.resource-card-visual::after {
  content: '';
  position: absolute;
  right: -50px;
  top: -50px;
  width: 140px;
  height: 140px;
  border-radius: 999px;
  pointer-events: none;
  opacity: 0.5;
  transition: opacity 0.35s ease, transform 0.35s ease, filter 0.35s ease;
}

.resource-card-visual:hover::after {
  opacity: 0.85;
  transform: scale(1.25);
  filter: blur(4px);
}

.resource-cpu::after {
  background: radial-gradient(circle, rgba(99, 102, 241, 0.15), transparent 70%);
}

.resource-memory::after {
  background: radial-gradient(circle, rgba(14, 165, 164, 0.15), transparent 70%);
}

.resource-gpu::after {
  background: radial-gradient(circle, rgba(245, 158, 11, 0.15), transparent 70%);
}

:global(html.dark) .resource-cpu::after {
  background: radial-gradient(circle, rgba(99, 102, 241, 0.25), transparent 70%);
}

:global(html.dark) .resource-memory::after {
  background: radial-gradient(circle, rgba(14, 165, 164, 0.2), transparent 70%);
}

:global(html.dark) .resource-gpu::after {
  background: radial-gradient(circle, rgba(245, 158, 11, 0.2), transparent 70%);
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
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-variant-numeric: tabular-nums;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.resource-card-visual:hover .resource-pill {
  transform: scale(1.08);
}

:global(html.dark) .resource-card-visual:hover .resource-pill-cpu {
  box-shadow: 0 0 10px rgba(99, 102, 241, 0.3);
}

:global(html.dark) .resource-card-visual:hover .resource-pill-memory {
  box-shadow: 0 0 10px rgba(14, 165, 164, 0.3);
}

:global(html.dark) .resource-card-visual:hover .resource-pill-gpu {
  box-shadow: 0 0 10px rgba(245, 158, 11, 0.3);
}

.resource-pill-cpu {
  color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
  border-color: rgba(99, 102, 241, 0.2);
}

.resource-pill-memory {
  color: #0d9488;
  background: rgba(13, 148, 136, 0.1);
  border-color: rgba(13, 148, 136, 0.22);
}

.resource-pill-gpu {
  color: #d97706;
  background: rgba(217, 119, 6, 0.1);
  border-color: rgba(217, 119, 6, 0.22);
}

:global(html.dark) .resource-pill-cpu {
  color: #a5b4fc;
  background: rgba(99, 102, 241, 0.15);
  border-color: rgba(99, 102, 241, 0.35);
}

:global(html.dark) .resource-pill-memory {
  color: #5eead4;
  background: rgba(14, 165, 164, 0.15);
  border-color: rgba(14, 165, 164, 0.35);
}

:global(html.dark) .resource-pill-gpu {
  color: #fcd34d;
  background: rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.35);
}

.resource-delta {
  display: inline-flex;
  align-items: center;
  margin-top: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.resource-delta.trend-up {
  color: #22c55e;
  background: rgba(34, 197, 94, 0.12);
}

.resource-delta.trend-down {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

.resource-delta.trend-flat {
  color: var(--home-text-muted);
  background: rgba(148, 163, 184, 0.12);
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
  transition: width 0.5s cubic-bezier(0.22, 1, 0.36, 1);
  position: relative;
}

.resource-progress-cpu {
  background: linear-gradient(90deg, #818cf8, #6366f1);
}

.resource-progress-memory {
  background: linear-gradient(90deg, #2dd4bf, #0d9488);
}

.resource-progress-gpu {
  background: linear-gradient(90deg, #fbbf24, #d97706);
}

:global(html.dark) .resource-progress-cpu {
  box-shadow: 0 0 8px rgba(99, 102, 241, 0.4);
}

:global(html.dark) .resource-progress-memory {
  box-shadow: 0 0 8px rgba(14, 165, 164, 0.4);
}

:global(html.dark) .resource-progress-gpu {
  box-shadow: 0 0 8px rgba(245, 158, 11, 0.4);
}

/* ========================================
   统计卡片
   ======================================== */
.stat-card-tech {
  position: relative;
  overflow: hidden;
  background: var(--home-card-bg);
  border: 1px solid var(--home-card-border);
  box-shadow: 0 1px 4px var(--home-card-shadow);
  transition: border-color 0.28s ease, box-shadow 0.28s ease, background 0.28s ease;
}

.stat-card-tech::after {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
  background: radial-gradient(circle at 30% 50%, rgba(99, 102, 241, 0.08), transparent 70%);
  transition: opacity 0.32s ease;
}

.stat-card-tech:hover {
  border-color: rgba(99, 102, 241, 0.28);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.06),
  0 1px 4px var(--home-card-shadow);
}

.stat-card-tech:hover::after {
  opacity: 1;
}

:global(html.dark) .stat-card-tech:hover {
  border-color: rgba(99, 102, 241, 0.4);
  box-shadow: 0 6px 24px rgba(99, 102, 241, 0.1),
  0 0 16px rgba(99, 102, 241, 0.05),
  0 1px 4px rgba(0, 0, 0, 0.2);
}

:global(html.dark) .stat-card-tech::after {
  background: radial-gradient(circle at 30% 50%, rgba(99, 102, 241, 0.12), transparent 70%);
}

.stat-card-glow::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.3), transparent);
  pointer-events: none;
  transition: height 0.28s ease, opacity 0.28s ease, box-shadow 0.28s ease;
  z-index: 1;
}

.stat-card-glow:hover::before {
  height: 2px;
  opacity: 1;
  box-shadow: 0 0 10px rgba(99, 102, 241, 0.2);
}

:global(html.dark) .stat-card-glow::before {
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.5), transparent);
}

:global(html.dark) .stat-card-glow:hover::before {
  box-shadow: 0 0 14px rgba(99, 102, 241, 0.35);
}

.stat-icon-tech {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.25s ease;
}

.stat-card-tech:hover .stat-icon-tech {
  transform: scale(1.1);
}

:global(html.dark) .stat-card-tech:hover .stat-icon-indigo {
  box-shadow: 0 0 12px rgba(99, 102, 241, 0.25);
}

:global(html.dark) .stat-card-tech:hover .stat-icon-green {
  box-shadow: 0 0 12px rgba(34, 197, 94, 0.25);
}

:global(html.dark) .stat-card-tech:hover .stat-icon-purple {
  box-shadow: 0 0 12px rgba(139, 92, 246, 0.25);
}

:global(html.dark) .stat-card-tech:hover .stat-icon-amber {
  box-shadow: 0 0 12px rgba(245, 158, 11, 0.25);
}

.stat-value-tech {
  transition: letter-spacing 0.25s ease;
}

.stat-card-tech:hover .stat-value-tech {
  letter-spacing: 1px;
}

.stat-icon-indigo {
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
}

.stat-icon-green {
  background: rgba(34, 197, 94, 0.12);
  color: #22c55e;
}

.stat-icon-purple {
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
}

.stat-icon-amber {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

:global(html.dark) .stat-icon-indigo {
  background: rgba(99, 102, 241, 0.18);
  color: #a5b4fc;
}

:global(html.dark) .stat-icon-green {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}

:global(html.dark) .stat-icon-purple {
  background: rgba(139, 92, 246, 0.15);
  color: #c4b5fd;
}

:global(html.dark) .stat-icon-amber {
  background: rgba(245, 158, 11, 0.15);
  color: #fcd34d;
}

.stat-value-tech {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.5px;
}

.stat-label-tech {
  font-size: 12px;
  letter-spacing: 0.2px;
}

/* ========================================
   健康状态
   ======================================== */
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
  background: rgba(22, 163, 74, 0.12);
  border: 1px solid rgba(22, 163, 74, 0.25);
}

:global(html.dark) .health-summary-badge {
  color: #4ade80;
  background: rgba(22, 163, 74, 0.15);
  border-color: rgba(34, 197, 94, 0.35);
}

.health-item-theme {
  position: relative;
  overflow: hidden;
  background: var(--home-hover-bg);
  border: 1px solid var(--home-border);
  transition: border-color 0.28s ease, background-color 0.28s ease, box-shadow 0.28s ease;
}

.health-item-theme::after {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
  background: linear-gradient(135deg, rgba(22, 163, 74, 0.05), transparent 60%);
  transition: opacity 0.3s ease;
}

.health-item-theme:hover::after {
  opacity: 1;
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

.health-icon-wrap {
  background: var(--home-hover-bg);
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.25s ease;
}

.health-item-theme:hover .health-icon-wrap {
  transform: scale(1.06);
}

:global(html.dark) .health-item-theme:hover .health-icon-wrap {
  box-shadow: 0 0 10px rgba(22, 163, 74, 0.12);
}

.health-status-badge {
  background: rgba(22, 163, 74, 0.12);
  border: 1px solid rgba(22, 163, 74, 0.35);
  color: #16a34a;
  animation: statusBadgePulse 2.2s ease-in-out infinite;
}

:global(html.dark) .health-status-badge {
  background: rgba(22, 163, 74, 0.15);
  border-color: rgba(34, 197, 94, 0.4);
  color: #4ade80;
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.2);
}

.health-status span:last-child {
  color: #16a34a;
}

:global(html.dark) .health-status span:last-child {
  color: #4ade80;
}

.health-item-theme:hover {
  border-color: rgba(22, 163, 74, 0.25);
  background: var(--home-card-bg);
  box-shadow: 0 4px 16px rgba(22, 163, 74, 0.06);
}

:global(html.dark) .health-item-theme:hover {
  border-color: rgba(34, 197, 94, 0.3);
  box-shadow: 0 4px 18px rgba(34, 197, 94, 0.08);
}

@keyframes statusBadgePulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(22, 163, 74, 0.2);
  }
  50% {
    transform: scale(1.04);
    box-shadow: 0 0 0 4px rgba(22, 163, 74, 0);
  }
}

/* ========================================
   快捷操作
   ======================================== */
.action-item-theme {
  position: relative;
  overflow: hidden;
  background: var(--home-hover-bg);
  border: 1px solid var(--home-border);
  cursor: pointer;
  user-select: none;
  transition: border-color 0.28s ease, background-color 0.28s ease, box-shadow 0.28s ease;
}

.action-item-theme::after {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
  background: radial-gradient(circle at 25% 50%, rgba(99, 102, 241, 0.08), transparent 70%);
  transition: opacity 0.3s ease;
}

.action-item-theme:hover::after {
  opacity: 1;
}

:global(html.dark) .action-item-theme::after {
  background: radial-gradient(circle at 25% 50%, rgba(99, 102, 241, 0.12), transparent 70%);
}

.action-item-theme:hover {
  border-color: rgba(99, 102, 241, 0.25);
  background: var(--home-card-bg);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.06);
}

:global(html.dark) .action-item-theme:hover {
  border-color: rgba(99, 102, 241, 0.4);
  box-shadow: 0 4px 18px rgba(99, 102, 241, 0.1);
}

.action-item-theme:active {
  box-shadow: 0 1px 4px var(--home-card-shadow);
  transition-duration: 0.08s;
}

.action-item-first {
  border-color: rgba(99, 102, 241, 0.25);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1), var(--home-hover-bg));
}

:global(html.dark) .action-item-first {
  border-color: rgba(99, 102, 241, 0.35);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.15), var(--home-hover-bg));
}

.action-icon-wrap {
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.25s ease;
}

.action-item-theme:hover .action-icon-wrap {
  transform: scale(1.1);
}

:global(html.dark) .action-item-theme:hover .action-icon-primary {
  box-shadow: 0 0 12px rgba(99, 102, 241, 0.25);
}

.action-icon-primary {
  background: rgba(99, 102, 241, 0.12);
  color: #6366f1;
}

:global(html.dark) .action-icon-primary {
  background: rgba(99, 102, 241, 0.2);
  color: #a5b4fc;
}

.action-icon-default {
  background: rgba(99, 102, 241, 0.06);
  color: var(--home-text-secondary);
}

:global(html.dark) .action-icon-default {
  background: rgba(99, 102, 241, 0.1);
}

/* ========================================
   最近任务
   ======================================== */
.recent-item-theme {
  --item-delay: 0s;
  position: relative;
  background: var(--home-hover-bg);
  border: 1px solid transparent;
  transition: border-color 0.25s ease, background-color 0.25s ease, box-shadow 0.25s ease, padding-left 0.25s ease;
}

.recent-item-theme::before {
  content: '';
  position: absolute;
  left: 0;
  top: 20%;
  bottom: 20%;
  width: 2px;
  border-radius: 999px;
  background: #6366f1;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.recent-item-theme:hover {
  border-color: var(--home-card-border);
  background: var(--home-card-bg);
  box-shadow: 0 2px 10px rgba(99, 102, 241, 0.05);
  padding-left: 16px;
}

.recent-item-theme:hover::before {
  opacity: 1;
}

:global(html.dark) .recent-item-theme:hover {
  border-color: rgba(99, 102, 241, 0.2);
  box-shadow: 0 2px 12px rgba(99, 102, 241, 0.06);
}

:global(html.dark) .recent-item-theme::before {
  background: #a5b4fc;
  box-shadow: 0 0 6px rgba(165, 180, 252, 0.4);
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

/* 任务状态标签 */
.recent-status {
  border: 1px solid transparent;
  font-weight: 600;
}

.recent-status.status-in_progress {
  background: rgba(34, 197, 94, 0.12);
  color: #22c55e;
  border-color: rgba(34, 197, 94, 0.25);
  position: relative;
  padding-left: 16px;
}

:global(html.dark) .recent-status.status-in_progress {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
  border-color: rgba(34, 197, 94, 0.35);
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

:global(html.dark) .recent-status.status-in_progress::before {
  background: #4ade80;
  box-shadow: 0 0 6px rgba(74, 222, 128, 0.4);
}

@keyframes runningDotPulse {
  0%, 100% {
    opacity: 0.55;
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.4);
  }
  50% {
    opacity: 1;
    box-shadow: 0 0 0 4px rgba(34, 197, 94, 0);
  }
}

.recent-status.status-success {
  background: rgba(99, 102, 241, 0.12);
  color: #6366f1;
  border-color: rgba(99, 102, 241, 0.22);
}

:global(html.dark) .recent-status.status-success {
  background: rgba(99, 102, 241, 0.15);
  color: #a5b4fc;
  border-color: rgba(99, 102, 241, 0.3);
}

.recent-status.status-not_started {
  background: rgba(148, 163, 184, 0.15);
  color: var(--home-text-muted);
  border-color: rgba(148, 163, 184, 0.22);
}

.recent-status.status-failed {
  background: rgba(248, 113, 113, 0.15);
  color: #f87171;
  border-color: rgba(248, 113, 113, 0.28);
}

:global(html.dark) .recent-status.status-failed {
  background: rgba(248, 113, 113, 0.18);
  color: #fca5a5;
  border-color: rgba(248, 113, 113, 0.35);
}

.recent-status.status-recommended {
  background: rgba(139, 92, 246, 0.15);
  color: #8b5cf6;
  border-color: rgba(139, 92, 246, 0.28);
}

:global(html.dark) .recent-status.status-recommended {
  background: rgba(139, 92, 246, 0.18);
  color: #c4b5fd;
  border-color: rgba(139, 92, 246, 0.35);
}

/* ========================================
   动画
   ======================================== */
.enter-rise {
  opacity: 0;
  animation: dashboardRise 0.6s cubic-bezier(0.22, 1, 0.36, 1) forwards;
  animation-delay: var(--enter-delay, 0s);
}

@keyframes dashboardRise {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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

.card-title {
  letter-spacing: 0.3px;
}

/* ========================================
   实时资源趋势图
   ======================================== */
.chart-realtime-h {
  height: 200px;
}

/* ========================================
   布局网格
   ======================================== */
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

/* ========================================
   无障碍 / 减少动画
   ======================================== */
@media (prefers-reduced-motion: reduce) {
  .enter-rise {
    animation: none;
    opacity: 1;
    transform: none;
  }

  .dashboard-shell::before,
  .dashboard-shell::after,
  .tech-card::before,
  .tech-card-scanline,
  .hero-scanline,
  .hero-health-dot,
  .hero-brand-dot,
  .health-status-badge,
  .recent-status.status-in_progress::before {
    animation: none;
  }

  .tech-card,
  .stat-card-tech,
  .recent-item-theme,
  .health-item-theme,
  .action-item-theme {
    transition: none;
    transform: none;
  }
}

/* ========================================
   响应式
   ======================================== */
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
