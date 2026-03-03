<script setup lang="ts">
import {computed, ref, onMounted, onBeforeUnmount, nextTick, watch} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {Document, VideoPlay, List, User, FolderOpened, CircleCheck} from '@element-plus/icons-vue'
import {getUserInfo} from '@/api/user'
import mysqlIcon from '@/assets/middleware/mysql.svg'
import redisIcon from '@/assets/middleware/redis.svg'
import rabbitmqIcon from '@/assets/middleware/rabbitmq.svg'
import fastapiIcon from '@/assets/middleware/fastapi.svg'
import {useSystemResourceStore} from '@/stores/systemResource'
import * as echarts from 'echarts'

const {t} = useI18n()
const router = useRouter()

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

const isDark = () => document.documentElement.classList.contains('dark')
const chartTextColor = () => isDark() ? '#e2e8f0' : '#1e293b'
const chartMutedColor = () => isDark() ? 'rgba(165,180,252,0.6)' : '#64748b'

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
]

const platformStats = {
  totalUsers: 28,
  totalTasks: 156,
  totalDatasets: 6,
  totalAlgorithms: 5
}

const systemHealthItems = [
  { key: 'mysql', labelKey: 'pages.dashboard.healthMysql', icon: mysqlIcon },
  { key: 'redis', labelKey: 'pages.dashboard.healthRedis', icon: redisIcon },
  { key: 'rabbitmq', labelKey: 'pages.dashboard.healthRabbitMQ', icon: rabbitmqIcon },
  { key: 'fastapi', labelKey: 'pages.dashboard.healthFastAPI', icon: fastapiIcon },
]

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

function initCharts() {
  const textColor = chartTextColor()
  const mutedColor = chartMutedColor()

  if (chartStatusRef.value) {
    chartStatus = echarts.init(chartStatusRef.value)
    chartStatus.setOption({
      backgroundColor: 'transparent',
      tooltip: {trigger: 'item'},
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
        emphasis: {itemStyle: {shadowBlur: 10, shadowOffsetY: 2}}
      }]
    })
  }

  if (chartTrendRef.value) {
    chartTrend = echarts.init(chartTrendRef.value)
    chartTrend.setOption({
      backgroundColor: 'transparent',
      tooltip: {trigger: 'axis', textStyle: {fontSize: 13}},
      grid: {left: 48, right: 16, top: 8, bottom: 24},
      xAxis: {
        type: 'category',
        data: taskTrendDays,
        axisLine: {lineStyle: {color: mutedColor}},
        axisLabel: {color: textColor, fontSize: 14, margin: 10}
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
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
            offset: 0,
            color: 'rgba(99,102,241,0.35)'
          }, {offset: 1, color: 'rgba(99,102,241,0.02)'}])
        }
      }]
    })
  }

  if (isAdmin.value && chartAlgorithmRef.value) {
    chartAlgorithm = echarts.init(chartAlgorithmRef.value)
    chartAlgorithm.setOption({
      backgroundColor: 'transparent',
      tooltip: {trigger: 'axis', textStyle: {fontSize: 13}},
      grid: {left: 56, right: 12, top: 16, bottom: 20},
      xAxis: {
        type: 'category',
        data: algorithmBarData.map(d => d.name),
        axisLine: {lineStyle: {color: mutedColor}},
        axisLabel: {color: textColor, fontSize: 14, rotate: 0, margin: 10}
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
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{offset: 0, color: '#818cf8'}, {
            offset: 1,
            color: '#6366f1'
          }]), borderRadius: [4, 4, 0, 0]
        }
      }]
    })
  }
  initRealtimeCharts()
}

function makeRealtimeLineOption(data: number[], color: string) {
  const textColor = chartTextColor()
  const mutedColor = chartMutedColor()
  const xData = data.map((_, i) => i)
  return {
    backgroundColor: 'transparent',
    grid: {left: 36, right: 12, top: 8, bottom: 20},
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
      splitLine: {lineStyle: {color: mutedColor, type: 'dashed', opacity: 0.3}},
      axisLabel: {color: textColor, fontSize: 11, formatter: '{value}%'},
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
    }],
  }
}

function initRealtimeCharts() {
  if (chartRealtimeCpuRef.value && !chartRealtimeCpu) {
    chartRealtimeCpu = echarts.init(chartRealtimeCpuRef.value)
    chartRealtimeCpu.setOption(makeRealtimeLineOption([...cpuUsageHistory.value], '#6366f1'))
  }
  if (chartRealtimeMemRef.value && !chartRealtimeMem) {
    chartRealtimeMem = echarts.init(chartRealtimeMemRef.value)
    chartRealtimeMem.setOption(makeRealtimeLineOption([...memoryUsageHistory.value], '#8b5cf6'))
  }
  if (chartRealtimeGpuRef.value && !chartRealtimeGpu) {
    chartRealtimeGpu = echarts.init(chartRealtimeGpuRef.value)
    chartRealtimeGpu.setOption(makeRealtimeLineOption([...gpuUsageHistory.value], '#a855f7'))
  }
}

function updateRealtimeCharts() {
  chartRealtimeCpu?.setOption(makeRealtimeLineOption([...cpuUsageHistory.value], '#6366f1'))
  chartRealtimeMem?.setOption(makeRealtimeLineOption([...memoryUsageHistory.value], '#8b5cf6'))
  chartRealtimeGpu?.setOption(makeRealtimeLineOption([...gpuUsageHistory.value], '#a855f7'))
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
  <div class="dashboard-page">
    <h2 class="page-title">{{ $t('pages.dashboard.title') }}</h2>
    <p class="page-desc">{{ $t('pages.dashboard.desc') }}</p>

    <section class="dashboard-section realtime-resource-section">
      <h3 class="section-title">{{ $t('pages.dashboard.realtimeResourceTrend') }}</h3>
      <div class="realtime-resource-charts">
        <div class="dashboard-card chart-card realtime-chart-card">
          <h3 class="card-title">{{ $t('pages.dashboard.cpu') }}</h3>
          <div ref="chartRealtimeCpuRef" class="chart-wrap chart-realtime"></div>
        </div>
        <div class="dashboard-card chart-card realtime-chart-card">
          <h3 class="card-title">{{ $t('pages.dashboard.memory') }}</h3>
          <div ref="chartRealtimeMemRef" class="chart-wrap chart-realtime"></div>
        </div>
        <div class="dashboard-card chart-card realtime-chart-card">
          <h3 class="card-title">{{ $t('pages.dashboard.gpu') }}</h3>
          <div ref="chartRealtimeGpuRef" class="chart-wrap chart-realtime"></div>
        </div>
      </div>
    </section>

    <section v-if="isAdmin" class="dashboard-section platform-section">
      <h3 class="section-title">{{ $t('pages.dashboard.platformOverview') }}</h3>
      <div class="platform-layout">
        <div class="platform-stats">
          <div class="stat-card platform-card">
            <div class="stat-icon platform-icon">
              <el-icon :size="24">
                <User/>
              </el-icon>
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ platformStats.totalUsers }}</span>
              <span class="stat-label">{{ $t('pages.dashboard.totalUsers') }}</span>
            </div>
          </div>
          <div class="stat-card platform-card">
            <div class="stat-icon platform-icon">
              <el-icon :size="24">
                <FolderOpened/>
              </el-icon>
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ platformStats.totalTasks }}</span>
              <span class="stat-label">{{ $t('pages.dashboard.totalTasks') }}</span>
            </div>
          </div>
          <div class="stat-card platform-card">
            <div class="stat-icon platform-icon">
              <el-icon :size="24">
                <Document/>
              </el-icon>
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ platformStats.totalDatasets }}</span>
              <span class="stat-label">{{ $t('pages.dashboard.totalDatasets') }}</span>
            </div>
          </div>
          <div class="stat-card platform-card">
            <div class="stat-icon platform-icon">
              <el-icon :size="24">
                <List/>
              </el-icon>
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ platformStats.totalAlgorithms }}</span>
              <span class="stat-label">{{ $t('pages.dashboard.totalAlgorithms') }}</span>
            </div>
          </div>
        </div>
        <div class="dashboard-card chart-card chart-card-admin">
          <h3 class="card-title">{{ $t('pages.dashboard.chartTasksByAlgorithm') }}</h3>
          <div ref="chartAlgorithmRef" class="chart-wrap chart-algo"></div>
        </div>
      </div>
    </section>

    <section class="dashboard-section">
      <div class="stat-cards flex gap-4 flex-wrap">
        <div class="stat-card">
          <div class="stat-icon">
            <el-icon :size="22">
              <List/>
            </el-icon>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.total }}</span>
            <span class="stat-label">{{ $t('pages.dashboard.statTotal') }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stat-icon-running">
            <el-icon :size="22">
              <VideoPlay/>
            </el-icon>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.running }}</span>
            <span class="stat-label">{{ $t('pages.dashboard.statRunning') }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stat-icon-success">
            <el-icon :size="22">
              <Document/>
            </el-icon>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.success }}</span>
            <span class="stat-label">{{ $t('pages.dashboard.statSuccess') }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <el-icon :size="22">
              <Document/>
            </el-icon>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.today }}</span>
            <span class="stat-label">{{ $t('pages.dashboard.statToday') }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="charts-row flex gap-4 flex-wrap items-stretch">
      <div class="dashboard-card chart-card chart-card-pie">
        <h3 class="card-title">{{ $t('pages.dashboard.chartTaskStatus') }}</h3>
        <div ref="chartStatusRef" class="chart-wrap chart-pie"></div>
      </div>
      <div class="dashboard-card chart-card chart-card-trend">
        <h3 class="card-title">{{ $t('pages.dashboard.chartTaskTrend') }}</h3>
        <div ref="chartTrendRef" class="chart-wrap chart-line"></div>
      </div>
    </section>

    <div class="dashboard-grid">
      <div class="dashboard-card recent-card">
        <div class="card-header flex items-center justify-between">
          <h3 class="card-title">{{ $t('pages.dashboard.recentTasks') }}</h3>
          <span class="card-link" @click="goTo('/home/monitor')">{{ $t('pages.dashboard.viewAll') }}</span>
        </div>
        <ul class="recent-list">
          <li
              v-for="task in recentTasks"
              :key="task.id"
              class="recent-item flex items-center justify-between gap-3"
              @click="goTo(`/home/monitor?taskId=${task.id}`)"
          >
            <div class="recent-info min-w-0 flex-1">
              <span class="recent-algo">{{ task.algorithmName }}</span>
              <span class="recent-data"> / {{ task.dataName }}</span>
            </div>
            <span class="recent-status" :class="'status-' + task.status.toLowerCase()">{{
                statusLabel(task.status)
              }}</span>
            <span class="recent-time">{{ task.createTime }}</span>
          </li>
        </ul>
        <p v-if="!recentTasks.length" class="recent-empty">{{ $t('pages.dashboard.noRecentTasks') }}</p>
      </div>

      <div class="dashboard-card resource-card system-health-card">
        <h3 class="card-title">{{ $t('pages.dashboard.systemHealth') }}</h3>
        <div class="health-grid">
          <div
            v-for="item in systemHealthItems"
            :key="item.key"
            class="health-item"
          >
            <div class="health-left">
              <div class="health-icon-wrap">
                <img :src="item.icon" class="health-svg" :alt="$t(item.labelKey)" />
              </div>
              <span class="health-name">{{ $t(item.labelKey) }}</span>
            </div>
            <span class="health-status health-status-ok">
              <span class="health-status-label">{{ $t('pages.dashboard.healthStatusLabel') }}：</span>
              <span class="health-status-badge">
                <el-icon class="health-status-icon"><CircleCheck /></el-icon>
              </span>
              <span class="health-status-text">{{ $t('pages.dashboard.healthHealthy') }}</span>
            </span>
          </div>
        </div>
      </div>

      <div class="dashboard-card actions-card">
        <h3 class="card-title">{{ $t('pages.dashboard.quickActions') }}</h3>
        <div class="actions-list flex flex-col gap-2">
          <button type="button" class="action-btn primary" @click="goTo('/home/monitor')">
            <el-icon>
              <VideoPlay/>
            </el-icon>
            <span>{{ $t('pages.dashboard.createTask') }}</span>
          </button>
          <button type="button" class="action-btn" @click="goTo('/home/monitor')">
            <el-icon>
              <List/>
            </el-icon>
            <span>{{ $t('pages.dashboard.taskList') }}</span>
          </button>
          <button type="button" class="action-btn" @click="goTo('/home/monitor')">
            <el-icon>
              <VideoPlay/>
            </el-icon>
            <span>{{ $t('pages.dashboard.monitor') }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-page {
  padding: 32px;
  height: 100%;
  overflow-y: auto;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--home-text-primary);
  margin-bottom: 8px;
}

.page-desc {
  font-size: 14px;
  color: var(--home-text-muted);
  margin-bottom: 24px;
}

.dashboard-section {
  margin-bottom: 24px;
}

.realtime-resource-section {
  margin-bottom: 24px;
}

.realtime-resource-charts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.realtime-chart-card {
  min-width: 0;
}

.chart-realtime {
  height: 140px;
  margin-top: 8px;
}

.dashboard-section.platform-section {
  margin-bottom: 20px;
}

.platform-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 20px;
  align-items: stretch;
}

.platform-stats {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 220px;
  min-width: 0;
  height: 100%;
}

.platform-stats .platform-card {
  flex: 1;
  min-height: 0;
  max-width: none;
}

.charts-row {
  margin-bottom: 24px;
}

.chart-card-pie {
  width: 260px;
  flex: 0 0 260px;
  min-height: 0;
}

.charts-row .chart-card-trend {
  flex: 1;
  min-width: 280px;
}

.chart-wrap {
  width: 100%;
  height: 220px;
  margin-top: 8px;
}

.chart-pie {
  height: 220px;
  margin-top: 12px;
}

.chart-line {
  height: 240px;
}

.chart-algo {
  height: 280px;
  margin-top: 8px;
}

.chart-card-admin {
  min-width: 0;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--home-text-secondary);
  margin-bottom: 12px;
}

.stat-cards {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.stat-card {
  min-width: 140px;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--home-card-bg);
  border: 1px solid var(--home-card-border);
  border-radius: 12px;
  box-shadow: 0 1px 3px var(--home-card-shadow);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  border-color: rgba(99, 102, 241, 0.25);
  box-shadow: 0 2px 8px var(--home-card-shadow);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(99, 102, 241, 0.12);
  color: var(--home-text-secondary);
}

.stat-icon-running {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.stat-icon-success {
  background: rgba(99, 102, 241, 0.15);
  color: var(--home-text-secondary);
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--home-text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: var(--home-text-muted);
}


.platform-icon {
  background: rgba(139, 92, 246, 0.12);
  color: var(--home-text-secondary);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: auto auto;
  gap: 20px;
}

.dashboard-card {
  padding: 20px;
  background: var(--home-card-bg);
  border: 1px solid var(--home-card-border);
  border-radius: 12px;
  box-shadow: 0 1px 3px var(--home-card-shadow);
}

.recent-card {
  grid-column: 1;
  grid-row: 1 / 3;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.card-header {
  margin-bottom: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--home-text-primary);
  margin: 0;
}

.card-link {
  font-size: 13px;
  color: var(--home-text-muted);
  cursor: pointer;
  transition: color 0.2s;
}

.card-link:hover {
  color: var(--home-text-secondary);
}

.recent-list {
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.recent-item {
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--home-hover-bg);
  border: 1px solid transparent;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.recent-item:hover {
  border-color: var(--home-card-border);
  background: var(--home-card-bg);
}

.recent-info {
  font-size: 13px;
  color: var(--home-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-data {
  color: var(--home-text-muted);
}

.recent-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 6px;
  flex-shrink: 0;
}

.status-in_progress {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.status-success {
  background: rgba(99, 102, 241, 0.15);
  color: var(--home-text-secondary);
}

.status-not_started {
  background: rgba(148, 163, 184, 0.2);
  color: var(--home-text-muted);
}

.status-failed {
  background: rgba(248, 113, 113, 0.2);
  color: #f87171;
}

.status-recommended {
  background: rgba(139, 92, 246, 0.2);
  color: #a78bfa;
}

.recent-time {
  font-size: 11px;
  color: var(--home-text-muted);
  flex-shrink: 0;
}

.recent-empty {
  font-size: 13px;
  color: var(--home-text-muted);
  margin: 16px 0 0;
  padding: 0;
}

.resource-card {
  grid-column: 2;
  grid-row: 1;
  padding: 16px;
}

.resource-card .card-title {
  margin-bottom: 12px;
}

.system-health-card .card-title {
  margin-bottom: 14px;
}

.health-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 16px;
  flex: 1;
  min-height: 0;
}

.system-health-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.health-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: var(--home-hover-bg);
  border-radius: 12px;
  border: 1px solid var(--home-border);
  transition: border-color 0.2s, background 0.2s;
}

.health-item:hover {
  border-color: rgba(99, 102, 241, 0.25);
  background: var(--home-card-bg);
}

.health-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.health-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--home-hover-bg);
  padding: 10px;
  box-sizing: border-box;
}

.health-svg {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.health-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--home-text-primary);
}

.health-item .health-status {
  flex-shrink: 0;
}

.health-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--home-text-muted);
}

.health-status-ok {
  color: #16a34a;
}

.health-status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(22, 163, 74, 0.15);
  border: 1.5px solid rgba(22, 163, 74, 0.4);
  flex-shrink: 0;
}

.health-status-icon {
  font-size: 14px;
  color: #16a34a;
}

.health-status-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--home-text-muted);
}

.health-status-text {
  font-size: 13px;
  font-weight: 600;
  color: #16a34a;
}

.actions-card {
  grid-column: 2;
  grid-row: 2;
}

.actions-list {
  margin-top: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 14px;
  font-size: 14px;
  color: var(--home-text-primary);
  background: var(--home-hover-bg);
  border: 1px solid var(--home-border);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.action-btn:hover {
  border-color: rgba(99, 102, 241, 0.3);
  background: var(--home-card-bg);
}

.action-btn.primary {
  background: rgba(99, 102, 241, 0.12);
  border-color: rgba(99, 102, 241, 0.25);
  color: var(--home-text-secondary);
}

.action-btn.primary:hover {
  background: rgba(99, 102, 241, 0.18);
  border-color: rgba(99, 102, 241, 0.4);
}

@media (max-width: 900px) {
  .realtime-resource-charts {
    grid-template-columns: 1fr;
  }

  .platform-layout {
    grid-template-columns: 1fr;
  }

  .platform-stats {
    width: auto;
    min-width: 0;
  }

  .chart-card-pie {
    width: 100%;
    flex: 1 1 100%;
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
