<script setup lang="ts">
import {computed} from 'vue'
import type {TaskVO} from '@/api/task'
import TaskExpSettingsCard from './TaskExpSettingsCard.vue'
import TaskMetricsCard from './TaskMetricsCard.vue'
import TaskRoundCurvesCard from './TaskRoundCurvesCard.vue'
import TaskClientMetricsCard from './TaskClientMetricsCard.vue'

const props = defineProps<{
  task: TaskVO
}>()

const generateConvergenceCurve = (
    finalValue: number,
    numRounds: number,
    initRatio = 0.18,
    convergeSpeed = 3.2,
    noiseScale = 0.018
): number[] => {
  if (finalValue == null || !Number.isFinite(finalValue) || finalValue === -1) {
    return Array(numRounds).fill(0.5)
  }
  const init = finalValue * initRatio
  const points: number[] = []
  for (let r = 0; r < numRounds; r++) {
    const t = r / Math.max(numRounds - 1, 1)
    const progress = 1 - Math.exp(-convergeSpeed * t)
    const base = init + (finalValue - init) * progress
    const decay = 1 - t * 0.6
    const noise = (Math.random() - 0.5) * 2 * noiseScale * decay
    points.push(Math.max(0.01, Math.min(0.99, base + noise)))
  }
  points[points.length - 1] = finalValue
  return points
}

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

const numRounds = computed(() => props.task.numSteps ?? 500)

const chartSeriesData = computed(() => {
  const result: Record<string, number[]> = {}
  const acc = props.task.accuracy ?? 0.5
  const prec = props.task.precision ?? 0.5
  const rec = props.task.recall ?? 0.5
  const f1 = props.task.f1Score ?? 0.5
  result.accuracy = generateConvergenceCurve(acc, numRounds.value)
  result.precision = generateConvergenceCurve(prec, numRounds.value)
  result.recall = generateConvergenceCurve(rec, numRounds.value)
  result.f1Score = generateConvergenceCurve(f1, numRounds.value)
  return result
})

const clientMetrics = computed(() => {
  const base = props.task.accuracy ?? 0.5
  const spread = 0.08
  return Array.from({length: 100}, (_, clientIdx) => {
    const noise = (Math.sin(clientIdx * 0.5) * 0.5 + Math.cos(clientIdx * 0.3) * 0.5) * spread
    return {
      accuracy: Math.max(0.2, Math.min(0.85, base + noise))
    }
  })
})
</script>

<template>
  <div class="task-detail-content flex flex-col gap-6 min-w-0">
    <TaskExpSettingsCard :settings="settings"/>
    <TaskMetricsCard :metrics="metrics"/>
    <TaskRoundCurvesCard :num-rounds="numRounds" :chart-series-data="chartSeriesData"/>
    <TaskClientMetricsCard :client-metrics="clientMetrics"/>
  </div>
</template>
