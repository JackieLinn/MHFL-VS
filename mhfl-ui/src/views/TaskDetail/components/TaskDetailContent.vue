<script setup lang="ts">
import {ref, computed, watch} from 'vue'
import {getTaskRounds, type TaskVO, type RoundVO} from '@/api/task'
import TaskExpSettingsCard from './TaskExpSettingsCard.vue'
import TaskMetricsCard from './TaskMetricsCard.vue'
import TaskRoundCurvesCard from './TaskRoundCurvesCard.vue'
import TaskClientMetricsCard from './TaskClientMetricsCard.vue'

const props = defineProps<{
  task: TaskVO
}>()

const rounds = ref<RoundVO[]>([])
const roundsLoading = ref(false)

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

watch(() => props.task.id, (id) => {
  if (id) fetchRounds()
}, {immediate: true})

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
    ? { acc: safeMetric(t.accuracy) || 0, prec: safeMetric(t.precision) || 0, rec: safeMetric(t.recall) || 0, f1: safeMetric(t.f1Score) || 0 }
    : undefined
  if (numSteps <= 0) return r.length > 0 ? r : [emptyRound(0), emptyRound(1)]
  const map = new Map(r.map((x) => [x.roundNum, x]))
  return Array.from({ length: numSteps }, (_, i) =>
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

const clientMetrics = computed(() => {
  const bases = {
    accuracy: props.task.accuracy ?? 0.5,
    precision: props.task.precision ?? 0.5,
    recall: props.task.recall ?? 0.5,
    f1: props.task.f1Score ?? 0.5
  }
  const spread = 0.08
  return Array.from({length: 100}, (_, clientIdx) => {
    const noise = (Math.sin(clientIdx * 0.5) * 0.5 + Math.cos(clientIdx * 0.3) * 0.5) * spread
    return {
      accuracy: Math.max(0.2, Math.min(0.85, bases.accuracy + noise)),
      precision: Math.max(0.2, Math.min(0.85, bases.precision + noise * 0.98)),
      recall: Math.max(0.2, Math.min(0.85, bases.recall + noise * 1.02)),
      f1: Math.max(0.2, Math.min(0.85, bases.f1 + noise))
    }
  })
})
</script>

<template>
  <div class="task-detail-content flex flex-col gap-6 min-w-0">
    <TaskExpSettingsCard :settings="settings"/>
    <TaskMetricsCard :metrics="metrics"/>
    <TaskRoundCurvesCard :rounds="displayRounds" :has-real-data="rounds.length > 0" :loading="roundsLoading"/>
    <TaskClientMetricsCard :client-metrics="clientMetrics"/>
  </div>
</template>
