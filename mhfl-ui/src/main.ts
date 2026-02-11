import {createApp} from 'vue'
import App from './App.vue'
import 'virtual:uno.css'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import router from './router'
import './assets/main.css'
import {initTheme} from './stores/theme'
import i18n from './locales'
import {initLocale} from './stores/locale'

// 初始化主题
initTheme()

// 初始化语言
initLocale()

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.use(i18n)
app.use(ElementPlus)
app.use(router)
app.mount('#app')
