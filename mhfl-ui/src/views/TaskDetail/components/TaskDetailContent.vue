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

const emptyRound = (i: number, metrics?: { acc: number; prec: number; rec: number; f1: number }): RoundVO => ({
  id: null,
  roundNum: i,
  loss: null,
  accuracy: metrics?.acc ?? 0,
  precision: metrics?.prec ?? 0,
  recall: metrics?.rec ?? 0,
  f1Score: metrics?.f1 ?? 0
})

/** 按 numSteps 生成横轴：无数据时全 0 或 task 指标；有部分数据时补齐缺失为 0 */
const displayRounds = computed((): RoundVO[] => {
  const r = rounds.value
  const numSteps = Math.max(0, props.task.numSteps ?? 0)
  const t = props.task
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
  return Array.from({length: numSteps}, (_, i) =>
      map.get(i) ?? emptyRound(i, r.length === 0 ? fallbackMetrics : undefined)
  )
})

const settings = computed(() => ({
  numNodes: props.task.numNodes,
  fraction: props.task.fraction,
  classesPerNode: props.task.classesPerNode,
  lowProb: props.task.lowProb,
  numSteps: props.task.numSteps,
  epochs: props.task.epochs
}))

const metrics = computed(() => ({
  loss: props.task.loss ?? -1,
  accuracy: props.task.accuracy ?? -1,
  precision: props.task.precision ?? -1,
  recall: props.task.recall ?? -1,
  f1Score: props.task.f1Score ?? -1
}))

</script>

<template>
  <div class="task-detail-content flex flex-col gap-6 min-w-0">
    <TaskExpSettingsCard :settings="settings"/>
    <TaskMetricsCard :metrics="metrics"/>
    <TaskRoundCurvesCard :rounds="displayRounds" :has-real-data="rounds.length > 0" :loading="roundsLoading"/>
    <TaskClientMetricsCard :clients="clients" :task-id="task.id" :num-steps="task.numSteps" :loading="clientsLoading"/>
  </div>
</template>
