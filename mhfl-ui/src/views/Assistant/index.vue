<script setup lang="ts">
import {ref, computed, nextTick, onMounted, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import {ElMessage, ElMessageBox} from 'element-plus'
import AssistantSidebar from './components/AssistantSidebar.vue'
import AssistantTopbar from './components/AssistantTopbar.vue'
import AssistantWelcome from './components/AssistantWelcome.vue'
import AssistantMessageList from './components/AssistantMessageList.vue'
import AssistantInput from './components/AssistantInput.vue'
import {
  createConversation,
  listConversations,
  getConversationDetail,
  deleteConversation,
  updateConversationTitle,
  chatStream,
  updateMessageFeedback,
  type ConversationVO,
  type ConversationDetailVO,
  type MessageVO,
} from '@/api/assistant'
import {getAccountInfo, type AccountVO} from '@/api/account'
import {getUserInfo} from '@/api/user'
import {useAssistantState} from '@/composables/useAssistantState'

interface Message {
  id: number
  role: 'user' | 'assistant'
  content: string
  time: string
  sources?: string[]
  streaming?: boolean
}

/** 页面内会话详情状态，messages 统一为 Message[] 便于流式追加 */
type ConversationDetailState = Omit<ConversationDetailVO, 'messages'> & { messages: Message[] }

interface ConversationWithTime extends ConversationVO {
  time: string
  messages?: Message[]
}

type MsgFeedback = Record<number, 'liked' | 'disliked' | null>

const {t} = useI18n()

// ===== 工具 =====
const formatTime = (dateStr: string): string => {
  if (!dateStr) return ''
  const d = new Date(dateStr.replace(' ', 'T'))
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today.getTime() - 86400000)
  const dDate = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  if (dDate.getTime() === today.getTime()) return `${t('common.today')} ${h}:${m}`
  if (dDate.getTime() === yesterday.getTime()) return `${t('common.yesterday')} ${h}:${m}`
  return dateStr.slice(0, 16).replace('T', ' ')
}

const toMessage = (m: MessageVO): Message => ({
  id: m.id,
  role: m.role as 'user' | 'assistant',
  content: m.content,
  time: formatTime(m.createTime),
  sources: m.sources ?? [],
})

const toConversation = (c: ConversationVO): ConversationWithTime => ({
  ...c,
  time: formatTime(c.updateTime),
})

// ===== 数据 =====
const {
  activeConvId,
  sidebarCollapsed,
  saveActiveCid,
  getStoredActiveCid,
} = useAssistantState()

const conversations = ref<ConversationWithTime[]>([])
const activeConvDetail = ref<ConversationDetailState | null>(null)
const inputText = ref('')
const isSending = ref(false)
const streamingMsgId = ref<number | null>(null)
const copiedMsgId = ref<number | null>(null)
const msgFeedback = ref<MsgFeedback>({})
const searchKeyword = ref('')
const loadingList = ref(false)
const loadingDetail = ref(false)

const msgListRef = ref<InstanceType<typeof AssistantMessageList> | null>(null)
const inputRef = ref<InstanceType<typeof AssistantInput> | null>(null)
const accountInfo = ref<AccountVO | null>(null)

const defaultAvatarUrl = (name: string) => {
  const n = (name || 'U').trim() || 'U'
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(n)}&background=6366f1&color=fff&size=68`
}

const userAvatarUrl = computed(() => {
  const info = accountInfo.value
  const name = info?.username || getUserInfo()?.username || 'U'
  if (info?.avatar) return info.avatar
  return defaultAvatarUrl(name)
})

// ===== 计算属性 =====
const activeConv = computed(() => {
  const detail = activeConvDetail.value
  if (!detail) return null
  const listItem = conversations.value.find(c => c.id === detail.id)
  return {
    id: detail.id,
    title: detail.title,
    preview: listItem?.preview ?? '',
    time: formatTime(detail.updateTime),
    messages: detail.messages,
  }
})

const topbarTitle = computed(() =>
    activeConv.value ? activeConv.value.title : t('pages.assistant.title')
)
const topbarDesc = computed(() =>
    activeConv.value
        ? t('assistant.msgCount', {count: activeConv.value.messages?.length ?? 0})
        : t('pages.assistant.desc')
)

const showWelcome = computed(() =>
    !activeConv.value || (activeConv.value.messages?.length ?? 0) === 0
)

const hasNoConversations = computed(() =>
    !loadingList.value && conversations.value.length === 0 && activeConvId.value === null
)

const sidebarConversations = computed(() =>
    conversations.value.map(c => ({id: c.id, title: c.title, preview: c.preview, time: formatTime(c.updateTime)}))
)

// ===== 加载 =====
const loadList = (keyword?: string) => {
  loadingList.value = true
  listConversations(
      (data) => {
        conversations.value = data.map(toConversation)
        const storedId = getStoredActiveCid()
        const found = storedId != null && data.some(c => c.id === storedId)
        if (found) {
          activeConvId.value = storedId!
          loadDetail(storedId)
        } else if (activeConvId.value != null && !data.some(c => c.id === activeConvId.value)) {
          activeConvId.value = data[0]?.id ?? null
          if (activeConvId.value) loadDetail(activeConvId.value)
          else activeConvDetail.value = null
          saveActiveCid(activeConvId.value)
        }
        loadingList.value = false
      },
      () => {
        loadingList.value = false
      },
      keyword ?? searchKeyword.value
  )
}

let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null
watch(searchKeyword, (kw) => {
  if (searchDebounceTimer) clearTimeout(searchDebounceTimer)
  searchDebounceTimer = setTimeout(() => {
    searchDebounceTimer = null
    loadList(kw ?? '')
  }, 300)
})

const loadDetail = (id: number) => {
  loadingDetail.value = true
  getConversationDetail(
      id,
      (data) => {
        activeConvDetail.value = {...data, messages: data.messages.map(toMessage)}
        msgFeedback.value = {}
        data.messages.forEach(m => {
          if (m.feedback === 1) msgFeedback.value[m.id] = 'liked'
          else if (m.feedback === -1) msgFeedback.value[m.id] = 'disliked'
        })
        loadingDetail.value = false
        nextTick(() => msgListRef.value?.scrollToBottom())
      },
      () => {
        loadingDetail.value = false
      }
  )
}

onMounted(() => {
  loadList()
  getAccountInfo(
      (data) => {
        accountInfo.value = data
      },
      () => {
      }
  )
})

// ===== 侧边栏操作 =====
const newChat = () => {
  createConversation(
      (data) => {
        activeConvId.value = data.id
        saveActiveCid(data.id)
        activeConvDetail.value = {
          id: data.id,
          title: '新建对话',
          messageCount: 0,
          createTime: '',
          updateTime: '',
          messages: [] as Message[],
        }
        inputText.value = ''
      },
      () => {
      }
  )
}

const selectConv = (id: number) => {
  activeConvId.value = id
  saveActiveCid(id)
  loadDetail(id)
}

const editConv = (id: number, currentTitle: string) => {
  ElMessageBox.prompt(t('assistant.editConvTitlePlaceholder'), t('assistant.editConvTitle'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    inputValue: currentTitle,
    inputPlaceholder: t('assistant.editConvTitlePlaceholder')
  }).then((result) => {
    const value = (result as { value?: string }).value ?? ''
    const trimmed = value.trim()
    if (!trimmed) return
    updateConversationTitle(
        id,
        trimmed.length > 30 ? trimmed.slice(0, 30) + '...' : trimmed,
        () => {
          const c = conversations.value.find(x => x.id === id)
          if (c) c.title = trimmed.length > 30 ? trimmed.slice(0, 30) + '...' : trimmed
          if (activeConvDetail.value?.id === id) {
            activeConvDetail.value = {...activeConvDetail.value!, title: c?.title ?? trimmed}
          }
        },
        () => {
        }
    )
  }).catch(() => {
  })
}

const deleteConv = (id: number) => {
  ElMessageBox.confirm(t('assistant.confirmDeleteConv'), t('assistant.deleteConv'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning'
  }).then(() => {
    deleteConversation(
        id,
        () => {
          const idx = conversations.value.findIndex(c => c.id === id)
          if (idx >= 0) conversations.value.splice(idx, 1)
          if (activeConvId.value === id) {
            activeConvId.value = conversations.value[0]?.id ?? null
            saveActiveCid(activeConvId.value)
            activeConvDetail.value = activeConvId.value ? null : activeConvDetail.value
            if (activeConvId.value) loadDetail(activeConvId.value)
            else activeConvDetail.value = null
          }
        },
        () => {
        }
    )
  }).catch(() => {
  })
}

// ===== 发送消息（流式） =====
const sendMessage = () => {
  const text = inputText.value.trim()
  if (!text || isSending.value) return

  const ensureCid = (): Promise<number> => {
    if (activeConvId.value) return Promise.resolve(activeConvId.value)
    return new Promise((resolve, reject) => {
      createConversation(
          (data) => {
            activeConvId.value = data.id
            saveActiveCid(data.id)
            activeConvDetail.value = {
              id: data.id,
              title: '新建对话',
              messageCount: 0,
              createTime: '',
              updateTime: '',
              messages: [] as Message[],
            }
            resolve(data.id)
          },
          (msg) => reject(new Error(msg))
      )
    })
  }

  ensureCid().then(cid => {
    const detail = activeConvDetail.value!
    const userMsg: Message = {
      id: -Date.now(),
      role: 'user',
      content: text,
      time: formatTime(new Date().toISOString()),
    }
    detail.messages = [...(detail.messages || []), userMsg]
    if (detail.title === '新建对话') {
      detail.title = text.length > 18 ? text.slice(0, 18) + '...' : text
    }

    inputText.value = ''
    nextTick(() => inputRef.value?.resetHeight())
    isSending.value = true
    nextTick(() => msgListRef.value?.scrollToBottom())

    const assistantMsgId = -Date.now() - 1
    streamingMsgId.value = null
    nextTick(() => msgListRef.value?.scrollToBottom())

    const ensureAssistantMessage = (): number => {
      const currentDetail = activeConvDetail.value
      if (!currentDetail) return -1
      const idx = currentDetail.messages.findIndex(m => m.id === assistantMsgId)
      if (idx >= 0) return idx
      const assistantMsg: Message = {
        id: assistantMsgId,
        role: 'assistant',
        content: '',
        time: formatTime(new Date().toISOString()),
        sources: [],
        streaming: true,
      }
      currentDetail.messages.push(assistantMsg)
      return currentDetail.messages.length - 1
    }

    const updateAssistantMessage = (updater: (current: Message) => Message) => {
      const currentDetail = activeConvDetail.value
      if (!currentDetail) return
      const idx = currentDetail.messages.findIndex(m => m.id === assistantMsgId)
      if (idx < 0) return
      const current = currentDetail.messages[idx]
      if (!current) return
      currentDetail.messages[idx] = updater(current)
    }

    chatStream(
        {cid, message: text},
        {
          onDelta: (content) => {
            if (streamingMsgId.value === null) {
              streamingMsgId.value = assistantMsgId
              ensureAssistantMessage()
            }
            updateAssistantMessage((current) => ({...current, content: `${current.content}${content}`}))
            nextTick(() => msgListRef.value?.scrollToBottom())
          },
          onDone: (fullContent) => {
            ensureAssistantMessage()
            updateAssistantMessage((current) => ({...current, content: fullContent, streaming: false}))
            streamingMsgId.value = null
            isSending.value = false
            loadList()
            loadDetail(cid)
            nextTick(() => msgListRef.value?.scrollToBottom())
          },
          onError: (msg) => {
            ensureAssistantMessage()
            updateAssistantMessage((current) => ({
              ...current,
              content: current.content || msg,
              streaming: false
            }))
            streamingMsgId.value = null
            isSending.value = false
            ElMessage.warning(msg)
            nextTick(() => msgListRef.value?.scrollToBottom())
          },
        }
    )
  }).catch(msg => {
    ElMessage.warning(msg)
  })
}

// ===== 消息操作 =====
const copyMessage = async (msgId: number, content: string) => {
  try {
    await navigator.clipboard.writeText(content)
  } catch { /* 静默忽略 */
  }
  copiedMsgId.value = msgId
  setTimeout(() => {
    copiedMsgId.value = null
  }, 1500)
}

const toggleLike = (msgId: number) => {
  const next = msgFeedback.value[msgId] === 'liked' ? null : 'liked'
  const code = next === 'liked' ? 1 : 0
  updateMessageFeedback(msgId, code, () => {
    msgFeedback.value = {...msgFeedback.value, [msgId]: next}
  }, () => {
  })
}

const toggleDislike = (msgId: number) => {
  const next = msgFeedback.value[msgId] === 'disliked' ? null : 'disliked'
  const code = next === 'disliked' ? -1 : 0
  updateMessageFeedback(msgId, code, () => {
    msgFeedback.value = {...msgFeedback.value, [msgId]: next}
  }, () => {
  })
}
</script>

<template>
  <div class="flex h-full overflow-hidden" style="background: var(--home-bg)">

    <AssistantSidebar
        :conversations="sidebarConversations"
        :active-conv-id="activeConvId"
        :collapsed="sidebarCollapsed"
        :is-sending="isSending"
        :search-keyword="searchKeyword"
        @select="selectConv"
        @edit="editConv"
        @delete="deleteConv"
        @new-chat="newChat"
        @toggle="sidebarCollapsed = !sidebarCollapsed"
        @update:search-keyword="searchKeyword = $event"
    />

    <main class="flex flex-col flex-1 overflow-hidden min-w-0">

      <AssistantTopbar :title="topbarTitle" :desc="topbarDesc"/>

      <!-- 暂无聊天记录 -->
      <div v-if="hasNoConversations" class="flex-1 flex flex-col items-center justify-center gap-3 px-8 py-10">
        <span class="i-mdi-chat-sleep-outline text-[48px] opacity-40" style="color: var(--home-text-muted)"></span>
        <p class="text-base" style="color: var(--home-text-muted)">{{ t('assistant.noChatHistory') }}</p>
        <button
            class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
            style="background: rgba(99, 102, 241, 0.15); color: #6366f1; border: 1px solid rgba(99, 102, 241, 0.3)"
            @click="newChat"
        >
          {{ t('assistant.newChat') }}
        </button>
      </div>

      <!-- 欢迎屏（有会话但无消息） -->
      <AssistantWelcome
          v-else-if="showWelcome"
          @ask="(text) => { inputText = text; nextTick(() => sendMessage()) }"
      />

      <!-- 消息列表 -->
      <AssistantMessageList
          v-else
          ref="msgListRef"
          :messages="activeConv?.messages ?? []"
          :streaming-msg-id="streamingMsgId"
          :is-sending="isSending"
          :copied-msg-id="copiedMsgId"
          :msg-feedback="msgFeedback"
          :user-avatar-url="userAvatarUrl"
          @copy="copyMessage"
          @like="toggleLike"
          @dislike="toggleDislike"
      />

      <AssistantInput
          ref="inputRef"
          v-model="inputText"
          :is-sending="isSending"
          @send="sendMessage"
      />

    </main>
  </div>
</template>
