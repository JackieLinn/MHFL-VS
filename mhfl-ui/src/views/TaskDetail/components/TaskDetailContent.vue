<script setup lang="ts">
import {ref, computed, watch, onBeforeUnmount} from 'vue'
import {getTaskRounds, getTaskClientsLatest, type TaskVO, type RoundVO, type ClientVO} from '@/api/task'
import {
  useTaskWebSocket,
  type RoundMessage,
  type ClientMessage,
  type StatusMessage
} from '@/composables/useTaskWebSocket'
import TaskExpSettingsCard from './TaskExpSettingsCard.vue'
import TaskMetricsCard from './TaskMetricsCard.vue'
import TaskRoundCurvesCard from './TaskRoundCurvesCard.vue'
import TaskClientMetricsCard from './TaskClientMetricsCard.vue'

const props = defineProps<{
  task: TaskVO
}>()

const emit = defineEmits<{
  statusChange: [status: string]
  progress: [payload: { current: number; total: number }]
}>()

const rounds = ref<RoundVO[]>([])
const roundsLoading = ref(false)
const clients = ref<ClientVO[]>([])
const clientsLoading = ref(false)

const fetchRounds = () => {
  const id = props.task.id
  if (!id) return
  roundsLoading.value = true
  getTaskRounds(
      id,
      (data) => {
        rounds.value = data ?? []
        roundsLoading.value = false
      },
      () => {
        roundsLoading.value = false
      }
  )
}

const fetchClients = () => {
  const id = props.task.id
  if (!id) return
  clientsLoading.value = true
  getTaskClientsLatest(
      id,
      (data) => {
        clients.value = data ?? []
        clientsLoading.value = false
      },
      () => {
        clientsLoading.value = false
      }
  )
}

const toRoundVO = (m: RoundMessage): RoundVO => ({
  id: null,
  roundNum: m.roundNum,
  loss: m.loss ?? null,
  accuracy: m.accuracy ?? null,
  precision: m.precision ?? null,
  recall: m.recall ?? null,
  f1Score: m.f1Score ?? null
})

const toClientVO = (m: ClientMessage): ClientVO => ({
  id: null,
  roundNum: m.roundNum,
  clientIndex: m.clientIndex,
  loss: m.loss ?? -1,
  accuracy: m.accuracy ?? -1,
  precision: m.precision ?? -1,
  recall: m.recall ?? -1,
  f1Score: m.f1Score ?? -1,
  timestamp: m.timestamp ?? null
})

const TERMINAL_STATUSES = ['SUCCESS', 'FAILED', 'CANCELLED']

const {connect, disconnect, connected} = useTaskWebSocket(props.task.id, {
  onRound: (msg) => {
    rounds.value = (() => {
      const map = new Map(rounds.value.map((r) => [r.roundNum, r]))
      map.set(msg.roundNum, toRoundVO(msg))
      return Array.from(map.values()).sort((a, b) => a.roundNum - b.roundNum)
    })()
  },
  onClient: (msg) => {
    const arr = [...clients.value]
    while (arr.length <= msg.clientIndex) {
      arr.push({
        id: null,
        roundNum: -1,
        clientIndex: arr.length,
        loss: -1,
        accuracy: -1,
        precision: -1,
        recall: -1,
        f1Score: -1,
        timestamp: null
      })
    }
    arr[msg.clientIndex] = toClientVO(msg)
    clients.value = arr
  },
  onStatus: (msg) => {
    emit('statusChange', msg.status)
    if (TERMINAL_STATUSES.includes(msg.status)) {
      disconnect()
    }
  }
})

watch(() => props.task.id, (id) => {
  if (id) {
    fetchRounds()
    fetchClients()
  }
}, {immediate: true})

watch(
    () => [props.task.id, props.task.status] as const,
    ([id, status]) => {
      if (!Number.isFinite(id) || id < 1) return
      if (status === 1) {
        connect()
      } else {
        disconnect()
      }
    },
    {immediate: true}
)

onBeforeUnmount(disconnect)

const toNum = (v: number | null | undefined) => (v != null && Number.isFinite(v) ? v : 0)

const emptyRound = (
    i: number,
    metrics?: { acc: number; prec: number; rec: number; f1: number } | null
): RoundVO => ({
  id: null,
  roundNum: i,
  loss: null,
  accuracy: metrics === undefined ? 0 : (metrics?.acc ?? null),
  precision: metrics === undefined ? 0 : (metrics?.prec ?? null),
  recall: metrics === undefined ? 0 : (metrics?.rec ?? null),
  f1Score: metrics === undefined ? 0 : (metrics?.f1 ?? null)
})

/**
 * 按 numSteps 生成横轴。
 * - 无数据时：全 0 或 task 指标占位。
 * - 有部分数据且进行中(status=1)：未训练轮次用 null，折线不拖到 0。
 * - 有部分数据且非进行中：未训练轮次用 0。
 */
const displayRounds = computed((): RoundVO[] => {
  const r = rounds.value
  const numSteps = Math.max(0, props.task.numSteps ?? 0)
  const t = props.task
  const isInProgress = props.task.status === 1
  const safeMetric = (v: number | null | undefined) => Math.max(0, toNum(v))
  const fallbackMetrics = numSteps > 0 && r.length === 0
      ? {
        acc: safeMetric(t.accuracy) || 0,
        prec: safeMetric(t.precision) || 0,
        rec: safeMetric(t.recall) || 0,
        f1: safeMetric(t.f1Score) || 0
      }
      : undefined
  if (numSteps <= 0) return r.length > 0 ? r : [emptyRound(0), emptyRound(1)]
  const map = new Map(r.map((x) => [x.roundNum, x]))
  return Array.from({length: numSteps}, (_, i) => {
    const existing = map.get(i)
    if (existing) return existing
    if (r.length === 0) return emptyRound(i, fallbackMetrics)
    return emptyRound(i, isInProgress ? null : undefined)
  })
})

const settings = computed(() => ({
  numNodes: props.task.numNodes,
  fraction: props.task.fraction,
  classesPerNode: props.task.classesPerNode,
  lowProb: props.task.lowProb,
  numSteps: props.task.numSteps,
  epochs: props.task.epochs
}))

/**
 * 训练指标：进行中且已有 rounds 时，从 rounds 取 accuracy 最高的轮次作为最佳指标实时更新；
 * 否则使用 task 的指标。
 */
const progressInfo = computed(() => {
  const total = Math.max(0, props.task.numSteps ?? 0)
  const current = rounds.value.length
  return {current, total}
})

watch(progressInfo, (v) => {
  emit('progress', v)
}, {immediate: true})

const metrics = computed(() => {
  const t = props.task
  const r = rounds.value
  const isInProgress = t.status === 1
  const fallback = (v: number | null | undefined) => (v != null && Number.isFinite(v) ? v : -1)
  if (isInProgress && r.length > 0) {
    const best = r.reduce<RoundVO | null>((acc, cur) => {
      const curAcc = cur.accuracy ?? -1
      const accAcc = acc?.accuracy ?? -1
      return curAcc > accAcc ? cur : acc
    }, null)
    if (best) {
      return {
        loss: fallback(best.loss),
        accuracy: fallback(best.accuracy),
        precision: fallback(best.precision),
        recall: fallback(best.recall),
        f1Score: fallback(best.f1Score)
      }
    }
  }
  return {
    loss: t.loss ?? -1,
    accuracy: t.accuracy ?? -1,
    precision: t.precision ?? -1,
    recall: t.recall ?? -1,
    f1Score: t.f1Score ?? -1
  }
})

</script>

<template>
  <div class="task-detail-content flex flex-col gap-6 min-w-0">
    <TaskExpSettingsCard :settings="settings"/>
    <TaskMetricsCard :metrics="metrics"/>
    <TaskRoundCurvesCard :rounds="displayRounds" :task-id="task.id" :has-real-data="rounds.length > 0"
                         :loading="roundsLoading"/>
    <TaskClientMetricsCard :clients="clients" :task-id="task.id" :num-steps="task.numSteps" :loading="clientsLoading"/>
  </div>
</template>
