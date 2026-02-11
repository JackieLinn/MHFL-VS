<script setup lang="ts">
import {computed} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {DataBoard, View, Setting} from '@element-plus/icons-vue'
import {useI18n} from 'vue-i18n'

const route = useRoute()
const router = useRouter()
const {t} = useI18n()

// 导航菜单（使用 computed 确保响应式）
const menuItems = computed(() => [
  {key: 'dashboard', label: t('sidebar.dashboard'), icon: DataBoard, path: '/home/dashboard'},
  {key: 'monitor', label: t('sidebar.monitor'), icon: View, path: '/home/monitor'},
  {key: 'admin', label: t('sidebar.admin'), icon: Setting, path: '/home/admin'}
])

// 当前激活的菜单项
const activeMenu = computed(() => {
  const path = route.path
  if (path.includes('/dashboard')) return 'dashboard'
  if (path.includes('/monitor')) return 'monitor'
  if (path.includes('/admin')) return 'admin'
  return 'dashboard'
})

// 切换导航
const handleMenuClick = (path: string) => {
  router.push(path)
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar-menu">
      <div
          v-for="item in menuItems"
          :key="item.key"
          class="menu-item"
          :class="{ active: activeMenu === item.key }"
          @click="handleMenuClick(item.path)"
      >
        <el-icon class="menu-icon">
          <component :is="item.icon"/>
        </el-icon>
        <span class="menu-label">{{ item.label }}</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
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

@media (max-width: 768px) {
  .sidebar {
    width: 200px;
  }

  .menu-label {
    font-size: 13px;
  }
}
</style>
