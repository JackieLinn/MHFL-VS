<script setup lang="ts">
import {computed, ref, watch, onMounted, onBeforeUnmount, nextTick} from 'vue'
import {useI18n} from 'vue-i18n'
import {useTheme} from '@/stores/theme'
import * as echarts from 'echarts'
import {algorithmKeys, chartColors} from './recommendedConstants'

const {t, locale} = useI18n()
const {actualTheme} = useTheme()
const props = defineProps<{
  dataset: 'cifar100' | 'tiny-imagenet'
  sigma: number
  rounds: number[]
  algorithmNames?: string[]
  chartSeriesData: Record<string, Array<Array<number | null>>>
  chartSeriesRawData: Record<string, Array<Array<number | null>>>
}>()
const emit = defineEmits<{
  sigmaChange: [value: number]
}>()

const sigmaLocal = ref(2.5)

const numRounds = computed(() => props.rounds?.length ?? 0)

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
const needDataZoom = computed(() => numRounds.value > 50)

const displayAlgoName = (idx: number) => {
  return props.algorithmNames?.[idx] || (algorithmKeys[idx] ? t(`pages.recommended.${algorithmKeys[idx].key}`) : '')
}

const onSigmaInput = (value: number | null) => {
  const v = value == null ? 0 : Math.round((value + Number.EPSILON) * 2) / 2
  sigmaLocal.value = v
  emit('sigmaChange', v)
}

const makeChartOption = (metricVal: string, titleKey: string) => {
  const textColor = chartTextColor()
  const mutedColor = chartMutedColor()
  const isDark = document.documentElement.classList.contains('dark')
  const rounds = props.rounds ?? []
  const seriesData = props.chartSeriesData[metricVal] ?? []
  const rawSeriesData = props.chartSeriesRawData[metricVal] ?? []

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
      formatter: (params: { axisValue?: number; data?: number | null; seriesIndex?: number; dataIndex?: number }[]) => {
        const round = params[0]?.axisValue ?? 0
        const lines = (params ?? []).map((p, i) => {
          const seriesIndex = p?.seriesIndex ?? i
          const dataIndex = p?.dataIndex ?? 0
          const rawVal = rawSeriesData[seriesIndex]?.[dataIndex]
          const fallbackVal = p?.data
          const metricVal = typeof rawVal === 'number' ? rawVal : (typeof fallbackVal === 'number' ? fallbackVal : null)
          const name = displayAlgoName(seriesIndex)
          const val = metricVal != null ? (metricVal * 100).toFixed(2) + '%' : '-'
          return `${name}: ${val}`
        })
        return `Round ${round}<br/>${lines.join('<br/>')}`
      }
    },
    legend: {
      type: 'scroll',
      bottom: needDataZoom.value ? 28 : 0,
      left: 'center',
      textStyle: {color: textColor, fontSize: 13},
      itemWidth: 16,
      itemHeight: 10,
      itemGap: 12,
      padding: [2, 0, 0, 0],
      data: algorithmKeys.map((_, idx) => displayAlgoName(idx))
    },
    grid: {left: 52, right: 20, top: 36, bottom: needDataZoom.value ? 110 : 78},
    dataZoom: needDataZoom.value
        ? [
          {
            type: 'slider',
            show: true,
            xAxisIndex: [0],
            start: 0,
            end: 100,
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
      data: rounds,
      name: t('pages.recommended.chartXAxis'),
      nameLocation: 'middle',
      nameGap: 38,
      nameTextStyle: {color: textColor, fontSize: 12, fontWeight: 500},
      axisLine: {lineStyle: {color: mutedColor}},
      axisLabel: {
        color: textColor,
        fontSize: 11,
        margin: 8,
        interval: (idx: number) => {
          if (idx === 0 || idx === numRounds.value - 1) return true
          const step = Math.max(1, Math.ceil(numRounds.value / 10))
          return (idx + 1) % step === 0
        }
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
    series: algorithmKeys.map((algo, idx) => ({
      name: displayAlgoName(idx),
      type: 'line',
      data: seriesData[idx] ?? [],
      smooth: false,
      connectNulls: true,
      symbol: 'none',
      lineStyle: {width: 2.5, color: chartColors[idx]},
      itemStyle: {color: chartColors[idx]},
      emphasis: {focus: 'series', lineStyle: {width: 3.5}}
    }))
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
      chartF1Inst.setOption(makeChartOption('f1', 'chartF1'))
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
  chartF1Inst?.setOption(makeChartOption('f1', 'chartF1'), {notMerge: true})
}

const resizeCharts = () => {
  chartAccuracyInst?.resize()
  chartPrecisionInst?.resize()
  chartRecallInst?.resize()
  chartF1Inst?.resize()
}

watch([() => props.dataset, () => props.rounds, () => props.algorithmNames, () => props.chartSeriesData, () => props.chartSeriesRawData, needDataZoom, actualTheme, locale], () => {
  updateCharts()
}, {deep: true})

watch(
    () => props.sigma,
    (value) => {
      sigmaLocal.value = value
    },
    {immediate: true}
)

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  initCharts()
  resizeObserver = new ResizeObserver(() => resizeCharts())
  const container = document.querySelector('.recommended-content')
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
  <section class="recommended-section recommended-tech-card relative overflow-hidden rounded-xl py-5 px-6 shrink-0">
    <div class="recommended-card-glow"></div>
    <div class="recommended-card-scanline"></div>
    <div class="recommended-curve-head relative z-[1] mb-4">
      <h3 class="recommended-section-title m-0 text-[15px] font-semibold flex items-center gap-2">
        <span class="i-mdi-chart-timeline-variant recommended-section-icon text-xl"></span>
        {{ $t('pages.recommended.chartSectionTitle') }}
      </h3>
      <div class="recommended-sigma-panel">
        <span class="recommended-sigma-label">{{ $t('pages.recommended.curveSigmaLabel') }}</span>
        <el-slider
            :model-value="sigmaLocal"
            :min="0"
            :max="5"
            :step="0.5"
            size="small"
            class="recommended-sigma-slider"
            @input="onSigmaInput"
        />
        <span class="recommended-sigma-value">σ = {{ sigmaLocal.toFixed(1) }}</span>
      </div>
    </div>
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5 relative z-[1]">
      <div class="recommended-chart-card">
        <div ref="chartAccuracyRef" class="recommended-chart-inner w-full h-[360px]"></div>
      </div>
      <div class="recommended-chart-card">
        <div ref="chartPrecisionRef" class="recommended-chart-inner w-full h-[360px]"></div>
      </div>
      <div class="recommended-chart-card">
        <div ref="chartRecallRef" class="recommended-chart-inner w-full h-[360px]"></div>
      </div>
      <div class="recommended-chart-card">
        <div ref="chartF1Ref" class="recommended-chart-inner w-full h-[360px]"></div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.recommended-curve-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.recommended-sigma-panel {
  min-width: 300px;
  max-width: 420px;
  width: clamp(300px, 40vw, 420px);
  height: 34px;
  padding: 0 10px;
  border-radius: 10px;
  border: 1px solid var(--home-card-border);
  background: var(--home-hover-bg);
  display: flex;
  align-items: center;
  gap: 8px;
}

.recommended-sigma-label {
  color: var(--home-text-muted);
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.recommended-sigma-value {
  color: var(--home-text-primary);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  min-width: 56px;
  text-align: right;
}

.recommended-sigma-slider {
  flex: 1;
  --el-slider-main-bg-color: #6366f1;
  --el-slider-runway-bg-color: var(--home-card-border);
  --el-slider-stop-bg-color: var(--home-card-border);
}

.recommended-chart-card {
  background: var(--home-hover-bg);
  border: 1px solid var(--home-card-border);
  border-radius: 12px;
  padding: 16px;
  padding-bottom: 24px;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}

.recommended-chart-card:hover {
  border-color: rgba(99, 102, 241, 0.3);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.06);
}

html.dark .recommended-chart-card {
  background: rgba(99, 102, 241, 0.03);
}

html.dark .recommended-chart-card:hover {
  border-color: rgba(99, 102, 241, 0.4);
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.08);
}

.recommended-chart-inner {
  min-height: 0;
}
</style>
