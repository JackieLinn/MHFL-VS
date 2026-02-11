<script setup lang="ts">
import {ref, onMounted, onUnmounted} from 'vue'
import {useRouter} from 'vue-router'
import {
  DataAnalysis,
  ArrowDown,
  User,
  SwitchButton,
  Connection,
  DataLine,
  VideoPlay
} from '@element-plus/icons-vue'
import {logout} from '@/api/auth'
import {getSystemResources, type SystemResources} from '@/api/home'
import {getUserInfo} from '@/api/user'
import ThemeSwitch from '@/components/ThemeSwitch.vue'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import {useI18n} from 'vue-i18n'

const router = useRouter()
const {t} = useI18n()
const loggingOut = ref(false)

const userInfo = getUserInfo()

// 退出登录
const handleLogout = () => {
  loggingOut.value = true
  logout(
      () => {
        loggingOut.value = false
        router.push('/login')
      },
      () => loggingOut.value = false
  )
}

// 打开GitHub链接
const openGitHub = () => {
  window.open('https://github.com/JackieLinn/MHFL-VS', '_blank', 'noopener,noreferrer')
}

// 系统资源信息
const systemResources = ref<SystemResources | null>(null)
const loadingResources = ref(false)
let resourceTimer: number | null = null

// 获取系统资源
const fetchSystemResources = () => {
  if (loadingResources.value) return

  loadingResources.value = true
  getSystemResources(
      (data) => {
        systemResources.value = data
        loadingResources.value = false
      },
      () => {
        loadingResources.value = false
      }
  )
}

// 格式化百分比
const formatPercent = (value: number | undefined): string => {
  if (value === undefined) return '0%'
  return `${value.toFixed(1)}%`
}

// 格式化GB（仅返回数字，不带单位）
const formatGB = (value: number | undefined): string => {
  if (value === undefined) return '0.0'
  return value.toFixed(1)
}

// 获取使用率颜色
const getUsageColor = (percent: number | undefined): string => {
  if (percent === undefined) return '#909399'
  if (percent < 50) return '#67c23a'
  if (percent < 80) return '#e6a23c'
  return '#f56c6c'
}

// 立即获取资源数据，不等待组件挂载
fetchSystemResources()

// 组件挂载时开始轮询
onMounted(() => {
  // 每3秒轮询一次
  resourceTimer = window.setInterval(fetchSystemResources, 3000)
})

// 组件卸载时清除定时器
onUnmounted(() => {
  if (resourceTimer !== null) {
    clearInterval(resourceTimer)
  }
})
</script>

<template>
  <header class="home-header">
    <div class="header-left">
      <div class="header-logo">
        <el-icon :size="22" color="white">
          <DataAnalysis/>
        </el-icon>
      </div>
      <div class="header-title">
        <h1>{{ $t('home.platform') }}</h1>
        <p>{{ $t('home.subtitle') }}</p>
      </div>
    </div>

    <div class="header-right flex items-center gap-4">
      <!-- 系统资源展示 -->
      <div class="flex items-center gap-5 pr-4 mr-2 border-r border-[var(--home-border)]">
        <!-- CPU -->
        <div v-if="systemResources?.cpu"
             class="flex items-center gap-2 px-2 py-1 rounded-md transition-colors hover:bg-[var(--home-hover-bg)]">
          <el-icon class="text-lg" :color="getUsageColor(systemResources.cpu.usagePercent)">
            <Connection/>
          </el-icon>
          <div class="flex flex-col gap-0.5">
            <span class="text-xs text-[var(--home-text-muted)] leading-none">{{ $t('home.cpu') }}</span>
            <span class="text-sm font-semibold leading-none"
                  :style="{color: getUsageColor(systemResources.cpu.usagePercent)}">
              {{ formatPercent(systemResources.cpu.usagePercent) }}
            </span>
            <span class="text-xs text-[var(--home-text-muted)] leading-none">
              {{ systemResources.cpu.cores }} {{ $t('home.cores') }}{{
                systemResources.cpu.coresLogical !== systemResources.cpu.cores ? ` / ${systemResources.cpu.coresLogical}${$t('home.threads')}` : ''
              }}
            </span>
          </div>
        </div>

        <!-- 内存 -->
        <div v-if="systemResources?.memory"
             class="flex items-center gap-2 px-2 py-1 rounded-md transition-colors hover:bg-[var(--home-hover-bg)]">
          <el-icon class="text-lg" :color="getUsageColor(systemResources.memory.usagePercent)">
            <DataLine/>
          </el-icon>
          <div class="flex flex-col gap-0.5">
            <span class="text-xs text-[var(--home-text-muted)] leading-none">{{ $t('home.memory') }}</span>
            <span class="text-sm font-semibold leading-none"
                  :style="{color: getUsageColor(systemResources.memory.usagePercent)}">
              {{ formatPercent(systemResources.memory.usagePercent) }}
            </span>
            <span class="text-xs text-[var(--home-text-muted)] leading-none">
              {{ formatGB(systemResources.memory.used) }} / {{ formatGB(systemResources.memory.total) }} GB
            </span>
          </div>
        </div>

        <!-- GPU -->
        <div v-if="systemResources?.gpu"
             class="flex items-center gap-2 px-2 py-1 rounded-md transition-colors hover:bg-[var(--home-hover-bg)]">
          <el-icon class="text-lg" :color="getUsageColor(systemResources.gpu.usagePercent)">
            <VideoPlay/>
          </el-icon>
          <div class="flex flex-col gap-0.5">
            <span class="text-xs text-[var(--home-text-muted)] leading-none">{{ $t('home.gpu') }}</span>
            <span class="text-sm font-semibold leading-none"
                  :style="{color: getUsageColor(systemResources.gpu.usagePercent)}">
              {{ formatPercent(systemResources.gpu.usagePercent) }}
            </span>
            <span class="text-xs text-[var(--home-text-muted)] leading-none">
              {{ formatGB(systemResources.gpu.used) }} / {{ formatGB(systemResources.gpu.total) }} GB
            </span>
          </div>
        </div>
      </div>

      <button
          type="button"
          class="w-9 h-9 rounded-lg flex-center bg-white/60 dark:bg-gray-800/60 backdrop-blur border border-gray-200/50 dark:border-gray-700/50 hover:border-indigo-400 dark:hover:border-indigo-500 text-gray-600 dark:text-gray-300 hover:text-indigo-500 transition-all cursor-pointer"
          :title="$t('common.viewSource')"
          @click="openGitHub"
      >
        <i class="i-mdi-github text-lg"></i>
      </button>

      <LocaleSwitch/>

      <ThemeSwitch/>

      <el-dropdown trigger="click">
        <div class="user-dropdown">
          <el-avatar :size="32" class="user-avatar">
            {{ userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
          </el-avatar>
          <span class="user-name">{{ userInfo?.username || $t('common.user') }}</span>
          <el-icon class="dropdown-arrow">
            <ArrowDown/>
          </el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              <el-icon class="mr-2">
                <User/>
              </el-icon>
              {{ userInfo?.username }}
            </el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">
              <el-icon class="mr-2">
                <SwitchButton/>
              </el-icon>
              {{ $t('common.logout') }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped>
.home-header {
  height: 64px;
  background: var(--home-header-bg);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--home-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-logo {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25);
}

.header-title h1 {
  font-size: 18px;
  font-weight: 600;
  color: var(--home-text-primary);
  line-height: 1.3;
}

.header-title p {
  font-size: 12px;
  color: var(--home-text-muted);
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.user-dropdown:hover {
  background: var(--home-hover-bg);
}

.user-avatar {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  color: var(--home-text-secondary);
}

.dropdown-arrow {
  color: var(--home-text-muted);
}

:deep(.el-avatar) {
  font-size: 14px;
}
</style>
