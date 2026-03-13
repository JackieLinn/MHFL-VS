/**
 * 将后端返回的训练相关错误消息映射为 i18n 文案，支持中英文切换。
 * 用于 start/stop 等接口的错误展示。
 */
import i18n from '@/locales'

/** 后端消息关键词 -> i18n key 映射（按匹配优先级，先匹配先返回） */
const PATTERNS: Array<{ pattern: (msg: string) => boolean; key: string }> = [
    {pattern: (m) => m.includes('GPU 不可用') || m.includes('未检测到 GPU'), key: 'pages.task.errors.gpuUnavailable'},
    {pattern: (m) => m.includes('显存不足'), key: 'pages.task.errors.gpuMemoryInsufficient'},
    {pattern: (m) => m.includes('不支持的数据集'), key: 'pages.task.errors.unsupportedDataset'},
    {pattern: (m) => m.includes('正在运行中') && m.includes('任务'), key: 'pages.task.errors.taskAlreadyRunning'},
    {pattern: (m) => m.includes('未在运行') && m.includes('任务'), key: 'pages.task.errors.taskNotRunning'},
    {pattern: (m) => m.includes('调用训练服务失败'), key: 'pages.task.errors.trainServiceFailed'},
    {pattern: (m) => m.includes('启动训练失败'), key: 'pages.task.errors.startFailed'},
    {pattern: (m) => m.includes('停止训练失败'), key: 'pages.task.errors.stopFailed'},
    {pattern: (m) => m.includes('任务已在训练中'), key: 'pages.task.errors.taskAlreadyInProgress'},
    {pattern: (m) => m.includes('该任务已训练过'), key: 'pages.task.errors.taskAlreadyTrained'},
    {pattern: (m) => m.includes('无权限启动'), key: 'pages.task.errors.noPermissionStart'},
    {pattern: (m) => m.includes('无权限停止'), key: 'pages.task.errors.noPermissionStop'},
    {pattern: (m) => m.includes('任务未在训练中'), key: 'pages.task.errors.taskNotInProgress'}
]

export const resolveTrainError = (message: string): string => {
    if (!message) return message
    const t = i18n.global.t
    for (const {pattern, key} of PATTERNS) {
        if (pattern(message)) {
            const translated = t(key)
            return translated !== key ? translated : message
        }
    }
    return message
}
