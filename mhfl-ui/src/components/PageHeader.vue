<script setup lang="ts">
import {ref, onMounted, onBeforeUnmount, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import AnalogClock from './AnalogClock.vue'
import {useSystemHealth} from '@/composables/useSystemHealth'

const {locale} = useI18n()
const {isAllHealthy, ensurePolling} = useSystemHealth()

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
const liveWeekday = ref('')
let liveClockTimer: ReturnType<typeof window.setInterval> | null = null

const WEEKDAY_ZH = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const WEEKDAY_EN = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

const updateLiveClock = () => {
  const now = new Date()
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  const ss = String(now.getSeconds()).padStart(2, '0')
  const yyyy = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const dd = String(now.getDate()).padStart(2, '0')
  const dayOfWeek = now.getDay()
  liveClock.value = `${hh}:${mm}:${ss}`
  liveDate.value = `${yyyy}-${month}-${dd}`
  liveWeekday.value = (locale.value?.startsWith('zh') ? WEEKDAY_ZH[dayOfWeek] : WEEKDAY_EN[dayOfWeek]) ?? ''
}

watch(locale, () => {
  updateLiveClock()
}, {immediate: false})

onMounted(() => {
  updateLiveClock()
  liveClockTimer = window.setInterval(updateLiveClock, 1000)
  ensurePolling()
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
    <div class="ph-center">
      <div class="ph-clocks-row">
        <AnalogClock
            timezone="Asia/Shanghai"
            :label="$t('pages.dashboard.clockBeijing')"
            :size="96"
            variant="roman"
        />
        <AnalogClock
            timezone="Asia/Tokyo"
            :label="$t('pages.dashboard.clockTokyo')"
            :size="96"
            variant="roman"
        />
        <AnalogClock
            timezone="Europe/London"
            :label="$t('pages.dashboard.clockLondon')"
            :size="96"
            variant="roman"
        />
        <AnalogClock
            timezone="America/New_York"
            :label="$t('pages.dashboard.clockNewYork')"
            :size="96"
            variant="roman"
        />
      </div>
    </div>
    <div class="ph-side">
      <div class="ph-time-block">
        <div class="ph-time-inner">
          <div class="ph-clock">{{ liveClock }}</div>
          <div class="ph-date">{{ liveDate }} {{ liveWeekday }}</div>
        </div>
      </div>
      <div class="ph-health-chip" :class="{ 'ph-health-chip--unhealthy': !isAllHealthy }">
        <span class="ph-health-dot"></span>
        <span>{{ isAllHealthy ? $t('pages.dashboard.healthHealthy') : $t('pages.dashboard.healthUnhealthy') }}</span>
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
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
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
  min-width: 0;
  justify-self: start;
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
   中间国际时钟
   ======================================== */
.ph-center {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  justify-self: center;
}

.ph-clocks-row {
  display: flex;
  align-items: flex-end;
  gap: 16px;
}

/* ========================================
   右侧数字时钟 + 健康
   ======================================== */
.ph-side {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 6px;
  justify-self: end;
}

.ph-time-block {
  display: flex;
  align-items: center;
}

.ph-time-inner {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
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
  font-size: 14px;
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

.ph-health-chip--unhealthy {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.1);
  border-color: rgba(220, 38, 38, 0.22);
}

html.dark .ph-health-chip {
  color: #4ade80;
  background: rgba(22, 163, 74, 0.12);
  border-color: rgba(34, 197, 94, 0.3);
}

html.dark .ph-health-chip--unhealthy {
  color: #fca5a5;
  background: rgba(248, 113, 113, 0.12);
  border-color: rgba(248, 113, 113, 0.3);
}

.ph-health-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #22c55e;
  animation: phHealthPulse 2s ease-in-out infinite;
}

.ph-health-chip--unhealthy .ph-health-dot {
  background: #dc2626;
  animation: phHealthPulseRed 2s ease-in-out infinite;
}

html.dark .ph-health-dot {
  box-shadow: 0 0 7px rgba(34, 197, 94, 0.6);
}

html.dark .ph-health-chip--unhealthy .ph-health-dot {
  background: #f87171;
  box-shadow: 0 0 7px rgba(248, 113, 113, 0.5);
}

@keyframes phHealthPulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.4);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(34, 197, 94, 0);
  }
}

@keyframes phHealthPulseRed {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(220, 38, 38, 0.4);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(220, 38, 38, 0);
  }
}

/* ========================================
   响应式
   ======================================== */
@media (max-width: 900px) {
  .ph-root {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto auto;
  }

  .ph-main {
    order: 1;
  }

  .ph-center {
    order: 2;
    width: 100%;
    justify-content: flex-start;
  }

  .ph-side {
    order: 3;
    align-items: flex-start;
  }

  .ph-clocks-row {
    flex-wrap: wrap;
    gap: 12px;
  }

  .ph-clock {
    font-size: 24px;
  }
}
</style>
