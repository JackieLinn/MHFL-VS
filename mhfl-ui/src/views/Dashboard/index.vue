<script setup lang="ts">
import {computed, ref, onMounted, onBeforeUnmount, nextTick, watch} from 'vue'
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

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  nextTick(() => {
    initCharts()
    resizeObserver = new ResizeObserver(() => resizeCharts())
    const el = document.querySelector('.dashboard-page')
    if (el) resizeObserver?.observe(el)
    window.addEventListener('resize', resizeCharts)
  })
})

onBeforeUnmount(() => {
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
    <h2 class="page-title enter-rise text-2xl font-bold mb-2 text-[var(--home-text-primary)]">
      {{ $t('pages.dashboard.title') }}
    </h2>
    <p class="page-desc enter-rise text-sm mb-6 text-[var(--home-text-muted)]" style="--enter-delay: 0.04s">
      {{ $t('pages.dashboard.desc') }}
    </p>

    <section class="mb-6 enter-rise" style="--enter-delay: 0.08s">
      <h3 class="section-title text-[15px] font-semibold mb-3 text-[var(--home-text-secondary)]">
        {{ $t('pages.dashboard.realtimeResourceTrend') }}
      </h3>
      <div class="realtime-resource-grid grid gap-4">
        <div
            class="dashboard-card min-w-0 p-5 rounded-xl border bg-[var(--home-card-bg)] border-[var(--home-card-border)]">
          <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
            {{ $t('pages.dashboard.cpu') }}</h3>
          <div ref="chartRealtimeCpuRef" class="chart-wrap chart-realtime-h w-full mt-2"></div>
        </div>
        <div
            class="dashboard-card min-w-0 p-5 rounded-xl border bg-[var(--home-card-bg)] border-[var(--home-card-border)]">
          <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
            {{ $t('pages.dashboard.memory') }}</h3>
          <div ref="chartRealtimeMemRef" class="chart-wrap chart-realtime-h w-full mt-2"></div>
        </div>
        <div
            class="dashboard-card min-w-0 p-5 rounded-xl border bg-[var(--home-card-bg)] border-[var(--home-card-border)]">
          <h3 class="card-title text-[15px] font-semibold m-0 text-[var(--home-text-primary)]">
            {{ $t('pages.dashboard.gpu') }}</h3>
          <div ref="chartRealtimeGpuRef" class="chart-wrap chart-realtime-h w-full mt-2"></div>
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
                stats.total
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
                stats.running
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
                stats.success
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
                stats.today
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
        <ul class="recent-list flex flex-col gap-1.5 flex-1 overflow-y-auto list-none p-0 m-0">
          <li
              v-for="task in recentTasks"
              :key="task.id"
              class="recent-item flex items-center justify-between gap-3 py-2.5 px-3 rounded-lg cursor-pointer transition-all duration-200 recent-item-theme"
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
        </ul>
        <p v-if="!recentTasks.length" class="text-[13px] text-[var(--home-text-muted)] mt-4 mb-0 p-0">
          {{ $t('pages.dashboard.noRecentTasks') }}</p>
      </div>

      <div
          class="resource-card dashboard-card flex flex-col min-h-0 py-3.5 px-4 rounded-xl border bg-[var(--home-card-bg)] border-[var(--home-card-border)]">
        <h3 class="card-title text-[15px] font-semibold mb-2.5 m-0 text-[var(--home-text-primary)]">
          {{ $t('pages.dashboard.systemHealth') }}</h3>
        <div class="grid grid-cols-2 gap-x-3.5 gap-y-2.5 flex-1 min-h-0 health-grid-inner">
          <div
              v-for="item in systemHealthItems"
              :key="item.key"
              class="health-item flex items-center justify-between py-3.5 px-4 rounded-xl border transition-all duration-200 health-item-theme"
          >
            <div class="flex items-center gap-3">
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
              <span class="text-[13px] font-semibold text-[var(--home-text-muted)]">{{
                  $t('pages.dashboard.healthStatusLabel')
                }}：</span>
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
  isolation: auto;
}

.dashboard-shell::before,
.dashboard-shell::after {
  display: none;
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
  box-shadow: 0 1px 3px var(--home-card-shadow);
  transition: transform 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease, background-color 0.22s ease;
  backdrop-filter: blur(8px);
}

.dashboard-card:hover {
  transform: translateY(-2px);
  border-color: rgba(99, 102, 241, 0.22);
  box-shadow: 0 10px 22px var(--home-card-shadow);
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

.health-item-theme:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.25);
  background: var(--home-card-bg);
}

.action-item-theme {
  background: var(--home-hover-bg);
  border: 1px solid var(--home-border);
  transition: transform 0.2s, border-color 0.2s, background-color 0.2s;
}

.action-item-theme:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.2);
  background: var(--home-card-bg);
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

  .dashboard-card,
  .stat-card-theme,
  .recent-item-theme,
  .health-item-theme,
  .action-item-theme,
  .health-status-badge,
  .recent-status.status-in_progress::before {
    animation: none;
    transition: none;
    transform: none;
  }
}

@media (max-width: 900px) {
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
