import { describe, expect, it, vi } from 'vitest'

vi.mock('element-plus', () => ({
  ElMessage: {
    warning: vi.fn(),
    error: vi.fn(),
    success: vi.fn(),
  },
}))

vi.mock('@/composables/usePageSize', () => ({
  clearAllPageSizePreferences: vi.fn(),
}))

vi.mock('@/composables/useTaskSortPreference', () => ({
  clearAllTaskSortPreferences: vi.fn(),
}))

vi.mock('@/utils/trainError', () => ({
  resolveTrainError: (msg: string) => msg,
}))

describe('utils auth helpers', () => {
  it('should store and take token from localStorage when remember=true', async () => {
    vi.resetModules()
    const mod = await import('@/utils')
    const expire = new Date(Date.now() + 60_000).toISOString()

    mod.storeAccessToken(true, 'token-a', expire, 'u', 1, 'admin')
    expect(mod.takeAccessToken()).toBe('token-a')
  })

  it('should store and take token from sessionStorage when remember=false', async () => {
    vi.resetModules()
    const mod = await import('@/utils')
    const expire = new Date(Date.now() + 60_000).toISOString()

    mod.storeAccessToken(false, 'token-b', expire, 'u', 1)
    expect(mod.takeAccessToken()).toBe('token-b')
  })

  it('should clear expired token', async () => {
    vi.resetModules()
    const mod = await import('@/utils')
    const expire = new Date(Date.now() - 60_000).toISOString()

    mod.storeAccessToken(true, 'expired', expire, 'u', 1)
    expect(mod.takeAccessToken()).toBeNull()
  })

  it('should return unauthorized=true when token missing', async () => {
    vi.resetModules()
    const mod = await import('@/utils')
    expect(mod.unauthorized()).toBe(true)
  })
})

