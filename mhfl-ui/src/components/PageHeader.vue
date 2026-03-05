<script setup lang="ts">
import {ref, onMounted, onBeforeUnmount} from 'vue'

withDefaults(defineProps<{
  title: string
  desc: string
  badge?: string
  enterDelay?: string
}>(), {
  badge: 'MHFL-VS',
  enterDelay: '0s',
})

const liveClock = ref('')
const liveDate = ref('')
let liveClockTimer: ReturnType<typeof window.setInterval> | null = null

const updateLiveClock = () => {
  const now = new Date()
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  const ss = String(now.getSeconds()).padStart(2, '0')
  const yyyy = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const dd = String(now.getDate()).padStart(2, '0')
  liveClock.value = `${hh}:${mm}:${ss}`
  liveDate.value = `${yyyy}-${month}-${dd}`
}

onMounted(() => {
  updateLiveClock()
  liveClockTimer = window.setInterval(updateLiveClock, 1000)
})

onBeforeUnmount(() => {
  if (liveClockTimer) {
    window.clearInterval(liveClockTimer)
    liveClockTimer = null
  }
})
</script>

<template>
  <div class="ph-root" :style="{ '--ph-enter-delay': enterDelay }">
    <div class="ph-scanline"></div>
    <div class="ph-main">
      <span class="ph-badge">
        <span class="ph-badge-dot"></span>
        {{ badge }}
      </span>
      <h2 class="ph-title">{{ title }}</h2>
      <p class="ph-desc">{{ desc }}</p>
    </div>
    <div class="ph-side">
      <div class="ph-clock">{{ liveClock }}</div>
      <div class="ph-date">{{ liveDate }}</div>
      <div class="ph-health-chip">
        <span class="ph-health-dot"></span>
        <span>{{ $t('pages.dashboard.healthHealthy') }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ========================================
   根容器
   ======================================== */
.ph-root {
  --ph-enter-delay: 0s;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--home-card-border);
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: radial-gradient(ellipse 60% 80% at 8% 50%, rgba(99, 102, 241, 0.1), transparent),
  var(--home-card-bg);
  box-shadow: 0 4px 20px var(--home-card-shadow);
  transition: border-color 0.32s ease, box-shadow 0.32s ease;
  opacity: 0;
  animation: phRise 0.6s cubic-bezier(0.22, 1, 0.36, 1) forwards;
  animation-delay: var(--ph-enter-delay);
}

html.dark .ph-root {
  background: radial-gradient(ellipse 60% 80% at 8% 50%, rgba(99, 102, 241, 0.16), transparent),
  var(--home-card-bg);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.28);
}

.ph-root::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.5), rgba(14, 165, 164, 0.35), transparent);
  pointer-events: none;
}

.ph-root:hover {
  border-color: rgba(99, 102, 241, 0.3);
  box-shadow: 0 8px 32px rgba(99, 102, 241, 0.08), 0 4px 16px var(--home-card-shadow);
}

html.dark .ph-root:hover {
  border-color: rgba(99, 102, 241, 0.45);
  box-shadow: 0 8px 36px rgba(99, 102, 241, 0.1), 0 4px 16px rgba(0, 0, 0, 0.3);
}

@keyframes phRise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ========================================
   扫描线
   ======================================== */
.ph-scanline {
  position: absolute;
  top: 0;
  left: -100%;
  width: 60%;
  height: 100%;
  background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(99, 102, 241, 0.04) 40%,
      rgba(99, 102, 241, 0.08) 50%,
      rgba(99, 102, 241, 0.04) 60%,
      transparent 100%
  );
  pointer-events: none;
  animation: phScanMove 12s ease-in-out infinite;
}

html.dark .ph-scanline {
  background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(99, 102, 241, 0.06) 40%,
      rgba(99, 102, 241, 0.13) 50%,
      rgba(99, 102, 241, 0.06) 60%,
      transparent 100%
  );
}

.ph-root:hover .ph-scanline {
  animation-duration: 5s;
}

@keyframes phScanMove {
  0% {
    left: -60%;
  }
  55% {
    left: 110%;
  }
  100% {
    left: 110%;
  }
}

/* ========================================
   左侧内容区
   ======================================== */
.ph-main {
  position: relative;
  z-index: 1;
  flex: 1 1 0;
  min-width: 0;
}

.ph-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.8px;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.22);
  padding: 3px 10px;
  border-radius: 999px;
  margin-bottom: 10px;
}

html.dark .ph-badge {
  color: #a5b4fc;
  background: rgba(99, 102, 241, 0.15);
  border-color: rgba(99, 102, 241, 0.35);
}

.ph-badge-dot {
  width: 5px;
  height: 5px;
  border-radius: 999px;
  background: #6366f1;
  animation: phDotPulse 2s ease-in-out infinite;
}

html.dark .ph-badge-dot {
  background: #a5b4fc;
  box-shadow: 0 0 6px rgba(165, 180, 252, 0.5);
}

@keyframes phDotPulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(0.75);
  }
}

.ph-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--home-text-primary);
  margin: 0 0 4px 0;
  line-height: 1.3;
}

.ph-desc {
  font-size: 13px;
  color: var(--home-text-muted);
  margin: 0;
  line-height: 1.5;
}

/* ========================================
   右侧时钟 + 健康
   ======================================== */
.ph-side {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 6px;
  flex-shrink: 0;
}

.ph-clock {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  color: var(--home-text-primary);
  letter-spacing: 2px;
  font-variant-numeric: tabular-nums;
}

html.dark .ph-clock {
  text-shadow: 0 0 28px rgba(99, 102, 241, 0.3);
}

.ph-date {
  font-size: 12px;
  color: var(--home-text-muted);
  letter-spacing: 0.5px;
  font-variant-numeric: tabular-nums;
}

.ph-health-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #16a34a;
  background: rgba(22, 163, 74, 0.1);
  border: 1px solid rgba(22, 163, 74, 0.22);
}

html.dark .ph-health-chip {
  color: #4ade80;
  background: rgba(22, 163, 74, 0.12);
  border-color: rgba(34, 197, 94, 0.3);
}

.ph-health-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #22c55e;
  animation: phHealthPulse 2s ease-in-out infinite;
}

html.dark .ph-health-dot {
  box-shadow: 0 0 7px rgba(34, 197, 94, 0.6);
}

@keyframes phHealthPulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.4);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(34, 197, 94, 0);
  }
}

/* ========================================
   响应式
   ======================================== */
@media (max-width: 900px) {
  .ph-root {
    flex-direction: column;
    align-items: flex-start;
  }

  .ph-side {
    align-items: flex-start;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 8px;
  }

  .ph-clock {
    font-size: 24px;
  }
}
</style>
