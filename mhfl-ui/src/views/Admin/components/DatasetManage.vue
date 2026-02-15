<script setup lang="ts">
import {useI18n} from 'vue-i18n'

const {t} = useI18n()
</script>

<template>
  <div class="dataset-manage placeholder-block">
    <p class="placeholder-text">{{ $t('pages.admin.datasetDeveloping') }}</p>
  </div>
</template>

<style scoped>
.placeholder-block {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 280px;
  background: var(--home-hover-bg);
  border-radius: 12px;
  border: 1px dashed var(--home-border);
}

.placeholder-text {
  font-size: 14px;
  color: var(--home-text-muted);
}
</style>
