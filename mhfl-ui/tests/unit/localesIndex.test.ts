import { describe, expect, it, vi } from 'vitest'

describe('locales index', () => {
  it('should use saved locale when valid', async () => {
    vi.resetModules()
    localStorage.setItem('mhfl_locale', 'en-US')
    const { default: i18n } = await import('@/locales')

    expect(i18n.global.locale.value).toBe('en-US')
  })

  it('should fallback to zh-CN when no saved locale', async () => {
    vi.resetModules()
    const { default: i18n } = await import('@/locales')
    expect(i18n.global.locale.value).toBe('zh-CN')
  })
})

