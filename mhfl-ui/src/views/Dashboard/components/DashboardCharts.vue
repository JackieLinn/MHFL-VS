<script setup lang="ts">
import {ref, watch, onMounted, onBeforeUnmount, nextTick} from 'vue'
import {getTaskStatusStats, getTaskTrend7Days} from '@/api/dashboard'
import {useTheme} from '@/stores/theme'
import * as echarts from 'echarts'

const {actualTheme} = useTheme()

const taskStatusPieData = ref([
  {value: 0, name: '未开始', itemStyle: {color: '#94a3b8'}},
  {value: 0, name: '进行中', itemStyle: {color: '#22c55e'}},
  {value: 0, name: '已完成', itemStyle: {color: '#6366f1'}},
  {value: 0, name: '失败', itemStyle: {color: '#f87171'}}
])

const taskTrendDays = ref<string[]>([])
const taskTrendValues = ref<number[]>([])

const chartStatusRef = ref<HTMLElement | null>(null)
const chartTrendRef = ref<HTMLElement | null>(null)
let chartStatus: echarts.ECharts | null = null
let chartTrend: echarts.ECharts | null = null

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

const getStatusPieOption = () => {
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
      data: taskStatusPieData.value,
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

const getTrendLineOption = () => {
  const textColor = chartTextColor()
  const mutedColor = chartMutedColor()
  const isDark = document.documentElement.classList.contains('dark')
  const vals = taskTrendValues.value
  const maxVal = vals.length > 0 ? Math.max(...vals, 1) : 1
  const yMax = Math.ceil(maxVal * 1.2) || 1
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
      data: taskTrendDays.value,
      axisLine: {lineStyle: {color: mutedColor}},
      axisLabel: {color: textColor, fontSize: 13, margin: 14}
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: yMax,
      splitNumber: Math.min(5, yMax),
      splitLine: {lineStyle: {color: mutedColor, type: 'dashed', opacity: 0.3}},
      axisLabel: {color: textColor, fontSize: 13}
    },
    series: [{
      type: 'line',
      data: taskTrendValues.value,
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

const resizeCharts = () => {
  chartStatus?.resize()
  chartTrend?.resize()
}

watch(actualTheme, () => {
  chartStatus?.setOption(getStatusPieOption(), {notMerge: true})
  chartTrend?.setOption(getTrendLineOption(), {notMerge: true})
})
watch(taskStatusPieData, () => {
  chartStatus?.setOption(getStatusPieOption(), {notMerge: true})
}, {deep: true})
watch([taskTrendDays, taskTrendValues], () => {
  chartTrend?.setOption(getTrendLineOption(), {notMerge: true})
}, {deep: true})

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  getTaskStatusStats((data) => {
    taskStatusPieData.value = [
      {value: data.notStarted, name: '未开始', itemStyle: {color: '#94a3b8'}},
      {value: data.inProgress, name: '进行中', itemStyle: {color: '#22c55e'}},
      {value: data.completed, name: '已完成', itemStyle: {color: '#6366f1'}},
      {value: data.failed, name: '失败', itemStyle: {color: '#f87171'}}
    ]
  })
  getTaskTrend7Days((data) => {
    taskTrendDays.value = data.dates
    taskTrendValues.value = data.counts
  })
  nextTick(() => {
    if (chartStatusRef.value) {
      chartStatus = echarts.init(chartStatusRef.value)
      chartStatus.setOption(getStatusPieOption())
    }
    if (chartTrendRef.value) {
      chartTrend = echarts.init(chartTrendRef.value)
      chartTrend.setOption(getTrendLineOption())
    }
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
})
</script>

<template>
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
</template>
