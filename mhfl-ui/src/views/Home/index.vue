<script setup lang="ts">
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {DataAnalysis, Monitor, Connection, TrendCharts, ArrowDown, User, SwitchButton} from '@element-plus/icons-vue'
import {logout} from '@/api/auth'
import ThemeSwitch from '@/components/ThemeSwitch.vue'

const router = useRouter()
const loggingOut = ref(false)

// 获取用户信息
const getUserInfo = () => {
  const str = localStorage.getItem('access_token') || sessionStorage.getItem('access_token')
  if (!str) return null
  try {
    return JSON.parse(str)
  } catch {
    return null
  }
}

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
</script>

<template>
  <div class="full-screen flex flex-col bg-gray-50 dark:bg-gray-900">
    <!-- 顶部导航栏 -->
    <header
        class="h-16 bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl border-b border-gray-200/50 dark:border-gray-700/50 flex-between px-6 flex-shrink-0">
      <div class="flex items-center gap-3">
        <div
            class="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex-center shadow-lg shadow-indigo-500/20">
          <el-icon :size="22" color="white">
            <DataAnalysis/>
          </el-icon>
        </div>
        <div>
          <h1 class="text-lg font-semibold text-gray-800 dark:text-white leading-tight">MHFL-VS Platform</h1>
          <p class="text-xs text-gray-400 dark:text-gray-500">全链路可视化与仿真平台</p>
        </div>
      </div>

      <div class="flex items-center gap-4">
        <ThemeSwitch/>

        <el-dropdown trigger="click">
          <div
              class="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700/50 cursor-pointer transition-colors">
            <el-avatar :size="32" class="bg-indigo-500 text-white font-semibold">
              {{ userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
            </el-avatar>
            <span class="text-sm text-gray-700 dark:text-gray-200">{{ userInfo?.username || '用户' }}</span>
            <el-icon class="text-gray-400">
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
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 主内容区域 -->
    <main class="flex-1 flex-center p-8 overflow-hidden">
      <div class="text-center max-w-3xl">
        <!-- Logo -->
        <div
            class="w-20 h-20 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex-center shadow-xl shadow-indigo-500/25">
          <el-icon :size="40" color="white">
            <DataAnalysis/>
          </el-icon>
        </div>

        <!-- 标题 -->
        <h2 class="text-3xl font-bold text-gray-800 dark:text-white mb-3">
          欢迎使用 MHFL-VS 可视化仿真平台
        </h2>
        <p class="text-gray-500 dark:text-gray-400 mb-10">
          Model Heterogeneous Federated Learning End-to-End Visualization and Simulation Platform
        </p>

        <!-- 功能卡片 -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-5 mb-10">
          <div class="card-glass p-6 hover:shadow-lg hover:-translate-y-1 transition-all cursor-pointer group">
            <el-icon :size="36" class="text-indigo-500 mb-4 group-hover:scale-110 transition-transform">
              <Monitor/>
            </el-icon>
            <h3 class="text-lg font-semibold text-gray-800 dark:text-white mb-2">实时监控</h3>
            <p class="text-sm text-gray-500 dark:text-gray-400">可视化监控联邦学习训练过程</p>
          </div>

          <div class="card-glass p-6 hover:shadow-lg hover:-translate-y-1 transition-all cursor-pointer group">
            <el-icon :size="36" class="text-purple-500 mb-4 group-hover:scale-110 transition-transform">
              <Connection/>
            </el-icon>
            <h3 class="text-lg font-semibold text-gray-800 dark:text-white mb-2">模型异构</h3>
            <p class="text-sm text-gray-500 dark:text-gray-400">支持不同结构模型的联邦学习</p>
          </div>

          <div class="card-glass p-6 hover:shadow-lg hover:-translate-y-1 transition-all cursor-pointer group">
            <el-icon :size="36" class="text-cyan-500 mb-4 group-hover:scale-110 transition-transform">
              <TrendCharts/>
            </el-icon>
            <h3 class="text-lg font-semibold text-gray-800 dark:text-white mb-2">数据分析</h3>
            <p class="text-sm text-gray-500 dark:text-gray-400">丰富的数据统计与分析功能</p>
          </div>
        </div>

        <p class="text-sm text-gray-400 dark:text-gray-500 italic">
          更多功能即将上线，敬请期待...
        </p>
      </div>
    </main>

    <!-- 底部 -->
    <footer
        class="py-4 text-center text-xs text-gray-400 dark:text-gray-500 border-t border-gray-200/50 dark:border-gray-700/50 flex-shrink-0">
      © 2026 MHFL-VS Platform. All rights reserved.
    </footer>
  </div>
</template>

<style scoped>
:deep(.el-avatar) {
  font-size: 14px;
}
</style>
