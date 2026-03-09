<script setup lang="ts">
import {metricKeys} from './taskDetailConstants'

defineProps<{
  metrics: {
    loss: number | null
    accuracy: number | null
    precision: number | null
    recall: number | null
    f1Score: number | null
  }
}>()
</script>

<template>
  <section class="task-detail-section task-detail-tech-card relative overflow-hidden rounded-xl py-5 px-6 shrink-0">
    <div class="task-detail-card-glow"></div>
    <div class="task-detail-card-scanline"></div>
    <h3 class="task-detail-section-title m-0 mb-4 text-[15px] font-semibold flex items-center gap-2 relative z-[1]">
      <span class="i-mdi-chart-line task-detail-section-icon text-xl"></span>
      {{ $t('pages.taskDetail.metricsTitle') }}
    </h3>
    <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-4 relative z-[1]">
      <div
          v-for="m in metricKeys"
          :key="m.key"
          class="task-detail-metric-square"
      >
        <span :class="[m.icon, 'task-detail-metric-icon']"></span>
        <span class="task-detail-metric-label">{{ $t(`pages.recommended.${m.key}`) }}</span>
        <span class="task-detail-metric-value tabular-nums">{{ m.format(metrics[m.val] ?? -1) }}</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.task-detail-metric-square {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 16px;
  border-radius: 12px;
  background: var(--home-hover-bg);
  border: 1px solid transparent;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}

.task-detail-metric-square:hover {
  border-color: rgba(99, 102, 241, 0.35);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.08);
}

.task-detail-metric-icon {
  font-size: 24px;
  color: #6366f1;
  opacity: 0.9;
}

html.dark .task-detail-metric-icon {
  color: #a5b4fc;
}

.task-detail-metric-label {
  font-size: 12px;
  color: var(--home-text-muted);
  font-weight: 500;
}

.task-detail-metric-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--home-text-primary);
  letter-spacing: 0.02em;
}
</style>
