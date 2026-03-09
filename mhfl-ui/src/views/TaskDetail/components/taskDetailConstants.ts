/** 任务详情页共享常量 */

export const metricKeys = [
    {
        key: 'metricLoss',
        val: 'loss',
        format: (v: number) => (v == null || v === -1 ? '—' : v.toFixed(4)),
        icon: 'i-mdi-chart-box-outline'
    },
    {
        key: 'metricAccuracy',
        val: 'accuracy',
        format: (v: number) => (v == null || v === -1 ? '—' : (v * 100).toFixed(2) + '%'),
        icon: 'i-mdi-target'
    },
    {
        key: 'metricPrecision',
        val: 'precision',
        format: (v: number) => (v == null || v === -1 ? '—' : (v * 100).toFixed(2) + '%'),
        icon: 'i-mdi-crosshairs-gps'
    },
    {
        key: 'metricRecall',
        val: 'recall',
        format: (v: number) => (v == null || v === -1 ? '—' : (v * 100).toFixed(2) + '%'),
        icon: 'i-mdi-chart-areaspline'
    },
    {
        key: 'metricF1',
        val: 'f1Score',
        format: (v: number) => (v == null || v === -1 ? '—' : (v * 100).toFixed(2) + '%'),
        icon: 'i-mdi-chart-bar'
    }
] as const

export const settingKeys = [
    {key: 'settingNumNodes', val: 'numNodes', icon: 'i-mdi-account-group-outline'},
    {key: 'settingFraction', val: 'fraction', icon: 'i-mdi-percent'},
    {key: 'settingClassesPerNode', val: 'classesPerNode', icon: 'i-mdi-label-multiple-outline'},
    {key: 'settingLowProb', val: 'lowProb', icon: 'i-mdi-chart-line'},
    {key: 'settingNumSteps', val: 'numSteps', icon: 'i-mdi-repeat'},
    {key: 'settingEpochs', val: 'epochs', icon: 'i-mdi-backup-restore'}
] as const

export const chartMetricKeys = [
    {key: 'chartAccuracy', val: 'accuracy'},
    {key: 'chartPrecision', val: 'precision'},
    {key: 'chartRecall', val: 'recall'},
    {key: 'chartF1', val: 'f1Score'}
] as const

export const getClientModel = (i: number) => ((i - 1) % 5) + 1

export const cnnColors = ['#3b82f6', '#22c55e', '#0d9488', '#d946ef', '#f59e0b'] as const
