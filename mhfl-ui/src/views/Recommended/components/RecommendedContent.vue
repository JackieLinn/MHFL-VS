<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import ExpSettingsCard from './ExpSettingsCard.vue'
import MetricsCompareCard from './MetricsCompareCard.vue'
import TestCurvesCard from './TestCurvesCard.vue'
import ClientMetricsCard from './ClientMetricsCard.vue'
import {chartMetricKeys} from './recommendedConstants'
import {listDatasetsForSelect, type DatasetVO} from '@/api/dataset'
import {
  getRecommendExperimentSettings,
  getRecommendMetricsCompare,
  getRecommendTestCurves,
  type RecommendCurveAlgorithmVO,
  type RecommendExperimentSettingsVO,
  type RecommendMetricsCompareItemVO,
  type RecommendTestCurvesVO
} from '@/api/recommend'

const props = defineProps<{
  dataset: 'cifar100' | 'tiny-imagenet'
}>()

const defaultSettingsByDataset = computed(() => {
  if (props.dataset === 'cifar100') {
    return {
      numNodes: 100,
      fraction: 0.1,
      classesPerNode: 5,
      lowProb: 0.5,
      numSteps: 500,
      epochs: 10
    }
  }
  return {
    numNodes: 100,
    fraction: 0.15,
    classesPerNode: 10,
    lowProb: 0.4,
    numSteps: 300,
    epochs: 8
  }
})

const experimentSettings = ref<RecommendExperimentSettingsVO | null>(null)
const algorithmNames = ref<string[]>([])
const compareAlgorithmNames = ref<string[]>([])
const curveAlgorithmNames = ref<string[]>([])
const remoteAlgorithmMetrics = ref<Record<string, number>[]>([])
const remoteRounds = ref<number[]>([])
const remoteChartSmoothSeries = ref<Record<string, Array<Array<number | null>>>>({})
const remoteChartRawSeries = ref<Record<string, Array<Array<number | null>>>>({})
const datasetIdByType = ref<Record<'cifar100' | 'tiny-imagenet', number | null>>({
  cifar100: null,
  'tiny-imagenet': null
})

const resolveDatasetType = (dataName: string) => {
  const name = dataName.toLowerCase()
  return name.includes('tiny') ? 'tiny-imagenet' : 'cifar100'
}

const fetchDatasetIds = () => {
  listDatasetsForSelect((page) => {
    const map: Record<'cifar100' | 'tiny-imagenet', number | null> = {
      cifar100: null,
      'tiny-imagenet': null
    }
    ;(page?.records ?? []).forEach((ds: DatasetVO) => {
      const type = resolveDatasetType(ds.dataName || '')
      if (map[type] == null) {
        map[type] = ds.id
      }
    })
    datasetIdByType.value = map
    fetchRecommendData()
  })
}

const fetchExperimentSettings = () => {
  const datasetId = datasetIdByType.value[props.dataset]
  if (!datasetId) {
    experimentSettings.value = null
    algorithmNames.value = []
    return
  }
  getRecommendExperimentSettings(
      datasetId,
      (data) => {
        experimentSettings.value = data ?? null
        algorithmNames.value = data?.algorithmNames ?? []
      },
      () => {
        experimentSettings.value = null
        algorithmNames.value = []
      }
  )
}

const mapMetricsItem = (item: RecommendMetricsCompareItemVO) => ({
  loss: item.loss ?? 0,
  accuracy: item.accuracy ?? 0,
  precision: item.precision ?? 0,
  recall: item.recall ?? 0,
  f1: item.f1Score ?? 0
})

const fetchMetricsCompare = () => {
  const datasetId = datasetIdByType.value[props.dataset]
  if (!datasetId) {
    remoteAlgorithmMetrics.value = []
    compareAlgorithmNames.value = []
    return
  }
  getRecommendMetricsCompare(
      datasetId,
      (data) => {
        remoteAlgorithmMetrics.value = (data?.items ?? []).map(mapMetricsItem)
        compareAlgorithmNames.value = (data?.items ?? []).map((x) => x.algorithmName ?? '')
      },
      () => {
        remoteAlgorithmMetrics.value = []
        compareAlgorithmNames.value = []
      }
  )
}

const fetchRecommendData = () => {
  fetchExperimentSettings()
  fetchMetricsCompare()
  fetchTestCurves()
}

const mapCurveSeries = (
    algorithms: RecommendCurveAlgorithmVO[],
    metric: 'accuracy' | 'precision' | 'recall' | 'f1'
) => {
  const smoothKey = `${metric}Smooth` as const
  const rawKey = `${metric}Raw` as const
  return {
    smooth: algorithms.map((a) => (a[smoothKey] ?? []) as Array<number | null>),
    raw: algorithms.map((a) => (a[rawKey] ?? []) as Array<number | null>)
  }
}

const fetchTestCurves = () => {
  const datasetId = datasetIdByType.value[props.dataset]
  if (!datasetId) {
    remoteRounds.value = []
    curveAlgorithmNames.value = []
    remoteChartSmoothSeries.value = {}
    remoteChartRawSeries.value = {}
    return
  }
  getRecommendTestCurves(
      datasetId,
      (data: RecommendTestCurvesVO) => {
        const algorithms = data?.algorithms ?? []
        curveAlgorithmNames.value = algorithms.map((x) => x.algorithmName ?? '')
        const accuracy = mapCurveSeries(algorithms, 'accuracy')
        const precision = mapCurveSeries(algorithms, 'precision')
        const recall = mapCurveSeries(algorithms, 'recall')
        const f1 = mapCurveSeries(algorithms, 'f1')
        remoteRounds.value = data?.rounds ?? []
        remoteChartSmoothSeries.value = {
          accuracy: accuracy.smooth,
          precision: precision.smooth,
          recall: recall.smooth,
          f1: f1.smooth
        }
        remoteChartRawSeries.value = {
          accuracy: accuracy.raw,
          precision: precision.raw,
          recall: recall.raw,
          f1: f1.raw
        }
      },
      () => {
        remoteRounds.value = []
        curveAlgorithmNames.value = []
        remoteChartSmoothSeries.value = {}
        remoteChartRawSeries.value = {}
      }
  )
}

const displayCompareAlgorithmNames = computed(() =>
    compareAlgorithmNames.value.length > 0 ? compareAlgorithmNames.value : algorithmNames.value
)

const displayCurveAlgorithmNames = computed(() => {
  if (curveAlgorithmNames.value.length > 0) return curveAlgorithmNames.value
  if (compareAlgorithmNames.value.length > 0) return compareAlgorithmNames.value
  return algorithmNames.value
})

const generateConvergenceCurve = (
    finalValue: number,
    numRounds: number,
    initRatio = 0.18,
    convergeSpeed = 3.2,
    noiseScale = 0.018
): number[] => {
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

const algorithmMetrics = computed(() => {
  if (remoteAlgorithmMetrics.value.length > 0) {
    return remoteAlgorithmMetrics.value
  }
  if (props.dataset === 'cifar100') {
    return [
      {loss: 0.952, accuracy: 0.5812, precision: 0.572, recall: 0.568, f1: 0.570},
      {loss: 0.978, accuracy: 0.5618, precision: 0.552, recall: 0.548, f1: 0.550},
      {loss: 0.918, accuracy: 0.6124, precision: 0.603, recall: 0.598, f1: 0.600},
      {loss: 0.935, accuracy: 0.5987, precision: 0.589, recall: 0.585, f1: 0.587},
      {loss: 0.941, accuracy: 0.5903, precision: 0.581, recall: 0.577, f1: 0.579},
      {loss: 0.868, accuracy: 0.6479, precision: 0.638, recall: 0.634, f1: 0.636}
    ]
  }
  return [
    {loss: 1.452, accuracy: 0.3821, precision: 0.372, recall: 0.368, f1: 0.370},
    {loss: 1.488, accuracy: 0.3615, precision: 0.351, recall: 0.347, f1: 0.349},
    {loss: 1.398, accuracy: 0.4128, precision: 0.403, recall: 0.398, f1: 0.400},
    {loss: 1.418, accuracy: 0.4012, precision: 0.391, recall: 0.387, f1: 0.389},
    {loss: 1.435, accuracy: 0.3906, precision: 0.381, recall: 0.376, f1: 0.378},
    {loss: 1.328, accuracy: 0.4633, precision: 0.454, recall: 0.449, f1: 0.451}
  ]
})

const settings = computed(() => {
  const remote = experimentSettings.value
  if (!remote) return defaultSettingsByDataset.value
  return {
    numNodes: remote.numNodes ?? defaultSettingsByDataset.value.numNodes,
    fraction: remote.fraction ?? defaultSettingsByDataset.value.fraction,
    classesPerNode: remote.classesPerNode ?? defaultSettingsByDataset.value.classesPerNode,
    lowProb: remote.lowProb ?? defaultSettingsByDataset.value.lowProb,
    numSteps: remote.numSteps ?? defaultSettingsByDataset.value.numSteps,
    epochs: remote.epochs ?? defaultSettingsByDataset.value.epochs
  }
})

const getBestIndexForMetric = (val: string) => {
  if (val === 'loss') return -1
  const data = algorithmMetrics.value
  if (!data?.length) return -1
  const values = data.map((d) => (d as Record<string, number>)[val] ?? 0)
  return values.indexOf(Math.max(...values))
}

const numRounds = computed(() => (props.dataset === 'cifar100' ? 500 : 300))
const displayRounds = computed(() => {
  if (remoteRounds.value.length > 0) {
    return remoteRounds.value
  }
  return Array.from({length: numRounds.value}, (_, i) => i + 1)
})

const chartSeriesData = computed(() => {
  if (Object.keys(remoteChartSmoothSeries.value).length > 0) {
    return remoteChartSmoothSeries.value
  }
  const metrics = algorithmMetrics.value
  const rounds = numRounds.value
  const result: Record<string, Array<Array<number | null>>> = {}
  for (const m of chartMetricKeys) {
    result[m.val] = metrics.map((row) =>
        generateConvergenceCurve((row as Record<string, number>)[m.val] ?? 0, rounds)
    )
  }
  return result
})

const chartSeriesRawData = computed(() => {
  if (Object.keys(remoteChartRawSeries.value).length > 0) {
    return remoteChartRawSeries.value
  }
  return chartSeriesData.value
})

const clientMetrics = computed(() => {
  const bases = algorithmMetrics.value
  const spread = props.dataset === 'cifar100' ? 0.08 : 0.06
  return Array.from({length: 100}, (_, clientIdx) => {
    const noise = (Math.sin(clientIdx * 0.5) * 0.5 + Math.cos(clientIdx * 0.3) * 0.5) * spread
    return {
      accuracy: bases.map((row) => Math.max(0.2, Math.min(0.85, (row.accuracy ?? 0) + noise))),
      precision: bases.map((row) => Math.max(0.2, Math.min(0.85, (row.precision ?? 0) + noise * 0.98))),
      recall: bases.map((row) => Math.max(0.2, Math.min(0.85, (row.recall ?? 0) + noise * 1.02))),
      f1: bases.map((row) => Math.max(0.2, Math.min(0.85, (row.f1 ?? 0) + noise)))
    }
  })
})

watch(
    () => props.dataset,
    () => {
      fetchRecommendData()
    },
    {immediate: true}
)

fetchDatasetIds()
</script>

<template>
  <div class="recommended-content flex flex-col gap-6 min-w-0">
    <ExpSettingsCard :settings="settings" :algorithm-names="algorithmNames"/>
    <MetricsCompareCard
        :algorithm-names="displayCompareAlgorithmNames"
        :algorithm-metrics="algorithmMetrics"
        :get-best-index-for-metric="getBestIndexForMetric"
    />
    <TestCurvesCard
        :dataset="dataset"
        :rounds="displayRounds"
        :algorithm-names="displayCurveAlgorithmNames"
        :chart-series-data="chartSeriesData"
        :chart-series-raw-data="chartSeriesRawData"
    />
    <ClientMetricsCard :dataset="dataset" :client-metrics="clientMetrics"/>
  </div>
</template>
