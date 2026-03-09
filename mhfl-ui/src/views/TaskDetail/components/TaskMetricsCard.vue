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
          v-for="(m, idx) in metricKeys"
          :key="m.key"
          class="task-detail-metric-square"
          :class="`task-detail-metric-square-c${idx}`"
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
  gap: 10px;
  padding: 22px 18px;
  border-radius: 14px;
  background: var(--home-hover-bg);
  border: 1px solid var(--home-card-border);
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}

.task-detail-metric-square-c0 {
  border-left: 4px solid #3b82f6;
}

.task-detail-metric-square-c1 {
  border-left: 4px solid #22c55e;
}

.task-detail-metric-square-c2 {
  border-left: 4px solid #0d9488;
}

.task-detail-metric-square-c3 {
  border-left: 4px solid #d946ef;
}

.task-detail-metric-square-c4 {
  border-left: 4px solid #f59e0b;
}

.task-detail-metric-square:hover {
  transform: translateY(-3px);
}

.task-detail-metric-square-c0:hover {
  border-color: #3b82f6;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.25);
  background: rgba(59, 130, 246, 0.06);
}

.task-detail-metric-square-c1:hover {
  border-color: #22c55e;
  box-shadow: 0 8px 24px rgba(34, 197, 94, 0.25);
  background: rgba(34, 197, 94, 0.06);
}

.task-detail-metric-square-c2:hover {
  border-color: #0d9488;
  box-shadow: 0 8px 24px rgba(13, 148, 136, 0.25);
  background: rgba(13, 148, 136, 0.06);
}

.task-detail-metric-square-c3:hover {
  border-color: #d946ef;
  box-shadow: 0 8px 24px rgba(217, 70, 239, 0.25);
  background: rgba(217, 70, 239, 0.06);
}

.task-detail-metric-square-c4:hover {
  border-color: #f59e0b;
  box-shadow: 0 8px 24px rgba(245, 158, 11, 0.25);
  background: rgba(245, 158, 11, 0.06);
}

html.dark .task-detail-metric-square-c0:hover {
  background: rgba(59, 130, 246, 0.1);
}

html.dark .task-detail-metric-square-c1:hover {
  background: rgba(34, 197, 94, 0.1);
}

html.dark .task-detail-metric-square-c2:hover {
  background: rgba(13, 148, 136, 0.1);
}

html.dark .task-detail-metric-square-c3:hover {
  background: rgba(217, 70, 239, 0.1);
}

html.dark .task-detail-metric-square-c4:hover {
  background: rgba(245, 158, 11, 0.1);
}

.task-detail-metric-icon {
  font-size: 28px;
  opacity: 0.95;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}

.task-detail-metric-square-c0 .task-detail-metric-icon {
  color: #3b82f6;
}

.task-detail-metric-square-c1 .task-detail-metric-icon {
  color: #22c55e;
}

.task-detail-metric-square-c2 .task-detail-metric-icon {
  color: #0d9488;
}

.task-detail-metric-square-c3 .task-detail-metric-icon {
  color: #d946ef;
}

.task-detail-metric-square-c4 .task-detail-metric-icon {
  color: #f59e0b;
}

.task-detail-metric-square:hover .task-detail-metric-icon {
  transform: scale(1.12);
}

.task-detail-metric-label {
  font-size: 12px;
  color: var(--home-text-muted);
  font-weight: 500;
}

.task-detail-metric-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--home-text-primary);
  letter-spacing: 0.02em;
  transition: color 0.25s ease;
}

.task-detail-metric-square-c0:hover .task-detail-metric-value {
  color: #3b82f6;
}

.task-detail-metric-square-c1:hover .task-detail-metric-value {
  color: #22c55e;
}

.task-detail-metric-square-c2:hover .task-detail-metric-value {
  color: #0d9488;
}

.task-detail-metric-square-c3:hover .task-detail-metric-value {
  color: #d946ef;
}

.task-detail-metric-square-c4:hover .task-detail-metric-value {
  color: #f59e0b;
}

@media (prefers-reduced-motion: reduce) {
  .task-detail-metric-square,
  .task-detail-metric-icon {
    transition: none;
  }

  .task-detail-metric-square:hover {
    transform: none;
  }

  .task-detail-metric-square:hover .task-detail-metric-icon {
    transform: none;
  }
}
</style>
