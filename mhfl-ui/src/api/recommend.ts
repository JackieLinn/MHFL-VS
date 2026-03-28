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
