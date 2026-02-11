import {createI18n} from 'vue-i18n'
import zhCN from './zh-CN.json'
import enUS from './en-US.json'

export type Locale = 'zh-CN' | 'en-US'

const LOCALE_KEY = 'mhfl_locale'

// 获取默认语言
const getDefaultLocale = (): Locale => {
    const saved = localStorage.getItem(LOCALE_KEY) as Locale | null
    if (saved && ['zh-CN', 'en-US'].includes(saved)) {
        return saved
    }
    return 'zh-CN' // 默认中文
}

const i18n = createI18n({
    legacy: false,
    locale: getDefaultLocale(),
    fallbackLocale: 'zh-CN',
    messages: {
        'zh-CN': zhCN,
        'en-US': enUS,
    },
})

export default i18n
