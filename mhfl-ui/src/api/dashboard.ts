/**
 * Dashboard 相关 API
 * 平台概览等聚合数据
 */
import {get} from '@/utils'

/** 平台概览统计（与后端 DashboardPlatformStatsVO 一致） */
export interface DashboardPlatformStatsVO {
    totalUsers: number
    totalTasks: number
    totalDatasets: number
    totalAlgorithms: number
}

/**
 * 获取平台概览统计（用户总数、任务总数、数据集总数、算法总数）。
 *
 * @param success 成功回调
 * @param failure 失败回调（可选）
 */
export const getPlatformStats = (
    success: (data: DashboardPlatformStatsVO) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get('/api/dashboard/platform-stats', success, failure)
}
