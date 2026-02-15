/**
 * 用户账户相关 API
 * 包含当前用户信息查询、更新、头像上传等
 */
import {get, put, postForm} from '@/utils'

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
