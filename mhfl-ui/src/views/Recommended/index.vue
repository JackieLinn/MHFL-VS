<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
</script>

<template>
  <div class="p-8 h-full flex flex-col">
    <PageHeader
        class="mb-5"
        :title="$t('pages.recommended.title')"
        :desc="$t('pages.recommended.desc')"
    />
    <div class="flex-1 flex flex-col items-center justify-center text-[var(--home-text-muted)]">
      <div class="placeholder-icon"></div>
      <p class="text-[15px] tracking-wide">{{ $t('pages.recommended.developing') }}</p>
    </div>
  </div>
</template>

<style scoped>
.placeholder-icon {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.08), rgba(139, 92, 246, 0.08));
  border: 1px solid rgba(99, 102, 241, 0.1);
  margin-bottom: 20px;
}
</style>
