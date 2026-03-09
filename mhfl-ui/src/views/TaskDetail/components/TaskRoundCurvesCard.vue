<script setup lang="ts">
import {computed, ref, watch, onMounted, onBeforeUnmount, nextTick} from 'vue'
import {useI18n} from 'vue-i18n'
import {useTheme} from '@/stores/theme'
import * as echarts from 'echarts'

const {t, locale} = useI18n()
const {actualTheme} = useTheme()

const props = defineProps<{
  numRounds: number
  chartSeriesData: Record<string, number[]>
}>()

const getChartColorVar = (name: string): string => {
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || (document.documentElement.classList.contains('dark') ? '#e2e8f0' : '#1e293b')
}

const chartTextColor = () => getChartColorVar('--home-text-primary')
const chartMutedColor = () => getChartColorVar('--home-text-muted')
const chartTooltipBg = () =>
    document.documentElement.classList.contains('dark')
        ? 'rgba(15, 23, 42, 0.92)'
        : 'rgba(255, 255, 255, 0.96)'
const chartTooltipBorder = () => getChartColorVar('--home-card-border')

const chartColor = '#6366f1'

const makeChartOption = (metricVal: string, titleKey: string) => {
  const textColor = chartTextColor()
  const mutedColor = chartMutedColor()
  const isDark = document.documentElement.classList.contains('dark')
  const rounds = Array.from({length: props.numRounds}, (_, i) => i + 1)
  const seriesData = props.chartSeriesData[metricVal] ?? []

  return {
    backgroundColor: 'transparent',
    title: {
      text: t(`pages.recommended.${titleKey}`),
      left: 0,
      top: 0,
      textStyle: {color: textColor, fontSize: 14, fontWeight: 600}
    },
    tooltip: {
      trigger: 'axis',
      textStyle: {fontSize: 12, color: textColor},
      axisPointer: {
        type: 'line',
        lineStyle: {color: isDark ? 'rgba(99,102,241,0.5)' : 'rgba(99,102,241,0.3)', type: 'dashed'}
      },
      backgroundColor: chartTooltipBg(),
      borderColor: chartTooltipBorder(),
      borderWidth: 1,
      padding: [8, 12],
      formatter: (params: { axisValue?: number; data?: number }[]) => {
        const round = params[0]?.axisValue ?? 0
        const d = params[0]?.data
        const val = typeof d === 'number' ? (d * 100).toFixed(2) + '%' : '-'
        return `Round ${round}<br/>${val}`
      }
    },
    grid: {left: 52, right: 20, top: 36, bottom: 48},
    xAxis: {
      type: 'category',
      data: rounds,
      name: t('pages.recommended.chartXAxis'),
      nameLocation: 'middle',
      nameGap: 36,
      nameTextStyle: {color: textColor, fontSize: 12, fontWeight: 500},
      axisLine: {lineStyle: {color: mutedColor}},
      axisLabel: {
        color: textColor,
        fontSize: 11,
        margin: 8,
        interval: (idx: number) => idx === 0 || idx % 50 === 49 || idx === props.numRounds - 1
      }
    },
    yAxis: {
      type: 'value',
      scale: true,
      axisLabel: {
        color: textColor,
        fontSize: 11,
        formatter: (v: number) => (v * 100).toFixed(1) + '%'
      },
      splitLine: {lineStyle: {color: mutedColor, type: 'dashed', opacity: 0.25}},
      axisLine: {show: false},
      axisTick: {show: false}
    },
    series: [{
      name: t('pages.recommended.chartAccuracy'),
      type: 'line',
      data: seriesData,
      smooth: 0.25,
      symbol: 'none',
      lineStyle: {width: 2.5, color: chartColor},
      itemStyle: {color: chartColor},
      emphasis: {focus: 'series', lineStyle: {width: 3.5}}
    }]
  }
}

const chartAccuracyRef = ref<HTMLElement | null>(null)
const chartPrecisionRef = ref<HTMLElement | null>(null)
const chartRecallRef = ref<HTMLElement | null>(null)
const chartF1Ref = ref<HTMLElement | null>(null)
let chartAccuracyInst: echarts.ECharts | null = null
let chartPrecisionInst: echarts.ECharts | null = null
let chartRecallInst: echarts.ECharts | null = null
let chartF1Inst: echarts.ECharts | null = null

const initCharts = () => {
  nextTick(() => {
    if (chartAccuracyRef.value && !chartAccuracyInst) {
      chartAccuracyInst = echarts.init(chartAccuracyRef.value)
      chartAccuracyInst.setOption(makeChartOption('accuracy', 'chartAccuracy'))
    }
    if (chartPrecisionRef.value && !chartPrecisionInst) {
      chartPrecisionInst = echarts.init(chartPrecisionRef.value)
      chartPrecisionInst.setOption(makeChartOption('precision', 'chartPrecision'))
    }
    if (chartRecallRef.value && !chartRecallInst) {
      chartRecallInst = echarts.init(chartRecallRef.value)
      chartRecallInst.setOption(makeChartOption('recall', 'chartRecall'))
    }
    if (chartF1Ref.value && !chartF1Inst) {
      chartF1Inst = echarts.init(chartF1Ref.value)
      chartF1Inst.setOption(makeChartOption('f1Score', 'chartF1'))
    }
    connectCharts()
  })
}

const connectCharts = () => {
  const charts = [chartAccuracyInst, chartPrecisionInst, chartRecallInst, chartF1Inst].filter(Boolean)
  if (charts.length === 4) echarts.connect(charts as echarts.ECharts[])
}

const updateCharts = () => {
  chartAccuracyInst?.setOption(makeChartOption('accuracy', 'chartAccuracy'), {notMerge: true})
  chartPrecisionInst?.setOption(makeChartOption('precision', 'chartPrecision'), {notMerge: true})
  chartRecallInst?.setOption(makeChartOption('recall', 'chartRecall'), {notMerge: true})
  chartF1Inst?.setOption(makeChartOption('f1Score', 'chartF1'), {notMerge: true})
}

const resizeCharts = () => {
  chartAccuracyInst?.resize()
  chartPrecisionInst?.resize()
  chartRecallInst?.resize()
  chartF1Inst?.resize()
}

watch([() => props.chartSeriesData, actualTheme, locale], () => {
  updateCharts()
}, {deep: true})

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  initCharts()
  resizeObserver = new ResizeObserver(() => resizeCharts())
  const container = document.querySelector('.task-detail-content')
  if (container) resizeObserver.observe(container)
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', resizeCharts)
  chartAccuracyInst?.dispose()
  chartPrecisionInst?.dispose()
  chartRecallInst?.dispose()
  chartF1Inst?.dispose()
})
</script>

<template>
  <section class="task-detail-section task-detail-tech-card relative overflow-hidden rounded-xl py-5 px-6 shrink-0">
    <div class="task-detail-card-glow"></div>
    <div class="task-detail-card-scanline"></div>
    <h3 class="task-detail-section-title m-0 mb-4 text-[15px] font-semibold flex items-center gap-2 relative z-[1]">
      <span class="i-mdi-chart-timeline-variant task-detail-section-icon text-xl"></span>
      {{ $t('pages.recommended.chartSectionTitle') }}
    </h3>
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5 relative z-[1]">
      <div class="task-detail-chart-card">
        <div ref="chartAccuracyRef" class="task-detail-chart-inner w-full h-[360px]"></div>
      </div>
      <div class="task-detail-chart-card">
        <div ref="chartPrecisionRef" class="task-detail-chart-inner w-full h-[360px]"></div>
      </div>
      <div class="task-detail-chart-card">
        <div ref="chartRecallRef" class="task-detail-chart-inner w-full h-[360px]"></div>
      </div>
      <div class="task-detail-chart-card">
        <div ref="chartF1Ref" class="task-detail-chart-inner w-full h-[360px]"></div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.task-detail-chart-card {
  background: var(--home-hover-bg);
  border: 1px solid var(--home-card-border);
  border-radius: 12px;
  padding: 16px;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}

.task-detail-chart-card:hover {
  border-color: rgba(99, 102, 241, 0.3);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.06);
}

.task-detail-chart-inner {
  min-height: 0;
}
</style>
