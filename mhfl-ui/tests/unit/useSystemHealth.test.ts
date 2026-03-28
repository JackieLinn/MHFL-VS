import { describe, expect, it, vi } from 'vitest'

vi.mock('@/api/dashboard', () => ({
  getSystemHealth: vi.fn(),
}))

describe('useSystemHealth', () => {
  it('should expose default values before polling', async () => {
    vi.resetModules()
    const mod = await import('@/composables/useSystemHealth')
    const state = mod.useSystemHealth()

    expect(state.health.value).toBeNull()
    expect(state.healthyCount.value).toBe(0)
    expect(state.isAllHealthy.value).toBe(false)
    expect(state.isHealthy('mysql')).toBe(false)
  })

  it('should update health when fetch succeeds', async () => {
    vi.useFakeTimers()
    const { getSystemHealth } = await import('@/api/dashboard')
    const mocked = vi.mocked(getSystemHealth)
    mocked.mockImplementation((success) => {
      success({ mysql: true, redis: true, rabbitmq: false, fastapi: true })
    })

    const mod = await import('@/composables/useSystemHealth')
    const state = mod.useSystemHealth()
    state.ensurePolling()

    expect(state.health.value).toEqual({ mysql: true, redis: true, rabbitmq: false, fastapi: true })
    expect(state.healthyCount.value).toBe(3)
    expect(state.isAllHealthy.value).toBe(false)
    expect(state.isHealthy('mysql')).toBe(true)

    vi.useRealTimers()
  })

  it('should fallback to all false when fetch fails', async () => {
    vi.useFakeTimers()
    vi.resetModules()
    const { getSystemHealth } = await import('@/api/dashboard')
    const mocked = vi.mocked(getSystemHealth)
    mocked.mockImplementation((_success, failure) => {
      failure?.('err', 500, '/api/dashboard/system-health')
    })

    const mod = await import('@/composables/useSystemHealth')
    const state = mod.useSystemHealth()
    state.ensurePolling()

    expect(state.health.value).toEqual({ mysql: false, redis: false, rabbitmq: false, fastapi: false })
    expect(state.healthyCount.value).toBe(0)
    expect(state.isHealthy('fastapi')).toBe(false)
    vi.useRealTimers()
  })

  it('should not register polling twice', async () => {
    vi.useFakeTimers()
    vi.resetModules()
    const { getSystemHealth } = await import('@/api/dashboard')
    const mocked = vi.mocked(getSystemHealth)
    mocked.mockImplementation((success) => {
      success({ mysql: true, redis: true, rabbitmq: true, fastapi: true })
    })
    const setIntervalSpy = vi.spyOn(globalThis, 'setInterval')

    const mod = await import('@/composables/useSystemHealth')
    const state = mod.useSystemHealth()
    state.ensurePolling()
    state.ensurePolling()

    expect(setIntervalSpy).toHaveBeenCalledTimes(1)
    expect(state.isAllHealthy.value).toBe(true)

    setIntervalSpy.mockRestore()
    vi.useRealTimers()
  })
})
