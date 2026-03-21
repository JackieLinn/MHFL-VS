<script setup lang="ts">
import {useI18n} from 'vue-i18n'

const {t} = useI18n()

const emit = defineEmits<{ ask: [text: string] }>()

const quickQuestions = [
  {icon: 'i-mdi-school-outline', textKey: 'assistant.quickQ1'},
  {icon: 'i-mdi-compare', textKey: 'assistant.quickQ2'},
  {icon: 'i-mdi-format-list-bulleted', textKey: 'assistant.quickQ3'},
  {icon: 'i-mdi-chart-line', textKey: 'assistant.quickQ4'},
]
</script>

<template>
  <div class="flex-1 flex flex-col items-center justify-center gap-3 px-8 py-10">
    <div class="welcome-avatar flex items-center justify-center w-[68px] h-[68px] rounded-[20px] mb-1.5">
      <span class="i-mdi-robot-excited-outline text-[32px] text-indigo-500"></span>
    </div>
    <h2 class="text-[21px] font-bold text-center" style="color: var(--home-text-primary)">
      {{ t('assistant.welcomeTitle') }}
    </h2>
    <div class="welcome-desc text-sm text-center max-w-[620px] leading-[1.7] space-y-2 whitespace-pre-line" style="color: var(--home-text-muted)">
      <p>{{ t('assistant.welcomeSub1') }}</p>
      <p>{{ t('assistant.welcomeSub2') }}</p>
    </div>
    <div class="grid grid-cols-2 gap-2.5 mt-5 w-full max-w-[680px] quick-grid">
      <button
          v-for="q in quickQuestions"
          :key="q.textKey"
          class="quick-card flex items-center gap-2.5 px-4 py-3.5 rounded-[11px] text-[13px] text-left cursor-pointer"
          @click="emit('ask', t(q.textKey))"
      >
        <span :class="[q.icon]" class="text-[19px] text-indigo-500 opacity-80 shrink-0"></span>
        <span class="leading-[1.4] whitespace-nowrap">{{ t(q.textKey) }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.welcome-avatar {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.12), rgba(139, 92, 246, 0.12));
  border: 1px solid rgba(99, 102, 241, 0.22);
}

.quick-grid {
  grid-template-columns: repeat(2, minmax(min-content, 1fr));
}

.quick-card {
  border: 1px solid var(--home-card-border);
  background: var(--home-card-bg);
  color: var(--home-text-secondary);
  transition: all 0.2s;
}

.quick-card:hover {
  border-color: rgba(99, 102, 241, 0.35);
  background: rgba(99, 102, 241, 0.05);
  color: #6366f1;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.1);
}
</style>
