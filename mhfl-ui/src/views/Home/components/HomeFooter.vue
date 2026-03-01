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
  padding: 14px 16px;
  text-align: center;
  font-size: 12px;
  color: var(--home-text-muted);
  border-top: 1px solid var(--home-border);
  flex-shrink: 0;
  position: relative;
  letter-spacing: 0.3px;
}

.home-footer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.12), transparent);
  pointer-events: none;
}
</style>
