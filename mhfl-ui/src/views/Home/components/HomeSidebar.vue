<script setup lang="ts">
import {computed, ref, onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {getUserInfo} from '@/api/user'

const route = useRoute()
const router = useRouter()
const {t} = useI18n()

const SIDEBAR_COLLAPSED_KEY = 'mhfl_sidebar_collapsed'

// 侧边栏收缩状态
const isCollapsed = ref(false)

// 初始化状态
onMounted(() => {
  const saved = localStorage.getItem(SIDEBAR_COLLAPSED_KEY)
  if (saved === 'true') {
    isCollapsed.value = true
  }
})

// 切换收缩状态
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
  localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(isCollapsed.value))
}

// 导航项：管理员显示「任务管理」，普通用户显示「我的任务」；所有人有推荐展示、智能助手；仅 admin 显示「系统管理」
type MenuItem = { key: string; label: string; icon: string; path: string }
const menuItems = computed<MenuItem[]>(() => {
  const isAdmin = getUserInfo()?.role === 'admin'
  const items: MenuItem[] = [
    {key: 'dashboard', label: t('sidebar.dashboard'), icon: 'i-mdi-view-dashboard-outline', path: '/home/dashboard'},
    {key: 'task', label: isAdmin ? t('sidebar.taskManage') : t('sidebar.myTasks'), icon: 'i-mdi-clipboard-list-outline', path: '/home/task'},
    {key: 'recommended', label: t('sidebar.recommendedShow'), icon: 'i-mdi-trophy-outline', path: '/home/recommended'},
    {key: 'assistant', label: t('sidebar.smartAssistant'), icon: 'i-mdi-robot-outline', path: '/home/assistant'},
  ]
  if (isAdmin) {
    items.push({key: 'admin', label: t('sidebar.admin'), icon: 'i-mdi-cog-outline', path: '/home/admin'})
  }
  return items
})

// 当前激活的菜单项
const activeMenu = computed(() => {
  const path = route.path
  if (path.includes('/dashboard')) return 'dashboard'
  if (path.includes('/task')) return 'task'
  if (path.includes('/recommended')) return 'recommended'
  if (path.includes('/assistant')) return 'assistant'
  if (path.includes('/admin')) return 'admin'
  return 'dashboard'
})

// 切换导航
const handleMenuClick = (path: string) => {
  router.push(path)
}
</script>

<template>
  <aside class="sidebar flex flex-col flex-shrink-0 relative transition-all" :class="{ collapsed: isCollapsed }">
    <div class="p-4 flex flex-col gap-2 flex-1">
      <div
          v-for="item in menuItems"
          :key="item.key"
          class="menu-item flex items-center gap-3 px-4 py-3 rounded-lg cursor-pointer transition-all whitespace-nowrap overflow-hidden"
          :class="{ active: activeMenu === item.key, 'justify-center px-3': isCollapsed }"
          @click="handleMenuClick(item.path)"
          :title="isCollapsed ? item.label : ''"
      >
        <span :class="item.icon" class="text-lg flex-shrink-0"></span>
        <span class="menu-label text-[15px] transition-opacity" v-show="!isCollapsed">{{ item.label }}</span>
      </div>
    </div>

    <!-- 收缩/展开按钮 -->
    <div class="collapse-btn p-3 cursor-pointer flex items-center justify-center transition-all border-t"
         @click="toggleCollapse">
      <span :class="isCollapsed ? 'i-mdi-chevron-right' : 'i-mdi-chevron-left'" class="text-lg transition-transform"></span>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 240px;
  background: var(--home-card-bg);
  border-right: 1px solid var(--home-border);
  transition: width 0.3s ease;
}

.sidebar.collapsed {
  width: 64px;
}

.menu-item {
  color: var(--home-text-secondary);
  position: relative;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.menu-item:hover {
  background: var(--home-hover-bg);
  color: var(--home-text-primary);
  transform: translateX(2px);
}

.menu-item.active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, rgba(139, 92, 246, 0.1) 100%);
  color: #6366f1;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.12);
}

.menu-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 55%;
  background: linear-gradient(to bottom, #6366f1, #8b5cf6);
  border-radius: 0 3px 3px 0;
}

.sidebar.collapsed .menu-label {
  opacity: 0;
  width: 0;
}

.collapse-btn {
  border-color: var(--home-border);
  color: var(--home-text-secondary);
  transition: all 0.25s ease;
}

.collapse-btn:hover {
  background: var(--home-hover-bg);
  color: var(--home-text-primary);
}

.collapse-btn:active {
  transform: scale(0.95);
}

@media (max-width: 768px) {
  .sidebar {
    width: 200px;
  }

  .sidebar.collapsed {
    width: 64px;
  }

  .menu-label {
    font-size: 14px;
  }
}
</style>
