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
  <div class="recommended-content">
    <!-- 实验设置 -->
    <section class="recommended-section recommended-tech-card">
      <div class="recommended-card-glow"></div>
      <div class="recommended-card-scanline"></div>
      <h3 class="recommended-section-title">
        <span class="i-mdi-cog-outline recommended-section-icon"></span>
        {{ $t('pages.recommended.expSettings') }}
      </h3>
      <div class="recommended-settings-grid">
        <div
            v-for="item in settingKeys"
            :key="item.key"
            class="recommended-setting-item"
        >
          <span :class="[item.icon, 'recommended-setting-icon']"></span>
          <div class="recommended-setting-text">
            <span class="recommended-setting-label">{{ $t(`pages.recommended.${item.key}`) }}</span>
            <span class="recommended-setting-value">{{ settings[item.val] }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 实验指标 -->
    <section class="recommended-section recommended-tech-card">
      <div class="recommended-card-glow"></div>
      <div class="recommended-card-scanline"></div>
      <h3 class="recommended-section-title">
        <span class="i-mdi-chart-line recommended-section-icon"></span>
        {{ $t('pages.recommended.expMetrics') }}
      </h3>
      <div class="recommended-metrics-grid">
        <div
            v-for="item in metricKeys"
            :key="item.key"
            class="recommended-metric-card"
            :class="`recommended-metric-${item.color}`"
        >
          <div class="recommended-metric-scanline"></div>
          <span :class="[item.icon, 'recommended-metric-icon']"></span>
          <span class="recommended-metric-value">{{ item.format(metrics[item.val]) }}</span>
          <span class="recommended-metric-label">{{ $t(`pages.recommended.${item.key}`) }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.recommended-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.recommended-content::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

.recommended-section {
  position: relative;
  overflow: hidden;
  background: var(--home-card-bg);
  border: 1px solid var(--home-card-border);
  border-radius: 12px;
  padding: 20px 24px;
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
  margin: 0 0 16px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--home-text-primary);
  letter-spacing: 0.3px;
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
  z-index: 1;
}

.recommended-section-icon {
  font-size: 20px;
  color: #6366f1;
  opacity: 0.9;
}

html.dark .recommended-section-icon {
  color: #a5b4fc;
}

.recommended-settings-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px 20px;
  position: relative;
  z-index: 1;
}

.recommended-setting-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  background: var(--home-hover-bg);
  border: 1px solid transparent;
  border-radius: 8px;
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
  font-size: 20px;
  color: #6366f1;
  opacity: 0.8;
  flex-shrink: 0;
  margin-top: 2px;
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}

.recommended-setting-item:hover .recommended-setting-icon {
  transform: scale(1.08);
}

html.dark .recommended-setting-icon {
  color: #a5b4fc;
}

.recommended-setting-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.recommended-setting-label {
  font-size: 12px;
  color: var(--home-text-muted);
}

.recommended-setting-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--home-text-primary);
  font-variant-numeric: tabular-nums;
  transition: letter-spacing 0.25s ease;
}

.recommended-setting-item:hover .recommended-setting-value {
  letter-spacing: 0.5px;
}

.recommended-metrics-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  position: relative;
  z-index: 1;
}

.recommended-metric-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 16px;
  background: var(--home-card-bg);
  border: 1px solid var(--home-card-border);
  border-radius: 12px;
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1), border-color 0.25s ease, box-shadow 0.25s ease;
  position: relative;
  overflow: hidden;
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
  font-size: 28px;
  opacity: 0.9;
  position: relative;
  z-index: 1;
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
  font-size: 22px;
  font-weight: 700;
  color: var(--home-text-primary);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.5px;
  position: relative;
  z-index: 1;
}

.recommended-metric-label {
  font-size: 12px;
  color: var(--home-text-muted);
  position: relative;
  z-index: 1;
}

@media (max-width: 900px) {
  .recommended-settings-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .recommended-metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
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
