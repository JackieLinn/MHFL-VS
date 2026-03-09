<script setup lang="ts">
import {settingKeys} from './taskDetailConstants'

defineProps<{
  settings: Record<string, string | number>
}>()
</script>

<template>
  <section class="task-detail-section task-detail-tech-card relative overflow-hidden rounded-xl py-5 px-6 shrink-0">
    <div class="task-detail-card-glow"></div>
    <div class="task-detail-card-scanline"></div>
    <h3 class="task-detail-section-title m-0 mb-4 text-[15px] font-semibold flex items-center gap-2 relative z-[1]">
      <span class="i-mdi-cog-outline task-detail-section-icon text-xl"></span>
      {{ $t('pages.recommended.expSettings') }}
    </h3>
    <div class="grid grid-cols-2 md:grid-cols-3 gap-x-5 gap-y-4 relative z-[1]">
      <div
          v-for="item in settingKeys"
          :key="item.key"
          class="task-detail-setting-item flex items-start gap-3 py-3 px-3.5 rounded-lg"
      >
        <span :class="[item.icon, 'task-detail-setting-icon shrink-0 mt-0.5 text-xl']"></span>
        <div class="task-detail-setting-text flex flex-col gap-1 min-w-0">
          <span class="task-detail-setting-label text-xs">{{ $t(`pages.recommended.${item.key}`) }}</span>
          <span class="task-detail-setting-value text-sm font-semibold tabular-nums">{{ settings[item.val] }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.task-detail-setting-item {
  position: relative;
  background: var(--home-hover-bg);
  border: 1px solid transparent;
  transition: border-color 0.25s ease, background 0.25s ease;
}

.task-detail-setting-item:hover {
  border-color: var(--home-card-border);
  background: var(--home-card-bg);
}

.task-detail-setting-icon {
  color: #6366f1;
  opacity: 0.8;
}

html.dark .task-detail-setting-icon {
  color: #a5b4fc;
}

.task-detail-setting-label {
  color: var(--home-text-muted);
}

.task-detail-setting-value {
  color: var(--home-text-primary);
}
</style>
