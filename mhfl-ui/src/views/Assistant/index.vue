<script setup lang="ts">
import {ref, computed, nextTick} from 'vue'
import {useI18n} from 'vue-i18n'
import {ElMessageBox} from 'element-plus'
import AssistantSidebar from './components/AssistantSidebar.vue'
import AssistantTopbar from './components/AssistantTopbar.vue'
import AssistantWelcome from './components/AssistantWelcome.vue'
import AssistantMessageList from './components/AssistantMessageList.vue'
import AssistantInput from './components/AssistantInput.vue'

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

type MsgFeedback = Record<number, 'liked' | 'disliked' | null>

const {t} = useI18n()

// ===== 工具 =====
const nowTime = () => {
  const d = new Date()
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
let msgIdCounter = 1000

// ===== AI 回复池（模拟）=====
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

// ===== 历史会话数据 =====
const conversations = ref<Conversation[]>([
  {
    id: 1, title: 'FedAvg 与 FedProto 对比',
    preview: 'FedAvg 通过对所有客户端模型参数求加权平均来聚合...', time: '今天 14:32',
    messages: [
      {id: 1, role: 'user', content: '请帮我比较一下 FedAvg 和 FedProto 算法的区别和适用场景。', time: '14:30'},
      {
        id: 2,
        role: 'assistant',
        time: '14:31',
        content: `**FedAvg（联邦平均）**\n\nFedAvg 是最经典的联邦学习算法，核心思路是：服务器将全局模型分发给客户端，客户端本地训练若干 epoch 后上传**模型参数**，服务器加权平均后得到新的全局模型。\n\n适用场景：\n- 客户端模型结构**相同**（同构场景）\n- 数据分布较均匀（IID 或轻度 Non-IID）\n\n---\n\n**FedProto（联邦原型）**\n\nFedProto 专为**模型异构**场景设计，客户端上传各类别的特征原型（Prototype），服务器聚合原型再分发，客户端以原型为正则化目标。\n\n适用场景：\n- 客户端模型结构**不同**（异构场景）\n- 数据 Non-IID 较为严重\n\n---\n\n| 维度 | FedAvg | FedProto |\n|------|--------|----------|\n| 模型同构性 | 要求同构 | 支持异构 |\n| 通信内容 | 模型参数 | 类别原型 |\n| Non-IID 鲁棒性 | 一般 | 较强 |`
      },
      {id: 3, role: 'user', content: '在 CIFAR-100 + 高度 Non-IID 下，推荐用哪个？', time: '14:31'},
      {
        id: 4,
        role: 'assistant',
        time: '14:32',
        content: '在 CIFAR-100 + 高度 Non-IID 的场景下，推荐使用 **FedProto**。\n\n原因：CIFAR-100 有 100 个类别，Non-IID 时每个节点只见到少数类别，FedAvg 的全局平均会导致模型遗忘局部分布。FedProto 的原型对齐机制能在特征空间建立共识，`classes_per_node` 较小时通常比 FedAvg 高出 5~15 个百分点。'
      }
    ]
  },
  {
    id: 2, title: 'Non-IID 参数配置建议',
    preview: '推荐 classes_per_node 设置为 5~10，low_prob 建议...', time: '今天 10:15',
    messages: [
      {
        id: 1,
        role: 'user',
        content: '我想做一个 Non-IID 实验，classes_per_node 和 low_prob 应该怎么配置？',
        time: '10:13'
      },
      {
        id: 2,
        role: 'assistant',
        time: '10:14',
        content: '**`classes_per_node`（每节点类别数）**\n\n- `100`：近似 IID\n- `10`：中度 Non-IID（推荐入门）\n- `5`：高度 Non-IID\n- `2`：极端 Non-IID\n\n**`low_prob`（稀少类别采样概率）**\n\n- `0.0`：只有主要类别数据\n- `0.1`：推荐值，保留少量稀少类别\n- `0.5`：接近均匀\n\n| 场景 | classes_per_node | low_prob |\n|------|-----------------|----------|\n| 轻度 | 20 | 0.3 |\n| 中度 | 10 | 0.1 |\n| 重度 | 5 | 0.05 |'
      },
      {id: 3, role: 'user', content: '谢谢，我先用中度配置跑一轮试试。', time: '10:15'},
      {
        id: 4,
        role: 'assistant',
        time: '10:15',
        content: '好的！中度配置是个不错的起点，祝实验顺利！\n\n跑完可以把训练曲线给我看，我帮你分析 loss/accuracy 趋势。'
      }
    ]
  },
  {
    id: 3, title: 'Tiny-ImageNet 显存不足问题',
    preview: 'Tiny-ImageNet 训练需要约 5.5GB 显存，建议...', time: '昨天 16:44',
    messages: [
      {id: 1, role: 'user', content: 'Tiny-ImageNet 跑的时候显存不够，有什么解决方案？', time: '16:40'},
      {
        id: 2,
        role: 'assistant',
        time: '16:42',
        content: 'Tiny-ImageNet（64×64）显存需求约是 CIFAR-100（32×32）的 2 倍。解决方案：\n\n**1. 减小 batch size**：64 → 32 → 16 逐步尝试\n\n**2. 降低 fraction**：`0.1` → `0.05`，减少并发客户端数\n\n**3. 混合精度训练（AMP）**：节省约 40~50% 显存，训练速度也会加快，推荐优先尝试\n\n**4. 梯度检查点**：以时间换空间，节省 30~50% 显存\n\n系统要求 Tiny-ImageNet 至少 **5.5GB 空闲显存**，可在仪表盘查看当前 GPU 状态。'
      },
      {id: 3, role: 'user', content: '好的，我用混合精度训练试试，谢谢！', time: '16:44'},
      {
        id: 4,
        role: 'assistant',
        time: '16:44',
        content: '混合精度是最推荐的方案，几乎无精度损失，显存和速度都有明显改善，好好试试！'
      }
    ]
  },
  {
    id: 4, title: '联邦学习入门介绍',
    preview: '联邦学习是一种分布式机器学习范式，允许多个参与方...', time: '3天前',
    messages: [
      {id: 1, role: 'user', content: '能简单介绍一下联邦学习是什么吗？', time: '09:10'},
      {
        id: 2,
        role: 'assistant',
        time: '09:11',
        content: '**联邦学习（Federated Learning）** 是 Google 于 2016 年提出的分布式机器学习范式。\n\n**核心思想**：数据不离开本地，模型到数据所在地训练，只上传**模型更新**（参数/梯度），保护隐私的同时协同训练全局模型。\n\n**标准流程**：服务器下发全局模型 → 客户端本地训练 → 上传更新 → 服务器聚合 → 重复直到收敛\n\n**主要挑战**：Non-IID 数据、通信效率、模型异构\n\n本平台（MHFL-VS）专注**模型异构**联邦学习，支持 FedAvg、FedProto、FedSSA、LG-FedAvg，欢迎探索！'
      }
    ]
  },
  {
    id: 5, title: 'num_steps 和 epochs 的区别',
    preview: 'num_steps 是全局通信轮数，epochs 是每个客户端本地训练...', time: '5天前',
    messages: [
      {id: 1, role: 'user', content: 'num_steps 和 epochs 有什么区别？', time: '11:05'},
      {
        id: 2,
        role: 'assistant',
        time: '11:06',
        content: '**`num_steps`（全局轮数）**：服务器与客户端的通信轮次，代表联邦迭代次数。建议 100~500，默认 200。\n\n**`epochs`（本地训练轮次）**：每个客户端每轮本地训练的 epoch 数。建议 1~10，过多会加剧 Non-IID 问题，默认 5。\n\n| 需求 | num_steps | epochs |\n|------|-----------|--------|\n| 快速验证 | 50 | 3 |\n| 标准训练 | 200 | 5 |\n| 充分收敛 | 500 | 5 |\n\n增加 `num_steps` 比增加 `epochs` 更稳定，前者增加全局协作，后者增加本地自适应。'
      }
    ]
  }
])

// ===== 页面状态 =====
const activeConvId = ref<number | null>(1)
const inputText = ref('')
const isSending = ref(false)
const sidebarCollapsed = ref(false)
const streamingMsgId = ref<number | null>(null)
const copiedMsgId = ref<number | null>(null)
const msgFeedback = ref<MsgFeedback>({})
const searchKeyword = ref('')

// 子组件引用
const msgListRef = ref<InstanceType<typeof AssistantMessageList> | null>(null)
const inputRef = ref<InstanceType<typeof AssistantInput> | null>(null)

// ===== 计算属性 =====
const activeConv = computed(() =>
    conversations.value.find(c => c.id === activeConvId.value) ?? null
)

const topbarTitle = computed(() =>
    activeConv.value ? activeConv.value.title : t('pages.assistant.title')
)
const topbarDesc = computed(() =>
    activeConv.value
        ? t('assistant.msgCount', {count: activeConv.value.messages.length})
        : t('pages.assistant.desc')
)

const showWelcome = computed(() =>
    !activeConv.value || activeConv.value.messages.length === 0
)

// ===== 侧边栏操作 =====
const newChat = () => {
  const id = Date.now()
  conversations.value.unshift({id, title: '新对话', preview: '', time: '刚刚', messages: []})
  activeConvId.value = id
  inputText.value = ''
}

const selectConv = (id: number) => {
  activeConvId.value = id
  nextTick(() => msgListRef.value?.scrollToBottom())
}

const editConv = (id: number, currentTitle: string) => {
  ElMessageBox.prompt(t('assistant.editConvTitlePlaceholder'), t('assistant.editConvTitle'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    inputValue: currentTitle,
    inputPlaceholder: t('assistant.editConvTitlePlaceholder')
  }).then(({ value }) => {
    const trimmed = (value ?? '').trim()
    if (!trimmed) return
    const conv = conversations.value.find(c => c.id === id)
    if (conv) conv.title = trimmed.length > 30 ? trimmed.slice(0, 30) + '...' : trimmed
  }).catch(() => {})
}

const deleteConv = (id: number) => {
  ElMessageBox.confirm(t('assistant.confirmDeleteConv'), t('assistant.deleteConv'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning'
  }).then(() => {
    const idx = conversations.value.findIndex(c => c.id === id)
    if (idx === -1) return
    conversations.value.splice(idx, 1)
    if (activeConvId.value === id) {
      activeConvId.value = conversations.value[0]?.id ?? null
      nextTick(() => msgListRef.value?.scrollToBottom())
    }
  }).catch(() => {})
}

// ===== 流式输出 =====
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
      nextTick(() => msgListRef.value?.scrollToBottom())
      return
    }
    const step = Math.min(Math.floor(Math.random() * 3) + 1, fullText.length - index)
    msg.content += fullText.slice(index, index + step)
    index += step
    nextTick(() => msgListRef.value?.scrollToBottom())
    setTimeout(tick, 18)
  }
  setTimeout(tick, 18)
}

// ===== 发送消息 =====
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
  nextTick(() => inputRef.value?.resetHeight())
  isSending.value = true
  nextTick(() => msgListRef.value?.scrollToBottom())

  const convId = activeConvId.value!
  setTimeout(() => {
    const conv2 = conversations.value.find(c => c.id === convId)
    if (!conv2) return
    const aiMsgId = ++msgIdCounter
    conv2.messages.push({id: aiMsgId, role: 'assistant', content: '', time: nowTime(), streaming: true})
    nextTick(() => {
      msgListRef.value?.scrollToBottom()
      streamText(convId, aiMsgId, getAiReply(text))
    })
  }, 600)
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
</script>

<template>
  <div class="flex h-full overflow-hidden" style="background: var(--home-bg)">

    <!-- 左侧历史侧边栏 -->
    <AssistantSidebar
        :conversations="conversations"
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

    <!-- 右侧主体 -->
    <main class="flex flex-col flex-1 overflow-hidden min-w-0">

      <!-- 顶部标题栏 -->
      <AssistantTopbar :title="topbarTitle" :desc="topbarDesc"/>

      <!-- 欢迎屏 -->
      <AssistantWelcome
          v-if="showWelcome"
          @ask="(text) => { inputText = text; nextTick(() => sendMessage()) }"
      />

      <!-- 消息列表 -->
      <AssistantMessageList
          v-else
          ref="msgListRef"
          :messages="activeConv!.messages"
          :streaming-msg-id="streamingMsgId"
          :is-sending="isSending"
          :copied-msg-id="copiedMsgId"
          :msg-feedback="msgFeedback"
          @copy="copyMessage"
          @like="toggleLike"
          @dislike="toggleDislike"
      />

      <!-- 输入框 -->
      <AssistantInput
          ref="inputRef"
          v-model="inputText"
          :is-sending="isSending"
          @send="sendMessage"
      />

    </main>
  </div>
</template>
