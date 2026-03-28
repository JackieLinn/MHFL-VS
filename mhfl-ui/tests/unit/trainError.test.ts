import { describe, expect, it, vi } from 'vitest'

vi.mock('@/locales', () => ({
  default: {
    global: {
      t: (key: string) => `translated:${key}`,
    },
  },
}))

import { resolveTrainError } from '@/utils/trainError'

describe('resolveTrainError', () => {
  it('should return translated text when message matches known pattern', () => {
    const result = resolveTrainError('GPU 不可用，未检测到 GPU')
    expect(result).toBe('translated:pages.task.errors.gpuUnavailable')
  })

  it('should return original message when no pattern matched', () => {
    const message = 'unknown error'
    expect(resolveTrainError(message)).toBe(message)
  })

  it('should return original when empty message', () => {
    expect(resolveTrainError('')).toBe('')
  })

  it('should return original message when i18n returns same key', async () => {
    vi.resetModules()
    vi.doMock('@/locales', () => ({
      default: {
        global: {
          t: (key: string) => key,
        },
      },
    }))
    const { resolveTrainError: fn } = await import('@/utils/trainError')
    const message = '任务已在训练中'
    expect(fn(message)).toBe(message)
  })

  it('should match another known pattern branch', () => {
    const result = resolveTrainError('停止训练失败')
    expect(result).toBe('translated:pages.task.errors.stopFailed')
  })
})
