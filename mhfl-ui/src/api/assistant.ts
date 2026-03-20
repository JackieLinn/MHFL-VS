/**
 * Assistant APIs
 */
import {del, get, post, put} from '@/utils'

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
    failure?: (message: string, code: number, url: string) => void
) => {
    get('/api/assistant/conversation/list', success, failure)
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

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

const clamp = (value: number, min: number, max: number) => {
    if (value < min) return min
    if (value > max) return max
    return value
}

const emitSimulatedStream = async (
    fullText: string,
    callbacks: StreamCallbacks
) => {
    const text = fullText ?? ''
    if (!text) {
        callbacks.onDone('')
        return
    }

    const chars = Array.from(text)
    const baseTickMs = 26
    const minDurationMs = clamp(chars.length * 24, 1800, 10000)
    const totalTicks = Math.max(1, Math.floor(minDurationMs / baseTickMs))
    const charsPerTick = clamp(Math.ceil(chars.length / totalTicks), 1, 4)

    await sleep(120)
    for (let i = 0; i < chars.length; i += charsPerTick) {
        const part = chars.slice(i, i + charsPerTick).join('')
        callbacks.onDelta(part)
        const lastChar = chars[Math.min(i + charsPerTick - 1, chars.length - 1)] ?? ''
        const pause = /[，。！？；：,.!?;:]/.test(lastChar) ? 90 : baseTickMs
        await sleep(pause)
    }
    callbacks.onDone(text)
}

/**
 * Stable streaming UX:
 * - Call non-stream chat endpoint for reliable DB persistence
 * - Render content incrementally on frontend
 */
export const chatStream = async (
    ro: ChatRequestRO,
    callbacks: StreamCallbacks
): Promise<void> => {
    await new Promise<void>((resolve) => {
        chat(
            ro,
            async (data) => {
                await emitSimulatedStream(data?.content ?? '', callbacks)
                resolve()
            },
            (message) => {
                callbacks.onError(message || 'Network error')
                resolve()
            }
        )
    })
}

export const updateMessageFeedback = (
    messageId: number,
    feedback: number,
    success: () => void,
    failure?: (message: string, code: number, url: string) => void
) => {
    put(`/api/assistant/message/${messageId}/feedback`, {feedback}, success, failure)
}
