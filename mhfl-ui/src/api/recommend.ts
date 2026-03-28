/**
 * 推荐页面相关 API
 * 当前先接入实验设置模块
 */
import {get} from '@/utils'

export interface RecommendExperimentSettingsVO {
    datasetId: number
    sourceTaskId: number | null
    numNodes: number | null
    fraction: number | null
    classesPerNode: number | null
    lowProb: number | null
    numSteps: number | null
    epochs: number | null
    algorithmNames: string[]
}

export interface RecommendMetricsCompareItemVO {
    taskId: number | null
    algorithmName: string | null
    loss: number | null
    accuracy: number | null
    precision: number | null
    recall: number | null
    f1Score: number | null
}

export interface RecommendMetricsCompareVO {
    datasetId: number
    items: RecommendMetricsCompareItemVO[]
}

export interface RecommendCurveAlgorithmVO {
    taskId: number | null
    algorithmName: string | null
    accuracyRaw: Array<number | null>
    precisionRaw: Array<number | null>
    recallRaw: Array<number | null>
    f1Raw: Array<number | null>
    accuracySmooth: Array<number | null>
    precisionSmooth: Array<number | null>
    recallSmooth: Array<number | null>
    f1Smooth: Array<number | null>
}

export interface RecommendTestCurvesVO {
    datasetId: number
    rounds: number[]
    algorithms: RecommendCurveAlgorithmVO[]
}

/**
 * 获取推荐页实验设置
 */
export const getRecommendExperimentSettings = (
    datasetId: number,
    success: (data: RecommendExperimentSettingsVO) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get(`/api/recommended/experiment-settings?datasetId=${datasetId}`, success, failure)
}

/**
 * 获取推荐页算法效果对比
 */
export const getRecommendMetricsCompare = (
    datasetId: number,
    success: (data: RecommendMetricsCompareVO) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get(`/api/recommended/metrics-compare?datasetId=${datasetId}`, success, failure)
}

/**
 * 获取推荐页测试集曲线（含原始值与平滑值）
 */
export const getRecommendTestCurves = (
    datasetId: number,
    success: (data: RecommendTestCurvesVO) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get(`/api/recommended/test-curves?datasetId=${datasetId}`, success, failure)
}
