/**
 * Assistant APIs
 */
import {del, get, post, put, takeAccessToken} from '@/utils'

const API_BASE = 'http://localhost:8088'

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

export const createConversation = (
    success: (data: CreateConversationVO) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    post('/api/assistant/conversation', {}, success, failure)
}

export const listConversations = (
    success: (data: ConversationVO[]) => void,
    failure?: (message: string, code: number, url: string) => void,
    keyword?: string
) => {
    const url = (keyword?.trim() ?? '') !== ''
        ? `/api/assistant/conversation/list?keyword=${encodeURIComponent(keyword!.trim())}`
        : '/api/assistant/conversation/list'
    get(url, success, failure)
}

export const getConversationDetail = (
    id: number,
    success: (data: ConversationDetailVO) => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    get(`/api/assistant/conversation/${id}`, success, failure)
}

export const deleteConversation = (
    id: number,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    del(`/api/assistant/conversation/${id}`, success, failure)
}

export const updateConversationTitle = (
    id: number,
    title: string,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    put(`/api/assistant/conversation/${id}/title`, {title}, success, failure)
}

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

interface StreamEventPayload {
    type?: 'start' | 'delta' | 'done' | 'error' | string
    content?: string
}

const normalizeSseChunk = (chunk: string) =>
    chunk.replace(/\r\n/g, '\n').replace(/\r/g, '\n')

const parseSseEventBlock = (eventBlock: string): StreamEventPayload | null => {
    const dataLines = eventBlock
        .split('\n')
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).trim())

    if (!dataLines.length) return null

    const jsonText = dataLines.join('\n')
    if (!jsonText) return null

    try {
        return JSON.parse(jsonText) as StreamEventPayload
    } catch {
        return null
    }
}

/**
 * Real backend stream chain:
 * frontend -> SpringBoot /api/assistant/chat/stream -> FastAPI /api/assistant/chat/stream
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

    let doneReceived = false
    let buffer = ''
    let fullFromDelta = ''

    try {
        const res = await fetch(`${API_BASE}/api/assistant/chat/stream`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'text/event-stream',
                'Authorization': `Bearer ${token}`,
            },
            body: JSON.stringify({
                cid: ro.cid ?? null,
                message: ro.message,
            }),
        })

        if (!res.ok) {
            const text = await res.text()
            let msg = `请求失败 (${res.status})`
            try {
                const parsed = JSON.parse(text)
                if (parsed?.message) msg = parsed.message
            } catch {
                // ignore JSON parse failure
            }
            callbacks.onError(msg)
            return
        }

        const reader = res.body?.getReader()
        if (!reader) {
            callbacks.onError('无法读取流式响应')
            return
        }

        const decoder = new TextDecoder()
        const consumePayload = (payload: StreamEventPayload | null) => {
            if (!payload?.type) return

            if (payload.type === 'start') return

            if (payload.type === 'delta') {
                const delta = payload.content ?? ''
                fullFromDelta += delta
                callbacks.onDelta(delta)
                return
            }

            if (payload.type === 'done') {
                doneReceived = true
                const full = payload.content ?? fullFromDelta
                callbacks.onDone(full)
                return
            }

            if (payload.type === 'error') {
                callbacks.onError(payload.content ?? '智能助手流式输出异常')
            }
        }

        // eslint-disable-next-line no-constant-condition
        while (true) {
            const {done, value} = await reader.read()
            if (done) break

            buffer += normalizeSseChunk(decoder.decode(value, {stream: true}))

            let sep = buffer.indexOf('\n\n')
            while (sep >= 0) {
                const eventBlock = buffer.slice(0, sep)
                buffer = buffer.slice(sep + 2)
                consumePayload(parseSseEventBlock(eventBlock))
                sep = buffer.indexOf('\n\n')
            }
        }

        const tail = normalizeSseChunk(buffer + decoder.decode()).trim()
        if (tail) {
            consumePayload(parseSseEventBlock(tail))
        }

        if (!doneReceived) {
            callbacks.onError('流式响应未正常结束')
        }
    } catch (e) {
        if (!doneReceived) {
            const message = e instanceof Error ? e.message : '网络错误'
            callbacks.onError(message)
        }
    }
}

export const updateMessageFeedback = (
    messageId: number,
    feedback: number,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    put(`/api/assistant/message/${messageId}/feedback`, {feedback}, success, failure)
}
