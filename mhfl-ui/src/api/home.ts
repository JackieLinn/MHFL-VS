/**
 * Home 相关 API
 * 包含首页相关的接口调用
 */
import {get, post} from '@/utils'

// 系统资源信息类型
export interface SystemResources {
    cpu: {
        usagePercent: number
        cores: number
        coresLogical: number
    }
    memory: {
        total: number
        used: number
        free: number
        usagePercent: number
    }
    gpu?: {
        total: number
        used: number
        free: number
        usagePercent: number
    }
}

/**
 * 获取系统资源信息
 * @param success 成功回调
 * @param failure 失败回调（可选）
 */
export const getSystemResources = (
    success: (data: SystemResources) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get('/api/system/resources', success, failure)
}
