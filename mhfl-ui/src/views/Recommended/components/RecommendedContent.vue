<script setup lang="ts">
import {computed} from 'vue'

const props = defineProps<{
  dataset: 'cifar100' | 'tiny-imagenet'
}>()

// 占位数据：后续由接口根据 dataset 参数获取
const settings = computed(() => {
  if (props.dataset === 'cifar100') {
    return {
      dataset: 'CIFAR-100',
      algorithm: 'FedAvg',
      numNodes: 100,
      fraction: 0.1,
      classesPerNode: 5,
      lowProb: 0.5,
      numSteps: 500,
      epochs: 10
    }
  }
  return {
    dataset: 'Tiny-ImageNet',
    algorithm: 'FedProto',
    numNodes: 100,
    fraction: 0.15,
    classesPerNode: 10,
    lowProb: 0.4,
    numSteps: 300,
    epochs: 8
  }
})

const metrics = computed(() => {
  if (props.dataset === 'cifar100') {
    return {loss: 0.892, accuracy: 0.684, precision: 0.672, recall: 0.658, f1: 0.665}
  }
  return {loss: 1.245, accuracy: 0.521, precision: 0.508, recall: 0.495, f1: 0.501}
})

const settingKeys = [
  {key: 'settingDataset', val: 'dataset', icon: 'i-mdi-database-outline'},
  {key: 'settingAlgorithm', val: 'algorithm', icon: 'i-mdi-code-braces'},
  {key: 'settingNumNodes', val: 'numNodes', icon: 'i-mdi-account-group-outline'},
  {key: 'settingFraction', val: 'fraction', icon: 'i-mdi-percent'},
  {key: 'settingClassesPerNode', val: 'classesPerNode', icon: 'i-mdi-label-multiple-outline'},
  {key: 'settingLowProb', val: 'lowProb', icon: 'i-mdi-chart-line'},
  {key: 'settingNumSteps', val: 'numSteps', icon: 'i-mdi-repeat'},
  {key: 'settingEpochs', val: 'epochs', icon: 'i-mdi-backup-restore'}
] as const

const metricKeys = [
  {key: 'metricLoss', val: 'loss', format: (v: number) => v.toFixed(3), icon: 'i-mdi-chart-line', color: 'indigo'},
  {
    key: 'metricAccuracy',
    val: 'accuracy',
    format: (v: number) => (v * 100).toFixed(2) + '%',
    icon: 'i-mdi-target',
    color: 'green'
  },
  {
    key: 'metricPrecision',
    val: 'precision',
    format: (v: number) => (v * 100).toFixed(2) + '%',
    icon: 'i-mdi-crosshairs-gps',
    color: 'teal'
  },
  {
    key: 'metricRecall',
    val: 'recall',
    format: (v: number) => (v * 100).toFixed(2) + '%',
    icon: 'i-mdi-chart-areaspline',
    color: 'purple'
  },
  {
    key: 'metricF1',
    val: 'f1',
    format: (v: number) => (v * 100).toFixed(2) + '%',
    icon: 'i-mdi-chart-bar',
    color: 'amber'
  }
] as const
</script>

<template>
  <div class="recommended-content flex-1 min-h-0 overflow-y-auto flex flex-col gap-6">
    <!-- 实验设置 -->
    <section class="recommended-section recommended-tech-card relative overflow-hidden rounded-xl py-5 px-6">
      <div class="recommended-card-glow"></div>
      <div class="recommended-card-scanline"></div>
      <h3 class="recommended-section-title m-0 mb-4 text-[15px] font-semibold flex items-center gap-2 relative z-[1]">
        <span class="i-mdi-cog-outline recommended-section-icon text-xl"></span>
        {{ $t('pages.recommended.expSettings') }}
      </h3>
      <div class="recommended-settings-grid grid grid-cols-2 md:grid-cols-4 gap-x-5 gap-y-3 relative z-[1]">
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
    </section>

    <!-- 实验指标 -->
    <section class="recommended-section recommended-tech-card relative overflow-hidden rounded-xl py-5 px-6">
      <div class="recommended-card-glow"></div>
      <div class="recommended-card-scanline"></div>
      <h3 class="recommended-section-title m-0 mb-4 text-[15px] font-semibold flex items-center gap-2 relative z-[1]">
        <span class="i-mdi-chart-line recommended-section-icon text-xl"></span>
        {{ $t('pages.recommended.expMetrics') }}
      </h3>
      <div class="recommended-metrics-grid grid grid-cols-2 md:grid-cols-5 gap-4 relative z-[1]">
        <div
            v-for="item in metricKeys"
            :key="item.key"
            class="recommended-metric-card flex flex-col items-center justify-center gap-2 py-5 px-4 rounded-xl"
            :class="`recommended-metric-${item.color}`"
        >
          <div class="recommended-metric-scanline"></div>
          <span :class="[item.icon, 'recommended-metric-icon text-[28px] relative z-[1]']"></span>
          <span class="recommended-metric-value text-2xl font-bold tabular-nums tracking-wide relative z-[1]">{{
              item.format(metrics[item.val])
            }}</span>
          <span class="recommended-metric-label text-xs relative z-[1]">{{ $t(`pages.recommended.${item.key}`) }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.recommended-content {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.recommended-content::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

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

.recommended-metric-card {
  position: relative;
  overflow: hidden;
  background: var(--home-card-bg);
  border: 1px solid var(--home-card-border);
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1), border-color 0.25s ease, box-shadow 0.25s ease;
}

.recommended-metric-card::after {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
  background: radial-gradient(circle at 50% 30%, rgba(99, 102, 241, 0.08), transparent 70%);
  transition: opacity 0.32s ease;
}

.recommended-metric-card:hover::after {
  opacity: 1;
}

html.dark .recommended-metric-card::after {
  background: radial-gradient(circle at 50% 30%, rgba(99, 102, 241, 0.12), transparent 70%);
}

.recommended-metric-scanline {
  position: absolute;
  top: 0;
  left: -100%;
  width: 60%;
  height: 100%;
  background: linear-gradient(90deg, transparent 0%, rgba(99, 102, 241, 0.04) 40%, rgba(99, 102, 241, 0.08) 50%, rgba(99, 102, 241, 0.04) 60%, transparent 100%);
  pointer-events: none;
  animation: recommendedScanlineMove 10s ease-in-out infinite;
}

.recommended-metric-card:hover .recommended-metric-scanline {
  animation-duration: 4s;
}

html.dark .recommended-metric-scanline {
  background: linear-gradient(90deg, transparent 0%, rgba(99, 102, 241, 0.05) 40%, rgba(99, 102, 241, 0.12) 50%, rgba(99, 102, 241, 0.05) 60%, transparent 100%);
}

.recommended-metric-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  opacity: 0.7;
}

.recommended-metric-indigo::before {
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
}

.recommended-metric-green::before {
  background: linear-gradient(90deg, #22c55e, #16a34a);
}

.recommended-metric-teal::before {
  background: linear-gradient(90deg, #0d9488, #14b8a6);
}

.recommended-metric-purple::before {
  background: linear-gradient(90deg, #8b5cf6, #a78bfa);
}

.recommended-metric-amber::before {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
}

.recommended-metric-icon {
  opacity: 0.9;
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}

.recommended-metric-card:hover .recommended-metric-icon {
  transform: scale(1.12);
}

.recommended-metric-indigo .recommended-metric-icon {
  color: #6366f1;
}

.recommended-metric-green .recommended-metric-icon {
  color: #22c55e;
}

.recommended-metric-teal .recommended-metric-icon {
  color: #0d9488;
}

.recommended-metric-purple .recommended-metric-icon {
  color: #8b5cf6;
}

.recommended-metric-amber .recommended-metric-icon {
  color: #f59e0b;
}

html.dark .recommended-metric-indigo .recommended-metric-icon {
  color: #a5b4fc;
}

html.dark .recommended-metric-green .recommended-metric-icon {
  color: #4ade80;
}

html.dark .recommended-metric-teal .recommended-metric-icon {
  color: #5eead4;
}

html.dark .recommended-metric-purple .recommended-metric-icon {
  color: #c4b5fd;
}

html.dark .recommended-metric-amber .recommended-metric-icon {
  color: #fcd34d;
}

.recommended-metric-card:hover {
  transform: translateY(-2px);
  border-color: rgba(99, 102, 241, 0.35);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.12);
}

html.dark .recommended-metric-card {
  background: var(--home-card-bg);
  border-color: var(--home-card-border);
}

html.dark .recommended-metric-card:hover {
  border-color: rgba(99, 102, 241, 0.45);
  box-shadow: 0 8px 28px rgba(99, 102, 241, 0.18);
}

.recommended-metric-value {
  color: var(--home-text-primary);
}

.recommended-metric-label {
  color: var(--home-text-muted);
}

@media (prefers-reduced-motion: reduce) {
  .recommended-card-scanline,
  .recommended-metric-scanline {
    animation: none;
    display: none;
  }

  .recommended-tech-card,
  .recommended-setting-item,
  .recommended-metric-card {
    transition: none;
  }

  .recommended-setting-item:hover .recommended-setting-icon,
  .recommended-metric-card:hover .recommended-metric-icon {
    transform: none;
  }

  .recommended-metric-card:hover {
    transform: none;
  }
}
</style>
