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
}

.page-desc {
  font-size: 14px;
  color: var(--home-text-muted);
  margin-bottom: 32px;
}

.page-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: var(--home-text-muted);
}

.page-placeholder p {
  font-size: 14px;
  color: var(--home-text-muted);
}
</style>
