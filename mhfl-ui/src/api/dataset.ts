/**
 * 数据集相关 API
 * 管理员管理数据集
 */
import {get, post, put, del} from '@/utils'

export interface DatasetVO {
    id: number
    dataName: string
    createTime: string
    updateTime: string
}

export interface ListDatasetRO {
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
 * 管理员分页查询数据集列表（关键字模糊 + 创建时间范围）
 */
export const listDatasetsAdmin = (
    params: ListDatasetRO,
    success: (data: IPage<DatasetVO>) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    const q = new URLSearchParams()
    if (params.keyword != null && params.keyword !== '') q.set('keyword', params.keyword)
    if (params.current != null) q.set('current', String(params.current))
    if (params.size != null) q.set('size', String(params.size))
    if (params.startTime != null) q.set('startTime', params.startTime)
    if (params.endTime != null) q.set('endTime', params.endTime)
    const query = q.toString()
    get(`/api/dataset/admin/list${query ? `?${query}` : ''}`, success, failure)
}

/**
 * 管理员创建数据集
 */
export const createDatasetAdmin = (
    dataName: string,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    const params = new URLSearchParams()
    params.append('dataName', dataName)
    post(`/api/dataset/admin/create?${params.toString()}`, null, success, failure)
}

/**
 * 管理员更新数据集名字
 */
export const updateDatasetAdmin = (
    id: number,
    dataName: string,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    const params = new URLSearchParams()
    params.append('dataName', dataName)
    put(`/api/dataset/admin/${id}?${params.toString()}`, null, success, failure)
}

/**
 * 管理员逻辑删除数据集
 */
export const deleteDatasetAdmin = (
    id: number,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    del(`/api/dataset/admin/${id}`, success, failure)
}
