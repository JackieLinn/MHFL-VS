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
    get('/api/dashboard/admin/platform-stats', success, failure)
}

/** 按算法统计任务数（算法名 -> 任务数） */
export type TasksByAlgorithm = Record<string, number>

/**
 * 获取按算法分组的任务数量，用于柱状图。
 *
 * @param success 成功回调
 * @param failure 失败回调（可选）
 */
export const getTasksByAlgorithm = (
    success: (data: TasksByAlgorithm) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get('/api/dashboard/admin/tasks-by-algorithm', success, failure)
}

/** 任务状态分布统计（与后端 DashboardTaskStatusStatsVO 一致） */
export interface DashboardTaskStatusStatsVO {
    notStarted: number
    inProgress: number
    completed: number
    failed: number
}

/**
 * 获取任务状态分布统计（未开始、进行中、已完成、失败），用于饼状图。
 *
 * @param success 成功回调
 * @param failure 失败回调（可选）
 */
export const getTaskStatusStats = (
    success: (data: DashboardTaskStatusStatsVO) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get('/api/dashboard/task-status-stats', success, failure)
}

/** 近7天任务趋势（与后端 DashboardTaskTrendVO 一致） */
export interface DashboardTaskTrendVO {
    dates: string[]
    counts: number[]
}

/**
 * 获取近 7 天任务趋势（含今天），用于折线图。
 *
 * @param success 成功回调
 * @param failure 失败回调（可选）
 */
export const getTaskTrend7Days = (
    success: (data: DashboardTaskTrendVO) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get('/api/dashboard/task-trend-7days', success, failure)
}
