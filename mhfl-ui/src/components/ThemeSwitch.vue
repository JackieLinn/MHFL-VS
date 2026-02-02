<script setup lang="ts">
import {computed} from 'vue'
import {Monitor, Sunny, Moon} from '@element-plus/icons-vue'
import {useTheme, type ThemeMode} from '@/stores/theme'

const {themeMode, setThemeMode} = useTheme()

const themeOptions = [
  {value: 'system' as ThemeMode, label: '跟随系统', icon: Monitor},
  {value: 'light' as ThemeMode, label: '浅色模式', icon: Sunny},
  {value: 'dark' as ThemeMode, label: '深色模式', icon: Moon},
]

const currentIcon = computed(() => {
  const option = themeOptions.find(o => o.value === themeMode.value)
  return option ? option.icon : Monitor
})

const handleCommand = (command: ThemeMode) => setThemeMode(command)
</script>

<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <button
        class="w-9 h-9 rounded-lg flex-center bg-white/60 dark:bg-gray-800/60 backdrop-blur border border-gray-200/50 dark:border-gray-700/50 hover:border-indigo-400 dark:hover:border-indigo-500 text-gray-600 dark:text-gray-300 hover:text-indigo-500 transition-all cursor-pointer">
      <el-icon :size="18">
        <component :is="currentIcon"/>
      </el-icon>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
            v-for="option in themeOptions"
            :key="option.value"
            :command="option.value"
            :class="{'is-active': themeMode === option.value}"
        >
          <el-icon class="mr-2">
            <component :is="option.icon"/>
          </el-icon>
          {{ option.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>
:deep(.el-dropdown-menu__item.is-active) {
  color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}
</style>
