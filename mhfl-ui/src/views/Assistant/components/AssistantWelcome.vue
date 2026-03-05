<script setup lang="ts">
import {useI18n} from 'vue-i18n'

const {t} = useI18n()

const emit = defineEmits<{ ask: [text: string] }>()

const quickQuestions = [
  {icon: 'i-mdi-compare', text: '对比 FedAvg 和 FedProto 算法'},
  {icon: 'i-mdi-chart-line', text: '如何配置 Non-IID 参数？'},
  {icon: 'i-mdi-memory', text: 'Tiny-ImageNet 显存需求是多少？'},
  {icon: 'i-mdi-school-outline', text: '联邦学习入门概念介绍'},
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
    <p class="text-sm text-center max-w-[440px] leading-[1.7]" style="color: var(--home-text-muted)">
      {{ t('assistant.welcomeSub') }}
    </p>
    <div class="grid grid-cols-2 gap-2.5 mt-5 max-w-[540px] w-full">
      <button
          v-for="q in quickQuestions"
          :key="q.text"
          class="quick-card flex items-center gap-2.5 px-4 py-3.5 rounded-[11px] text-[13px] text-left cursor-pointer"
          @click="emit('ask', q.text)"
      >
        <span :class="[q.icon]" class="text-[19px] text-indigo-500 opacity-80 shrink-0"></span>
        <span class="leading-[1.4]">{{ q.text }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.welcome-avatar {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.12), rgba(139, 92, 246, 0.12));
  border: 1px solid rgba(99, 102, 241, 0.22);
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
