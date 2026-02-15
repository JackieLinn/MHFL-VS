/**
 * 用户账户相关 API
 * 包含当前用户信息查询、更新、头像上传等
 */
import {get, put, post, del, postForm} from '@/utils'

// 性别：后端 @JsonValue 序列化为 code 0/1/2
export type GenderCode = 0 | 1 | 2

export interface AccountVO {
    id: number
    username: string
    gender: GenderCode
    email: string
    telephone: string
    avatar: string | null
    role: string
    birthday: string | null // yyyy-MM-dd
    age: number | null
    createTime: string
    updateTime: string
}

export interface UpdateAccountRO {
    username?: string
    gender?: GenderCode
    telephone?: string
    birthday?: string // yyyy-MM-dd
}

/**
 * 获取当前登录用户信息
 */
export const getAccountInfo = (
    success: (data: AccountVO) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get('/api/account/info', success, failure)
}

/**
 * 更新当前用户信息（username、gender、telephone、birthday 可选）
 */
export const updateAccount = (
    ro: UpdateAccountRO,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    put('/api/account/update', ro, success, failure)
}

/**
 * 上传用户头像（form-data 字段 file），成功返回完整头像 URL
 */
export const uploadAvatar = (
    file: File,
    success: (avatarUrl: string) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    const formData = new FormData()
    formData.append('file', file)
    postForm('/api/file/avatar/upload', formData, success, failure)
}

// ========== 管理员接口 ==========

export interface ListAccountRO {
    keyword?: string
    current?: number
    size?: number
    startTime?: string // yyyy-MM-dd
    endTime?: string // yyyy-MM-dd
}

export interface CreateAccountRO {
    username: string
    email: string
    telephone: string
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
 * 管理员分页查询用户列表（关键字模糊 + 创建时间范围）
 */
export const listAccountsAdmin = (
    params: ListAccountRO,
    success: (data: IPage<AccountVO>) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    const q = new URLSearchParams()
    if (params.keyword != null && params.keyword !== '') q.set('keyword', params.keyword)
    if (params.current != null) q.set('current', String(params.current))
    if (params.size != null) q.set('size', String(params.size))
    if (params.startTime != null) q.set('startTime', params.startTime)
    if (params.endTime != null) q.set('endTime', params.endTime)
    const query = q.toString()
    get(`/api/account/admin/list${query ? `?${query}` : ''}`, success, failure)
}

/**
 * 管理员创建用户（默认密码 123456，role=user）
 */
export const createAccountAdmin = (
    ro: CreateAccountRO,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    post('/api/account/admin/create', ro, success, failure)
}

/**
 * 管理员逻辑删除用户
 */
export const deleteAccountAdmin = (
    id: number,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    del(`/api/account/admin/${id}`, success, failure)
}
