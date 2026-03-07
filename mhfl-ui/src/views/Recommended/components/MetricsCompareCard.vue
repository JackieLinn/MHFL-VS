<script setup lang="ts">
import {algorithmKeys, metricKeys} from './recommendedConstants'

defineProps<{
  algorithmMetrics: Record<string, number>[]
  getBestIndexForMetric: (val: string) => number
}>()
</script>

<template>
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
</template>

<style scoped>
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
</style>
