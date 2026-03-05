<script setup lang="ts">
defineProps<{
  title: string
  desc: string
}>()
</script>

<template>
  <div class="chat-topbar flex items-center justify-between shrink-0 px-6 py-3.5">
    <div class="flex items-center gap-3">
      <span class="i-mdi-robot-outline text-2xl text-indigo-500 shrink-0"></span>
      <div>
        <div class="text-[15px] font-bold leading-[1.3]" style="color: var(--home-text-primary)">{{ title }}</div>
        <div class="text-[12px] mt-px" style="color: var(--home-text-muted)">{{ desc }}</div>
      </div>
    </div>
    <div
        class="chat-topbar-badge flex items-center gap-1.5 text-xs font-semibold tracking-[0.04em] px-2.5 py-1 rounded-full">
      <span class="chat-topbar-badge-dot w-1.5 h-1.5 rounded-full"></span>
      MHFL-VS
    </div>
  </div>
</template>

<style scoped>
.chat-topbar {
  background: var(--home-bg);
  border-bottom: 1px solid var(--home-border);
}

.chat-topbar-badge {
  color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.18);
}

.chat-topbar-badge-dot {
  background: #6366f1;
  animation: badgePulse 2s ease-in-out infinite;
}

@keyframes badgePulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.35;
  }
}
</style>
