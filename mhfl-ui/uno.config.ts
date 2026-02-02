import { defineConfig, presetUno, presetAttributify, presetIcons } from 'unocss'

export default defineConfig({
  presets: [
    presetUno(),
    presetAttributify(),
    presetIcons({
      scale: 1.2,
      cdn: 'https://esm.sh/',
    }),
  ],
  shortcuts: {
    // 布局
    'flex-center': 'flex items-center justify-center',
    'flex-between': 'flex items-center justify-between',
    'flex-col-center': 'flex flex-col items-center justify-center',
    
    // 全屏容器
    'full-screen': 'w-screen h-screen overflow-hidden',
    
    // 卡片
    'card': 'bg-white dark:bg-gray-800 rounded-2xl shadow-lg',
    'card-glass': 'bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl rounded-2xl border border-gray-200/50 dark:border-gray-700/50',
    
    // 按钮
    'btn-primary': 'w-full h-11 text-base font-medium rounded-lg',
    
    // 输入框容器
    'input-group': 'w-full',
    
    // 链接无下划线
    'link-plain': 'no-underline hover:no-underline',
  },
  theme: {
    colors: {
      primary: {
        DEFAULT: '#6366f1',
        50: '#eef2ff',
        100: '#e0e7ff',
        200: '#c7d2fe',
        300: '#a5b4fc',
        400: '#818cf8',
        500: '#6366f1',
        600: '#4f46e5',
        700: '#4338ca',
        800: '#3730a3',
        900: '#312e81',
      },
    },
  },
  safelist: [
    'i-carbon-user',
    'i-carbon-locked',
    'i-carbon-email',
    'i-carbon-phone',
    'i-carbon-password',
    'i-carbon-checkmark',
    'i-carbon-sun',
    'i-carbon-moon',
    'i-carbon-laptop',
  ],
})
