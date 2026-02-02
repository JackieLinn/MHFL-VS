import axios, {type AxiosInstance, type AxiosError, type AxiosResponse} from "axios";
import {ElMessage} from "element-plus";

// =========================================================================
// 类型定义
// =========================================================================

// 后端返回的标准数据结构
interface ResponseData<T = any> {
    code: number;
    data: T;
    message: string;
}

// 本地存储的 Token 结构
interface AuthItem {
    token: string;
    expire: string;
    username: string;
    id: number | string;
}

const authItemName = "access_token";

// =========================================================================
// 配置 Axios
// =========================================================================
const instance: AxiosInstance = axios.create({
    baseURL: 'http://localhost:8088',
    timeout: 10000,
    // 让所有状态码都进入 then，方便统一处理
    validateStatus: () => true
});

// =========================================================================
// 基础工具函数
// =========================================================================

// 默认网络错误处理
const defaultError = (error: AxiosError) => {
    console.error('Network Error:', error);
    if (error.message?.includes('timeout')) {
        ElMessage.error('请求超时，请检查网络连接');
    } else if (error.message?.includes('Network Error')) {
        ElMessage.error('网络连接失败，请检查后端服务是否启动');
    } else {
        ElMessage.error('网络错误，请稍后重试');
    }
};

// 删除 Token
const deleteAccessToken = (): void => {
    localStorage.removeItem(authItemName);
    sessionStorage.removeItem(authItemName);
};

// 获取 Token
const takeAccessToken = (): string | null => {
    const str = localStorage.getItem(authItemName) || sessionStorage.getItem(authItemName);
    if (!str) return null;

    try {
        const authObj: AuthItem = JSON.parse(str);
        if (new Date(authObj.expire) <= new Date()) {
            deleteAccessToken();
            ElMessage.warning("登录状态已过期，请重新登录！");
            return null;
        }
        return authObj.token;
    } catch (e) {
        return null;
    }
};

// 存储 Token
const storeAccessToken = (remember: boolean, token: string, expire: string, username: string, id: number | string): void => {
    const authObj: AuthItem = {token, expire, username, id};
    const str = JSON.stringify(authObj);
    if (remember)
        localStorage.setItem(authItemName, str);
    else
        sessionStorage.setItem(authItemName, str);
};

// 获取请求头
const accessHeader = (): Record<string, string> => {
    const token = takeAccessToken();
    return token ? {'Authorization': `Bearer ${token}`} : {};
};

// =========================================================================
// 统一响应处理
// =========================================================================

/**
 * 处理响应数据
 * @param response Axios 响应对象
 * @param url 请求地址
 * @param success 成功回调
 * @param failure 失败回调（可选，主要用于重置 loading 等状态）
 */
const handleResponse = (
    response: AxiosResponse<ResponseData>,
    url: string,
    success: (data: any) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    const {data, status} = response;

    // 情况1：后端返回标准 JSON 格式 { code, data, message }
    if (data && typeof data === 'object' && 'code' in data) {
        if (data.code === 200) {
            success(data.data);
        } else {
            const message = data.message || '请求失败';
            // 始终显示错误信息
            ElMessage.warning(message);
            console.warn(`请求地址: ${url}, 状态码: ${data.code}, 错误信息: ${message}`);
            // 调用 failure 回调（如果提供），用于重置 loading 等状态
            failure?.(message, data.code, url);
        }
        return;
    }

    // 情况2：HTTP 状态码非 200，但没有标准响应体
    if (status !== 200) {
        const message = `请求失败 (${status})`;
        ElMessage.warning(message);
        console.warn(`请求地址: ${url}, HTTP状态码: ${status}, 错误信息: ${message}`);
        failure?.(message, status, url);
        return;
    }

    // 情况3：其他情况，直接返回数据
    success(data);
};

// =========================================================================
// 核心请求封装
// =========================================================================

const internalPost = (
    url: string,
    data: any,
    headers: any,
    success: (data: any) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    instance.post(url, data, {headers})
        .then((response: AxiosResponse<ResponseData>) => {
            handleResponse(response, url, success, failure);
        })
        .catch((err: AxiosError) => {
            defaultError(err);
            // 网络错误时也调用 failure，确保 loading 状态能重置
            failure?.('网络错误', 0, url);
        });
};

const internalGet = (
    url: string,
    headers: any,
    success: (data: any) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    instance.get(url, {headers})
        .then((response: AxiosResponse<ResponseData>) => {
            handleResponse(response, url, success, failure);
        })
        .catch((err: AxiosError) => {
            defaultError(err);
            failure?.('网络错误', 0, url);
        });
};

// =========================================================================
// 统一导出函数
// =========================================================================

const get = (
    url: string,
    success: (data: any) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    internalGet(url, accessHeader(), success, failure);
};

const post = (
    url: string,
    data: any,
    success: (data: any) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    internalPost(url, data, accessHeader(), success, failure);
};

const login = (
    username: string,
    password: string,
    remember: boolean,
    success: (data: any) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    internalPost('/auth/login', {username, password}, {
        'Content-Type': 'application/x-www-form-urlencoded'
    }, (data: any) => {
        storeAccessToken(remember, data.token, data.expire, data.username, data.id);
        ElMessage.success(`欢迎 ${data.username} ~`);
        success(data);
    }, failure);
};

const logout = (
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get('/auth/logout', () => {
        deleteAccessToken();
        ElMessage.success('欢迎您再次使用');
        success();
    }, failure);
};

const unauthorized = (): boolean => {
    return !takeAccessToken();
};

// 统一导出
export {
    login,
    logout,
    get,
    post,
    unauthorized,
    takeAccessToken,
    deleteAccessToken,
    storeAccessToken,
    internalPost,
    internalGet
};
