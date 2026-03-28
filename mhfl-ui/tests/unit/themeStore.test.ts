import { describe, expect, it, vi } from 'vitest'

describe('theme store', () => {
  it('should set mode and persist to localStorage', async () => {
    vi.resetModules()
    const mod = await import('@/stores/theme')
    mod.setThemeMode('dark')

    expect(mod.getThemeMode()).toBe('dark')
    expect(mod.getActualTheme()).toBe('dark')
    expect(localStorage.getItem('mhfl_theme')).toBe('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('should init from saved theme', async () => {
    vi.resetModules()
    localStorage.setItem('mhfl_theme', 'light')
    const mod = await import('@/stores/theme')
    mod.initTheme()

    expect(mod.getThemeMode()).toBe('light')
    expect(mod.getActualTheme()).toBe('light')
  })

  it('should follow system theme in system mode', async () => {
    vi.resetModules()
    vi.stubGlobal(
      'matchMedia',
      vi.fn().mockImplementation(() => ({
        matches: true,
        media: '',
        onchange: null,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        addListener: vi.fn(),
        removeListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    )

    const mod = await import('@/stores/theme')
    mod.setThemeMode('system')
    expect(mod.getActualTheme()).toBe('dark')
  })

  it('should use light theme when no matchMedia support', async () => {
    vi.resetModules()
    vi.stubGlobal('matchMedia', undefined as unknown as typeof window.matchMedia)
    const mod = await import('@/stores/theme')
    mod.setThemeMode('system')
    expect(mod.getActualTheme()).toBe('light')
  })

  it('should ignore invalid saved theme and keep default mode', async () => {
    vi.resetModules()
    localStorage.setItem('mhfl_theme', 'bad')
    const mod = await import('@/stores/theme')
    mod.initTheme()
    expect(mod.getThemeMode()).toBe('system')
  })

  it('should not switch theme on system event when mode is not system', async () => {
    vi.resetModules()
    const listeners: Array<() => void> = []
    vi.stubGlobal(
      'matchMedia',
      vi.fn().mockImplementation(() => ({
        matches: false,
        media: '',
        onchange: null,
        addEventListener: (_event: string, cb: () => void) => listeners.push(cb),
        removeEventListener: vi.fn(),
        addListener: vi.fn(),
        removeListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    )

    const mod = await import('@/stores/theme')
    mod.setThemeMode('dark')
    mod.initTheme()
    listeners.forEach((cb) => cb())
    expect(mod.getActualTheme()).toBe('dark')
  })
})
