<script setup lang="ts">
import {ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {marked} from 'marked'
import markedKatex from 'marked-katex-extension'
import DOMPurify from 'dompurify'
import 'katex/dist/katex.min.css'

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
  userAvatarUrl?: string
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

marked.setOptions({gfm: true, breaks: true})
marked.use(markedKatex({throwOnError: false, nonStandard: true}))

/** 将误用为块级的短公式 $$...$$ 转为行内 $...$，避免符号与冒号/文字分行 */
const normalizeInlineMath = (raw: string): string => {
  return raw.replace(/\$\$([^$\n]+?)\$\$/g, (_, content) => {
    const t = content.trim()
    if (t.includes('\n') || /\\frac|\\sum|\\int|\\begin|\\\\/.test(t) || t.length > 45)
      return `$$${content}$$`
    return `$${t}$`
  })
}

const formatContent = (raw: string): string => {
  if (!raw) return ''
  const normalized = normalizeInlineMath(raw)
  const html = marked.parse(normalized, {async: false}) as string
  return DOMPurify.sanitize(html)
}

defineExpose({scrollToBottom})
</script>

<template>
  <div ref="listRef" class="flex-1 overflow-y-auto flex flex-col gap-[18px] px-6 py-5">
    <template v-for="msg in messages" :key="msg.id">

      <!-- 用户消息 -->
      <div v-if="msg.role === 'user'" class="flex items-start gap-2.5 flex-row-reverse">
        <div
            class="msg-avatar msg-avatar--user flex items-center justify-center w-[34px] h-[34px] rounded-[9px] text-[17px] shrink-0 mt-0.5 overflow-hidden">
          <img
              v-if="userAvatarUrl"
              :src="userAvatarUrl"
              alt="avatar"
              class="w-full h-full object-cover"
          />
          <span v-else class="i-mdi-account"></span>
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

    <!-- 思考中：与「正在输出」徽章样式一致 -->
    <div v-if="isSending && streamingMsgId === null" class="flex items-start gap-2.5">
      <div
          class="msg-avatar msg-avatar--ai flex items-center justify-center w-[34px] h-[34px] rounded-[9px] text-[17px] shrink-0 mt-0.5">
        <span class="i-mdi-robot-outline"></span>
      </div>
      <div
          class="msg-bubble msg-bubble--ai rounded-[14px] rounded-bl-[4px] flex items-center gap-2 px-4 py-3">
        <span class="streaming-badge flex items-center gap-1 text-[11px] font-medium px-1.5 py-px rounded-[10px]">
          <span class="streaming-dot w-[5px] h-[5px] rounded-full"></span>
          {{ t('assistant.thinking') }}
        </span>
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

/* 消息内容 :deep - Markdown 渲染样式 */
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

.msg-content :deep(em) {
  font-style: italic;
}

.msg-content :deep(code) {
  background: rgba(99, 102, 241, .1);
  color: #6366f1;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
}

.msg-content :deep(pre) {
  margin: 10px 0;
  padding: 12px;
  background: var(--home-hover-bg);
  border: 1px solid var(--home-border);
  border-radius: 6px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.5;
}

.msg-content :deep(pre code) {
  background: none;
  padding: 0;
  color: var(--home-text-primary);
  font-size: inherit;
}

.msg-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--home-border);
  margin: 10px 0;
}

.msg-content :deep(h1), .msg-content :deep(h2), .msg-content :deep(h3),
.msg-content :deep(h4), .msg-content :deep(h5), .msg-content :deep(h6) {
  margin: 12px 0 8px;
  font-weight: 700;
  color: var(--home-text-primary);
  line-height: 1.4;
}

.msg-content :deep(h1) {
  font-size: 1.25em;
}

.msg-content :deep(h2) {
  font-size: 1.15em;
}

.msg-content :deep(h3) {
  font-size: 1.08em;
}

.msg-content :deep(h4), .msg-content :deep(h5), .msg-content :deep(h6) {
  font-size: 1em;
}

.msg-content :deep(ul), .msg-content :deep(ol) {
  margin: 8px 0;
  padding-left: 1.5em;
}

.msg-content :deep(li) {
  margin: 4px 0;
  color: var(--home-text-secondary);
}

.msg-content :deep(blockquote) {
  margin: 10px 0;
  padding: 8px 12px;
  border-left: 4px solid rgba(99, 102, 241, .5);
  background: rgba(99, 102, 241, .04);
  color: var(--home-text-secondary);
}

.msg-content :deep(a) {
  color: #6366f1;
  text-decoration: none;
}

.msg-content :deep(a:hover) {
  text-decoration: underline;
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

.msg-content :deep(td), .msg-content :deep(th) {
  padding: 5px 9px;
  color: var(--home-text-secondary);
}

.msg-content :deep(tr:first-child td),
.msg-content :deep(tr:first-child th) {
  font-weight: 700;
  color: var(--home-text-primary);
  background: rgba(99, 102, 241, .04);
}

/* KaTeX 数学公式 */
.msg-content :deep(.katex) {
  font-size: 1.1em;
}

.msg-content :deep(.katex-display) {
  margin: 10px 0;
  overflow-x: auto;
  overflow-y: hidden;
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

</style>
