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
  <div class="home-page">
    <!-- 顶部导航栏 -->
    <header class="home-header">
      <div class="header-left">
        <div class="header-logo">
          <el-icon :size="22" color="white">
            <DataAnalysis/>
          </el-icon>
        </div>
        <div class="header-title">
          <h1>MHFL-VS Platform</h1>
          <p>全链路可视化与仿真平台</p>
        </div>
      </div>

      <div class="header-right">
        <ThemeSwitch/>

        <el-dropdown trigger="click">
          <div class="user-dropdown">
            <el-avatar :size="32" class="user-avatar">
              {{ userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
            </el-avatar>
            <span class="user-name">{{ userInfo?.username || '用户' }}</span>
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
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 主内容区域 -->
    <main class="home-main">
      <div class="main-content">
        <!-- Logo -->
        <div class="main-logo">
          <el-icon :size="40" color="white">
            <DataAnalysis/>
          </el-icon>
        </div>

        <!-- 标题 -->
        <h2 class="main-title">欢迎使用 MHFL-VS 可视化仿真平台</h2>
        <p class="main-subtitle">
          Model Heterogeneous Federated Learning End-to-End Visualization and Simulation Platform
        </p>

        <!-- 功能卡片 -->
        <div class="feature-cards">
          <div class="feature-card">
            <el-icon :size="36" class="card-icon icon-indigo">
              <Monitor/>
            </el-icon>
            <h3 class="card-title">实时监控</h3>
            <p class="card-desc">可视化监控联邦学习训练过程</p>
          </div>

          <div class="feature-card">
            <el-icon :size="36" class="card-icon icon-purple">
              <Connection/>
            </el-icon>
            <h3 class="card-title">模型异构</h3>
            <p class="card-desc">支持不同结构模型的联邦学习</p>
          </div>

          <div class="feature-card">
            <el-icon :size="36" class="card-icon icon-cyan">
              <TrendCharts/>
            </el-icon>
            <h3 class="card-title">数据分析</h3>
            <p class="card-desc">丰富的数据统计与分析功能</p>
          </div>
        </div>

        <p class="coming-soon">更多功能即将上线，敬请期待...</p>
      </div>
    </main>

    <!-- 底部 -->
    <footer class="home-footer">
      © 2026 MHFL-VS Platform. All rights reserved.
    </footer>
  </div>
</template>

<style scoped>
.home-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--home-bg);
  overflow: hidden;
}

/* ============ 顶部导航 ============ */
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

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
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

/* ============ 主内容 ============ */
.home-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  overflow: hidden;
}

.main-content {
  text-align: center;
  max-width: 800px;
}

.main-logo {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  border-radius: 20px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 32px rgba(99, 102, 241, 0.3);
}

.main-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--home-text-primary);
  margin-bottom: 12px;
}

.main-subtitle {
  font-size: 14px;
  color: var(--home-text-muted);
  margin-bottom: 40px;
}

/* 功能卡片 */
.feature-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

.feature-card {
  padding: 24px;
  background: var(--home-card-bg);
  border: 1px solid var(--home-card-border);
  border-radius: 16px;
  transition: all 0.3s;
  cursor: pointer;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px var(--home-card-shadow);
  border-color: rgba(99, 102, 241, 0.3);
}

.card-icon {
  margin-bottom: 16px;
  transition: transform 0.3s;
}

.feature-card:hover .card-icon {
  transform: scale(1.1);
}

.icon-indigo {
  color: #6366f1;
}

.icon-purple {
  color: #a855f7;
}

.icon-cyan {
  color: #06b6d4;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--home-text-primary);
  margin-bottom: 8px;
}

.card-desc {
  font-size: 13px;
  color: var(--home-text-muted);
}

.coming-soon {
  font-size: 14px;
  color: var(--home-text-muted);
  font-style: italic;
}

/* ============ 底部 ============ */
.home-footer {
  padding: 16px;
  text-align: center;
  font-size: 12px;
  color: var(--home-text-muted);
  border-top: 1px solid var(--home-border);
  flex-shrink: 0;
}

/* ============ 响应式 ============ */
@media (max-width: 768px) {
  .feature-cards {
    grid-template-columns: 1fr;
  }

  .main-title {
    font-size: 22px;
  }
}

:deep(.el-avatar) {
  font-size: 14px;
}
</style>
