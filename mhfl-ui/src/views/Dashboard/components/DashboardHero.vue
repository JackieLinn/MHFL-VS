<script setup lang="ts">
import {ref, onMounted, onBeforeUnmount} from 'vue'

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
  <section class="hero-panel enter-rise mb-6" style="--enter-delay: 0.02s">
    <div class="hero-scanline"></div>
    <div class="hero-main">
      <span class="hero-brand">
        <span class="hero-brand-dot"></span>
        MHFL-VS
      </span>
      <h2 class="page-title text-2xl font-bold mb-2 text-[var(--home-text-primary)]">
        {{ $t('pages.dashboard.title') }}
      </h2>
      <p class="page-desc text-sm mb-0 text-[var(--home-text-muted)]">
        {{ $t('pages.dashboard.desc') }}
      </p>
    </div>
    <div class="hero-side">
      <div class="hero-clock">{{ liveClock }}</div>
      <div class="hero-date">{{ liveDate }}</div>
      <div class="hero-health-chip">
        <span class="hero-health-dot"></span>
        <span>{{ $t('pages.dashboard.healthHealthy') }}</span>
      </div>
    </div>
  </section>
</template>
