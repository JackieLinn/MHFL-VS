<script setup lang="ts">
import {computed, ref, watch, onMounted, onBeforeUnmount, nextTick} from 'vue'
import {useI18n} from 'vue-i18n'
import {useTheme} from '@/stores/theme'
import * as echarts from 'echarts'
import type {RoundVO} from '@/api/task'

const {t, locale} = useI18n()
const {actualTheme} = useTheme()

const props = withDefaults(
  defineProps<{
    rounds: RoundVO[]
    taskId?: number
    hasRealData?: boolean
    loading?: boolean
  }>(),
  { hasRealData: true }
)

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

/** 横坐标：后端 roundNum 从 0 开始，前端展示 +1 */
const xAxisLabels = computed(() =>
    props.rounds.map((r) => (r.roundNum ?? 0) + 1)
)

/** 横坐标刻度自适应：约 6~12 个标签，步长取整（500→50, 100→10, 1000→100） */
const xAxisInterval = computed(() => {
  const n = xAxisLabels.value.length
  if (n <= 1) return 1
  const maxRound = xAxisLabels.value[n - 1] ?? 1
  return Math.max(1, Math.ceil(maxRound / 10))
})

/** 是否显示该横坐标刻度（首、尾、以及 step 整数倍） */
const showXAxisLabel = (idx: number): boolean => {
  const labels = xAxisLabels.value
  if (idx < 0 || idx >= labels.length) return false
  const val = labels[idx] ?? 0
  const step = xAxisInterval.value
  if (idx === 0 || idx === labels.length - 1) return true
  return val % step === 0
}

const chartSeriesData = computed(() => {
  const r = props.rounds
  const toVal = (v: number | null | undefined) =>
      v != null && Number.isFinite(v) ? v : null
  return {
    accuracy: r.map((x) => toVal(x.accuracy)),
    precision: r.map((x) => toVal(x.precision)),
    recall: r.map((x) => toVal(x.recall)),
    f1Score: r.map((x) => toVal(x.f1Score))
  }
})

const metricTitleKeys: Record<string, string> = {
  accuracy: 'chartAccuracy',
  precision: 'chartPrecision',
  recall: 'chartRecall',
  f1Score: 'chartF1'
}

const STORAGE_KEY_PREFIX = 'mhfl-task-datazoom-'

const needDataZoom = computed(() => xAxisLabels.value.length > 1)

const getStoredZoom = (): { start: number; end: number } | null => {
  const tid = props.taskId
  if (!tid) return null
  try {
    const s = localStorage.getItem(STORAGE_KEY_PREFIX + tid)
    if (!s) return null
    const obj = JSON.parse(s) as { start?: number; end?: number }
    if (typeof obj?.start === 'number' && typeof obj?.end === 'number') return obj
    return null
  } catch {
    return null
  }
}

const saveZoom = (start: number, end: number) => {
  const tid = props.taskId
  if (!tid) return
  try {
    localStorage.setItem(STORAGE_KEY_PREFIX + tid, JSON.stringify({ start, end }))
  } catch {
    /* ignore */
  }
}

const dataZoomRange = ref<{ start: number; end: number } | null>(null)

const initDataZoomRange = () => {
  const stored = getStoredZoom()
  dataZoomRange.value = stored ?? { start: 0, end: 100 }
}

const makeChartOption = (metricVal: 'accuracy' | 'precision' | 'recall' | 'f1Score') => {
  const titleKey = metricTitleKeys[metricVal] ?? metricVal
  const textColor = chartTextColor()
  const mutedColor = chartMutedColor()
  const isDark = document.documentElement.classList.contains('dark')
  const labels = xAxisLabels.value
  const seriesData = chartSeriesData.value[metricVal]

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
    grid: {
      left: 52,
      right: 20,
      top: 36,
      bottom: needDataZoom.value ? 92 : 48
    },
    dataZoom: needDataZoom.value
      ? [
          {
            type: 'slider',
            show: true,
            xAxisIndex: [0],
            start: dataZoomRange.value?.start ?? 0,
            end: dataZoomRange.value?.end ?? 100,
            left: 50,
            right: 50,
            height: 22,
            bottom: 2,
            borderColor: 'transparent',
            fillerColor: 'rgba(99, 102, 241, 0.2)',
            handleStyle: {color: chartColor},
            textStyle: {color: textColor, fontSize: 11},
            minSpan: 5,
            maxSpan: 100,
            showDetail: true,
            showDataShadow: 'auto',
            dataBackground: {lineStyle: {opacity: 0.3}, areaStyle: {opacity: 0.05}}
          }
        ]
      : undefined,
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      name: t('pages.recommended.chartXAxis'),
      nameLocation: 'middle',
      nameGap: 52,
      nameTextStyle: {color: textColor, fontSize: 12, fontWeight: 500},
      axisLine: {lineStyle: {color: mutedColor}},
      axisLabel: {
        color: textColor,
        fontSize: 11,
        margin: 8,
        interval: (idx: number) => showXAxisLabel(idx)
      }
    },
    yAxis: {
      type: 'value',
      scale: props.hasRealData,
      min: props.hasRealData
        ? (value: { min: number }) => Math.max(0, (value.min ?? 0) - 0.02)
        : 0,
      max: props.hasRealData
        ? (value: { max: number }) => Math.min(1, (value.max ?? 1) + 0.02)
        : 1,
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
      name: t(`pages.recommended.chart${metricVal.charAt(0).toUpperCase() + metricVal.slice(1)}`),
      type: 'line',
      data: seriesData,
      connectNulls: true,
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

const onDataZoom = (ev: { start?: number; end?: number; batch?: Array<{ start?: number; end?: number }> }) => {
  const b = ev?.batch?.[0] ?? ev
  const start = b?.start
  const end = b?.end
  if (typeof start === 'number' && typeof end === 'number') {
    dataZoomRange.value = { start, end }
    saveZoom(start, end)
  }
}

const initCharts = () => {
  nextTick(() => {
    initDataZoomRange()
    if (chartAccuracyRef.value && !chartAccuracyInst) {
      chartAccuracyInst = echarts.init(chartAccuracyRef.value)
      chartAccuracyInst.setOption(makeChartOption('accuracy'))
      chartAccuracyInst.on('dataZoom', onDataZoom)
    }
    if (chartPrecisionRef.value && !chartPrecisionInst) {
      chartPrecisionInst = echarts.init(chartPrecisionRef.value)
      chartPrecisionInst.setOption(makeChartOption('precision'))
      chartPrecisionInst.on('dataZoom', onDataZoom)
    }
    if (chartRecallRef.value && !chartRecallInst) {
      chartRecallInst = echarts.init(chartRecallRef.value)
      chartRecallInst.setOption(makeChartOption('recall'))
      chartRecallInst.on('dataZoom', onDataZoom)
    }
    if (chartF1Ref.value && !chartF1Inst) {
      chartF1Inst = echarts.init(chartF1Ref.value)
      chartF1Inst.setOption(makeChartOption('f1Score'))
      chartF1Inst.on('dataZoom', onDataZoom)
    }
    connectCharts()
  })
}

const connectCharts = () => {
  const charts = [chartAccuracyInst, chartPrecisionInst, chartRecallInst, chartF1Inst].filter(Boolean)
  if (charts.length === 4) echarts.connect(charts as echarts.ECharts[])
}

const updateCharts = () => {
  chartAccuracyInst?.setOption(makeChartOption('accuracy'), {notMerge: true})
  chartPrecisionInst?.setOption(makeChartOption('precision'), {notMerge: true})
  chartRecallInst?.setOption(makeChartOption('recall'), {notMerge: true})
  chartF1Inst?.setOption(makeChartOption('f1Score'), {notMerge: true})
}

const resizeCharts = () => {
  chartAccuracyInst?.resize()
  chartPrecisionInst?.resize()
  chartRecallInst?.resize()
  chartF1Inst?.resize()
}

watch(() => props.taskId, () => {
  initDataZoomRange()
  updateCharts()
})

watch([chartSeriesData, needDataZoom, actualTheme, locale], () => {
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
  chartAccuracyInst?.off('dataZoom', onDataZoom)
  chartPrecisionInst?.off('dataZoom', onDataZoom)
  chartRecallInst?.off('dataZoom', onDataZoom)
  chartF1Inst?.off('dataZoom', onDataZoom)
  chartAccuracyInst?.dispose()
  chartPrecisionInst?.dispose()
  chartRecallInst?.dispose()
  chartF1Inst?.dispose()
})
</script>

<template>
  <section v-loading="loading"
           class="task-detail-section task-detail-tech-card relative overflow-hidden rounded-xl py-5 px-6 shrink-0">
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
  padding-bottom: 24px;
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
