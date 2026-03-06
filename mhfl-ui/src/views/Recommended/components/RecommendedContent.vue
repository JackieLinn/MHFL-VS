<script setup lang="ts">
import {computed} from 'vue'
import {useI18n} from 'vue-i18n'

const {t} = useI18n()
const props = defineProps<{
  dataset: 'cifar100' | 'tiny-imagenet'
}>()

// 6 个算法 key，用于 i18n 和数据结构
const algorithmKeys = [
  {key: 'algoStandalone', color: 'indigo'},
  {key: 'algoFedProto', color: 'green'},
  {key: 'algoFedAvg', color: 'teal'},
  {key: 'algoFedSSA', color: 'purple'},
  {key: 'algoLGFedAvg', color: 'amber'},
  {key: 'algoOurs', color: 'rose'}
] as const

// 占位数据：后续由接口根据 dataset 参数获取。6 组数据，每组 5 个指标
const algorithmMetrics = computed(() => {
  if (props.dataset === 'cifar100') {
    return [
      {loss: 0.892, accuracy: 0.684, precision: 0.672, recall: 0.658, f1: 0.665},
      {loss: 0.915, accuracy: 0.658, precision: 0.645, recall: 0.632, f1: 0.638},
      {loss: 0.878, accuracy: 0.698, precision: 0.685, recall: 0.672, f1: 0.678},
      {loss: 0.865, accuracy: 0.712, precision: 0.698, recall: 0.688, f1: 0.693},
      {loss: 0.901, accuracy: 0.668, precision: 0.655, recall: 0.642, f1: 0.648},
      {loss: 0.888, accuracy: 0.675, precision: 0.662, recall: 0.652, f1: 0.657}
    ]
  }
  return [
    {loss: 1.245, accuracy: 0.521, precision: 0.508, recall: 0.495, f1: 0.501},
    {loss: 1.312, accuracy: 0.498, precision: 0.485, recall: 0.472, f1: 0.478},
    {loss: 1.198, accuracy: 0.538, precision: 0.525, recall: 0.512, f1: 0.518},
    {loss: 1.176, accuracy: 0.552, precision: 0.538, recall: 0.525, f1: 0.531},
    {loss: 1.268, accuracy: 0.505, precision: 0.492, recall: 0.478, f1: 0.485},
    {loss: 1.222, accuracy: 0.515, precision: 0.502, recall: 0.488, f1: 0.495}
  ]
})

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
            <th class="recommended-th-metric text-left py-3 px-4 text-xs font-semibold text-[var(--home-text-muted)]">
              {{ $t('pages.recommended.expMetrics') }}
            </th>
            <th
                v-for="algo in algorithmKeys"
                :key="algo.key"
                class="recommended-th-algo py-3 px-4 text-center text-sm font-semibold"
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
                v-for="(_, idx) in algorithmKeys"
                :key="idx"
                class="recommended-td-value py-3 px-4 text-center text-lg font-bold tabular-nums text-[var(--home-text-primary)]"
            >
              {{ m.format(algorithmMetrics[idx][m.val]) }}
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </section>
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

.recommended-algo-badge-indigo {
  color: #6366f1;
  background: rgba(99, 102, 241, 0.12);
  border: 1px solid rgba(99, 102, 241, 0.25);
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

.recommended-algo-badge-purple {
  color: #8b5cf6;
  background: rgba(139, 92, 246, 0.12);
  border: 1px solid rgba(139, 92, 246, 0.25);
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

html.dark .recommended-algo-badge-indigo {
  color: #a5b4fc;
  background: rgba(99, 102, 241, 0.18);
  border-color: rgba(99, 102, 241, 0.35);
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

html.dark .recommended-algo-badge-purple {
  color: #c4b5fd;
  background: rgba(139, 92, 246, 0.18);
  border-color: rgba(139, 92, 246, 0.35);
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

.recommended-th-indigo {
  color: #6366f1;
}

.recommended-th-green {
  color: #22c55e;
}

.recommended-th-teal {
  color: #0d9488;
}

.recommended-th-purple {
  color: #8b5cf6;
}

.recommended-th-amber {
  color: #f59e0b;
}

.recommended-th-rose {
  color: #f43f5e;
}

html.dark .recommended-th-indigo {
  color: #a5b4fc;
}

html.dark .recommended-th-green {
  color: #4ade80;
}

html.dark .recommended-th-teal {
  color: #5eead4;
}

html.dark .recommended-th-purple {
  color: #c4b5fd;
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
