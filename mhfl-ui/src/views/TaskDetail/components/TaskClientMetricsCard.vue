<script setup lang="ts">
import {ref, computed, watch, nextTick} from 'vue'
import {useI18n} from 'vue-i18n'
import * as echarts from 'echarts'
import {
  getClientModel,
  cnnColors,
  clientMetricOptions,
  clientMetricColors
} from './taskDetailConstants'

const {t} = useI18n()

type ClientMetricKey = 'accuracy' | 'precision' | 'recall' | 'f1'

const props = defineProps<{
  clientMetrics: { accuracy: number; precision: number; recall: number; f1: number }[]
}>()

const selectedClientDetailMetric = ref<ClientMetricKey>('accuracy')
const clientDetailVisible = ref(false)
const selectedClientId = ref(0)

const numRounds = computed(() => 500)

const openClientDetail = (clientIndex: number) => {
  selectedClientId.value = clientIndex
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

const getClientMetricVal = (clientIdx: number, key: ClientMetricKey): number =>
    props.clientMetrics[clientIdx]?.[key] ?? 0

const clientDetailChartData = computed(() => {
  const clientIdx = selectedClientId.value - 1
  if (clientIdx < 0) return null
  const m = selectedClientDetailMetric.value
  const finalVal = getClientMetricVal(clientIdx, m)
  const init = finalVal * 0.18
  const points: number[] = []
  for (let r = 0; r < numRounds.value; r++) {
    const tVal = r / Math.max(numRounds.value - 1, 1)
    const progress = 1 - Math.exp(-3.2 * tVal)
    const noise = (Math.random() - 0.5) * 0.02
    points.push(Math.max(0.01, Math.min(0.99, init + (finalVal - init) * progress + noise)))
  }
  points[points.length - 1] = finalVal
  return points
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
  if (!data?.length) return
  const textColor = chartTextColor()
  const mutedColor = chartMutedColor()
  const isDark = document.documentElement.classList.contains('dark')
  const rounds = Array.from({length: numRounds.value}, (_, i) => i + 1)
  const metricKey = selectedClientDetailMetric.value
  const option = {
    backgroundColor: 'transparent',
    title: {
      text: t('pages.recommended.clientDetailTitle'),
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
    series: [{
      name: t(`pages.recommended.${clientMetricOptions.find(o => o.val === metricKey)?.key ?? 'clientMetricAccuracy'}`),
      type: 'line',
      data,
      smooth: 0.25,
      symbol: 'none',
      lineStyle: {width: 2.5, color: '#6366f1'},
      itemStyle: {color: '#6366f1'},
      emphasis: {focus: 'series', lineStyle: {width: 3.5}}
    }]
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
</script>

<template>
  <section class="task-detail-section task-detail-tech-card relative overflow-hidden rounded-xl py-5 px-6 shrink-0">
    <div class="task-detail-card-glow"></div>
    <div class="task-detail-card-scanline"></div>
    <div class="mb-4 relative z-[1]">
      <h3 class="task-detail-section-title m-0 text-[15px] font-semibold flex items-center gap-2">
        <span class="i-mdi-account-multiple-outline task-detail-section-icon text-xl"></span>
        {{ $t('pages.taskDetail.clientMetricsTitle') }}
      </h3>
    </div>
    <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3 relative z-[1]">
      <div
          v-for="i in 100"
          :key="i"
          class="task-detail-client-cell"
          :class="`task-detail-client-cell-cnn${getClientModel(i)}`"
          @click="openClientDetail(i)"
      >
        <div class="task-detail-client-header">
          <span class="task-detail-client-label">{{ $t('pages.recommended.clientLabel') }} {{ i }}</span>
          <span
              class="task-detail-client-model-badge"
              :style="{borderColor: cnnColors[getClientModel(i) - 1], color: cnnColors[getClientModel(i) - 1]}"
          >
            {{ $t(`pages.recommended.clientModelCnn${getClientModel(i)}`) }}
          </span>
        </div>
        <div class="task-detail-client-rings-grid">
          <div
              v-for="(opt, oi) in clientMetricOptions"
              :key="opt.val"
              class="task-detail-client-mini-ring"
              :title="`${$t('pages.recommended.' + opt.key)}: ${(getClientMetricVal(i - 1, opt.val) * 100).toFixed(2)}%`"
          >
            <div class="task-detail-client-mini-ring-wrap">
              <svg viewBox="0 0 36 36" class="task-detail-client-mini-svg">
                <circle class="task-detail-client-ring-bg" cx="18" cy="18" r="15.9"/>
                <circle
                    class="task-detail-client-ring-fill"
                    cx="18"
                    cy="18"
                    r="15.9"
                    stroke-dasharray="100"
                    :stroke-dashoffset="100 - getClientMetricVal(i - 1, opt.val) * 100"
                    :style="{stroke: clientMetricColors[oi]}"
                />
              </svg>
              <span class="task-detail-client-mini-value tabular-nums">
                {{ (getClientMetricVal(i - 1, opt.val) * 100).toFixed(1) }}%
              </span>
            </div>
            <span class="task-detail-client-mini-label">{{ $t(`pages.taskDetail.metricFull.${opt.val}`) }}</span>
          </div>
        </div>
        <button
            type="button"
            class="task-detail-client-detail-btn"
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
      modal-class="task-detail-client-dialog"
      @closed="onClientDetailClosed"
  >
    <div v-if="selectedClientId > 0" class="task-detail-client-detail-content">
      <div class="task-detail-client-detail-info mb-4">
        <span class="task-detail-client-detail-label">{{ $t('pages.recommended.clientLabel') }} {{
            selectedClientId
          }}</span>
        <span class="task-detail-client-detail-model">
          {{
            $t('pages.recommended.clientModelLabel')
          }}: {{ $t(`pages.recommended.clientModelCnn${getClientModel(selectedClientId)}`) }}
        </span>
      </div>
      <div class="flex flex-wrap gap-2 mb-4">
        <button
            v-for="opt in clientMetricOptions"
            :key="opt.val"
            type="button"
            class="task-detail-detail-metric-btn px-3 py-1.5 text-xs rounded transition-colors"
            :class="selectedClientDetailMetric === opt.val ? 'task-detail-detail-metric-active' : ''"
            @click="selectedClientDetailMetric = opt.val"
        >
          {{ $t(`pages.recommended.${opt.key}`) }}
        </button>
      </div>
      <div
          class="task-detail-client-detail-chart-wrap rounded-xl border border-[var(--home-card-border)] bg-[var(--home-hover-bg)] p-4"
      >
        <div ref="clientDetailChartRef" class="w-full h-[340px]"></div>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.task-detail-client-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px 12px;
  border-radius: 14px;
  background: var(--home-hover-bg);
  border: 1px solid var(--home-card-border);
  border-left: 4px solid var(--client-accent, var(--home-card-border));
  transition: all 0.28s cubic-bezier(0.22, 1, 0.36, 1);
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.task-detail-client-cell:hover {
  border-color: rgba(99, 102, 241, 0.45);
  background: var(--home-card-bg);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.12);
  transform: translateY(-2px);
}

html.dark .task-detail-client-cell {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

html.dark .task-detail-client-cell:hover {
  border-color: rgba(99, 102, 241, 0.55);
  box-shadow: 0 6px 24px rgba(99, 102, 241, 0.18);
}

.task-detail-client-rings-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px 12px;
  margin: 12px 0 10px;
  width: 100%;
  min-width: 140px;
}

.task-detail-client-mini-ring {
  min-width: 0;
}

.task-detail-client-mini-ring-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
}

.task-detail-client-mini-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.task-detail-client-mini-value {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--home-text-primary);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}

.task-detail-client-mini-label {
  display: block;
  font-size: 11px;
  color: var(--home-text-muted);
  text-align: center;
  margin-top: 3px;
  font-weight: 500;
  line-height: 1.2;
}

.task-detail-client-detail-btn {
  font-size: 11px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid var(--home-card-border);
  background: transparent;
  color: var(--home-text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.task-detail-client-detail-btn:hover {
  border-color: #6366f1;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
}

.task-detail-client-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
}

.task-detail-client-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--home-text-muted);
  white-space: nowrap;
}

.task-detail-client-model-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
  border: 1px solid;
  background: transparent;
  letter-spacing: 0.3px;
}

.task-detail-client-cell-cnn1 {
  --client-accent: #3b82f6;
}

.task-detail-client-cell-cnn2 {
  --client-accent: #22c55e;
}

.task-detail-client-cell-cnn3 {
  --client-accent: #0d9488;
}

.task-detail-client-cell-cnn4 {
  --client-accent: #d946ef;
}

.task-detail-client-cell-cnn5 {
  --client-accent: #f59e0b;
}

.task-detail-client-ring-bg {
  fill: none;
  stroke: var(--home-card-border);
  stroke-width: 2.5;
}

.task-detail-client-ring-fill {
  fill: none;
  stroke: #6366f1;
  stroke-width: 2.5;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.35s ease;
}

.task-detail-client-detail-info {
  display: flex;
  gap: 16px;
  align-items: center;
}

.task-detail-client-detail-label {
  font-weight: 600;
  color: var(--home-text-primary);
}

.task-detail-client-detail-model {
  font-size: 13px;
  color: var(--home-text-muted);
}

.task-detail-detail-metric-btn {
  border: 1px solid var(--home-card-border);
  background: var(--home-hover-bg);
  color: var(--home-text-muted);
  transition: all 0.2s ease;
}

.task-detail-detail-metric-btn:hover {
  border-color: rgba(99, 102, 241, 0.4);
  color: #6366f1;
}

.task-detail-detail-metric-active {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.12);
  color: #6366f1;
}

.task-detail-client-detail-chart-wrap {
  transition: border-color 0.2s ease;
}

.task-detail-client-detail-chart-wrap:hover {
  border-color: rgba(99, 102, 241, 0.3);
}

@media (prefers-reduced-motion: reduce) {
  .task-detail-client-cell {
    transition: none;
  }
}
</style>

<style>
.task-detail-client-dialog.el-overlay {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
