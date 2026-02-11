/**
 * 用户相关 API
 * 包含用户信息获取等接口
 */

// 用户信息类型
export interface UserInfo {
    token: string
    expire: string
    username: string
    id: number | string
}

/**
 * 获取当前用户信息
 * 从 localStorage 或 sessionStorage 中读取
 */
export const getUserInfo = (): UserInfo | null => {
    const str = localStorage.getItem('access_token') || sessionStorage.getItem('access_token')
    if (!str) return null
    try {
        return JSON.parse(str)
    } catch {
        return null
    }
}
