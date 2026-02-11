import {ref} from 'vue'
import i18n, {type Locale} from '@/locales'

const LOCALE_KEY = 'mhfl_locale'

// 当前语言
const currentLocale = ref<Locale>('zh-CN')

/**
 * 设置语言
 */
export const setLocale = (locale: Locale) => {
    currentLocale.value = locale
    i18n.global.locale.value = locale
    localStorage.setItem(LOCALE_KEY, locale)
}

/**
 * 获取当前语言
 */
export const getLocale = (): Locale => {
    return currentLocale.value
}

/**
 * 初始化语言
 */
export const initLocale = () => {
    const saved = localStorage.getItem(LOCALE_KEY) as Locale | null
    if (saved && ['zh-CN', 'en-US'].includes(saved)) {
        currentLocale.value = saved
        i18n.global.locale.value = saved
    } else {
        currentLocale.value = 'zh-CN'
        i18n.global.locale.value = 'zh-CN'
    }
}

/**
 * 使用语言 Hook
 */
export const useLocale = () => {
    return {
        currentLocale,
        setLocale,
        getLocale,
    }
}
