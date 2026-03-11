<script setup lang="ts">
import {ref, computed, watch, onMounted, onBeforeUnmount, nextTick} from 'vue'
import {getUserInfo} from '@/api/user'
import {getPlatformStats} from '@/api/dashboard'
import {useTheme} from '@/stores/theme'
import * as echarts from 'echarts'

const {actualTheme} = useTheme()
const isAdmin = computed(() => getUserInfo()?.role === 'admin')

const platformStats = ref({totalUsers: 0, totalTasks: 0, totalDatasets: 0, totalAlgorithms: 0})

const algorithmBarData = [
  {name: 'FedAvg', value: 45},
  {name: 'FedProto', value: 38},
  {name: 'LG-FedAvg', value: 32},
  {name: 'FedSSA', value: 25},
  {name: 'Standalone', value: 16}
]

const chartAlgorithmRef = ref<HTMLElement | null>(null)
let chartAlgorithm: echarts.ECharts | null = null

const getChartColorVar = (name: string): string => {
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || (document.documentElement.classList.contains('dark') ? '#e2e8f0' : '#1e293b')
}

const chartTextColor = () => getChartColorVar('--home-text-primary')
const chartMutedColor = () => getChartColorVar('--home-text-muted')
const chartTooltipBg = () => document.documentElement.classList.contains('dark')
    ? 'rgba(15, 23, 42, 0.92)'
    : 'rgba(255, 255, 255, 0.96)'
const chartTooltipBorder = () => getChartColorVar('--home-card-border')

const getAlgorithmBarOption = () => {
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

const resizeChart = () => {
  chartAlgorithm?.resize()
}

watch(actualTheme, () => {
  chartAlgorithm?.setOption(getAlgorithmBarOption(), {notMerge: true})
})

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  if (!isAdmin.value) return
  getPlatformStats((data) => {
    platformStats.value = data
  })
  nextTick(() => {
    if (chartAlgorithmRef.value) {
      chartAlgorithm = echarts.init(chartAlgorithmRef.value)
      chartAlgorithm.setOption(getAlgorithmBarOption())
    }
    resizeObserver = new ResizeObserver(() => resizeChart())
    const el = document.querySelector('.dashboard-page')
    if (el) resizeObserver?.observe(el)
    window.addEventListener('resize', resizeChart)
  })
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', resizeChart)
  chartAlgorithm?.dispose()
})
</script>

<template>
  <section v-if="isAdmin" class="mb-5 enter-rise" style="--enter-delay: 0.12s">
    <h3 class="section-title text-[15px] font-semibold mb-3 text-[var(--home-text-secondary)]">
      {{ $t('pages.dashboard.platformOverview') }}
    </h3>
    <div class="grid gap-5 platform-layout-grid">
      <div class="flex flex-col gap-2.5 w-[220px] min-w-0 h-full platform-stats-col">
        <div class="stat-card-tech flex items-center gap-3 p-4 flex-1 min-h-0 max-w-none rounded-xl">
          <div class="stat-icon-tech stat-icon-purple">
            <span class="i-mdi-account-group-outline text-[22px]"></span>
          </div>
          <div class="flex flex-col gap-0.5">
            <span class="stat-value-tech text-[var(--home-text-primary)]">{{ platformStats.totalUsers }}</span>
            <span class="stat-label-tech text-[var(--home-text-muted)]">{{ $t('pages.dashboard.totalUsers') }}</span>
          </div>
        </div>
        <div class="stat-card-tech flex items-center gap-3 p-4 flex-1 min-h-0 max-w-none rounded-xl">
          <div class="stat-icon-tech stat-icon-purple">
            <span class="i-mdi-clipboard-list-outline text-[22px]"></span>
          </div>
          <div class="flex flex-col gap-0.5">
            <span class="stat-value-tech text-[var(--home-text-primary)]">{{ platformStats.totalTasks }}</span>
            <span class="stat-label-tech text-[var(--home-text-muted)]">{{ $t('pages.dashboard.totalTasks') }}</span>
          </div>
        </div>
        <div class="stat-card-tech flex items-center gap-3 p-4 flex-1 min-h-0 max-w-none rounded-xl">
          <div class="stat-icon-tech stat-icon-purple">
            <span class="i-mdi-database-outline text-[22px]"></span>
          </div>
          <div class="flex flex-col gap-0.5">
            <span class="stat-value-tech text-[var(--home-text-primary)]">{{ platformStats.totalDatasets }}</span>
            <span class="stat-label-tech text-[var(--home-text-muted)]">{{ $t('pages.dashboard.totalDatasets') }}</span>
          </div>
        </div>
        <div class="stat-card-tech flex items-center gap-3 p-4 flex-1 min-h-0 max-w-none rounded-xl">
          <div class="stat-icon-tech stat-icon-purple">
            <span class="i-mdi-code-braces text-[22px]"></span>
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
</template>
