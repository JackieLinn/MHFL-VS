<script setup lang="ts">
import {computed} from 'vue'
import {useRouter} from 'vue-router'
import {List, DataBoard, Setting, MagicStick, Star} from '@element-plus/icons-vue'
import {getUserInfo} from '@/api/user'

const router = useRouter()
const isAdmin = computed(() => getUserInfo()?.role === 'admin')

const quickActionItemsAdmin = [
  {key: 'dashboard', labelKey: 'pages.dashboard.actionDashboard', icon: DataBoard},
  {key: 'taskManage', labelKey: 'pages.dashboard.actionTaskManage', icon: List},
  {key: 'systemAdmin', labelKey: 'pages.dashboard.actionSystemAdmin', icon: Setting},
  {key: 'smartAssistant', labelKey: 'pages.dashboard.actionSmartAssistant', icon: MagicStick},
]
const quickActionItemsUser = [
  {key: 'dashboard', labelKey: 'pages.dashboard.actionDashboard', icon: DataBoard},
  {key: 'myTasks', labelKey: 'pages.dashboard.actionMyTasks', icon: List},
  {key: 'recommendedShow', labelKey: 'pages.dashboard.actionRecommendedShow', icon: Star},
  {key: 'smartAssistant', labelKey: 'pages.dashboard.actionSmartAssistant', icon: MagicStick},
]
const quickActionItems = computed(() => isAdmin.value ? quickActionItemsAdmin : quickActionItemsUser)

const quickActionRouteMap: Record<string, string> = {
  dashboard: '/home/dashboard',
  taskManage: '/home/task',
  myTasks: '/home/task',
  recommendedShow: '/home/recommended',
  systemAdmin: '/home/admin',
  smartAssistant: '/home/assistant',
}

const goTo = (path: string) => router.push(path)

const handleQuickAction = (key: string) => {
  const targetRoute = quickActionRouteMap[key]
  if (targetRoute) {
    goTo(targetRoute)
  }
}
</script>

<template>
  <div class="actions-card tech-card flex flex-col min-h-0 py-3.5 px-4 rounded-xl">
    <div class="tech-card-glow"></div>
    <h3 class="card-title text-[15px] font-semibold mb-2 m-0 text-[var(--home-text-primary)]">
      {{ $t('pages.dashboard.quickActions') }}</h3>
    <div class="grid grid-cols-2 gap-2 mt-2">
      <div
          v-for="(item, index) in quickActionItems"
          :key="item.key"
          class="action-item flex items-center gap-2.5 py-2.5 px-3 rounded-[10px] transition-all duration-200 action-item-theme"
          :class="index === 0 ? 'action-item-first' : ''"
          role="button"
          tabindex="0"
          @click="handleQuickAction(item.key)"
          @keyup.enter="handleQuickAction(item.key)"
      >
        <div
            class="action-icon-wrap w-10 h-10 rounded-[10px] flex items-center justify-center flex-shrink-0"
            :class="index === 0 ? 'action-icon-primary' : 'action-icon-default'"
        >
          <el-icon class="text-xl">
            <component :is="item.icon"/>
          </el-icon>
        </div>
        <span class="action-label text-sm font-semibold text-[var(--home-text-primary)]">{{
            $t(item.labelKey)
          }}</span>
      </div>
    </div>
  </div>
</template>
