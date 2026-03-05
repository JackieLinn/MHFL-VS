<script setup lang="ts">
import {ref, computed, nextTick, watch} from 'vue'
import {useI18n} from 'vue-i18n'

const {t} = useI18n()

// ===================== 输入框自动扩展 =====================
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const MAX_INPUT_LINES = 3
const LINE_HEIGHT_PX = Math.ceil(14 * 1.6) // 23px

const autoResize = () => {
  const el = textareaRef.value
  if (!el) return
  // 先设为 1px，让浏览器重新计算 scrollHeight（确保清空后能正确收缩）
  el.style.height = '1px'
  const maxH = LINE_HEIGHT_PX * MAX_INPUT_LINES + 4
  const newH = Math.max(LINE_HEIGHT_PX, Math.min(el.scrollHeight, maxH))
  el.style.height = newH + 'px'
  el.style.overflowY = el.scrollHeight > maxH ? 'auto' : 'hidden'
}

const resetInputHeight = () => {
  const el = textareaRef.value
  if (!el) return
  el.style.height = LINE_HEIGHT_PX + 'px'
  el.style.overflowY = 'hidden'
}

// ===================== 类型定义 =====================
interface Message {
  id: number
  role: 'user' | 'assistant'
  content: string
  time: string
  streaming?: boolean
}

interface Conversation {
  id: number
  title: string
  preview: string
  time: string
  messages: Message[]
}

// ===================== 工具 =====================
const nowTime = () => {
  const d = new Date()
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

let msgIdCounter = 1000

// ===================== AI 回复池（模拟）=====================
const aiReplies = {
  fedavg: 'FedAvg 通过对所有客户端模型参数加权平均来聚合全局模型，适合**同构模型 + 轻度 Non-IID** 场景，通信内容是完整模型参数。\n\n主要优势：实现简单、通信轮次少、适合大规模部署。但在高度 Non-IID 场景下，客户端梯度方向差异过大，容易导致全局模型收敛缓慢甚至发散。\n\n如需在异构设备上部署，建议配合 FedProto 使用。',
  fedproto: 'FedProto 专为**模型异构**场景设计，每个客户端上传各类别的特征原型（Prototype），服务器聚合原型后分发，客户端只需对齐特征空间，无需相同模型结构。\n\n核心优势：\n- 支持客户端使用不同大小/结构的模型\n- 通信量远小于传输完整参数\n- 在高度 Non-IID 下鲁棒性更强\n\n本平台的 CIFAR-100 + Non-IID 场景推荐优先使用 FedProto。',
  noniid: '推荐从**中度 Non-IID** 开始实验：`classes_per_node=10, low_prob=0.1`。\n\n这个组合在 CIFAR-100 上表现稳定，收敛曲线通常比较平滑，之后再根据结果决定是否加强异质性。\n\n参数说明：\n- `classes_per_node`：每个节点拥有的类别数，越小异质性越强\n- `low_prob`：稀少类别的采样概率，越小分布越不均匀\n\n建议先跑 50~100 轮观察 loss 趋势，再决定是否调整。',
  gpu: 'CIFAR-100 训练约需 **3GB 显存**，Tiny-ImageNet 约需 **5.5GB**。\n\n可在仪表盘首页实时查看当前 GPU 使用率，显存不足时建议：\n1. 降低 `fraction`（减少并发客户端）\n2. 开启混合精度训练（AMP），可节省约 40~50% 显存\n3. 逐步降低 batch size：64 → 32 → 16',
  epochs: '`num_steps` 是**全局通信轮数**（服务器-客户端迭代次数），`epochs` 是每个客户端在每轮本地训练的轮次。\n\n建议 `num_steps=200, epochs=5` 作为标准配置起点。\n\n增加 `num_steps` 比增加 `epochs` 更稳定：前者增加全局协作次数，后者增加本地自适应程度——过多 epochs 会加剧 Non-IID 漂移问题。',
  fallback: '这是个好问题！目前我的知识库主要覆盖联邦学习相关内容，如算法对比、参数配置、训练调优等。\n\n你可以尝试问我：\n- 各算法（FedAvg、FedProto、FedSSA、LG-FedAvg）的区别\n- Non-IID 参数如何配置\n- GPU 显存不足的解决方案\n- num_steps 与 epochs 的选择策略\n\n或者直接前往**推荐展示**页面，查看经过验证的成功案例和参数配置！'
}

const getAiReply = (input: string): string => {
  const lower = input.toLowerCase()
  if (lower.includes('fedavg') && !lower.includes('fedproto')) return aiReplies.fedavg
  if (lower.includes('fedproto')) return aiReplies.fedproto
  if (lower.includes('non-iid') || lower.includes('noniid') || lower.includes('异质') || lower.includes('classes_per_node')) return aiReplies.noniid
  if (lower.includes('gpu') || lower.includes('显存') || lower.includes('imagenet')) return aiReplies.gpu
  if (lower.includes('epoch') || lower.includes('num_step') || lower.includes('轮')) return aiReplies.epochs
  return aiReplies.fallback
}

// ===================== 历史会话数据 =====================
const conversations = ref<Conversation[]>([
  {
    id: 1,
    title: 'FedAvg 与 FedProto 对比',
    preview: 'FedAvg 通过对所有客户端模型参数求加权平均来聚合...',
    time: '今天 14:32',
    messages: [
      {id: 1, role: 'user', content: '请帮我比较一下 FedAvg 和 FedProto 算法的区别和适用场景。', time: '14:30'},
      {
        id: 2, role: 'assistant',
        content: `**FedAvg（联邦平均）**\n\nFedAvg 是最经典的联邦学习算法，核心思路是：服务器将全局模型分发给客户端，客户端本地训练若干 epoch 后上传**模型参数**，服务器加权平均后得到新的全局模型。\n\n适用场景：\n- 客户端模型结构**相同**（同构场景）\n- 数据分布较均匀（IID 或轻度 Non-IID）\n\n---\n\n**FedProto（联邦原型）**\n\nFedProto 专为**模型异构**场景设计，客户端上传各类别的特征原型（Prototype），服务器聚合原型再分发，客户端以原型为正则化目标。\n\n适用场景：\n- 客户端模型结构**不同**（异构场景）\n- 数据 Non-IID 较为严重\n\n---\n\n| 维度 | FedAvg | FedProto |\n|------|--------|----------|\n| 模型同构性 | 要求同构 | 支持异构 |\n| 通信内容 | 模型参数 | 类别原型 |\n| Non-IID 鲁棒性 | 一般 | 较强 |`,
        time: '14:31'
      },
      {id: 3, role: 'user', content: '在 CIFAR-100 + 高度 Non-IID 下，推荐用哪个？', time: '14:31'},
      {
        id: 4, role: 'assistant',
        content: '在 CIFAR-100 + 高度 Non-IID 的场景下，推荐使用 **FedProto**。\n\n原因：CIFAR-100 有 100 个类别，Non-IID 时每个节点只见到少数类别，FedAvg 的全局平均会导致模型遗忘局部分布。FedProto 的原型对齐机制能在特征空间建立共识，`classes_per_node` 较小时通常比 FedAvg 高出 5~15 个百分点。',
        time: '14:32'
      }
    ]
  },
  {
    id: 2,
    title: 'Non-IID 参数配置建议',
    preview: '推荐 classes_per_node 设置为 5~10，low_prob 建议...',
    time: '今天 10:15',
    messages: [
      {
        id: 1,
        role: 'user',
        content: '我想做一个 Non-IID 实验，classes_per_node 和 low_prob 应该怎么配置？',
        time: '10:13'
      },
      {
        id: 2, role: 'assistant',
        content: '**`classes_per_node`（每节点类别数）**\n\n- `100`：近似 IID\n- `10`：中度 Non-IID（推荐入门）\n- `5`：高度 Non-IID\n- `2`：极端 Non-IID\n\n**`low_prob`（稀少类别采样概率）**\n\n- `0.0`：只有主要类别数据\n- `0.1`：推荐值，保留少量稀少类别\n- `0.5`：接近均匀\n\n| 场景 | classes_per_node | low_prob |\n|------|-----------------|----------|\n| 轻度 | 20 | 0.3 |\n| 中度 | 10 | 0.1 |\n| 重度 | 5 | 0.05 |',
        time: '10:14'
      },
      {id: 3, role: 'user', content: '谢谢，我先用中度配置跑一轮试试。', time: '10:15'},
      {
        id: 4,
        role: 'assistant',
        content: '好的！中度配置是个不错的起点，祝实验顺利！\n\n跑完可以把训练曲线给我看，我帮你分析 loss/accuracy 趋势。',
        time: '10:15'
      }
    ]
  },
  {
    id: 3,
    title: 'Tiny-ImageNet 显存不足问题',
    preview: 'Tiny-ImageNet 训练需要约 5.5GB 显存，建议...',
    time: '昨天 16:44',
    messages: [
      {id: 1, role: 'user', content: 'Tiny-ImageNet 跑的时候显存不够，有什么解决方案？', time: '16:40'},
      {
        id: 2, role: 'assistant',
        content: 'Tiny-ImageNet（64×64）显存需求约是 CIFAR-100（32×32）的 2 倍。解决方案：\n\n**1. 减小 batch size**：64 → 32 → 16 逐步尝试\n\n**2. 降低 fraction**：`0.1` → `0.05`，减少并发客户端数\n\n**3. 混合精度训练（AMP）**：节省约 40~50% 显存，训练速度也会加快，推荐优先尝试\n\n**4. 梯度检查点**：以时间换空间，节省 30~50% 显存\n\n系统要求 Tiny-ImageNet 至少 **5.5GB 空闲显存**，可在仪表盘查看当前 GPU 状态。',
        time: '16:42'
      },
      {id: 3, role: 'user', content: '好的，我用混合精度训练试试，谢谢！', time: '16:44'},
      {
        id: 4,
        role: 'assistant',
        content: '混合精度是最推荐的方案，几乎无精度损失，显存和速度都有明显改善，好好试试！',
        time: '16:44'
      }
    ]
  },
  {
    id: 4,
    title: '联邦学习入门介绍',
    preview: '联邦学习是一种分布式机器学习范式，允许多个参与方...',
    time: '3天前',
    messages: [
      {id: 1, role: 'user', content: '能简单介绍一下联邦学习是什么吗？', time: '09:10'},
      {
        id: 2, role: 'assistant',
        content: '**联邦学习（Federated Learning）** 是 Google 于 2016 年提出的分布式机器学习范式。\n\n**核心思想**：数据不离开本地，模型到数据所在地训练，只上传**模型更新**（参数/梯度），保护隐私的同时协同训练全局模型。\n\n**标准流程**：服务器下发全局模型 → 客户端本地训练 → 上传更新 → 服务器聚合 → 重复直到收敛\n\n**主要挑战**：Non-IID 数据、通信效率、模型异构\n\n本平台（MHFL-VS）专注**模型异构**联邦学习，支持 FedAvg、FedProto、FedSSA、LG-FedAvg，欢迎探索！',
        time: '09:11'
      }
    ]
  },
  {
    id: 5,
    title: 'num_steps 和 epochs 的区别',
    preview: 'num_steps 是全局通信轮数，epochs 是每个客户端本地训练...',
    time: '5天前',
    messages: [
      {id: 1, role: 'user', content: 'num_steps 和 epochs 有什么区别？', time: '11:05'},
      {
        id: 2, role: 'assistant',
        content: '**`num_steps`（全局轮数）**：服务器与客户端的通信轮次，代表联邦迭代次数。建议 100~500，默认 200。\n\n**`epochs`（本地训练轮次）**：每个客户端每轮本地训练的 epoch 数。建议 1~10，过多会加剧 Non-IID 问题，默认 5。\n\n| 需求 | num_steps | epochs |\n|------|-----------|--------|\n| 快速验证 | 50 | 3 |\n| 标准训练 | 200 | 5 |\n| 充分收敛 | 500 | 5 |\n\n增加 `num_steps` 比增加 `epochs` 更稳定，前者增加全局协作，后者增加本地自适应。',
        time: '11:06'
      }
    ]
  }
])

// ===================== 状态 =====================
const activeConvId = ref<number | null>(1)
const inputText = ref('')
const isSending = ref(false)
const chatBodyRef = ref<HTMLElement | null>(null)
const searchKeyword = ref('')
const sidebarCollapsed = ref(false)
const streamingMsgId = ref<number | null>(null)

// 监听 inputText，编程式清空时（如发送后）也能收缩
watch(inputText, (val) => {
  if (!val) nextTick(() => resetInputHeight())
})

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const filteredConversations = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return conversations.value
  return conversations.value.filter(c =>
      c.title.toLowerCase().includes(kw) || c.preview.toLowerCase().includes(kw)
  )
})

const activeConv = computed(() =>
    conversations.value.find(c => c.id === activeConvId.value) ?? null
)

const selectConv = (id: number) => {
  if (isSending.value) return
  activeConvId.value = id
  nextTick(() => scrollToBottom())
}

const newChat = () => {
  const id = Date.now()
  conversations.value.unshift({id, title: '新对话', preview: '', time: '刚刚', messages: []})
  activeConvId.value = id
  inputText.value = ''
}

const scrollToBottom = () => {
  if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
}

// ===================== 流式输出 =====================
const streamText = (convId: number, msgId: number, fullText: string) => {
  const conv = conversations.value.find(c => c.id === convId)
  if (!conv) return
  const msg = conv.messages.find(m => m.id === msgId)
  if (!msg) return

  streamingMsgId.value = msgId
  let index = 0

  const tick = () => {
    if (index >= fullText.length) {
      msg.streaming = false
      streamingMsgId.value = null
      isSending.value = false
      conv.preview = fullText.slice(0, 40)
      nextTick(() => scrollToBottom())
      return
    }
    const step = Math.min(Math.floor(Math.random() * 3) + 1, fullText.length - index)
    msg.content += fullText.slice(index, index + step)
    index += step
    nextTick(() => scrollToBottom())
    setTimeout(tick, 18)
  }
  setTimeout(tick, 18)
}

// ===================== 发送消息 =====================
const sendMessage = () => {
  const text = inputText.value.trim()
  if (!text || isSending.value) return
  if (!activeConv.value) newChat()

  const conv = conversations.value.find(c => c.id === activeConvId.value)
  if (!conv) return

  conv.messages.push({id: ++msgIdCounter, role: 'user', content: text, time: nowTime()})
  if (conv.title === '新对话' && conv.messages.filter(m => m.role === 'user').length === 1)
    conv.title = text.length > 18 ? text.slice(0, 18) + '...' : text
  conv.preview = text

  inputText.value = ''
  nextTick(() => resetInputHeight())
  isSending.value = true
  nextTick(() => scrollToBottom())

  const convId = activeConvId.value!
  setTimeout(() => {
    const conv2 = conversations.value.find(c => c.id === convId)
    if (!conv2) return
    const fullReply = getAiReply(text)
    const aiMsgId = ++msgIdCounter
    conv2.messages.push({id: aiMsgId, role: 'assistant', content: '', time: nowTime(), streaming: true})
    nextTick(() => {
      scrollToBottom();
      streamText(convId, aiMsgId, fullReply)
    })
  }, 600)
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    sendMessage()
  }
}

// ===================== Markdown 简单渲染 =====================
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

// ===================== 消息操作 =====================
const copiedMsgId = ref<number | null>(null)
// 用 Record 存每条消息的反馈状态，方便响应式追踪
const msgFeedback = ref<Record<number, 'liked' | 'disliked' | null>>({})

const copyMessage = async (msgId: number, content: string) => {
  try {
    await navigator.clipboard.writeText(content)
  } catch {
    // Clipboard API 不可用时静默忽略
  }
  copiedMsgId.value = msgId
  setTimeout(() => {
    copiedMsgId.value = null
  }, 1500)
}

const toggleLike = (msgId: number) => {
  msgFeedback.value = {
    ...msgFeedback.value,
    [msgId]: msgFeedback.value[msgId] === 'liked' ? null : 'liked'
  }
}

const toggleDislike = (msgId: number) => {
  msgFeedback.value = {
    ...msgFeedback.value,
    [msgId]: msgFeedback.value[msgId] === 'disliked' ? null : 'disliked'
  }
}

// ===================== 快捷提问 =====================
const quickQuestions = [
  {icon: 'i-mdi-compare', text: '对比 FedAvg 和 FedProto 算法'},
  {icon: 'i-mdi-chart-line', text: '如何配置 Non-IID 参数？'},
  {icon: 'i-mdi-memory', text: 'Tiny-ImageNet 显存需求是多少？'},
  {icon: 'i-mdi-school-outline', text: '联邦学习入门概念介绍'},
]

const askQuickQuestion = (text: string) => {
  inputText.value = text
  nextTick(() => sendMessage())
}
</script>

<template>
  <div class="assistant-page flex h-full overflow-hidden">

    <!-- ======= 左侧历史会话栏 ======= -->
    <aside class="conv-sidebar flex flex-col shrink-0 overflow-hidden"
           :class="{ 'conv-sidebar--collapsed': sidebarCollapsed }">

      <!-- 展开状态 -->
      <template v-if="!sidebarCollapsed">
        <!-- 品牌头部 -->
        <div class="conv-sidebar-header flex items-center justify-between shrink-0 px-3 py-3.5">
          <div class="flex items-center gap-2">
            <span class="i-mdi-robot-outline text-xl text-indigo-500 shrink-0"></span>
            <span class="text-[15px] font-bold tracking-[0.02em] whitespace-nowrap"
                  style="color: var(--home-text-primary)">AI 助手</span>
          </div>
          <div class="flex gap-1">
            <button class="icon-btn" @click="newChat" :title="t('assistant.newChat')">
              <span class="i-mdi-square-edit-outline"></span>
            </button>
            <button class="icon-btn" @click="toggleSidebar" :title="t('assistant.collapseSidebar')">
              <span class="i-mdi-chevron-double-left"></span>
            </button>
          </div>
        </div>

        <!-- 搜索框 -->
        <div class="conv-search relative shrink-0 px-2.5 py-2">
          <span
              class="conv-search-icon i-mdi-magnify absolute left-5 top-1/2 -translate-y-1/2 text-[14px] pointer-events-none flex"
              style="color: var(--home-text-muted); line-height: 1;"></span>
          <input
              v-model="searchKeyword"
              class="conv-search-input w-full text-[13px] rounded-lg outline-none"
              :placeholder="t('assistant.searchPlaceholder')"
          />
        </div>

        <!-- 会话列表 -->
        <div class="flex-1 overflow-y-auto px-1.5 py-1">
          <div class="text-[11px] font-semibold uppercase tracking-[0.08em] px-2 pt-2 pb-1.5"
               style="color: var(--home-text-muted)">
            {{ t('assistant.recentChats') }}
          </div>
          <div
              v-for="conv in filteredConversations"
              :key="conv.id"
              class="conv-item flex items-start gap-2.5 p-2.5 rounded-[9px] cursor-pointer mb-0.5"
              :class="{ 'conv-item--active': activeConvId === conv.id }"
              @click="selectConv(conv.id)"
          >
            <div
                class="conv-item-icon flex shrink-0 items-center justify-center w-[30px] h-[30px] rounded-[7px] text-[15px] mt-px text-indigo-500">
              <span class="i-mdi-chat-outline"></span>
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-[13px] font-semibold truncate" style="color: var(--home-text-primary)">{{
                  conv.title
                }}
              </div>
              <div class="text-[12px] truncate mt-0.5" style="color: var(--home-text-muted)">{{ conv.preview }}</div>
              <div class="text-[11px] mt-0.5 opacity-65" style="color: var(--home-text-muted)">{{ conv.time }}</div>
            </div>
          </div>

          <div v-if="filteredConversations.length === 0" class="flex flex-col items-center gap-2 py-8 px-4 text-[13px]"
               style="color: var(--home-text-muted)">
            <span class="i-mdi-chat-sleep-outline text-[26px] opacity-45"></span>
            <span>{{ t('assistant.noConvFound') }}</span>
          </div>
        </div>

        <!-- 底部提示 -->
        <div class="conv-sidebar-footer flex items-center gap-1.5 shrink-0 px-3.5 py-2.5 text-[11px]"
             style="color: var(--home-text-muted); line-height: 1;">
          <span class="i-mdi-information-outline text-[13px] shrink-0 flex items-center" style="line-height: 1;"></span>
          <span class="flex items-center" style="line-height: 1;">{{ t('assistant.footerTip') }}</span>
        </div>
      </template>

      <!-- 收起状态：只显示展开 + 新建 -->
      <template v-else>
        <div class="flex flex-col items-center gap-1 py-3">
          <button class="icon-btn icon-btn--collapsed" @click="toggleSidebar" :title="t('assistant.expandSidebar')">
            <span class="i-mdi-chevron-double-right"></span>
          </button>
          <button class="icon-btn icon-btn--collapsed" @click="newChat" :title="t('assistant.newChat')">
            <span class="i-mdi-square-edit-outline"></span>
          </button>
        </div>
      </template>

    </aside>

    <!-- ======= 右侧主体 ======= -->
    <main class="flex flex-col flex-1 overflow-hidden min-w-0">

      <!-- 顶部标题栏 -->
      <div class="chat-topbar flex items-center justify-between shrink-0 px-6 py-3.5">
        <div class="flex items-center gap-3">
          <span class="i-mdi-robot-outline text-2xl text-indigo-500 shrink-0"></span>
          <div>
            <div class="text-[15px] font-bold leading-[1.3]" style="color: var(--home-text-primary)">
              {{ activeConv ? activeConv.title : t('pages.assistant.title') }}
            </div>
            <div class="text-[12px] mt-px" style="color: var(--home-text-muted)">
              {{
                activeConv ? t('assistant.msgCount', {count: activeConv.messages.length}) : t('pages.assistant.desc')
              }}
            </div>
          </div>
        </div>
        <div
            class="chat-topbar-badge flex items-center gap-1.5 text-xs font-semibold tracking-[0.04em] px-2.5 py-1 rounded-full">
          <span class="chat-topbar-badge-dot w-1.5 h-1.5 rounded-full"></span>
          MHFL-VS
        </div>
      </div>

      <!-- 欢迎屏 -->
      <div v-if="!activeConv || activeConv.messages.length === 0"
           class="flex-1 flex flex-col items-center justify-center gap-3 px-8 py-10">
        <div class="welcome-avatar flex items-center justify-center w-[68px] h-[68px] rounded-[20px] mb-1.5">
          <span class="i-mdi-robot-excited-outline text-[32px] text-indigo-500"></span>
        </div>
        <h2 class="text-[21px] font-bold text-center" style="color: var(--home-text-primary)">
          {{ t('assistant.welcomeTitle') }}</h2>
        <p class="text-sm text-center max-w-[440px] leading-[1.7]" style="color: var(--home-text-muted)">
          {{ t('assistant.welcomeSub') }}</p>
        <div class="grid grid-cols-2 gap-2.5 mt-5 max-w-[540px] w-full">
          <button
              v-for="q in quickQuestions"
              :key="q.text"
              class="quick-card flex items-center gap-2.5 px-4 py-3.5 rounded-[11px] text-[13px] text-left cursor-pointer"
              @click="askQuickQuestion(q.text)"
          >
            <span :class="[q.icon]" class="text-[19px] text-indigo-500 opacity-80 shrink-0"></span>
            <span class="leading-[1.4]">{{ q.text }}</span>
          </button>
        </div>
      </div>

      <!-- 聊天消息区 -->
      <div v-else ref="chatBodyRef" class="flex-1 overflow-y-auto flex flex-col gap-[18px] px-6 py-5">
        <template v-for="msg in activeConv.messages" :key="msg.id">

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
                  正在输出
                </span>
              </div>
              <!-- 内容 -->
              <div v-if="msg.content" class="msg-content" v-html="formatContent(msg.content)"></div>
              <span v-else class="stream-cursor-only"></span>
              <span v-if="streamingMsgId === msg.id" class="stream-cursor"></span>
              <!-- 操作按钮 -->
              <div v-if="!msg.streaming" class="msg-actions flex gap-1 mt-2">
                <!-- 复制 -->
                <button
                    class="msg-action-btn flex items-center justify-center w-[25px] h-[25px] rounded-[6px] text-[13px] cursor-pointer"
                    :class="{ 'msg-action-btn--copied': copiedMsgId === msg.id }"
                    :title="t('assistant.copyMsg')"
                    @click="copyMessage(msg.id, msg.content)"
                >
                  <span :class="copiedMsgId === msg.id ? 'i-mdi-check' : 'i-mdi-content-copy'"></span>
                </button>
                <!-- 点赞 -->
                <button
                    class="msg-action-btn flex items-center justify-center w-[25px] h-[25px] rounded-[6px] text-[13px] cursor-pointer"
                    :class="{ 'msg-action-btn--liked': msgFeedback[msg.id] === 'liked' }"
                    :title="t('assistant.likeMsg')"
                    @click="toggleLike(msg.id)"
                >
                  <span :class="msgFeedback[msg.id] === 'liked' ? 'i-mdi-thumb-up' : 'i-mdi-thumb-up-outline'"></span>
                </button>
                <!-- 踩 -->
                <button
                    class="msg-action-btn flex items-center justify-center w-[25px] h-[25px] rounded-[6px] text-[13px] cursor-pointer"
                    :class="{ 'msg-action-btn--disliked': msgFeedback[msg.id] === 'disliked' }"
                    :title="t('assistant.dislikeMsg')"
                    @click="toggleDislike(msg.id)"
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
          <div class="msg-bubble msg-bubble--ai rounded-[14px] rounded-bl-[4px] flex items-center gap-1.5 px-4 py-3.5">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
          </div>
        </div>
      </div>

      <!-- 底部输入区 -->
      <div class="chat-input-area shrink-0 px-6 pt-3.5 pb-4.5">
        <div class="chat-input-wrap flex items-end gap-2 px-4 py-2.5 rounded-[14px]">
          <textarea
              ref="textareaRef"
              v-model="inputText"
              class="chat-input flex-1 text-sm leading-[1.6] resize-none overflow-y-hidden font-inherit p-0 border-none outline-none bg-transparent"
              :placeholder="t('assistant.inputPlaceholder')"
              :disabled="isSending"
              @keydown="handleKeydown"
              @input="autoResize"
          ></textarea>
          <button
              class="input-send-btn flex items-center justify-center w-[34px] h-[34px] rounded-[9px] text-[16px] shrink-0 transition-all"
              :class="{ 'input-send-btn--active': inputText.trim() && !isSending }"
              :disabled="!inputText.trim() || isSending"
              @click="sendMessage"
              :title="t('assistant.send')"
          >
            <span v-if="!isSending" class="i-mdi-send"></span>
            <span v-else class="i-mdi-loading spin"></span>
          </button>
        </div>
        <p class="text-[11px] text-center mt-1.5 opacity-75" style="color: var(--home-text-muted)">
          {{ t('assistant.inputHint') }}</p>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* ===== 侧边栏宽度过渡（不能用原子类）===== */
.conv-sidebar {
  width: 272px;
  background: var(--home-card-bg);
  border-right: 1px solid var(--home-border);
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.conv-sidebar--collapsed {
  width: 52px;
}

/* ===== 侧边栏分区边框（主题变量）===== */
.conv-sidebar-header {
  border-bottom: 1px solid var(--home-border);
}

.conv-sidebar-footer {
  border-top: 1px solid var(--home-border);
}

/* ===== 搜索输入框（伪类 + 主题）===== */
.conv-search-input {
  padding: 6px 10px 6px 32px;
  border: 1px solid var(--home-border);
  background: var(--home-bg);
  color: var(--home-text-primary);
  transition: border-color 0.2s;
}

.conv-search-input::placeholder {
  color: var(--home-text-muted);
}

.conv-search-input:focus {
  border-color: rgba(99, 102, 241, 0.5);
}

/* ===== 通用图标按钮（hover + 主题）===== */
.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 1px solid var(--home-border);
  background: transparent;
  border-radius: 8px;
  color: var(--home-text-muted);
  font-size: 16px;
  cursor: pointer;
  transition: all 0.18s;
  flex-shrink: 0;
}

.icon-btn:hover {
  border-color: rgba(99, 102, 241, 0.4);
  color: #6366f1;
  background: rgba(99, 102, 241, 0.06);
}

.icon-btn--collapsed {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  font-size: 17px;
}

/* ===== 会话条目（hover + active 主题色）===== */
.conv-item {
  border: 1px solid transparent;
  transition: background 0.16s;
}

.conv-item:hover {
  background: var(--home-hover-bg);
}

.conv-item--active {
  background: rgba(99, 102, 241, 0.08);
  border-color: rgba(99, 102, 241, 0.18);
}

.conv-item-icon {
  background: rgba(99, 102, 241, 0.08);
}

.conv-item--active .conv-item-icon {
  background: rgba(99, 102, 241, 0.18);
}

/* ===== 顶部栏（主题背景 + 边框）===== */
.chat-topbar {
  background: var(--home-bg);
  border-bottom: 1px solid var(--home-border);
}

/* ===== 顶部徽章（主题色）===== */
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

/* ===== 欢迎头像（渐变背景）===== */
.welcome-avatar {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.12), rgba(139, 92, 246, 0.12));
  border: 1px solid rgba(99, 102, 241, 0.22);
}

/* ===== 快捷问题卡片（hover）===== */
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

/* ===== 消息头像 ===== */
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

/* ===== 消息气泡 ===== */
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

/* ===== 流式状态徽章 ===== */
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
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(0.7);
  }
}

/* ===== 流式光标 ===== */
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
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* ===== 消息内容 :deep ===== */
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
  background: rgba(99, 102, 241, 0.1);
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
  background: rgba(99, 102, 241, 0.04);
}

/* ===== 消息操作按钮（hover）===== */
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
  border-color: rgba(99, 102, 241, 0.4);
  color: #6366f1;
  background: rgba(99, 102, 241, 0.06);
}

/* 复制成功：绿色 */
.msg-action-btn--copied {
  border-color: rgba(16, 185, 129, 0.4) !important;
  color: #10b981 !important;
  background: rgba(16, 185, 129, 0.08) !important;
}

/* 点赞激活：蓝紫色 */
.msg-action-btn--liked {
  border-color: rgba(99, 102, 241, 0.45) !important;
  color: #6366f1 !important;
  background: rgba(99, 102, 241, 0.1) !important;
}

/* 踩激活：琥珀橙色 */
.msg-action-btn--disliked {
  border-color: rgba(245, 158, 11, 0.45) !important;
  color: #f59e0b !important;
  background: rgba(245, 158, 11, 0.08) !important;
}

/* ===== 打字动画 ===== */
.typing-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #6366f1;
  opacity: 0.5;
  animation: typingBounce 1.2s infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typingBounce {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.35;
  }
  30% {
    transform: translateY(-6px);
    opacity: 1;
  }
}

/* ===== 输入区（主题 + 伪类）===== */
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

/* ===== 发送按钮 ===== */
.input-send-btn {
  border: none;
  background: rgba(99, 102, 241, 0.15);
  color: rgba(99, 102, 241, 0.5);
  cursor: not-allowed;
}

.input-send-btn--active {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(99, 102, 241, 0.32);
}

.input-send-btn--active:hover {
  transform: scale(1.06);
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.42);
}

/* ===== 旋转动画 ===== */
.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
