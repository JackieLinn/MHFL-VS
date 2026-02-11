<script setup lang="ts">
import {useI18n} from 'vue-i18n'

const {t} = useI18n()
</script>

<template>
  <div class="admin-page">
    <h2 class="page-title">{{ $t('pages.admin.title') }}</h2>
    <p class="page-desc">{{ $t('pages.admin.desc') }}</p>
    <div class="page-placeholder">
      <p>{{ $t('pages.admin.developing') }}</p>
    </div>
  </div>
</template>

<style scoped>
.admin-page {
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
