<script setup lang="ts">
import {computed, ref, onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {DataBoard, View, Setting, Fold, Expand} from '@element-plus/icons-vue'
import {useI18n} from 'vue-i18n'

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
        <el-icon class="text-lg flex-shrink-0">
          <component :is="item.icon"/>
        </el-icon>
        <span class="menu-label text-sm transition-opacity" v-show="!isCollapsed">{{ item.label }}</span>
      </div>
    </div>

    <!-- 收缩/展开按钮 -->
    <div class="collapse-btn p-3 cursor-pointer flex items-center justify-center transition-all border-t"
         @click="toggleCollapse">
      <el-icon class="text-lg transition-transform">
        <Fold v-if="!isCollapsed"/>
        <Expand v-else/>
      </el-icon>
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

.sidebar.collapsed .menu-label {
  opacity: 0;
  width: 0;
}

.collapse-btn {
  border-color: var(--home-border);
  color: var(--home-text-secondary);
}

.collapse-btn:hover {
  background: var(--home-hover-bg);
  color: var(--home-text-primary);
}

@media (max-width: 768px) {
  .sidebar {
    width: 200px;
  }

  .sidebar.collapsed {
    width: 64px;
  }

  .menu-label {
    font-size: 13px;
  }
}
</style>
