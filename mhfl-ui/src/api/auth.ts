/**
 * 认证相关 API
 * 包含登录、注册、重置密码、验证码等接口
 */
import {get, post, internalPost, storeAccessToken, logout as utilsLogout} from '@/utils'
import {ElMessage} from 'element-plus'

// =========================================================================
// 类型定义
// =========================================================================

export interface CaptchaData {
    captchaId: string
    captchaImage: string
}

export interface LoginParams {
    username: string
    password: string
    captchaId: string
    captchaCode: string
    remember: boolean
}

export interface RegisterParams {
    username: string
    password: string
    email: string
    telephone: string
    code: string
}

export interface ResetPasswordParams {
    email: string
    code: string
    password: string
}

// =========================================================================
// 验证码相关
// =========================================================================

/**
 * 获取图形验证码
 */
export const getCaptcha = (
    success: (data: CaptchaData) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get('/captcha/generate', success, failure)
}

/**
 * 发送邮箱验证码
 * @param email 邮箱地址
 * @param type 类型：register | reset
 * @param success 成功处理
 * @param failure 失败处理
 */
export const sendEmailCode = (
    email: string,
    type: 'register' | 'reset',
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get(`/auth/ask-code?email=${encodeURIComponent(email)}&type=${type}`, () => {
        ElMessage.success('验证码已发送，请查收邮箱')
        success()
    }, failure)
}

// =========================================================================
// 登录相关
// =========================================================================

/**
 * 用户登录（带验证码）
 */
export const login = (
    params: LoginParams,
    success: (data: any) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    const formData = new URLSearchParams()
    formData.append('username', params.username)
    formData.append('password', params.password)
    formData.append('captchaId', params.captchaId)
    formData.append('captchaCode', params.captchaCode)

    internalPost('/auth/login', formData.toString(), {
        'Content-Type': 'application/x-www-form-urlencoded'
    }, (data: any) => {
        storeAccessToken(params.remember, data.token, data.expire, data.username, data.id)
        ElMessage.success(`欢迎回来，${data.username}`)
        success(data)
    }, failure)
}

/**
 * 用户登出 - 直接使用 utils 中的 logout
 */
export const logout = utilsLogout

// =========================================================================
// 注册相关
// =========================================================================

/**
 * 用户注册
 */
export const register = (
    params: RegisterParams,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    post('/auth/register', params, () => {
        ElMessage.success('注册成功，请登录')
        success()
    }, failure)
}

// =========================================================================
// 重置密码相关
// =========================================================================

/**
 * 确认重置密码（验证邮箱验证码）
 */
export const confirmReset = (
    email: string,
    code: string,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    post('/auth/reset-confirm', {email, code}, success, failure)
}

/**
 * 重置密码
 */
export const resetPassword = (
    params: ResetPasswordParams,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    post('/auth/reset-password', params, () => {
        ElMessage.success('密码重置成功，请使用新密码登录')
        success()
    }, failure)
}
