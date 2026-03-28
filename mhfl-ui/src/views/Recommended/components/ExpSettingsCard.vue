<script setup lang="ts">
import {computed} from 'vue'
import {algorithmKeys, settingKeys} from './recommendedConstants'

const props = defineProps<{
  settings: Record<string, string | number>
  algorithmNames?: string[]
}>()

const badgeColors = ['blue', 'green', 'teal', 'fuchsia', 'amber', 'rose'] as const

const displayAlgorithms = computed(() => {
  if (props.algorithmNames && props.algorithmNames.length > 0) {
    return props.algorithmNames.map((name, idx) => ({
      name,
      key: '',
      color: badgeColors[idx % badgeColors.length]
    }))
  }
  return algorithmKeys.map((algo, idx) => ({
    name: '',
    key: algo.key,
    color: badgeColors[idx % badgeColors.length]
  }))
})
</script>

<template>
  <section class="recommended-section recommended-tech-card relative overflow-hidden rounded-xl py-5 px-6 shrink-0">
    <div class="recommended-card-glow"></div>
    <div class="recommended-card-scanline"></div>
    <h3 class="recommended-section-title m-0 mb-4 text-[15px] font-semibold flex items-center gap-2 relative z-[1]">
      <span class="i-mdi-cog-outline recommended-section-icon text-xl"></span>
      {{ $t('pages.recommended.expSettings') }}
    </h3>
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
    <div class="relative z-[1]">
      <span class="recommended-setting-label text-xs block mb-2">{{ $t('pages.recommended.settingAlgorithm') }}</span>
      <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
        <span
            v-for="algo in displayAlgorithms"
            :key="algo.name || algo.key"
            class="recommended-algo-badge recommended-algo-badge-grid"
            :class="`recommended-algo-badge-${algo.color}`"
        >
          {{ algo.name || $t(`pages.recommended.${algo.key}`) }}
        </span>
      </div>
    </div>
  </section>
</template>

<style scoped>
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

@media (prefers-reduced-motion: reduce) {
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
