<script setup lang="ts">
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {
  DataAnalysis,
  Monitor,
  ArrowDown,
  User,
  SwitchButton,
  List,
  DataBoard
} from '@element-plus/icons-vue'
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

// 导航菜单
const menuItems = [
  {key: 'task', label: '任务管理', icon: List},
  {key: 'monitor', label: '实时监控', icon: Monitor},
  {key: 'analysis', label: '数据分析', icon: DataBoard}
]

// 当前选中的导航
const activeMenu = ref('task')

// 切换导航
const handleMenuClick = (key: string) => {
  activeMenu.value = key
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
      <!-- 左侧导航栏 -->
      <aside class="sidebar">
        <div class="sidebar-menu">
          <div
              v-for="item in menuItems"
              :key="item.key"
              class="menu-item"
              :class="{ active: activeMenu === item.key }"
              @click="handleMenuClick(item.key)"
          >
            <el-icon class="menu-icon">
              <component :is="item.icon"/>
            </el-icon>
            <span class="menu-label">{{ item.label }}</span>
          </div>
        </div>
      </aside>

      <!-- 右侧内容区域 -->
      <div class="content-area">
        <!-- 任务管理 -->
        <div v-if="activeMenu === 'task'" class="content-panel">
          <h2 class="panel-title">任务管理</h2>
          <p class="panel-desc">在这里管理联邦学习训练任务</p>
          <div class="panel-placeholder">
            <el-icon :size="48" class="placeholder-icon">
              <List/>
            </el-icon>
            <p>任务管理功能开发中...</p>
          </div>
        </div>

        <!-- 实时监控 -->
        <div v-if="activeMenu === 'monitor'" class="content-panel">
          <h2 class="panel-title">实时监控</h2>
          <p class="panel-desc">实时监控联邦学习训练过程</p>
          <div class="panel-placeholder">
            <el-icon :size="48" class="placeholder-icon">
              <Monitor/>
            </el-icon>
            <p>实时监控功能开发中...</p>
          </div>
        </div>

        <!-- 数据分析 -->
        <div v-if="activeMenu === 'analysis'" class="content-panel">
          <h2 class="panel-title">数据分析</h2>
          <p class="panel-desc">查看和分析训练数据</p>
          <div class="panel-placeholder">
            <el-icon :size="48" class="placeholder-icon">
              <DataBoard/>
            </el-icon>
            <p>数据分析功能开发中...</p>
          </div>
        </div>
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
  overflow: hidden;
}

/* ============ 左侧导航栏 ============ */
.sidebar {
  width: 240px;
  background: var(--home-card-bg);
  border-right: 1px solid var(--home-border);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.sidebar-menu {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  color: var(--home-text-secondary);
}

.menu-item:hover {
  background: var(--home-hover-bg);
  color: var(--home-text-primary);
}

.menu-item.active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, rgba(139, 92, 246, 0.1) 100%);
  color: #6366f1;
  font-weight: 500;
}

.menu-icon {
  font-size: 18px;
}

.menu-label {
  font-size: 14px;
}

/* ============ 右侧内容区域 ============ */
.content-area {
  flex: 1;
  overflow-y: auto;
  background: var(--home-bg);
}

.content-panel {
  padding: 32px;
  height: 100%;
}

.panel-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--home-text-primary);
  margin-bottom: 8px;
}

.panel-desc {
  font-size: 14px;
  color: var(--home-text-muted);
  margin-bottom: 32px;
}

.panel-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: var(--home-text-muted);
}

.placeholder-icon {
  color: var(--home-text-muted);
  margin-bottom: 16px;
  opacity: 0.5;
}

.panel-placeholder p {
  font-size: 14px;
  color: var(--home-text-muted);
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
  .sidebar {
    width: 200px;
  }

  .menu-label {
    font-size: 13px;
  }

  .content-panel {
    padding: 24px;
  }

  .panel-title {
    font-size: 20px;
  }
}

:deep(.el-avatar) {
  font-size: 14px;
}
</style>
