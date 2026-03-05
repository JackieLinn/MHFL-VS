<script setup lang="ts">
import {ref, computed, nextTick} from 'vue'
import {useI18n} from 'vue-i18n'

const {t} = useI18n()

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
      {id: 1, role: 'user', content: '我想做一个 Non-IID 实验，classes_per_node 和 low_prob 应该怎么配置？', time: '10:13'},
      {
        id: 2, role: 'assistant',
        content: '**`classes_per_node`（每节点类别数）**\n\n- `100`：近似 IID\n- `10`：中度 Non-IID（推荐入门）\n- `5`：高度 Non-IID\n- `2`：极端 Non-IID\n\n**`low_prob`（稀少类别采样概率）**\n\n- `0.0`：只有主要类别数据\n- `0.1`：推荐值，保留少量稀少类别\n- `0.5`：接近均匀\n\n| 场景 | classes_per_node | low_prob |\n|------|-----------------|----------|\n| 轻度 | 20 | 0.3 |\n| 中度 | 10 | 0.1 |\n| 重度 | 5 | 0.05 |',
        time: '10:14'
      },
      {id: 3, role: 'user', content: '谢谢，我先用中度配置跑一轮试试。', time: '10:15'},
      {id: 4, role: 'assistant', content: '好的！中度配置是个不错的起点，祝实验顺利！\n\n跑完可以把训练曲线给我看，我帮你分析 loss/accuracy 趋势。', time: '10:15'}
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
      {id: 4, role: 'assistant', content: '混合精度是最推荐的方案，几乎无精度损失，显存和速度都有明显改善，好好试试！', time: '16:44'}
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
// 正在流式输出的消息 id
const streamingMsgId = ref<number | null>(null)

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
  conversations.value.unshift({
    id,
    title: '新对话',
    preview: '',
    time: '刚刚',
    messages: []
  })
  activeConvId.value = id
  inputText.value = ''
}

const scrollToBottom = () => {
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
}

// ===================== 流式输出 =====================
const streamText = (convId: number, msgId: number, fullText: string) => {
  const conv = conversations.value.find(c => c.id === convId)
  if (!conv) return

  // 找到占位消息
  const msg = conv.messages.find(m => m.id === msgId)
  if (!msg) return

  streamingMsgId.value = msgId
  let index = 0
  // 每次追加 1~3 个字符，模拟不均匀的流式速度
  const INTERVAL = 18

  const tick = () => {
    if (index >= fullText.length) {
      msg.streaming = false
      streamingMsgId.value = null
      isSending.value = false
      // 更新侧边栏预览
      conv.preview = fullText.slice(0, 40)
      nextTick(() => scrollToBottom())
      return
    }
    // 一次追加 1~3 个字符（让速度看起来更自然）
    const step = Math.min(Math.floor(Math.random() * 3) + 1, fullText.length - index)
    msg.content += fullText.slice(index, index + step)
    index += step
    nextTick(() => scrollToBottom())
    setTimeout(tick, INTERVAL)
  }

  setTimeout(tick, INTERVAL)
}

// ===================== 发送消息 =====================
const sendMessage = () => {
  const text = inputText.value.trim()
  if (!text || isSending.value) return

  // 若没有选中会话或选中的是空会话，新建一条
  if (!activeConv.value) {
    newChat()
  }

  const conv = conversations.value.find(c => c.id === activeConvId.value)
  if (!conv) return

  // 添加用户消息
  conv.messages.push({id: ++msgIdCounter, role: 'user', content: text, time: nowTime()})

  // 更新会话标题（仅首条消息时）
  if (conv.title === '新对话' && conv.messages.filter(m => m.role === 'user').length === 1) {
    conv.title = text.length > 18 ? text.slice(0, 18) + '...' : text
  }
  conv.preview = text

  inputText.value = ''
  isSending.value = true
  nextTick(() => scrollToBottom())

  // 先延迟一点，模拟"思考"，然后开始流式输出
  const convId = activeConvId.value!
  setTimeout(() => {
    const conv2 = conversations.value.find(c => c.id === convId)
    if (!conv2) return

    const fullReply = getAiReply(text)
    const aiMsgId = ++msgIdCounter

    // 添加空占位消息，streaming=true
    conv2.messages.push({
      id: aiMsgId,
      role: 'assistant',
      content: '',
      time: nowTime(),
      streaming: true
    })

    nextTick(() => {
      scrollToBottom()
      streamText(convId, aiMsgId, fullReply)
    })
  }, 600)
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
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
  <div class="assistant-page">
    <!-- ======= 左侧历史会话栏 ======= -->
    <aside class="conv-sidebar" :class="{ 'conv-sidebar--collapsed': sidebarCollapsed }">

      <!-- 展开状态 -->
      <template v-if="!sidebarCollapsed">
        <div class="conv-sidebar-header">
          <div class="conv-brand">
            <span class="i-mdi-robot-outline conv-brand-icon"></span>
            <span class="conv-brand-text">AI 助手</span>
          </div>
          <div class="conv-header-btns">
            <button class="icon-btn" @click="newChat" :title="t('assistant.newChat')">
              <span class="i-mdi-square-edit-outline"></span>
            </button>
            <button class="icon-btn" @click="toggleSidebar" :title="t('assistant.collapseSidebar')">
              <span class="i-mdi-chevron-double-left"></span>
            </button>
          </div>
        </div>

        <div class="conv-search">
          <span class="i-mdi-magnify conv-search-icon"></span>
          <input
            v-model="searchKeyword"
            class="conv-search-input"
            :placeholder="t('assistant.searchPlaceholder')"
          />
        </div>

        <div class="conv-list">
          <div class="conv-list-label">{{ t('assistant.recentChats') }}</div>
          <div
            v-for="conv in filteredConversations"
            :key="conv.id"
            class="conv-item"
            :class="{ 'conv-item--active': activeConvId === conv.id }"
            @click="selectConv(conv.id)"
          >
            <div class="conv-item-icon">
              <span class="i-mdi-chat-outline"></span>
            </div>
            <div class="conv-item-body">
              <div class="conv-item-title">{{ conv.title }}</div>
              <div class="conv-item-preview">{{ conv.preview }}</div>
              <div class="conv-item-time">{{ conv.time }}</div>
            </div>
          </div>
          <div v-if="filteredConversations.length === 0" class="conv-empty">
            <span class="i-mdi-chat-sleep-outline conv-empty-icon"></span>
            <span>{{ t('assistant.noConvFound') }}</span>
          </div>
        </div>

        <div class="conv-sidebar-footer">
          <span class="i-mdi-information-outline"></span>
          <span>{{ t('assistant.footerTip') }}</span>
        </div>
      </template>

      <!-- 收起状态：只显示图标列 -->
      <template v-else>
        <div class="collapsed-btns">
          <button class="icon-btn icon-btn--collapsed" @click="toggleSidebar" :title="t('assistant.expandSidebar')">
            <span class="i-mdi-chevron-double-right"></span>
          </button>
          <button class="icon-btn icon-btn--collapsed" @click="newChat" :title="t('assistant.newChat')">
            <span class="i-mdi-square-edit-outline"></span>
          </button>
          <div class="collapsed-divider"></div>
          <button
            v-for="conv in conversations.slice(0, 6)"
            :key="conv.id"
            class="icon-btn icon-btn--collapsed"
            :class="{ 'icon-btn--active': activeConvId === conv.id }"
            :title="conv.title"
            @click="selectConv(conv.id)"
          >
            <span class="i-mdi-chat-outline"></span>
          </button>
        </div>
      </template>

    </aside>

    <!-- ======= 右侧主体 ======= -->
    <main class="chat-main">
      <!-- 顶部简洁标题栏 -->
      <div class="chat-topbar">
        <div class="chat-topbar-left">
          <span class="i-mdi-robot-outline chat-topbar-icon"></span>
          <div>
            <div class="chat-topbar-title">
              {{ activeConv ? activeConv.title : t('pages.assistant.title') }}
            </div>
            <div class="chat-topbar-sub">
              {{ activeConv
                ? t('assistant.msgCount', {count: activeConv.messages.length})
                : t('pages.assistant.desc') }}
            </div>
          </div>
        </div>
        <div class="chat-topbar-badge">
          <span class="chat-topbar-badge-dot"></span>
          MHFL-VS
        </div>
      </div>

      <!-- 欢迎屏 -->
      <div v-if="!activeConv || activeConv.messages.length === 0" class="chat-welcome">
        <div class="welcome-avatar">
          <span class="i-mdi-robot-excited-outline welcome-avatar-icon"></span>
        </div>
        <h2 class="welcome-title">{{ t('assistant.welcomeTitle') }}</h2>
        <p class="welcome-sub">{{ t('assistant.welcomeSub') }}</p>
        <div class="quick-grid">
          <button
            v-for="q in quickQuestions"
            :key="q.text"
            class="quick-card"
            @click="askQuickQuestion(q.text)"
          >
            <span :class="[q.icon, 'quick-card-icon']"></span>
            <span class="quick-card-text">{{ q.text }}</span>
          </button>
        </div>
      </div>

      <!-- 聊天消息区 -->
      <div v-else ref="chatBodyRef" class="chat-body">
        <template v-for="msg in activeConv.messages" :key="msg.id">

          <!-- 用户消息 -->
          <div v-if="msg.role === 'user'" class="msg-row msg-row--user">
            <div class="msg-bubble msg-bubble--user">
              <p>{{ msg.content }}</p>
              <span class="msg-time-user">{{ msg.time }}</span>
            </div>
            <div class="msg-avatar msg-avatar--user">
              <span class="i-mdi-account"></span>
            </div>
          </div>

          <!-- AI 消息 -->
          <div v-else class="msg-row msg-row--ai">
            <div class="msg-avatar msg-avatar--ai">
              <span class="i-mdi-robot-outline"></span>
            </div>
            <div class="msg-bubble msg-bubble--ai">
              <div class="msg-ai-header">
                <span class="msg-ai-name">MHFL 助手</span>
                <span class="msg-time">{{ msg.time }}</span>
                <!-- 流式输出中的状态提示 -->
                <span v-if="streamingMsgId === msg.id" class="streaming-badge">
                  <span class="streaming-dot"></span>
                  正在输出
                </span>
              </div>

              <!-- 有内容就渲染，还没内容就显示光标占位 -->
              <div v-if="msg.content" class="msg-content" v-html="formatContent(msg.content)"></div>
              <span v-else class="stream-cursor-only"></span>

              <!-- 流式光标（输出中尾部追加） -->
              <span v-if="streamingMsgId === msg.id" class="stream-cursor"></span>

              <!-- 操作按钮（输出完成后才显示） -->
              <div v-if="!msg.streaming" class="msg-actions">
                <button class="msg-action-btn" :title="t('assistant.copyMsg')">
                  <span class="i-mdi-content-copy"></span>
                </button>
                <button class="msg-action-btn" :title="t('assistant.likeMsg')">
                  <span class="i-mdi-thumb-up-outline"></span>
                </button>
                <button class="msg-action-btn" :title="t('assistant.dislikeMsg')">
                  <span class="i-mdi-thumb-down-outline"></span>
                </button>
              </div>
            </div>
          </div>

        </template>

        <!-- 思考中动画（600ms 延迟期） -->
        <div v-if="isSending && streamingMsgId === null" class="msg-row msg-row--ai">
          <div class="msg-avatar msg-avatar--ai">
            <span class="i-mdi-robot-outline"></span>
          </div>
          <div class="msg-bubble msg-bubble--ai msg-bubble--typing">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
          </div>
        </div>
      </div>

      <!-- 底部输入区 -->
      <div class="chat-input-area">
        <div class="chat-input-wrap">
          <textarea
            v-model="inputText"
            class="chat-input"
            :placeholder="t('assistant.inputPlaceholder')"
            rows="1"
            :disabled="isSending"
            @keydown="handleKeydown"
          ></textarea>
          <button
            class="input-send-btn"
            :class="{ 'input-send-btn--active': inputText.trim() && !isSending }"
            :disabled="!inputText.trim() || isSending"
            @click="sendMessage"
            :title="t('assistant.send')"
          >
            <span v-if="!isSending" class="i-mdi-send"></span>
            <span v-else class="i-mdi-loading spin"></span>
          </button>
        </div>
        <p class="chat-input-hint">{{ t('assistant.inputHint') }}</p>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* ===================== 布局 ===================== */
.assistant-page {
  display: flex;
  height: 100%;
  overflow: hidden;
  background: var(--home-bg);
}

/* ===================== 左侧历史侧边栏 ===================== */
.conv-sidebar {
  width: 272px;
  min-width: 272px;
  display: flex;
  flex-direction: column;
  background: var(--home-card-bg);
  border-right: 1px solid var(--home-border);
  overflow: hidden;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
              min-width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.conv-sidebar--collapsed {
  width: 52px;
  min-width: 52px;
}

/* 收起状态按钮列 */
.collapsed-btns {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 0;
}

.collapsed-divider {
  width: 28px;
  height: 1px;
  background: var(--home-border);
  margin: 6px 0;
}

.conv-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px 12px;
  border-bottom: 1px solid var(--home-border);
  flex-shrink: 0;
}

.conv-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.conv-brand-icon {
  font-size: 20px;
  color: #6366f1;
  flex-shrink: 0;
}

.conv-brand-text {
  font-size: 15px;
  font-weight: 700;
  color: var(--home-text-primary);
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.conv-header-btns {
  display: flex;
  gap: 4px;
}

/* 通用图标按钮 */
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

.icon-btn--active {
  background: rgba(99, 102, 241, 0.12);
  border-color: rgba(99, 102, 241, 0.3);
  color: #6366f1;
}

/* 搜索框 */
.conv-search {
  position: relative;
  padding: 10px 10px 6px;
  flex-shrink: 0;
}

.conv-search-icon {
  position: absolute;
  left: 20px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  color: var(--home-text-muted);
  pointer-events: none;
}

.conv-search-input {
  width: 100%;
  padding: 6px 10px 6px 32px;
  border: 1px solid var(--home-border);
  border-radius: 8px;
  background: var(--home-bg);
  color: var(--home-text-primary);
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}

.conv-search-input::placeholder {color: var(--home-text-muted);}
.conv-search-input:focus {border-color: rgba(99, 102, 241, 0.5);}

/* 会话列表 */
.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 6px;
}

.conv-list-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--home-text-muted);
  padding: 8px 8px 5px;
}

.conv-item {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  padding: 9px;
  border-radius: 9px;
  cursor: pointer;
  transition: background 0.16s;
  margin-bottom: 2px;
  border: 1px solid transparent;
}

.conv-item:hover {background: var(--home-hover-bg);}

.conv-item--active {
  background: rgba(99, 102, 241, 0.08);
  border-color: rgba(99, 102, 241, 0.18);
}

.conv-item-icon {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  background: rgba(99, 102, 241, 0.08);
  color: #6366f1;
  font-size: 15px;
  margin-top: 1px;
}

.conv-item--active .conv-item-icon {background: rgba(99, 102, 241, 0.18);}

.conv-item-body {flex: 1; min-width: 0;}

.conv-item-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--home-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-item-preview {
  font-size: 12px;
  color: var(--home-text-muted);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-item-time {
  font-size: 11px;
  color: var(--home-text-muted);
  margin-top: 3px;
  opacity: 0.65;
}

.conv-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px 16px;
  color: var(--home-text-muted);
  font-size: 13px;
}

.conv-empty-icon {font-size: 26px; opacity: 0.45;}

.conv-sidebar-footer {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 11px 14px;
  border-top: 1px solid var(--home-border);
  font-size: 11px;
  color: var(--home-text-muted);
  flex-shrink: 0;
}

/* ===================== 右侧主体 ===================== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* 顶部标题栏 */
.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  border-bottom: 1px solid var(--home-border);
  background: var(--home-bg);
  flex-shrink: 0;
}

.chat-topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-topbar-icon {
  font-size: 24px;
  color: #6366f1;
  flex-shrink: 0;
}

.chat-topbar-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--home-text-primary);
  line-height: 1.3;
}

.chat-topbar-sub {
  font-size: 12px;
  color: var(--home-text-muted);
  margin-top: 1px;
}

.chat-topbar-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.18);
  padding: 4px 10px;
  border-radius: 20px;
  letter-spacing: 0.04em;
}

.chat-topbar-badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #6366f1;
  animation: badgePulse 2s ease-in-out infinite;
}

@keyframes badgePulse {
  0%, 100% {opacity: 1;}
  50% {opacity: 0.35;}
}

/* ===================== 欢迎屏 ===================== */
.chat-welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 32px;
  gap: 12px;
}

.welcome-avatar {
  width: 68px;
  height: 68px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.12), rgba(139, 92, 246, 0.12));
  border: 1px solid rgba(99, 102, 241, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
}

.welcome-avatar-icon {font-size: 32px; color: #6366f1;}

.welcome-title {
  font-size: 21px;
  font-weight: 700;
  color: var(--home-text-primary);
  text-align: center;
}

.welcome-sub {
  font-size: 14px;
  color: var(--home-text-muted);
  text-align: center;
  max-width: 440px;
  line-height: 1.7;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-top: 20px;
  max-width: 540px;
  width: 100%;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 15px;
  border: 1px solid var(--home-card-border);
  border-radius: 11px;
  background: var(--home-card-bg);
  color: var(--home-text-secondary);
  font-size: 13px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-card:hover {
  border-color: rgba(99, 102, 241, 0.35);
  background: rgba(99, 102, 241, 0.05);
  color: #6366f1;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.1);
}

.quick-card-icon {font-size: 19px; flex-shrink: 0; color: #6366f1; opacity: 0.8;}
.quick-card-text {line-height: 1.4;}

/* ===================== 消息区 ===================== */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.msg-row--user {flex-direction: row-reverse;}

/* 头像 */
.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  flex-shrink: 0;
  margin-top: 2px;
}

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
.msg-bubble {
  max-width: 66%;
  border-radius: 14px;
  padding: 11px 15px;
  font-size: 14px;
  line-height: 1.75;
  position: relative;
}

.msg-bubble--user {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 2px 10px rgba(99, 102, 241, 0.28);
}

.msg-bubble--ai {
  background: var(--home-card-bg);
  border: 1px solid var(--home-card-border);
  color: var(--home-text-primary);
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 6px var(--home-card-shadow);
}

/* 用户消息时间 */
.msg-time-user {
  display: block;
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.65;
  text-align: right;
}

/* AI 消息头部 */
.msg-ai-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.msg-ai-name {font-size: 12px; font-weight: 700; color: #6366f1;}
.msg-time {font-size: 11px; color: var(--home-text-muted);}

/* 流式输出状态徽章 */
.streaming-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #10b981;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.2);
  padding: 1px 7px;
  border-radius: 10px;
  font-weight: 500;
}

.streaming-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #10b981;
  animation: streamDotPulse 1s ease-in-out infinite;
}

@keyframes streamDotPulse {
  0%, 100% {opacity: 1; transform: scale(1);}
  50% {opacity: 0.4; transform: scale(0.7);}
}

/* 流式光标 */
.stream-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: #6366f1;
  margin-left: 1px;
  vertical-align: text-bottom;
  border-radius: 1px;
  animation: cursorBlink 0.7s step-end infinite;
}

.stream-cursor-only {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: #6366f1;
  vertical-align: text-bottom;
  border-radius: 1px;
  animation: cursorBlink 0.7s step-end infinite;
}

@keyframes cursorBlink {
  0%, 100% {opacity: 1;}
  50% {opacity: 0;}
}

/* 消息内容 */
.msg-content :deep(p) {margin-bottom: 8px;}
.msg-content :deep(p:last-child) {margin-bottom: 0;}
.msg-content :deep(strong) {color: var(--home-text-primary); font-weight: 700;}

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

.msg-content :deep(tr) {border-bottom: 1px solid var(--home-border);}

.msg-content :deep(td) {
  padding: 5px 9px;
  color: var(--home-text-secondary);
}

.msg-content :deep(tr:first-child td) {
  font-weight: 700;
  color: var(--home-text-primary);
  background: rgba(99, 102, 241, 0.04);
}

/* 消息操作按钮 */
.msg-actions {
  display: flex;
  gap: 4px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.msg-bubble:hover .msg-actions {opacity: 1;}

.msg-action-btn {
  width: 25px;
  height: 25px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--home-border);
  border-radius: 6px;
  background: transparent;
  color: var(--home-text-muted);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.msg-action-btn:hover {
  border-color: rgba(99, 102, 241, 0.4);
  color: #6366f1;
  background: rgba(99, 102, 241, 0.06);
}

/* 思考中打字动画 */
.msg-bubble--typing {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 14px 16px;
}

.typing-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #6366f1;
  opacity: 0.5;
  animation: typingBounce 1.2s infinite;
}

.typing-dot:nth-child(2) {animation-delay: 0.2s;}
.typing-dot:nth-child(3) {animation-delay: 0.4s;}

@keyframes typingBounce {
  0%, 60%, 100% {transform: translateY(0); opacity: 0.35;}
  30% {transform: translateY(-6px); opacity: 1;}
}

/* ===================== 底部输入区 ===================== */
.chat-input-area {
  padding: 14px 24px 18px;
  border-top: 1px solid var(--home-border);
  background: var(--home-bg);
  flex-shrink: 0;
}

.chat-input-wrap {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 9px 10px 9px 15px;
  border: 1px solid var(--home-border);
  border-radius: 14px;
  background: var(--home-card-bg);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.chat-input-wrap:focus-within {
  border-color: rgba(99, 102, 241, 0.45);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.07);
}

.chat-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: var(--home-text-primary);
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  max-height: 130px;
  overflow-y: auto;
  font-family: inherit;
}

.chat-input::placeholder {color: var(--home-text-muted);}

.input-send-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 9px;
  background: rgba(99, 102, 241, 0.15);
  color: rgba(99, 102, 241, 0.5);
  font-size: 16px;
  cursor: not-allowed;
  transition: all 0.18s;
  flex-shrink: 0;
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

.chat-input-hint {
  margin-top: 7px;
  font-size: 11px;
  color: var(--home-text-muted);
  text-align: center;
  opacity: 0.75;
}

/* 旋转动画 */
.spin {animation: spin 1s linear infinite;}

@keyframes spin {
  from {transform: rotate(0deg);}
  to {transform: rotate(360deg);}
}
</style>
