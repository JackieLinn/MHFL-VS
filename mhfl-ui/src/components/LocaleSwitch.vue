<script setup lang="ts">
import {computed} from 'vue'
import {useLocale, type Locale} from '@/stores/locale'

const {currentLocale, setLocale} = useLocale()

const localeOptions: { value: Locale; label: string; flag: string }[] = [
  {value: 'zh-CN', label: '简体中文', flag: '🇨🇳'},
  {value: 'en-US', label: 'English', flag: '🇺🇸'},
]

const handleCommand = (command: Locale) => {
  setLocale(command)
}
</script>

<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <button
        class="w-9 h-9 rounded-lg flex-center bg-white/60 dark:bg-gray-800/60 backdrop-blur border border-gray-200/50 dark:border-gray-700/50 hover:border-indigo-400 dark:hover:border-indigo-500 text-gray-600 dark:text-gray-300 hover:text-indigo-500 transition-all cursor-pointer">
      <i class="i-mdi-translate text-lg"></i>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
            v-for="option in localeOptions"
            :key="option.value"
            :command="option.value"
            :class="{'is-active': currentLocale === option.value}"
        >
          <span class="mr-2">{{ option.flag }}</span>
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
