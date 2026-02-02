<script setup lang="ts">
import {ref} from 'vue'
import ThemeSwitch from '@/components/ThemeSwitch.vue'
import LoginForm from './components/LoginForm.vue'
import RegisterForm from './components/RegisterForm.vue'
import ResetPasswordForm from './components/ResetPasswordForm.vue'

type PanelType = 'login' | 'register' | 'reset'

const currentPanel = ref<PanelType>('login')

const switchPanel = (panel: PanelType) => {
  currentPanel.value = panel
}
</script>

<template>
  <div
      class="login-page full-screen relative bg-gradient-to-br from-indigo-50 via-white to-cyan-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900">
    <!-- 背景装饰 -->
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <div
          class="absolute -top-40 -left-40 w-80 h-80 bg-indigo-400/20 dark:bg-indigo-600/10 rounded-full blur-3xl"></div>
      <div class="absolute top-1/2 -right-20 w-60 h-60 bg-cyan-400/20 dark:bg-cyan-600/10 rounded-full blur-3xl"></div>
      <div
          class="absolute -bottom-20 left-1/3 w-72 h-72 bg-purple-400/15 dark:bg-purple-600/10 rounded-full blur-3xl"></div>
    </div>

    <!-- 主题切换 -->
    <div class="absolute top-4 right-4 z-50">
      <ThemeSwitch/>
    </div>

    <!-- 主内容 -->
    <div class="relative z-10 w-full h-full flex">
      <!-- 左侧品牌区域 -->
      <div class="hidden lg:flex flex-1 flex-col-center p-8">
        <div class="max-w-md text-center">
          <!-- Logo -->
          <div
              class="w-20 h-20 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex-center shadow-xl shadow-indigo-500/30">
            <el-icon :size="40" color="white">
              <DataAnalysis/>
            </el-icon>
          </div>

          <!-- 标题 -->
          <h1 class="text-4xl font-bold text-gray-800 dark:text-white mb-3 tracking-wide">MHFL-VS</h1>
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-4 font-medium tracking-wider">
            Model Heterogeneous Federated Learning
          </p>
          <p class="text-lg text-gray-600 dark:text-gray-300 mb-8 leading-relaxed">
            模型异构联邦学习<br/>
            全链路可视化与仿真平台
          </p>

          <!-- 特性列表 -->
          <div class="space-y-3 text-left inline-block">
            <div class="flex items-center gap-3 text-gray-600 dark:text-gray-300">
              <div class="w-6 h-6 rounded-full bg-green-100 dark:bg-green-900/50 flex-center">
                <el-icon class="text-green-600 dark:text-green-400" :size="14">
                  <Check/>
                </el-icon>
              </div>
              <span>支持异构模型联邦学习</span>
            </div>
            <div class="flex items-center gap-3 text-gray-600 dark:text-gray-300">
              <div class="w-6 h-6 rounded-full bg-green-100 dark:bg-green-900/50 flex-center">
                <el-icon class="text-green-600 dark:text-green-400" :size="14">
                  <Check/>
                </el-icon>
              </div>
              <span>实时可视化训练监控</span>
            </div>
            <div class="flex items-center gap-3 text-gray-600 dark:text-gray-300">
              <div class="w-6 h-6 rounded-full bg-green-100 dark:bg-green-900/50 flex-center">
                <el-icon class="text-green-600 dark:text-green-400" :size="14">
                  <Check/>
                </el-icon>
              </div>
              <span>多场景仿真与分析</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区域 -->
      <div class="flex-1 flex-center p-4 lg:p-8">
        <div class="w-full max-w-md">
          <!-- 移动端 Logo -->
          <div class="lg:hidden text-center mb-6">
            <div
                class="w-14 h-14 mx-auto mb-3 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex-center shadow-lg shadow-indigo-500/30">
              <el-icon :size="28" color="white">
                <DataAnalysis/>
              </el-icon>
            </div>
            <h1 class="text-2xl font-bold text-gray-800 dark:text-white">MHFL</h1>
          </div>

          <!-- 表单卡片 -->
          <div class="card-glass p-6 lg:p-8">
            <Transition name="slide" mode="out-in">
              <LoginForm v-if="currentPanel === 'login'" key="login" @switch="switchPanel"/>
              <RegisterForm v-else-if="currentPanel === 'register'" key="register" @switch="switchPanel"/>
              <ResetPasswordForm v-else key="reset" @switch="switchPanel"/>
            </Transition>
          </div>

          <!-- 底部版权 -->
          <p class="text-center text-xs text-gray-400 dark:text-gray-500 mt-6">
            © 2026 MHFL-VS Platform. All rights reserved.
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 过渡动画 */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.25s ease-out;
}

.slide-enter-from {
  opacity: 0;
  transform: translateX(16px);
}

.slide-leave-to {
  opacity: 0;
  transform: translateX(-16px);
}
</style>
