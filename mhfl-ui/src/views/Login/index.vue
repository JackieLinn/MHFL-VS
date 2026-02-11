<script setup lang="ts">
import {ref} from 'vue'
import ThemeSwitch from '@/components/ThemeSwitch.vue'
import LoginForm from './components/LoginForm.vue'
import RegisterForm from './components/RegisterForm.vue'
import ResetPasswordForm from './components/ResetPasswordForm.vue'
import {Connection, DataAnalysis, Monitor, TrendCharts} from "@element-plus/icons-vue";

type PanelType = 'login' | 'register' | 'reset'

const currentPanel = ref<PanelType>('login')

const switchPanel = (panel: PanelType) => {
  currentPanel.value = panel
}
</script>

<template>
  <div class="login-page">
    <!-- 动态背景 -->
    <div class="bg-layer">
      <div class="gradient-bg"></div>
      <div class="grid-pattern"></div>

      <!-- 浮动装饰 -->
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
        <div class="shape shape-4"></div>
        <div class="shape shape-5"></div>
      </div>

      <!-- 光晕 -->
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
    </div>

    <!-- GitHub链接和主题切换 -->
    <div class="theme-switch-btn flex items-center gap-2">
      <a
          href="https://github.com/JackieLinn/MHFL-VS"
          target="_blank"
          rel="noopener noreferrer"
          class="w-9 h-9 rounded-lg flex-center bg-white/60 dark:bg-gray-800/60 backdrop-blur border border-gray-200/50 dark:border-gray-700/50 hover:border-indigo-400 dark:hover:border-indigo-500 text-gray-600 dark:text-gray-300 hover:text-indigo-500 transition-all cursor-pointer no-underline"
          title="查看项目源码"
      >
        <i class="i-mdi-github text-lg"></i>
      </a>
      <ThemeSwitch/>
    </div>

    <!-- 主内容 -->
    <div class="main-container">
      <!-- 左侧品牌区域 -->
      <div class="brand-section">
        <div class="brand-content">
          <!-- Logo -->
          <div class="logo-wrapper">
            <div class="logo-ring"></div>
            <div class="logo">
              <el-icon :size="44" color="white">
                <DataAnalysis/>
              </el-icon>
            </div>
          </div>

          <!-- 标题 -->
          <h1 class="brand-title">MHFL-VS</h1>
          <p class="brand-abbr">Model Heterogeneous Federated Learning</p>
          <p class="brand-abbr">End-to-End Visualization and Simulation</p>

          <div class="divider"></div>

          <h2 class="brand-subtitle">模型异构联邦学习</h2>
          <p class="brand-desc">全链路可视化与仿真平台</p>

          <!-- 核心能力 -->
          <div class="capabilities">
            <div class="capability-item">
              <div class="capability-icon">
                <el-icon :size="20">
                  <Connection/>
                </el-icon>
              </div>
              <div class="capability-text">
                <span class="capability-title">模型异构</span>
                <span class="capability-desc">支持不同架构模型协同训练</span>
              </div>
            </div>
            <div class="capability-item">
              <div class="capability-icon">
                <el-icon :size="20">
                  <Monitor/>
                </el-icon>
              </div>
              <div class="capability-text">
                <span class="capability-title">全链路可视化</span>
                <span class="capability-desc">训练过程实时监控与分析</span>
              </div>
            </div>
            <div class="capability-item">
              <div class="capability-icon">
                <el-icon :size="20">
                  <TrendCharts/>
                </el-icon>
              </div>
              <div class="capability-text">
                <span class="capability-title">仿真模拟</span>
                <span class="capability-desc">多场景联邦学习实验环境</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区域 -->
      <div class="form-section">
        <div class="form-card">
          <div class="card-decoration">
            <div class="deco-line deco-line-1"></div>
            <div class="deco-line deco-line-2"></div>
          </div>

          <Transition name="slide" mode="out-in">
            <LoginForm v-if="currentPanel === 'login'" key="login" @switch="switchPanel"/>
            <RegisterForm v-else-if="currentPanel === 'register'" key="register" @switch="switchPanel"/>
            <ResetPasswordForm v-else key="reset" @switch="switchPanel"/>
          </Transition>
        </div>

        <p class="copyright">© 2026 MHFL-VS Platform. All rights reserved.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ============ 基础布局 ============ */
.login-page {
  width: 100vw;
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
  overflow-y: auto;
  background: var(--login-bg);
}

/* ============ 背景层 ============ */
.bg-layer {
  position: fixed;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.gradient-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg,
  var(--login-gradient-1) 0%,
  var(--login-gradient-2) 25%,
  var(--login-gradient-3) 50%,
  var(--login-gradient-2) 75%,
  var(--login-gradient-1) 100%
  );
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
}

@keyframes gradientShift {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.grid-pattern {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(var(--login-grid-color) 1px, transparent 1px),
  linear-gradient(90deg, var(--login-grid-color) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse at center, black 0%, transparent 70%);
}

.floating-shapes {
  position: absolute;
  inset: 0;
}

.shape {
  position: absolute;
  border-radius: 50%;
  opacity: var(--login-shape-opacity);
  filter: blur(1px);
}

.shape-1 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.3) 0%, transparent 70%);
  top: -100px;
  left: -100px;
  animation: float1 20s ease-in-out infinite;
}

.shape-2 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.25) 0%, transparent 70%);
  top: 20%;
  right: 10%;
  animation: float2 25s ease-in-out infinite;
}

.shape-3 {
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.2) 0%, transparent 70%);
  bottom: 20%;
  left: 15%;
  animation: float3 18s ease-in-out infinite;
}

.shape-4 {
  width: 100px;
  height: 100px;
  background: radial-gradient(circle, rgba(244, 114, 182, 0.2) 0%, transparent 70%);
  top: 60%;
  right: 25%;
  animation: float1 22s ease-in-out infinite reverse;
}

.shape-5 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.15) 0%, transparent 70%);
  bottom: -50px;
  right: -50px;
  animation: float2 30s ease-in-out infinite;
}

@keyframes float1 {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  33% {
    transform: translate(30px, -30px) rotate(120deg);
  }
  66% {
    transform: translate(-20px, 20px) rotate(240deg);
  }
}

@keyframes float2 {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(-40px, 30px) scale(1.1);
  }
}

@keyframes float3 {
  0%, 100% {
    transform: translate(0, 0);
  }
  25% {
    transform: translate(20px, -20px);
  }
  50% {
    transform: translate(40px, 0);
  }
  75% {
    transform: translate(20px, 20px);
  }
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: var(--login-glow-opacity);
}

.glow-1 {
  width: 500px;
  height: 500px;
  background: #6366f1;
  top: -200px;
  left: 20%;
  animation: glowPulse 8s ease-in-out infinite;
}

.glow-2 {
  width: 400px;
  height: 400px;
  background: #8b5cf6;
  bottom: -150px;
  right: 10%;
  animation: glowPulse 10s ease-in-out infinite reverse;
}

@keyframes glowPulse {
  0%, 100% {
    opacity: var(--login-glow-opacity);
    transform: scale(1);
  }
  50% {
    opacity: calc(var(--login-glow-opacity) * 1.3);
    transform: scale(1.1);
  }
}

/* ============ 主题切换按钮 ============ */
.theme-switch-btn {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 100;
}

/* ============ 主容器 ============ */
.main-container {
  position: relative;
  z-index: 10;
  width: 100%;
  min-height: 100vh;
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px;
}

/* ============ 品牌区域 ============ */
.brand-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.brand-content {
  max-width: 500px;
}

.logo-wrapper {
  position: relative;
  width: 100px;
  height: 100px;
  margin-bottom: 32px;
}

.logo-ring {
  position: absolute;
  inset: -8px;
  border: 2px solid transparent;
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 3s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.logo {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a855f7 100%);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 20px 40px rgba(99, 102, 241, 0.3);
  animation: logoFloat 6s ease-in-out infinite;
}

@keyframes logoFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

.brand-title {
  font-size: 56px;
  font-weight: 800;
  letter-spacing: 2px;
  margin-bottom: 12px;
  color: var(--login-text-primary);
}

.brand-abbr {
  font-size: 12px;
  color: var(--login-text-muted);
  letter-spacing: 1px;
  line-height: 1.6;
}

.divider {
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, #6366f1, #a855f7);
  border-radius: 2px;
  margin: 24px 0;
}

.brand-subtitle {
  font-size: 26px;
  font-weight: 600;
  color: var(--login-text-primary);
  margin-bottom: 8px;
}

.brand-desc {
  font-size: 16px;
  color: var(--login-text-secondary);
  margin-bottom: 32px;
}

/* 核心能力 */
.capabilities {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.capability-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  background: var(--login-capability-bg);
  border: 1px solid var(--login-capability-border);
  border-radius: 12px;
  transition: all 0.3s;
}

.capability-item:hover {
  background: var(--login-capability-hover-bg);
  border-color: var(--login-capability-hover-border);
  transform: translateX(8px);
}

.capability-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(139, 92, 246, 0.2));
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #818cf8;
  flex-shrink: 0;
}

.capability-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.capability-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--login-text-primary);
}

.capability-desc {
  font-size: 13px;
  color: var(--login-text-muted);
}

/* ============ 表单区域 ============ */
.form-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-card {
  position: relative;
  width: 100%;
  max-width: 440px;
  background: var(--login-card-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--login-card-border);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 25px 50px var(--login-card-shadow);
}

.card-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  border-radius: 24px;
}

.deco-line {
  position: absolute;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.5), transparent);
  height: 1px;
}

.deco-line-1 {
  top: 0;
  left: 0;
  right: 0;
  animation: lineMove 3s ease-in-out infinite;
}

.deco-line-2 {
  bottom: 0;
  left: 0;
  right: 0;
  animation: lineMove 3s ease-in-out infinite reverse;
}

@keyframes lineMove {
  0%, 100% {
    transform: translateX(-100%);
    opacity: 0;
  }
  50% {
    transform: translateX(0);
    opacity: 1;
  }
}

.copyright {
  margin-top: 24px;
  font-size: 12px;
  color: var(--login-text-muted);
}

/* ============ 过渡动画 ============ */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* ============ 响应式 ============ */
@media (max-width: 1024px) {
  .main-container {
    flex-direction: column;
    padding: 24px;
    min-height: auto;
  }

  .brand-section {
    padding: 24px;
    flex: none;
  }

  .brand-content {
    text-align: center;
    max-width: 100%;
  }

  .logo-wrapper {
    margin: 0 auto 24px;
  }

  .brand-title {
    font-size: 42px;
  }

  .brand-subtitle {
    font-size: 20px;
  }

  .brand-desc {
    font-size: 14px;
    margin-bottom: 24px;
  }

  .capabilities {
    max-width: 400px;
    margin: 0 auto;
  }

  .form-section {
    padding: 24px;
    flex: none;
  }

  .form-card {
    padding: 32px 24px;
  }
}

@media (max-width: 640px) {
  .main-container {
    padding: 16px;
  }

  .brand-section {
    padding: 16px;
  }

  .brand-title {
    font-size: 32px;
  }

  .brand-abbr {
    font-size: 11px;
  }

  .divider {
    margin: 16px auto;
  }

  .capabilities {
    display: none;
  }

  .form-section {
    padding: 16px;
  }

  .form-card {
    padding: 24px 20px;
  }
}
</style>
