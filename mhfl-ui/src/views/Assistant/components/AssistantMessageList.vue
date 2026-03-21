<script setup lang="ts">
import {ref} from 'vue'
import {useI18n} from 'vue-i18n'

interface Message {
  id: number
  role: 'user' | 'assistant'
  content: string
  time: string
  sources?: string[]
  streaming?: boolean
}

type MsgFeedback = Record<number, 'liked' | 'disliked' | null>

const {t} = useI18n()

defineProps<{
  messages: Message[]
  streamingMsgId: number | null
  isSending: boolean
  copiedMsgId: number | null
  msgFeedback: MsgFeedback
}>()

const emit = defineEmits<{
  copy: [msgId: number, content: string]
  like: [msgId: number]
  dislike: [msgId: number]
}>()

const listRef = ref<HTMLElement | null>(null)
const scrollToBottom = () => {
  if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
}

const formatContent = (raw: string): string => {
  return raw
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/`(.*?)`/g, '<code>$1</code>')
      .replace(/\n\n/g, '</p><p>')
      .replace(/\n/g, '<br>')
      .replace(/^/, '<p>')
      .replace(/$/, '</p>')
      .replace(/---/g, '<hr>')
      .replace(/\|(.+)\|/g, (match) => {
        const cells = match.split('|').filter(c => c.trim() !== '')
        return '<tr>' + cells.map(c => `<td>${c.trim()}</td>`).join('') + '</tr>'
      })
}

defineExpose({scrollToBottom})
</script>

<template>
  <div ref="listRef" class="flex-1 overflow-y-auto flex flex-col gap-[18px] px-6 py-5">
    <template v-for="msg in messages" :key="msg.id">

      <!-- 用户消息 -->
      <div v-if="msg.role === 'user'" class="flex items-start gap-2.5 flex-row-reverse">
        <div
            class="msg-avatar msg-avatar--user flex items-center justify-center w-[34px] h-[34px] rounded-[9px] text-[17px] shrink-0 mt-0.5">
          <span class="i-mdi-account"></span>
        </div>
        <div
            class="msg-bubble msg-bubble--user max-w-[66%] rounded-[14px] rounded-br-[4px] px-4 py-3 text-sm leading-[1.75]">
          <p>{{ msg.content }}</p>
          <span class="block text-[11px] mt-1 text-right opacity-65">{{ msg.time }}</span>
        </div>
      </div>

      <!-- AI 消息 -->
      <div v-else class="flex items-start gap-2.5">
        <div
            class="msg-avatar msg-avatar--ai flex items-center justify-center w-[34px] h-[34px] rounded-[9px] text-[17px] shrink-0 mt-0.5">
          <span class="i-mdi-robot-outline"></span>
        </div>
        <div
            class="msg-bubble msg-bubble--ai max-w-[66%] rounded-[14px] rounded-bl-[4px] px-4 py-3 text-sm leading-[1.75]">
          <!-- 消息头 -->
          <div class="flex items-center gap-2 mb-1.5">
            <span class="text-xs font-bold text-indigo-500">MHFL 助手</span>
            <span class="text-[11px]" style="color: var(--home-text-muted)">{{ msg.time }}</span>
            <span v-if="streamingMsgId === msg.id"
                  class="streaming-badge flex items-center gap-1 text-[11px] font-medium px-1.5 py-px rounded-[10px]">
              <span class="streaming-dot w-[5px] h-[5px] rounded-full"></span>
              {{ t('assistant.streaming') }}
            </span>
          </div>
          <!-- 内容 -->
          <div v-if="msg.content" class="msg-content" v-html="formatContent(msg.content)"></div>
          <span v-else class="stream-cursor-only"></span>
          <span v-if="streamingMsgId === msg.id" class="stream-cursor"></span>
          <!-- 参考来源 -->
          <div class="msg-sources mt-2 pt-2" style="border-top: 1px solid var(--home-border)">
            <span class="msg-sources-label text-[11px] mr-2" style="color: var(--home-text-muted)">参考：</span>
            <template v-if="(msg.sources ?? []).length">
              <span
                  v-for="(s, i) in (msg.sources ?? [])"
                  :key="i"
                  class="msg-source-tag"
              >{{ s }}</span>
            </template>
            <span v-else class="msg-source-none" style="color: var(--home-text-muted)">无</span>
          </div>
          <!-- 操作按钮 -->
          <div v-if="!msg.streaming" class="msg-actions flex gap-1 mt-2">
            <button
                class="msg-action-btn flex items-center justify-center w-[25px] h-[25px] rounded-[6px] text-[13px] cursor-pointer"
                :class="{ 'msg-action-btn--copied': copiedMsgId === msg.id }"
                :title="t('assistant.copyMsg')"
                @click="emit('copy', msg.id, msg.content)"
            >
              <span :class="copiedMsgId === msg.id ? 'i-mdi-check' : 'i-mdi-content-copy'"></span>
            </button>
            <button
                class="msg-action-btn flex items-center justify-center w-[25px] h-[25px] rounded-[6px] text-[13px] cursor-pointer"
                :class="{ 'msg-action-btn--liked': msgFeedback[msg.id] === 'liked' }"
                :title="t('assistant.likeMsg')"
                @click="emit('like', msg.id)"
            >
              <span :class="msgFeedback[msg.id] === 'liked' ? 'i-mdi-thumb-up' : 'i-mdi-thumb-up-outline'"></span>
            </button>
            <button
                class="msg-action-btn flex items-center justify-center w-[25px] h-[25px] rounded-[6px] text-[13px] cursor-pointer"
                :class="{ 'msg-action-btn--disliked': msgFeedback[msg.id] === 'disliked' }"
                :title="t('assistant.dislikeMsg')"
                @click="emit('dislike', msg.id)"
            >
              <span
                  :class="msgFeedback[msg.id] === 'disliked' ? 'i-mdi-thumb-down' : 'i-mdi-thumb-down-outline'"></span>
            </button>
          </div>
        </div>
      </div>

    </template>

    <!-- 思考中动画 -->
    <div v-if="isSending && streamingMsgId === null" class="flex items-start gap-2.5">
      <div
          class="msg-avatar msg-avatar--ai flex items-center justify-center w-[34px] h-[34px] rounded-[9px] text-[17px] shrink-0 mt-0.5">
        <span class="i-mdi-robot-outline"></span>
      </div>
      <div
          class="msg-bubble msg-bubble--ai msg-bubble--typing rounded-[14px] rounded-bl-[4px] flex items-center gap-2 px-4 py-3.5">
        <span class="typing-dot"></span>
        <span class="typing-dot"></span>
        <span class="typing-dot"></span>
        <span class="typing-label text-[13px]" style="color: var(--home-text-muted)">{{
            t('assistant.thinking')
          }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 消息头像 */
.msg-avatar--ai {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.18), rgba(139, 92, 246, 0.18));
  border: 1px solid rgba(99, 102, 241, 0.22);
  color: #6366f1;
}

.msg-avatar--user {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.12), rgba(20, 184, 166, 0.12));
  border: 1px solid rgba(16, 185, 129, 0.22);
  color: #10b981;
}

/* 气泡 */
.msg-bubble--user {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  box-shadow: 0 2px 10px rgba(99, 102, 241, 0.28);
}

.msg-bubble--ai {
  background: var(--home-card-bg);
  border: 1px solid var(--home-card-border);
  color: var(--home-text-primary);
  box-shadow: 0 2px 6px var(--home-card-shadow);
}

/* 流式徽章 */
.streaming-badge {
  color: #10b981;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.streaming-dot {
  background: #10b981;
  animation: streamDotPulse 1s ease-in-out infinite;
}

@keyframes streamDotPulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1)
  }
  50% {
    opacity: .4;
    transform: scale(.7)
  }
}

/* 流式光标 */
.stream-cursor, .stream-cursor-only {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: #6366f1;
  vertical-align: text-bottom;
  border-radius: 1px;
  animation: cursorBlink 0.7s step-end infinite;
}

.stream-cursor {
  margin-left: 1px;
}

@keyframes cursorBlink {
  0%, 100% {
    opacity: 1
  }
  50% {
    opacity: 0
  }
}

/* 消息内容 :deep */
.msg-content :deep(p) {
  margin-bottom: 8px;
}

.msg-content :deep(p:last-child) {
  margin-bottom: 0;
}

.msg-content :deep(strong) {
  color: var(--home-text-primary);
  font-weight: 700;
}

.msg-content :deep(code) {
  background: rgba(99, 102, 241, .1);
  color: #6366f1;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
}

.msg-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--home-border);
  margin: 10px 0;
}

.msg-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0;
  font-size: 13px;
}

.msg-content :deep(tr) {
  border-bottom: 1px solid var(--home-border);
}

.msg-content :deep(td) {
  padding: 5px 9px;
  color: var(--home-text-secondary);
}

.msg-content :deep(tr:first-child td) {
  font-weight: 700;
  color: var(--home-text-primary);
  background: rgba(99, 102, 241, .04);
}

/* 参考来源标签（椭圆形，深浅色模式适配） */
.msg-sources {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.msg-source-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 500;
  background: var(--home-hover-bg);
  border: 1px solid var(--home-border);
  color: var(--home-text-secondary);
}

.msg-source-none {
  font-size: 11px;
}

/* 操作按钮 */
.msg-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.msg-bubble--ai:hover .msg-actions {
  opacity: 1;
}

.msg-action-btn {
  border: 1px solid var(--home-border);
  background: transparent;
  color: var(--home-text-muted);
  transition: all 0.15s;
}

.msg-action-btn:hover {
  border-color: rgba(99, 102, 241, .4);
  color: #6366f1;
  background: rgba(99, 102, 241, .06);
}

.msg-action-btn--copied {
  border-color: rgba(16, 185, 129, .4) !important;
  color: #10b981 !important;
  background: rgba(16, 185, 129, .08) !important;
}

.msg-action-btn--liked {
  border-color: rgba(99, 102, 241, .45) !important;
  color: #6366f1 !important;
  background: rgba(99, 102, 241, .1) !important;
}

.msg-action-btn--disliked {
  border-color: rgba(245, 158, 11, .45) !important;
  color: #f59e0b !important;
  background: rgba(245, 158, 11, .08) !important;
}

/* 打字动画 */
.typing-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #6366f1;
  opacity: .5;
  animation: typingBounce 1.2s infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: .2s
}

.typing-dot:nth-child(3) {
  animation-delay: .4s
}

@keyframes typingBounce {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: .35
  }
  30% {
    transform: translateY(-6px);
    opacity: 1
  }
}
</style>
