import axios, {type AxiosInstance, type AxiosResponse} from "axios";
import {ElMessage} from "element-plus";

// =========================================================================
// 1. 类型定义
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
// 2. 配置 Axios (绑定后端地址的核心位置！)
// =========================================================================
const instance: AxiosInstance = axios.create({
    // 【关键】这里绑定后端地址和端口
    baseURL: 'http://localhost:8088',
    timeout: 10000
});

// =========================================================================
// 3. 基础工具函数 (全部改为 const)
// =========================================================================

const defaultError = (error: any) => {
    console.error(error);
    ElMessage.error('发生了一些错误，请联系管理员');
};

const defaultFailure = (message: string, status: number, url: string) => {
    console.warn(`请求地址: ${url}, 状态码: ${status}, 错误信息: ${message}`);
    ElMessage.warning(message);
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
    const authObj: AuthItem = {
        token: token,
        expire: expire,
        username: username,
        id: id,
    };
    const str = JSON.stringify(authObj);
    if (remember)
        localStorage.setItem(authItemName, str);
    else
        sessionStorage.setItem(authItemName, str);
};

// 获取请求头
const accessHeader = (): Record<string, string> => {
    const token = takeAccessToken();
    return token ? {
        'Authorization': `Bearer ${token}`
    } : {};
};

// =========================================================================
// 4. 核心请求封装 (改为 const)
// 注意：因为 const 没有变量提升，内部函数必须定义在 export 函数之前
// =========================================================================

const internalPost = (
    url: string,
    data: any,
    headers: any,
    success: (data: any) => void,
    failure: (message: string, code: number, url: string) => void = defaultFailure,
    error: (err: any) => void = defaultError
) => {
    instance.post(url, data, {headers: headers})
        .then(({data}: AxiosResponse<ResponseData>) => {
            if (data.code === 200)
                success(data.data);
            else
                failure(data.message, data.code, url);
        })
        .catch(err => error(err));
};

const internalGet = (
    url: string,
    headers: any,
    success: (data: any) => void,
    failure: (message: string, code: number, url: string) => void = defaultFailure,
    error: (err: any) => void = defaultError
) => {
    instance.get(url, {headers: headers})
        .then(({data}: AxiosResponse<ResponseData>) => {
            if (data.code === 200)
                success(data.data);
            else
                failure(data.message, data.code, url);
        })
        .catch(err => error(err));
};

// =========================================================================
// 5. 导出函数 (改为 const)
// =========================================================================

const get = (url: string, success: (data: any) => void, failure: (message: string, code: number, url: string) => void = defaultFailure) => {
    internalGet(url, accessHeader(), success, failure);
};

const post = (url: string, data: any, success: (data: any) => void, failure: (message: string, code: number, url: string) => void = defaultFailure) => {
    internalPost(url, data, accessHeader(), success, failure);
};

const login = (username: string, password: string, remember: boolean, success: (data: any) => void, failure: (message: string, code: number, url: string) => void = defaultFailure) => {
    internalPost('/auth/login', {
        username: username,
        password: password
    }, {
        'Content-Type': 'application/x-www-form-urlencoded'
    }, (data: any) => {
        storeAccessToken(remember, data.token, data.expire, data.username, data.id);
        ElMessage.success(`欢迎 ${data.username} ~`);
        success(data);
    }, failure);
};

const logout = (success: () => void, failure: (message: string, code: number, url: string) => void = defaultFailure) => {
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
export {login, logout, get, post, unauthorized, takeAccessToken, deleteAccessToken};
