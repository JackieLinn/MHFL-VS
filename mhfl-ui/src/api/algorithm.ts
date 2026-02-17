/**
 * 算法相关 API
 * 管理员管理算法
 */
import {get, post, put, del} from '@/utils'

export interface AlgorithmVO {
    id: number
    algorithmName: string
    createTime: string
    updateTime: string
}

export interface ListAlgorithmRO {
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
 * 管理员分页查询算法列表（关键字模糊 + 创建时间范围）
 */
export const listAlgorithmsAdmin = (
    params: ListAlgorithmRO,
    success: (data: IPage<AlgorithmVO>) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    const q = new URLSearchParams()
    if (params.keyword != null && params.keyword !== '') q.set('keyword', params.keyword)
    if (params.current != null) q.set('current', String(params.current))
    if (params.size != null) q.set('size', String(params.size))
    if (params.startTime != null) q.set('startTime', params.startTime)
    if (params.endTime != null) q.set('endTime', params.endTime)
    const query = q.toString()
    get(`/api/algorithm/admin/list${query ? `?${query}` : ''}`, success, failure)
}

/**
 * 管理员创建算法
 */
export const createAlgorithmAdmin = (
    algorithmName: string,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    const params = new URLSearchParams()
    params.append('algorithmName', algorithmName)
    post(`/api/algorithm/admin/create?${params.toString()}`, null, success, failure)
}

/**
 * 管理员更新算法名字
 */
export const updateAlgorithmAdmin = (
    id: number,
    algorithmName: string,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    const params = new URLSearchParams()
    params.append('algorithmName', algorithmName)
    put(`/api/algorithm/admin/${id}?${params.toString()}`, null, success, failure)
}

/**
 * 管理员逻辑删除算法
 */
export const deleteAlgorithmAdmin = (
    id: number,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    del(`/api/algorithm/admin/${id}`, success, failure)
}
