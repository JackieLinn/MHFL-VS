<script setup lang="ts">
import {computed, ref, watch, nextTick} from 'vue'
import {useI18n} from 'vue-i18n'
import * as echarts from 'echarts'
import {
  algorithmKeys,
  chartColors,
  chartMetricKeys,
  clientMetricOptions,
  cnnColors,
  getClientModel
} from './recommendedConstants'
import type {RecommendClientMetricType} from '@/api/recommend'

const {t} = useI18n()
const props = defineProps<{
  dataset: 'cifar100' | 'tiny-imagenet'
  selectedClientMetric?: RecommendClientMetricType
  clientMetrics: { accuracy: number[]; precision: number[]; recall: number[]; f1: number[] }[]
}>()
const emit = defineEmits<{
  metricChange: [metric: RecommendClientMetricType]
}>()

const selectedClientMetric = ref<RecommendClientMetricType>(props.selectedClientMetric ?? 'accuracy')
const clientDetailVisible = ref(false)
const selectedClientId = ref(0)
const selectedClientDetailMetric = ref<RecommendClientMetricType>('accuracy')

const onMetricClick = (metric: RecommendClientMetricType) => {
  selectedClientMetric.value = metric
  emit('metricChange', metric)
}

const numRounds = computed(() => (props.dataset === 'cifar100' ? 500 : 300))

const openClientDetail = (clientIndex: number) => {
  selectedClientId.value = clientIndex
  selectedClientDetailMetric.value = selectedClientMetric.value
  clientDetailVisible.value = true
}

const clientDetailChartRef = ref<HTMLElement | null>(null)
let clientDetailChartInst: echarts.ECharts | null = null

const onClientDetailClosed = () => {
  clientDetailChartInst?.dispose()
  clientDetailChartInst = null
}

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

const isClientTrainedInRound = (clientIndex: number, round: number) =>
    clientIndex % 10 === round % 10

const clientDetailChartData = computed(() => {
  const clientIdx = selectedClientId.value - 1
  if (clientIdx < 0) return null
  const metrics = props.clientMetrics[clientIdx]
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
          const tVal = r / Math.max(rounds - 1, 1)
          const progress = 1 - Math.exp(-3.2 * tVal)
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

watch(clientDetailVisible, (visible) => {
  if (visible) {
    nextTick(() => setTimeout(initClientDetailChart, 50))
  }
})
watch([selectedClientId, selectedClientDetailMetric, clientDetailChartData], () => {
  if (clientDetailVisible.value) updateClientDetailChart()
}, {deep: true})
watch(
    () => props.selectedClientMetric,
    (value) => {
      if (!value) return
      selectedClientMetric.value = value
    },
    {immediate: true}
)
</script>

<template>
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
            @click="onMetricClick(opt.val)"
        >
          {{ $t(`pages.recommended.${opt.key}`) }}
        </button>
      </div>
    </div>
    <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3 relative z-[1]">
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
</template>

<style scoped>
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
  padding: 14px 10px;
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
  grid-template-columns: repeat(3, 1fr);
  gap: 6px 8px;
  margin-bottom: 6px;
  width: 100%;
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
  font-size: 11px;
  font-weight: 600;
  color: var(--home-text-primary);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.03em;
  line-height: 1;
}

.recommended-client-detail-btn {
  font-size: 11px;
  padding: 5px 10px;
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
  gap: 3px;
  margin-bottom: 10px;
}

.recommended-client-label {
  font-size: 12px;
  color: var(--home-text-muted);
  white-space: nowrap;
}

.recommended-client-model-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
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

@media (prefers-reduced-motion: reduce) {
  .recommended-client-cell {
    transition: none;
  }
}
</style>

<style>
.client-detail-dialog.el-overlay {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
