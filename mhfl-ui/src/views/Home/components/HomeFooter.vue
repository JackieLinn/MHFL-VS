<script setup lang="ts">
import {useI18n} from 'vue-i18n'

const {t} = useI18n()
</script>

<template>
  <footer class="home-footer">
    {{ $t('common.copyright') }}
  </footer>
</template>

<style scoped>
.home-footer {
  padding: 16px;
  text-align: center;
  font-size: 12px;
  color: var(--home-text-muted);
  border-top: 1px solid var(--home-border);
  flex-shrink: 0;
}
</style>
