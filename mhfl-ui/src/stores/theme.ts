import {ref} from 'vue'

export type ThemeMode = 'system' | 'light' | 'dark'

const THEME_KEY = 'mhfl_theme'

// 当前主题模式
const themeMode = ref<ThemeMode>('system')

// 实际应用的主题（light 或 dark）
const actualTheme = ref<'light' | 'dark'>('light')

/**
 * 获取系统主题偏好
 */
const getSystemTheme = (): 'light' | 'dark' => {
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
        return 'dark'
    }
    return 'light'
}

/**
 * 应用主题到 DOM
 */
const applyTheme = (theme: 'light' | 'dark') => {
    actualTheme.value = theme
    document.documentElement.setAttribute('data-theme', theme)
    document.documentElement.classList.remove('light', 'dark')
    document.documentElement.classList.add(theme)
}

/**
 * 更新主题
 */
const updateTheme = () => {
    let theme: 'light' | 'dark'

    if (themeMode.value === 'system') {
        theme = getSystemTheme()
    } else {
        theme = themeMode.value
    }

    applyTheme(theme)
}

/**
 * 设置主题模式
 */
export const setThemeMode = (mode: ThemeMode) => {
    themeMode.value = mode
    localStorage.setItem(THEME_KEY, mode)
    updateTheme()
}

/**
 * 获取当前主题模式
 */
export const getThemeMode = () => themeMode.value

/**
 * 获取实际应用的主题
 */
export const getActualTheme = () => actualTheme.value

/**
 * 初始化主题
 */
export const initTheme = () => {
    // 从本地存储读取
    const savedTheme = localStorage.getItem(THEME_KEY) as ThemeMode | null
    if (savedTheme && ['system', 'light', 'dark'].includes(savedTheme)) {
        themeMode.value = savedTheme
    }

    updateTheme()

    // 监听系统主题变化
    if (window.matchMedia) {
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
            if (themeMode.value === 'system') {
                updateTheme()
            }
        })
    }
}

/**
 * 使用主题 Hook
 */
export const useTheme = () => {
    return {
        themeMode,
        actualTheme,
        setThemeMode,
        getSystemTheme,
    }
}
