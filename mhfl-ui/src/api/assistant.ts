/**
 * 智能助手相关 API
 * 会话 CRUD、流式/非流式聊天、消息反馈
 */
import {del, get, post, put, takeAccessToken} from '@/utils'
import {ElMessage} from 'element-plus'

const API_BASE = 'http://localhost:8088'

// =========================================================================
// 类型定义
// =========================================================================

export interface CreateConversationVO {
    id: number
}

export interface ConversationVO {
    id: number
    title: string
    preview: string
    updateTime: string
    messageCount: number
}

export interface MessageVO {
    id: number
    role: 'user' | 'assistant'
    content: string
    sequenceNum: number
    sources: string[]
    feedback: 0 | 1 | -1
    createTime: string
}

export interface ConversationDetailVO {
    id: number
    title: string
    messageCount: number
    createTime: string
    updateTime: string
    messages: MessageVO[]
}

export interface ChatResponseVO {
    content: string
    sources: string[]
}

export interface ChatRequestRO {
    cid?: number | null
    message: string
}

export interface FeedbackRO {
    feedback: number
}

// =========================================================================
// 会话 CRUD
// =========================================================================

/**
 * 创建或复用空会话
 */
export const createConversation = (
    success: (data: CreateConversationVO) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    post('/api/assistant/conversation', {}, success, failure)
}

/**
 * 会话列表（仅 message_count > 0）
 */
export const listConversations = (
    success: (data: ConversationVO[]) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get('/api/assistant/conversation/list', success, failure)
}

/**
 * 会话详情
 */
export const getConversationDetail = (
    id: number,
    success: (data: ConversationDetailVO) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get(`/api/assistant/conversation/${id}`, success, failure)
}

/**
 * 删除会话
 */
export const deleteConversation = (
    id: number,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    del(`/api/assistant/conversation/${id}`, success, failure)
}

/**
 * 更新会话标题
 */
export const updateConversationTitle = (
    id: number,
    title: string,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    put(`/api/assistant/conversation/${id}/title`, {title}, success, failure)
}

// =========================================================================
// 聊天
// =========================================================================

/**
 * 非流式聊天（备用）
 */
export const chat = (
    ro: ChatRequestRO,
    success: (data: ChatResponseVO) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    post('/api/assistant/chat', ro, success, failure)
}

export interface StreamCallbacks {
    onDelta: (content: string) => void
    onDone: (fullContent: string) => void
    onError: (message: string) => void
}

/**
 * 流式聊天。使用 fetch + ReadableStream 解析 SSE。
 */
export const chatStream = async (
    ro: ChatRequestRO,
    callbacks: StreamCallbacks
): Promise<void> => {
    const token = takeAccessToken()
    if (!token) {
        callbacks.onError('未登录或登录已过期')
        return
    }
    try {
        const res = await fetch(`${API_BASE}/api/assistant/chat/stream`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`,
            },
            body: JSON.stringify({cid: ro.cid ?? null, message: ro.message}),
        })
        if (!res.ok) {
            const text = await res.text()
            let msg = `请求失败 (${res.status})`
            try {
                const json = JSON.parse(text)
                if (json.message) msg = json.message
            } catch {
                // ignore
            }
            callbacks.onError(msg)
            return
        }
        const reader = res.body?.getReader()
        if (!reader) {
            callbacks.onError('无法读取响应流')
            return
        }
        const decoder = new TextDecoder()
        let buffer = ''
        for (; ;) {
            const {done, value} = await reader.read()
            if (done) break
            buffer += decoder.decode(value, {stream: true})
            const lines = buffer.split('\n\n')
            buffer = lines.pop() ?? ''
            for (const line of lines) {
                if (line.startsWith('data: ')) {
                    try {
                        const obj = JSON.parse(line.slice(6))
                        const type = obj.type
                        const content = obj.content ?? ''
                        if (type === 'delta') {
                            callbacks.onDelta(content)
                        } else if (type === 'done') {
                            callbacks.onDone(content)
                        } else if (type === 'error') {
                            callbacks.onError(content)
                        }
                    } catch {
                        // ignore parse error
                    }
                }
            }
        }
        if (buffer.trim() && buffer.startsWith('data: ')) {
            try {
                const obj = JSON.parse(buffer.slice(6))
                if (obj.type === 'done') callbacks.onDone(obj.content ?? '')
            } catch {
                // ignore
            }
        }
    } catch (e) {
        const msg = e instanceof Error ? e.message : '网络错误'
        callbacks.onError(msg)
    }
}

// =========================================================================
// 消息反馈
// =========================================================================

/**
 * 更新消息反馈（点赞/点踩）
 */
export const updateMessageFeedback = (
    messageId: number,
    feedback: number,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    put(`/api/assistant/message/${messageId}/feedback`, {feedback}, success, failure)
}
