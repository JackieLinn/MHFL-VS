/** 推荐页共享常量与工具 */

export const algorithmKeys = [
    {key: 'algoStandalone', color: 'blue'},
    {key: 'algoFedProto', color: 'green'},
    {key: 'algoFedAvg', color: 'teal'},
    {key: 'algoFedSSA', color: 'fuchsia'},
    {key: 'algoLGFedAvg', color: 'amber'},
    {key: 'algoOurs', color: 'rose'}
] as const

export const chartColors = ['#3b82f6', '#22c55e', '#0d9488', '#d946ef', '#f59e0b', '#f43f5e'] as const

export const cnnColors = ['#3b82f6', '#22c55e', '#0d9488', '#d946ef', '#f59e0b'] as const

export const settingKeys = [
    {key: 'settingNumNodes', val: 'numNodes', icon: 'i-mdi-account-group-outline'},
    {key: 'settingFraction', val: 'fraction', icon: 'i-mdi-percent'},
    {key: 'settingClassesPerNode', val: 'classesPerNode', icon: 'i-mdi-label-multiple-outline'},
    {key: 'settingLowProb', val: 'lowProb', icon: 'i-mdi-chart-line'},
    {key: 'settingNumSteps', val: 'numSteps', icon: 'i-mdi-repeat'},
    {key: 'settingEpochs', val: 'epochs', icon: 'i-mdi-backup-restore'}
] as const

export const metricKeys = [
    {key: 'metricLoss', val: 'loss', format: (v: number) => v.toFixed(3), icon: 'i-mdi-chart-box-outline'},
    {key: 'metricAccuracy', val: 'accuracy', format: (v: number) => (v * 100).toFixed(2) + '%', icon: 'i-mdi-target'},
    {
        key: 'metricPrecision',
        val: 'precision',
        format: (v: number) => (v * 100).toFixed(2) + '%',
        icon: 'i-mdi-crosshairs-gps'
    },
    {
        key: 'metricRecall',
        val: 'recall',
        format: (v: number) => (v * 100).toFixed(2) + '%',
        icon: 'i-mdi-chart-areaspline'
    },
    {key: 'metricF1', val: 'f1', format: (v: number) => (v * 100).toFixed(2) + '%', icon: 'i-mdi-chart-bar'}
] as const

export const chartMetricKeys = [
    {key: 'chartAccuracy', val: 'accuracy'},
    {key: 'chartPrecision', val: 'precision'},
    {key: 'chartRecall', val: 'recall'},
    {key: 'chartF1', val: 'f1'}
] as const

export const clientMetricOptions = [
    {val: 'accuracy' as const, key: 'clientMetricAccuracy'},
    {val: 'precision' as const, key: 'clientMetricPrecision'},
    {val: 'recall' as const, key: 'clientMetricRecall'},
    {val: 'f1' as const, key: 'clientMetricF1'}
] as const

export const getClientModel = (i: number) => ((i - 1) % 5) + 1
