import { describe, expect, it, vi } from 'vitest'

describe('locale store', () => {
  it('should set locale and persist', async () => {
    vi.resetModules()
    const mod = await import('@/stores/locale')
    mod.setLocale('en-US')

    expect(mod.getLocale()).toBe('en-US')
    expect(localStorage.getItem('mhfl_locale')).toBe('en-US')
  })

  it('should init locale from storage when valid', async () => {
    vi.resetModules()
    localStorage.setItem('mhfl_locale', 'en-US')
    const mod = await import('@/stores/locale')
    mod.initLocale()

    expect(mod.getLocale()).toBe('en-US')
  })

  it('should fallback to zh-CN when stored locale invalid', async () => {
    vi.resetModules()
    localStorage.setItem('mhfl_locale', 'invalid')
    const mod = await import('@/stores/locale')
    mod.initLocale()

    expect(mod.getLocale()).toBe('zh-CN')
  })
})

