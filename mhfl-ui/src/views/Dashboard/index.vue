<script setup lang="ts">
import {useI18n} from 'vue-i18n'

const {t} = useI18n()
</script>

<template>
  <div class="dashboard-page">
    <h2 class="page-title">{{ $t('pages.dashboard.title') }}</h2>
    <p class="page-desc">{{ $t('pages.dashboard.desc') }}</p>
    <div class="page-placeholder">
      <p>{{ $t('pages.dashboard.developing') }}</p>
    </div>
  </div>
</template>

<style scoped>
.dashboard-page {
  padding: 32px;
  height: 100%;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--home-text-primary);
  margin-bottom: 8px;
  animation: fadeSlideUp 0.4s ease both;
}

.page-desc {
  font-size: 14px;
  color: var(--home-text-muted);
  margin-bottom: 32px;
  animation: fadeSlideUp 0.4s ease 0.1s both;
}

.page-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: var(--home-text-muted);
  animation: fadeSlideUp 0.5s ease 0.2s both;
}

.page-placeholder::before {
  content: '';
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.08), rgba(139, 92, 246, 0.08));
  border: 1px solid rgba(99, 102, 241, 0.1);
  margin-bottom: 20px;
  animation: subtlePulse 3s ease-in-out infinite;
}

.page-placeholder p {
  font-size: 15px;
  color: var(--home-text-muted);
  letter-spacing: 0.3px;
}
</style>
