/**
 * 任务相关 API
 * 任务列表、详情、创建、启动、停止、删除、推荐等
 */
import {get, post, put, del} from '@/utils'

/** 任务状态：后端 @JsonValue 序列化为 code 0-5 */
export type TaskStatusCode = 0 | 1 | 2 | 3 | 4 | 5

export interface TaskVO {
    id: number
    uid: number
    dataName: string
    algorithmName: string
    username: string
    numNodes: number
    fraction: number
    classesPerNode: number
    lowProb: number
    numSteps: number
    epochs: number
    loss: number | null
    accuracy: number | null
    precision: number | null
    recall: number | null
    f1Score: number | null
    status: TaskStatusCode
    createTime: string
    updateTime: string
}

export interface ListTaskRO {
    keyword?: string
    current?: number
    size?: number
    startTime?: string // yyyy-MM-dd
    endTime?: string // yyyy-MM-dd
}

/** 分页结果（与后端 IPage 一致） */
export interface IPage<T> {
    records: T[]
    total: number
    size: number
    current: number
    pages: number
}

/**
 * 分页查询任务列表（关键字 + 创建时间范围）
 * 非管理员仅当前用户任务，管理员返回全部
 */
export const listTasks = (
    params: ListTaskRO,
    success: (data: IPage<TaskVO>) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    const q = new URLSearchParams()
    if (params.keyword != null && params.keyword !== '') q.set('keyword', params.keyword)
    if (params.current != null) q.set('current', String(params.current))
    if (params.size != null) q.set('size', String(params.size))
    if (params.startTime != null) q.set('startTime', params.startTime)
    if (params.endTime != null) q.set('endTime', params.endTime)
    const query = q.toString()
    get(`/api/task/list${query ? `?${query}` : ''}`, success, failure)
}

/** Round 轮次数据（后端 roundNum 从 0 开始，前端展示时 +1） */
export interface RoundVO {
    id: number | null
    roundNum: number
    loss: number | null
    accuracy: number | null
    precision: number | null
    recall: number | null
    f1Score: number | null
}

/**
 * 获取任务轮次列表（历史曲线数据）
 */
export const getTaskRounds = (
    taskId: number,
    success: (data: RoundVO[]) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get(`/api/task/${taskId}/rounds`, success, failure)
}

/**
 * 获取任务详情
 */
export const getTaskDetail = (
    id: number,
    success: (data: TaskVO) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get(`/api/task/${id}`, success, failure)
}

/** 创建任务请求对象（与后端 CreateTaskRO 一致） */
export interface CreateTaskRO {
    did: number
    aid: number
    numNodes: number
    fraction: number
    classesPerNode: number
    lowProb: number
    numSteps: number
    epochs: number
}

/** 创建任务结果 */
export interface CreateTaskResultVO {
    taskId: number
    copied: boolean
    recommendedSameConfig: boolean
}

/**
 * 创建任务
 */
export const createTask = (
    ro: CreateTaskRO,
    success: (data: CreateTaskResultVO) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    post('/api/task', ro, success, failure)
}

/**
 * 启动训练
 */
export const startTask = (
    id: number,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    post(`/api/task/${id}/start`, null, success, failure)
}

/**
 * 删除任务（推荐任务不可删）
 */
export const deleteTask = (
    id: number,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    del(`/api/task/${id}`, success, failure)
}

/**
 * 设为推荐 / 取消推荐（仅管理员；仅 SUCCESS/RECOMMENDED 可操作）
 */
export const setRecommend = (
    id: number,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    put(`/api/task/admin/${id}/recommend`, {}, success, failure)
}
