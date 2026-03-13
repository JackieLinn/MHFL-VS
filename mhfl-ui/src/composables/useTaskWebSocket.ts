/**
 * 任务训练实时监控 WebSocket
 * 连接 /ws/task/{taskId}，首包发送 JWT 鉴权，接收 Round/Client/Status 消息
 */
import {ref, onBeforeUnmount} from 'vue'
import {takeAccessToken} from '@/utils'

const WS_BASE = import.meta.env.VITE_WS_BASE ?? 'ws://localhost:8088'

/** 全局唯一活跃连接，切换任务时关闭旧连接 */
let activeWs: WebSocket | null = null

export interface RoundMessage {
    taskId: number
    roundNum: number
    loss?: number | null
    accuracy?: number | null
    precision?: number | null
    recall?: number | null
    f1Score?: number | null
    timestamp?: string | null
}

export interface ClientMessage {
    taskId: number
    roundNum: number
    clientIndex: number
    loss?: number | null
    accuracy?: number | null
    precision?: number | null
    recall?: number | null
    f1Score?: number | null
    timestamp?: string | null
}

export interface StatusMessage {
    taskId: number
    status: string
    message?: string | null
    timestamp?: string | null
}

export interface UseTaskWebSocketCallbacks {
    onRound: (msg: RoundMessage) => void
    onClient: (msg: ClientMessage) => void
    onStatus: (msg: StatusMessage) => void
}

export const useTaskWebSocket = (taskId: number, callbacks: UseTaskWebSocketCallbacks) => {
    const connected = ref(false)
    let ws: WebSocket | null = null

    const disconnect = () => {
        if (activeWs) {
            activeWs.close()
            activeWs = null
        }
        ws = null
        connected.value = false
    }

    const connect = () => {
        if (!Number.isFinite(taskId) || taskId < 1) return
        const token = takeAccessToken()
        if (!token) return

        disconnect()
        const url = `${WS_BASE.replace(/\/$/, '')}/ws/task/${taskId}`
        ws = new WebSocket(url)
        activeWs = ws

        ws.onopen = () => {
            ws?.send(JSON.stringify({token}))
        }

        const setConnectedAnd = (fn: () => void) => {
            if (!connected.value) connected.value = true
            fn()
        }

        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data)
                if (data == null || typeof data !== 'object') return

                if ('status' in data && typeof data.status === 'string') {
                    setConnectedAnd(() => callbacks.onStatus(data as StatusMessage))
                    return
                }
                if ('clientIndex' in data && typeof data.clientIndex === 'number') {
                    setConnectedAnd(() => callbacks.onClient(data as ClientMessage))
                    return
                }
                if ('roundNum' in data && typeof data.roundNum === 'number') {
                    setConnectedAnd(() => callbacks.onRound(data as RoundMessage))
                }
            } catch {
                // ignore parse error
            }
        }

        ws.onclose = () => {
            if (activeWs === ws) activeWs = null
            connected.value = false
            ws = null
        }

        ws.onerror = () => {
            connected.value = false
        }
    }

    onBeforeUnmount(disconnect)

    return {connect, disconnect, connected}
}
