<script setup lang="ts">
import {computed, ref, watch, onMounted, onBeforeUnmount, nextTick} from 'vue'
import {useI18n} from 'vue-i18n'
import {useTheme} from '@/stores/theme'
import * as echarts from 'echarts'

const {t, locale} = useI18n()
const {actualTheme} = useTheme()
const props = defineProps<{
  dataset: 'cifar100' | 'tiny-imagenet'
}>()

// 6 个算法 key，用于 i18n 和数据结构
const algorithmKeys = [
  {key: 'algoStandalone', color: 'blue'},
  {key: 'algoFedProto', color: 'green'},
  {key: 'algoFedAvg', color: 'teal'},
  {key: 'algoFedSSA', color: 'fuchsia'},
  {key: 'algoLGFedAvg', color: 'amber'},
  {key: 'algoOurs', color: 'rose'}
] as const

// 6 个算法在折线图中的颜色（与徽章一致）
const chartColors = ['#3b82f6', '#22c55e', '#0d9488', '#d946ef', '#f59e0b', '#f43f5e'] as const

// 生成缓慢上升并收敛的曲线：带适当波折，finalValue 为收敛值
const generateConvergenceCurve = (
    finalValue: number,
    numRounds: number,
    initRatio = 0.18,
    convergeSpeed = 3.2,
    noiseScale = 0.018
): number[] => {
  const init = finalValue * initRatio
  const points: number[] = []
  for (let r = 0; r < numRounds; r++) {
    const t = r / Math.max(numRounds - 1, 1)
    const progress = 1 - Math.exp(-convergeSpeed * t)
    const base = init + (finalValue - init) * progress
    const decay = 1 - t * 0.6
    const noise = (Math.random() - 0.5) * 2 * noiseScale * decay
    points.push(Math.max(0.01, Math.min(0.99, base + noise)))
  }
  points[points.length - 1] = finalValue
  return points
}

// 占位数据：后续由接口根据 dataset 参数获取。6 组数据，每组 5 个指标；Ours 均为最优
const algorithmMetrics = computed(() => {
  if (props.dataset === 'cifar100') {
    return [
      {loss: 0.952, accuracy: 0.5812, precision: 0.572, recall: 0.568, f1: 0.570},
      {loss: 0.978, accuracy: 0.5618, precision: 0.552, recall: 0.548, f1: 0.550},
      {loss: 0.918, accuracy: 0.6124, precision: 0.603, recall: 0.598, f1: 0.600},
      {loss: 0.935, accuracy: 0.5987, precision: 0.589, recall: 0.585, f1: 0.587},
      {loss: 0.941, accuracy: 0.5903, precision: 0.581, recall: 0.577, f1: 0.579},
      {loss: 0.868, accuracy: 0.6479, precision: 0.638, recall: 0.634, f1: 0.636}
    ]
  }
  return [
    {loss: 1.452, accuracy: 0.3821, precision: 0.372, recall: 0.368, f1: 0.370},
    {loss: 1.488, accuracy: 0.3615, precision: 0.351, recall: 0.347, f1: 0.349},
    {loss: 1.398, accuracy: 0.4128, precision: 0.403, recall: 0.398, f1: 0.400},
    {loss: 1.418, accuracy: 0.4012, precision: 0.391, recall: 0.387, f1: 0.389},
    {loss: 1.435, accuracy: 0.3906, precision: 0.381, recall: 0.376, f1: 0.378},
    {loss: 1.328, accuracy: 0.4633, precision: 0.454, recall: 0.449, f1: 0.451}
  ]
})

// 100 个客户端四个指标：占位数据，后续由接口获取
const clientMetricOptions = [
  {val: 'accuracy', key: 'clientMetricAccuracy'},
  {val: 'precision', key: 'clientMetricPrecision'},
  {val: 'recall', key: 'clientMetricRecall'},
  {val: 'f1', key: 'clientMetricF1'}
] as const

const selectedClientMetric = ref<'accuracy' | 'precision' | 'recall' | 'f1'>('accuracy')

const clientDetailVisible = ref(false)
const selectedClientId = ref(0)

const openClientDetail = (clientIndex: number) => {
  selectedClientId.value = clientIndex
  selectedClientDetailMetric.value = selectedClientMetric.value
  clientDetailVisible.value = true
}

const selectedClientDetailMetric = ref<'accuracy' | 'precision' | 'recall' | 'f1'>('accuracy')

const clientDetailChartRef = ref<HTMLElement | null>(null)
let clientDetailChartInst: echarts.ECharts | null = null

const closeClientDetail = () => {
  clientDetailVisible.value = false
  clientDetailChartInst?.dispose()
  clientDetailChartInst = null
}

const onClientDetailClosed = () => {
  clientDetailChartInst?.dispose()
  clientDetailChartInst = null
}

const initClientDetailChart = () => {
  nextTick(() => {
    if (!clientDetailChartRef.value || !clientDetailVisible.value) return
    clientDetailChartInst?.dispose()
    clientDetailChartInst = echarts.init(clientDetailChartRef.value)
    updateClientDetailChart()
  })
}

const updateClientDetailChart = () => {
  if (!clientDetailChartInst || !clientDetailVisible.value) return
  const data = clientDetailChartData.value
  const metric = selectedClientDetailMetric.value
  if (!data?.[metric]) return
  const textColor = chartTextColor()
  const mutedColor = chartMutedColor()
  const isDark = document.documentElement.classList.contains('dark')
  const rounds = Array.from({length: numRounds.value}, (_, i) => i + 1)
  const option = {
    backgroundColor: 'transparent',
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
        const lines = (params ?? []).map((p, i) => {
          const name = algorithmKeys[i] ? t(`pages.recommended.${algorithmKeys[i].key}`) : ''
          const val = typeof p?.data === 'number' ? (p.data * 100).toFixed(2) + '%' : '-'
          return `${name}: ${val}`
        })
        return `Round ${round}<br/>${lines.join('<br/>')}`
      }
    },
    legend: {
      type: 'scroll',
      bottom: 0,
      left: 'center',
      textStyle: {color: textColor, fontSize: 12},
      itemWidth: 20,
      itemHeight: 10,
      itemGap: 14,
      padding: [2, 0, 0, 0],
      data: algorithmKeys.map((a) => t(`pages.recommended.${a.key}`))
    },
    grid: {left: 52, right: 20, top: 24, bottom: 72},
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
        interval: (idx: number) => idx === 0 || idx % 50 === 49 || idx === numRounds.value - 1
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
      name: t(`pages.recommended.${algo.key}`),
      type: 'line',
      data: data[metric]?.[idx] ?? [],
      smooth: 0.25,
      symbol: 'none',
      lineStyle: {width: 2.5, color: chartColors[idx]},
      itemStyle: {color: chartColors[idx]},
      emphasis: {focus: 'series', lineStyle: {width: 3.5}}
    }))
  }
  clientDetailChartInst.setOption(option, {notMerge: true})
}

// 客户端 i 对应模型：1-5 循环
const getClientModel = (i: number) => ((i - 1) % 5) + 1

const cnnColors = ['#3b82f6', '#22c55e', '#0d9488', '#d946ef', '#f59e0b'] as const

// 100 客户端 × 6 算法 × 4 指标；基于 algorithmMetrics 加 per-client 噪声
const clientMetrics = computed(() => {
  const bases = algorithmMetrics.value
  const spread = props.dataset === 'cifar100' ? 0.08 : 0.06
  return Array.from({length: 100}, (_, clientIdx) => {
    const noise = (Math.sin(clientIdx * 0.5) * 0.5 + Math.cos(clientIdx * 0.3) * 0.5) * spread
    return {
      accuracy: bases.map((row) => Math.max(0.2, Math.min(0.85, (row.accuracy ?? 0) + noise))),
      precision: bases.map((row) => Math.max(0.2, Math.min(0.85, (row.precision ?? 0) + noise * 0.98))),
      recall: bases.map((row) => Math.max(0.2, Math.min(0.85, (row.recall ?? 0) + noise * 1.02))),
      f1: bases.map((row) => Math.max(0.2, Math.min(0.85, (row.f1 ?? 0) + noise)))
    }
  })
})

// 客户端 clientIndex 在 round r 是否被选中（每轮 10 个：r%10, 10+r%10, ..., 90+r%10）
const isClientTrainedInRound = (clientIndex: number, round: number) =>
    clientIndex % 10 === round % 10

// 生成客户端详情曲线：500 轮，被训练则更新，否则沿用上一值
const clientDetailChartData = computed(() => {
  const clientIdx = selectedClientId.value - 1
  if (clientIdx < 0) return null
  const metrics = clientMetrics.value[clientIdx]
  if (!metrics) return null
  const rounds = numRounds.value
  const result: Record<string, number[][]> = {}
  for (const m of chartMetricKeys) {
    result[m.val] = algorithmKeys.map((_, ai) => {
      const finalVal = metrics[m.val]?.[ai] ?? 0
      const init = finalVal * 0.18
      const points: number[] = []
      let last = init
      for (let r = 0; r < rounds; r++) {
        if (isClientTrainedInRound(clientIdx, r)) {
          const t = r / Math.max(rounds - 1, 1)
          const progress = 1 - Math.exp(-3.2 * t)
          last = init + (finalVal - init) * progress + (Math.random() - 0.5) * 0.02
        }
        points.push(Math.max(0.01, Math.min(0.99, last)))
      }
      points[points.length - 1] = finalVal
      return points
    })
  }
  return result
})

watch(clientDetailVisible, (visible) => {
  if (visible) {
    nextTick(() => setTimeout(initClientDetailChart, 50))
  }
})
watch([selectedClientId, selectedClientDetailMetric, clientDetailChartData], () => {
  if (clientDetailVisible.value) updateClientDetailChart()
}, {deep: true})

// 6 个实验设置（不含数据集，顶部已有数据集切换）
const settingKeys = [
  {key: 'settingNumNodes', val: 'numNodes', icon: 'i-mdi-account-group-outline'},
  {key: 'settingFraction', val: 'fraction', icon: 'i-mdi-percent'},
  {key: 'settingClassesPerNode', val: 'classesPerNode', icon: 'i-mdi-label-multiple-outline'},
  {key: 'settingLowProb', val: 'lowProb', icon: 'i-mdi-chart-line'},
  {key: 'settingNumSteps', val: 'numSteps', icon: 'i-mdi-repeat'},
  {key: 'settingEpochs', val: 'epochs', icon: 'i-mdi-backup-restore'}
] as const

const settings = computed(() => {
  if (props.dataset === 'cifar100') {
    return {
      numNodes: 100,
      fraction: 0.1,
      classesPerNode: 5,
      lowProb: 0.5,
      numSteps: 500,
      epochs: 10
    }
  }
  return {
    numNodes: 100,
    fraction: 0.15,
    classesPerNode: 10,
    lowProb: 0.4,
    numSteps: 300,
    epochs: 8
  }
})

// 每行指标的最优值索引：loss 不可比不标，其余取最大
const getBestIndexForMetric = (val: string) => {
  if (val === 'loss') return -1
  const data = algorithmMetrics.value
  if (!data?.length) return -1
  const values = data.map((d) => (d as Record<string, number>)[val] ?? 0)
  return values.indexOf(Math.max(...values))
}

const metricKeys = [
  {key: 'metricLoss', val: 'loss', format: (v: number) => v.toFixed(3), icon: 'i-mdi-chart-line'},
  {key: 'metricAccuracy', val: 'accuracy', format: (v: number) => (v * 100).toFixed(2) + '%', icon: 'i-mdi-target'},
  {
    key: 'metricPrecision',
    val: 'precision',
    format: (v: number) => (v * 100).toFixed(2) + '%',
    icon: 'i-mdi-crosshairs-gps'
  },
  {
    key: 'metricRecall',
    val: 'recall',
    format: (v: number) => (v * 100).toFixed(2) + '%',
    icon: 'i-mdi-chart-areaspline'
  },
  {key: 'metricF1', val: 'f1', format: (v: number) => (v * 100).toFixed(2) + '%', icon: 'i-mdi-chart-bar'}
] as const

// 折线图用到的 4 个指标（不含 loss）
const chartMetricKeys = [
  {key: 'chartAccuracy', val: 'accuracy'},
  {key: 'chartPrecision', val: 'precision'},
  {key: 'chartRecall', val: 'recall'},
  {key: 'chartF1', val: 'f1'}
] as const

const numRounds = computed(() => (props.dataset === 'cifar100' ? 500 : 300))

// 每个指标的 6 条曲线数据
const chartSeriesData = computed(() => {
  const metrics = algorithmMetrics.value
  const rounds = numRounds.value
  const result: Record<string, number[][]> = {}
  for (const m of chartMetricKeys) {
    result[m.val] = metrics.map((row) =>
        generateConvergenceCurve((row as Record<string, number>)[m.val] ?? 0, rounds)
    )
  }
  return result
})

const chartAccuracyRef = ref<HTMLElement | null>(null)
const chartPrecisionRef = ref<HTMLElement | null>(null)
const chartRecallRef = ref<HTMLElement | null>(null)
const chartF1Ref = ref<HTMLElement | null>(null)
let chartAccuracyInst: echarts.ECharts | null = null
let chartPrecisionInst: echarts.ECharts | null = null
let chartRecallInst: echarts.ECharts | null = null
let chartF1Inst: echarts.ECharts | null = null

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

const makeChartOption = (metricVal: string, titleKey: string) => {
  const textColor = chartTextColor()
  const mutedColor = chartMutedColor()
  const isDark = document.documentElement.classList.contains('dark')
  const rounds = Array.from({length: numRounds.value}, (_, i) => i + 1)
  const seriesData = chartSeriesData.value[metricVal] ?? []

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
        const lines = (params ?? []).map((p, i) => {
          const name = algorithmKeys[i] ? t(`pages.recommended.${algorithmKeys[i].key}`) : ''
          const val = typeof p?.data === 'number' ? (p.data * 100).toFixed(2) + '%' : '-'
          return `${name}: ${val}`
        })
        return `Round ${round}<br/>${lines.join('<br/>')}`
      }
    },
    legend: {
      type: 'scroll',
      bottom: 0,
      left: 'center',
      textStyle: {color: textColor, fontSize: 13},
      itemWidth: 16,
      itemHeight: 10,
      itemGap: 12,
      padding: [2, 0, 0, 0],
      data: algorithmKeys.map((a) => t(`pages.recommended.${a.key}`))
    },
    grid: {left: 52, right: 20, top: 36, bottom: 78},
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
        interval: (idx: number) => idx === 0 || idx % 50 === 49 || idx === numRounds.value - 1
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
      name: t(`pages.recommended.${algo.key}`),
      type: 'line',
      data: seriesData[idx] ?? [],
      smooth: 0.25,
      symbol: 'none',
      lineStyle: {width: 2.5, color: chartColors[idx]},
      itemStyle: {color: chartColors[idx]},
      emphasis: {focus: 'series', lineStyle: {width: 3.5}}
    }))
  }
}

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

watch([() => props.dataset, actualTheme, locale], () => {
  updateCharts()
})

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
  <div class="recommended-content flex flex-col gap-6 min-w-0">
    <!-- 实验设置 -->
    <section class="recommended-section recommended-tech-card relative overflow-hidden rounded-xl py-5 px-6 shrink-0">
      <div class="recommended-card-glow"></div>
      <div class="recommended-card-scanline"></div>
      <h3 class="recommended-section-title m-0 mb-4 text-[15px] font-semibold flex items-center gap-2 relative z-[1]">
        <span class="i-mdi-cog-outline recommended-section-icon text-xl"></span>
        {{ $t('pages.recommended.expSettings') }}
      </h3>
      <!-- 6 项实验设置：2x3 网格，两边上下对齐 -->
      <div class="grid grid-cols-2 md:grid-cols-3 gap-x-5 gap-y-4 relative z-[1] mb-5">
        <div
            v-for="item in settingKeys"
            :key="item.key"
            class="recommended-setting-item flex items-start gap-3 py-3 px-3.5 rounded-lg"
        >
          <span :class="[item.icon, 'recommended-setting-icon shrink-0 mt-0.5 text-xl']"></span>
          <div class="recommended-setting-text flex flex-col gap-1 min-w-0">
            <span class="recommended-setting-label text-xs">{{ $t(`pages.recommended.${item.key}`) }}</span>
            <span class="recommended-setting-value text-sm font-semibold tabular-nums">{{ settings[item.val] }}</span>
          </div>
        </div>
      </div>
      <!-- 6 个算法徽章：2x3 网格，与设置区视觉统一 -->
      <div class="relative z-[1]">
        <span class="recommended-setting-label text-xs block mb-2">{{ $t('pages.recommended.settingAlgorithm') }}</span>
        <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
          <span
              v-for="algo in algorithmKeys"
              :key="algo.key"
              class="recommended-algo-badge recommended-algo-badge-grid"
              :class="`recommended-algo-badge-${algo.color}`"
          >
            {{ $t(`pages.recommended.${algo.key}`) }}
          </span>
        </div>
      </div>
    </section>

    <!-- 算法效果对比：表格式布局，数字更大更清晰 -->
    <section class="recommended-section recommended-tech-card relative overflow-hidden rounded-xl py-5 px-6 shrink-0">
      <div class="recommended-card-glow"></div>
      <div class="recommended-card-scanline"></div>
      <h3 class="recommended-section-title m-0 mb-4 text-[15px] font-semibold flex items-center gap-2 relative z-[1]">
        <span class="i-mdi-chart-line recommended-section-icon text-xl"></span>
        {{ $t('pages.recommended.expMetricsCompare') }}
      </h3>
      <div class="recommended-metrics-table relative z-[1] overflow-x-auto">
        <table class="w-full min-w-[560px] border-collapse">
          <thead>
          <tr>
            <th class="recommended-th-metric text-left py-3 px-4 text-sm font-semibold text-[var(--home-text-muted)]">
              {{ $t('pages.recommended.expMetrics') }}
            </th>
            <th
                v-for="algo in algorithmKeys"
                :key="algo.key"
                class="recommended-th-algo py-3 px-4 text-center text-base font-semibold"
                :class="`recommended-th-${algo.color}`"
            >
              {{ $t(`pages.recommended.${algo.key}`) }}
            </th>
          </tr>
          </thead>
          <tbody>
          <tr
              v-for="m in metricKeys"
              :key="m.key"
              class="recommended-metric-row"
          >
            <td class="recommended-td-metric py-3 px-4">
              <span :class="[m.icon, 'recommended-metric-row-icon']"></span>
              <span class="text-sm font-medium text-[var(--home-text-muted)]">{{
                  $t(`pages.recommended.${m.key}`)
                }}</span>
            </td>
            <td
                v-for="(algo, idx) in algorithmKeys"
                :key="algo.key"
                class="recommended-td-value py-3 px-4 text-center text-lg font-bold tabular-nums"
                :class="idx === getBestIndexForMetric(m.val) ? 'recommended-td-best' : 'text-[var(--home-text-primary)]'"
            >
              {{ m.format(algorithmMetrics[idx]?.[m.val] ?? 0) }}
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 4 个指标折线图：轮次 vs 指标值，6 算法同色系 -->
    <section class="recommended-section recommended-tech-card relative overflow-hidden rounded-xl py-5 px-6 shrink-0">
      <div class="recommended-card-glow"></div>
      <div class="recommended-card-scanline"></div>
      <h3 class="recommended-section-title m-0 mb-4 text-[15px] font-semibold flex items-center gap-2 relative z-[1]">
        <span class="i-mdi-chart-timeline-variant recommended-section-icon text-xl"></span>
        {{ $t('pages.recommended.chartSectionTitle') }}
      </h3>
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

    <!-- 各客户端指标：100 个客户端，5 列 20 行，圆形进度环 + 指标切换 -->
    <section class="recommended-section recommended-tech-card relative overflow-hidden rounded-xl py-5 px-6 shrink-0">
      <div class="recommended-card-glow"></div>
      <div class="recommended-card-scanline"></div>
      <div class="flex flex-wrap items-center justify-between gap-4 mb-4 relative z-[1]">
        <h3 class="recommended-section-title m-0 text-[15px] font-semibold flex items-center gap-2">
          <span class="i-mdi-account-multiple-outline recommended-section-icon text-xl"></span>
          {{ $t('pages.recommended.clientAccuracyTitle') }}
        </h3>
        <div class="flex rounded-lg overflow-hidden border border-[var(--home-card-border)]">
          <button
              v-for="opt in clientMetricOptions"
              :key="opt.val"
              type="button"
              class="recommended-client-metric-btn px-3 py-1.5 text-xs font-medium transition-colors"
              :class="selectedClientMetric === opt.val ? 'recommended-client-metric-btn-active' : 'recommended-client-metric-btn-inactive'"
              @click="selectedClientMetric = opt.val"
          >
            {{ $t(`pages.recommended.${opt.key}`) }}
          </button>
        </div>
      </div>
      <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-3 lg:grid-cols-4 gap-4 relative z-[1]">
        <div
            v-for="i in 100"
            :key="i"
            class="recommended-client-cell"
            :class="`recommended-client-cell-cnn${getClientModel(i)}`"
            @click="openClientDetail(i)"
        >
          <div class="recommended-client-header">
            <span class="recommended-client-label">{{ $t('pages.recommended.clientLabel') }} {{ i }}</span>
            <span
                class="recommended-client-model-badge"
                :style="{borderColor: cnnColors[getClientModel(i) - 1], color: cnnColors[getClientModel(i) - 1]}"
            >
              {{ $t(`pages.recommended.clientModelCnn${getClientModel(i)}`) }}
            </span>
          </div>
          <div class="recommended-client-rings-grid">
            <div
                v-for="(algo, ai) in algorithmKeys"
                :key="algo.key"
                class="recommended-client-mini-ring"
                :title="`${$t('pages.recommended.' + algo.key)}: ${((clientMetrics[i - 1]?.[selectedClientMetric]?.[ai] ?? 0) * 100).toFixed(2)}%`"
            >
              <div class="recommended-client-mini-ring-wrap">
                <svg viewBox="0 0 36 36" class="recommended-client-mini-svg">
                  <circle class="recommended-client-ring-bg" cx="18" cy="18" r="15.9"/>
                  <circle
                      class="recommended-client-ring-fill"
                      cx="18"
                      cy="18"
                      r="15.9"
                      :stroke-dasharray="100"
                      :stroke-dashoffset="100 - (clientMetrics[i - 1]?.[selectedClientMetric]?.[ai] ?? 0) * 100"
                      :style="{stroke: chartColors[ai]}"
                  />
                </svg>
                <span class="recommended-client-mini-value tabular-nums">
                  {{ ((clientMetrics[i - 1]?.[selectedClientMetric]?.[ai] ?? 0) * 100).toFixed(2) }}%
                </span>
              </div>
            </div>
          </div>
          <button
              type="button"
              class="recommended-client-detail-btn"
              @click.stop="openClientDetail(i)"
          >
            {{ $t('pages.recommended.clientDetailBtn') }}
          </button>
        </div>
      </div>
    </section>

    <!-- 客户端详情弹窗 -->
    <el-dialog
        v-model="clientDetailVisible"
        :title="$t('pages.recommended.clientDetailTitle')"
        width="640px"
        append-to-body
        align-center
        destroy-on-close
        modal-class="client-detail-dialog"
        @closed="onClientDetailClosed"
    >
      <div v-if="selectedClientId > 0" class="client-detail-content">
        <div class="client-detail-info mb-4">
          <span class="client-detail-label">{{ $t('pages.recommended.clientLabel') }} {{ selectedClientId }}</span>
          <span class="client-detail-model">{{
              $t('pages.recommended.clientModelLabel')
            }}: {{ $t(`pages.recommended.clientModelCnn${getClientModel(selectedClientId)}`) }}</span>
        </div>
        <div class="flex flex-wrap gap-2 mb-4">
          <button
              v-for="opt in clientMetricOptions"
              :key="opt.val"
              type="button"
              class="client-detail-metric-btn px-3 py-1.5 text-xs rounded"
              :class="selectedClientDetailMetric === opt.val ? 'client-detail-metric-active' : ''"
              @click="selectedClientDetailMetric = opt.val"
          >
            {{ $t(`pages.recommended.${opt.key}`) }}
          </button>
        </div>
        <div
            class="client-detail-chart-wrap rounded-xl border border-[var(--home-card-border)] bg-[var(--home-hover-bg)] p-4">
          <div ref="clientDetailChartRef" class="w-full h-[340px]"></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.recommended-section {
  background: var(--home-card-bg);
  border: 1px solid var(--home-card-border);
  box-shadow: 0 2px 8px var(--home-card-shadow);
  transition: border-color 0.32s ease, box-shadow 0.32s ease, background 0.32s ease;
}

.recommended-tech-card:hover {
  border-color: rgba(99, 102, 241, 0.35);
  box-shadow: 0 8px 30px rgba(99, 102, 241, 0.08), 0 2px 8px var(--home-card-shadow);
}

html.dark .recommended-tech-card:hover {
  border-color: rgba(99, 102, 241, 0.5);
  box-shadow: 0 8px 36px rgba(99, 102, 241, 0.12), 0 0 24px rgba(99, 102, 241, 0.06), 0 2px 8px rgba(0, 0, 0, 0.3);
}

.recommended-tech-card:hover .recommended-card-glow {
  opacity: 1;
  height: 2px;
  box-shadow: 0 0 12px rgba(99, 102, 241, 0.3);
}

html.dark .recommended-tech-card:hover .recommended-card-glow {
  box-shadow: 0 0 20px rgba(99, 102, 241, 0.45);
}

.recommended-tech-card:hover .recommended-card-scanline {
  animation-duration: 3s;
}

.recommended-card-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, rgba(99, 102, 241, 0.5) 30%, rgba(14, 165, 164, 0.4) 50%, rgba(99, 102, 241, 0.5) 70%, transparent 100%);
  opacity: 0.6;
  pointer-events: none;
  transition: opacity 0.32s ease, height 0.32s ease, box-shadow 0.32s ease;
}

html.dark .recommended-card-glow {
  opacity: 0.8;
  background: linear-gradient(90deg, transparent 0%, rgba(99, 102, 241, 0.7) 30%, rgba(14, 165, 164, 0.5) 50%, rgba(99, 102, 241, 0.7) 70%, transparent 100%);
}

.recommended-card-scanline {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent 0%, rgba(99, 102, 241, 0.03) 45%, rgba(99, 102, 241, 0.06) 50%, rgba(99, 102, 241, 0.03) 55%, transparent 100%);
  pointer-events: none;
  animation: recommendedScanlineMove 8s ease-in-out infinite;
}

html.dark .recommended-card-scanline {
  background: linear-gradient(90deg, transparent 0%, rgba(99, 102, 241, 0.04) 45%, rgba(99, 102, 241, 0.1) 50%, rgba(99, 102, 241, 0.04) 55%, transparent 100%);
}

@keyframes recommendedScanlineMove {
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

.recommended-section-title {
  color: var(--home-text-primary);
  letter-spacing: 0.3px;
}

.recommended-section-icon {
  color: #6366f1;
  opacity: 0.9;
}

html.dark .recommended-section-icon {
  color: #a5b4fc;
}

.recommended-chart-card {
  background: var(--home-hover-bg);
  border: 1px solid var(--home-card-border);
  border-radius: 12px;
  padding: 16px;
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

.recommended-client-metric-btn-inactive {
  color: var(--home-text-muted);
  background: transparent;
}

.recommended-client-metric-btn-inactive:hover {
  color: var(--home-text-primary);
  background: var(--home-hover-bg);
}

.recommended-client-metric-btn-active {
  color: #fff;
  background: #6366f1;
}

html.dark .recommended-client-metric-btn-active {
  background: #6366f1;
}

.recommended-client-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px 12px;
  border-radius: 12px;
  background: var(--home-hover-bg);
  border: 1px solid transparent;
  border-left: 3px solid var(--client-accent, var(--home-card-border));
  transition: all 0.25s cubic-bezier(0.22, 1, 0.36, 1);
  cursor: pointer;
}

.recommended-client-cell:hover {
  border-color: rgba(99, 102, 241, 0.4);
  background: var(--home-card-bg);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.12);
  transform: translateY(-2px);
}

.recommended-client-rings-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
  margin-bottom: 6px;
  width: 100%;
  min-height: 64px;
}

.recommended-client-mini-ring {
  min-width: 0;
}

.recommended-client-mini-ring-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
}

.recommended-client-mini-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.recommended-client-mini-value {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  font-weight: 600;
  color: var(--home-text-primary);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.03em;
  line-height: 1;
}

.recommended-client-detail-btn {
  font-size: 11px;
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid var(--home-card-border);
  background: transparent;
  color: var(--home-text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.recommended-client-detail-btn:hover {
  border-color: #6366f1;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
}

.client-detail-info {
  display: flex;
  gap: 16px;
  align-items: center;
}

.client-detail-label {
  font-weight: 600;
  color: var(--home-text-primary);
}

.client-detail-model {
  font-size: 13px;
  color: var(--home-text-muted);
}

.client-detail-metric-btn {
  border: 1px solid var(--home-card-border);
  background: var(--home-hover-bg);
  color: var(--home-text-muted);
  transition: all 0.2s ease;
}

.client-detail-metric-btn:hover {
  border-color: rgba(99, 102, 241, 0.4);
  color: #6366f1;
}

.client-detail-metric-active {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.12);
  color: #6366f1;
}

.client-detail-chart-wrap {
  transition: border-color 0.2s ease;
}

.client-detail-chart-wrap:hover {
  border-color: rgba(99, 102, 241, 0.3);
}

html.dark .recommended-client-cell:hover {
  border-color: rgba(99, 102, 241, 0.5);
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.15);
}

.recommended-client-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  margin-bottom: 14px;
}

.recommended-client-label {
  font-size: 12px;
  color: var(--home-text-muted);
  white-space: nowrap;
}

.recommended-client-model-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
  border: 1px solid;
  background: transparent;
  letter-spacing: 0.3px;
}

.recommended-client-cell-cnn1 {
  --client-accent: #3b82f6;
}

.recommended-client-cell-cnn2 {
  --client-accent: #22c55e;
}

.recommended-client-cell-cnn3 {
  --client-accent: #0d9488;
}

.recommended-client-cell-cnn4 {
  --client-accent: #d946ef;
}

.recommended-client-cell-cnn5 {
  --client-accent: #f59e0b;
}

.recommended-client-ring-wrap {
  position: relative;
  width: 52px;
  height: 52px;
}

.recommended-client-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.recommended-client-ring-bg {
  fill: none;
  stroke: var(--home-card-border);
  stroke-width: 3;
}

.recommended-client-ring-fill {
  fill: none;
  stroke: #6366f1;
  stroke-width: 3;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.35s ease;
}

html.dark .recommended-client-ring-fill {
  stroke: #818cf8;
}

.recommended-client-ring-value {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: var(--home-text-primary);
  font-variant-numeric: tabular-nums;
}

.recommended-setting-item {
  position: relative;
  background: var(--home-hover-bg);
  border: 1px solid transparent;
  transition: border-color 0.25s ease, background 0.25s ease, box-shadow 0.25s ease, padding-left 0.25s ease;
}

.recommended-setting-item::before {
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

.recommended-setting-item:hover {
  border-color: var(--home-card-border);
  background: var(--home-card-bg);
  box-shadow: 0 2px 10px rgba(99, 102, 241, 0.05);
  padding-left: 18px;
}

.recommended-setting-item:hover::before {
  opacity: 1;
}

html.dark .recommended-setting-item:hover {
  border-color: rgba(99, 102, 241, 0.2);
  box-shadow: 0 2px 12px rgba(99, 102, 241, 0.06);
}

html.dark .recommended-setting-item::before {
  background: #a5b4fc;
  box-shadow: 0 0 6px rgba(165, 180, 252, 0.4);
}

.recommended-setting-icon {
  color: #6366f1;
  opacity: 0.8;
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}

.recommended-setting-item:hover .recommended-setting-icon {
  transform: scale(1.08);
}

html.dark .recommended-setting-icon {
  color: #a5b4fc;
}

.recommended-setting-label {
  color: var(--home-text-muted);
}

.recommended-setting-value {
  color: var(--home-text-primary);
  transition: letter-spacing 0.25s ease;
}

.recommended-setting-item:hover .recommended-setting-value {
  letter-spacing: 0.5px;
}

/* 6 个算法徽章 */
.recommended-algo-badge {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.recommended-algo-badge-grid {
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.recommended-algo-badge:hover {
  transform: translateY(-1px);
}

.recommended-algo-badge-blue {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.12);
  border: 1px solid rgba(59, 130, 246, 0.25);
}

.recommended-algo-badge-green {
  color: #22c55e;
  background: rgba(34, 197, 94, 0.12);
  border: 1px solid rgba(34, 197, 94, 0.25);
}

.recommended-algo-badge-teal {
  color: #0d9488;
  background: rgba(13, 148, 136, 0.12);
  border: 1px solid rgba(13, 148, 136, 0.25);
}

.recommended-algo-badge-fuchsia {
  color: #d946ef;
  background: rgba(217, 70, 239, 0.12);
  border: 1px solid rgba(217, 70, 239, 0.25);
}

.recommended-algo-badge-amber {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.25);
}

.recommended-algo-badge-rose {
  color: #f43f5e;
  background: rgba(244, 63, 94, 0.12);
  border: 1px solid rgba(244, 63, 94, 0.25);
}

html.dark .recommended-algo-badge-blue {
  color: #60a5fa;
  background: rgba(59, 130, 246, 0.18);
  border-color: rgba(59, 130, 246, 0.35);
}

html.dark .recommended-algo-badge-green {
  color: #4ade80;
  background: rgba(34, 197, 94, 0.18);
  border-color: rgba(34, 197, 94, 0.35);
}

html.dark .recommended-algo-badge-teal {
  color: #5eead4;
  background: rgba(14, 165, 164, 0.18);
  border-color: rgba(14, 165, 164, 0.35);
}

html.dark .recommended-algo-badge-fuchsia {
  color: #e879f9;
  background: rgba(217, 70, 239, 0.18);
  border-color: rgba(217, 70, 239, 0.35);
}

html.dark .recommended-algo-badge-amber {
  color: #fcd34d;
  background: rgba(245, 158, 11, 0.18);
  border-color: rgba(245, 158, 11, 0.35);
}

html.dark .recommended-algo-badge-rose {
  color: #fb7185;
  background: rgba(244, 63, 94, 0.18);
  border-color: rgba(244, 63, 94, 0.35);
}

/* 指标对比表格 */
.recommended-metrics-table {
  border-radius: 10px;
  border: 1px solid var(--home-border);
  overflow: hidden;
}

.recommended-th-metric {
  width: 140px;
  min-width: 120px;
}

.recommended-th-algo {
  min-width: 90px;
}

.recommended-th-blue {
  color: #3b82f6;
}

.recommended-th-green {
  color: #22c55e;
}

.recommended-th-teal {
  color: #0d9488;
}

.recommended-th-fuchsia {
  color: #d946ef;
}

.recommended-th-amber {
  color: #f59e0b;
}

.recommended-th-rose {
  color: #f43f5e;
}

html.dark .recommended-th-blue {
  color: #60a5fa;
}

html.dark .recommended-th-green {
  color: #4ade80;
}

html.dark .recommended-th-teal {
  color: #5eead4;
}

html.dark .recommended-th-fuchsia {
  color: #e879f9;
}

html.dark .recommended-th-amber {
  color: #fcd34d;
}

html.dark .recommended-th-rose {
  color: #fb7185;
}

.recommended-metric-row {
  border-top: 1px solid var(--home-border);
  transition: background 0.2s ease;
}

.recommended-metric-row:hover {
  background: var(--home-hover-bg);
}

.recommended-td-metric {
  display: flex;
  align-items: center;
  gap: 8px;
}

.recommended-metric-row-icon {
  font-size: 18px;
  color: #6366f1;
  opacity: 0.85;
}

html.dark .recommended-metric-row-icon {
  color: #a5b4fc;
}

.recommended-td-value {
  min-width: 90px;
}

.recommended-td-best {
  color: #059669;
  background: rgba(5, 150, 105, 0.12);
}

html.dark .recommended-td-best {
  color: #34d399;
  background: rgba(5, 150, 105, 0.18);
}

@media (prefers-reduced-motion: reduce) {
  .recommended-card-scanline {
    animation: none;
    display: none;
  }

  .recommended-tech-card,
  .recommended-setting-item,
  .recommended-algo-badge {
    transition: none;
  }

  .recommended-setting-item:hover .recommended-setting-icon {
    transform: none;
  }

  .recommended-algo-badge:hover {
    transform: none;
  }
}
</style>

<style>
/* 客户端详情弹窗：modal-class 使 overlay 居中，确保弹窗在页面中央可见 */
.client-detail-dialog.el-overlay {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
