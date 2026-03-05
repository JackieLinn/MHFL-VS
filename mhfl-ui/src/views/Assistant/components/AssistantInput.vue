<script setup lang="ts">
import {ref, watch, nextTick} from 'vue'
import {useI18n} from 'vue-i18n'

const {t} = useI18n()

const props = defineProps<{
  modelValue: string
  isSending: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [val: string]
  send: []
}>()

// ===== 自动高度扩展 =====
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const MAX_LINES = 3
const LINE_H = Math.ceil(14 * 1.6) // 23px

const autoResize = () => {
  const el = textareaRef.value
  if (!el) return
  el.style.height = '1px'
  const maxH = LINE_H * MAX_LINES + 4
  el.style.height = Math.max(LINE_H, Math.min(el.scrollHeight, maxH)) + 'px'
  el.style.overflowY = el.scrollHeight > maxH ? 'auto' : 'hidden'
}

const resetHeight = () => {
  const el = textareaRef.value
  if (!el) return
  el.style.height = LINE_H + 'px'
  el.style.overflowY = 'hidden'
}

// 编程式清空时收缩
watch(() => props.modelValue, (val) => {
  if (!val) nextTick(() => resetHeight())
})

// 暴露给父组件在发送后调用
defineExpose({resetHeight})

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    emit('send')
  }
}
</script>

<template>
  <div class="chat-input-area shrink-0 px-6 pt-3.5 pb-4.5">
    <div class="chat-input-wrap flex items-end gap-2 px-4 py-2.5 rounded-[14px]">
      <textarea
          ref="textareaRef"
          :value="modelValue"
          class="chat-input flex-1 text-sm leading-[1.6] resize-none overflow-y-hidden font-inherit p-0 border-none outline-none bg-transparent"
          :placeholder="t('assistant.inputPlaceholder')"
          :disabled="isSending"
          @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value); autoResize()"
          @keydown="handleKeydown"
      ></textarea>
      <button
          class="input-send-btn flex items-center justify-center w-[34px] h-[34px] rounded-[9px] text-[16px] shrink-0 transition-all"
          :class="{ 'input-send-btn--active': modelValue.trim() && !isSending }"
          :disabled="!modelValue.trim() || isSending"
          @click="emit('send')"
          :title="t('assistant.send')"
      >
        <span v-if="!isSending" class="i-mdi-send"></span>
        <span v-else class="i-mdi-loading spin"></span>
      </button>
    </div>
    <p class="text-[11px] text-center mt-1.5 opacity-75" style="color: var(--home-text-muted)">
      {{ t('assistant.inputHint') }}
    </p>
  </div>
</template>

<style scoped>
.chat-input-area {
  background: var(--home-bg);
  border-top: 1px solid var(--home-border);
}

.chat-input-wrap {
  border: 1px solid var(--home-border);
  background: var(--home-card-bg);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.chat-input-wrap:focus-within {
  border-color: rgba(99, 102, 241, 0.45);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.07);
}

.chat-input {
  color: var(--home-text-primary);
  height: 23px;
  min-height: 23px;
  align-self: center;
}

.chat-input::placeholder {
  color: var(--home-text-muted);
  line-height: 1.6;
}

.input-send-btn {
  border: none;
  background: rgba(99, 102, 241, .15);
  color: rgba(99, 102, 241, .5);
  cursor: not-allowed;
}

.input-send-btn--active {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(99, 102, 241, .32);
}

.input-send-btn--active:hover {
  transform: scale(1.06);
  box-shadow: 0 4px 14px rgba(99, 102, 241, .42);
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg)
  }
  to {
    transform: rotate(360deg)
  }
}
</style>
