<script setup lang="ts">
import {ref, onMounted, onUnmounted, computed} from 'vue'
import {useRouter} from 'vue-router'
import {
  DataAnalysis,
  ArrowDown,
  User,
  SwitchButton,
  Connection,
  DataLine,
  VideoPlay,
  EditPen,
  Camera,
  Loading
} from '@element-plus/icons-vue'
import {ElMessage} from 'element-plus'
import {logout} from '@/api/auth'
import {getSystemResources, type SystemResources} from '@/api/home'
import {getUserInfo} from '@/api/user'
import {
  getAccountInfo,
  updateAccount,
  uploadAvatar,
  type AccountVO,
  type GenderCode,
  type UpdateAccountRO
} from '@/api/account'
import ThemeSwitch from '@/components/ThemeSwitch.vue'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import {useI18n} from 'vue-i18n'

const router = useRouter()
const {t} = useI18n()
const loggingOut = ref(false)
const userInfo = getUserInfo()

// 用户信息面板
const profileVisible = ref(false)
const accountInfo = ref<AccountVO | null>(null)
const loadingAccount = ref(false)
const editing = ref(false)
const saving = ref(false)
const uploadingAvatar = ref(false)

// 编辑表单
const form = ref<UpdateAccountRO>({
  username: '',
  gender: undefined,
  telephone: '',
  birthday: ''
})

// 默认头像：无头像时使用 UI Avatars 开源接口
const defaultAvatarUrl = (name: string) => {
  const n = (name || 'U').trim() || 'U'
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(n)}&background=6366f1&color=fff&size=128`
}

// 当前展示的头像 URL（用户头像为空则用默认）
const headerAvatarUrl = computed(() => {
  const info = accountInfo.value
  const name = info?.username || userInfo?.username || 'U'
  if (info?.avatar) return info.avatar
  return defaultAvatarUrl(name)
})

const panelAvatarUrl = computed(() => {
  const info = accountInfo.value
  const name = info?.username || userInfo?.username || 'U'
  if (info?.avatar) return info.avatar
  return defaultAvatarUrl(name)
})

// 打开用户信息面板时拉取/刷新数据
const openProfile = () => {
  profileVisible.value = true
  editing.value = false
  loadAccountInfo()
}

const loadAccountInfo = () => {
  loadingAccount.value = true
  getAccountInfo(
      (data) => {
        accountInfo.value = data
        form.value = {
          username: data.username,
          gender: data.gender,
          telephone: data.telephone ?? '',
          birthday: data.birthday ?? ''
        }
        loadingAccount.value = false
      },
      () => {
        loadingAccount.value = false
      }
  )
}

// 性别展示文案
const genderLabel = (code: GenderCode | undefined): string => {
  if (code === undefined) return t('header.unknown')
  if (code === 1) return t('header.male')
  if (code === 2) return t('header.female')
  return t('header.unknown')
}

// 生日/年龄未设置时显示
const notSet = () => t('header.notSet')

// 进入编辑
const startEdit = () => {
  editing.value = true
}

// 取消编辑
const cancelEdit = () => {
  editing.value = false
  if (accountInfo.value) {
    form.value = {
      username: accountInfo.value.username,
      gender: accountInfo.value.gender,
      telephone: accountInfo.value.telephone ?? '',
      birthday: accountInfo.value.birthday ?? ''
    }
  }
}

// 保存修改
const saveProfile = () => {
  saving.value = true
  updateAccount(
      form.value,
      () => {
        ElMessage.success(t('header.save') + ' ' + (locale.value === 'zh-CN' ? '成功' : 'Success'))
        saving.value = false
        editing.value = false
        loadAccountInfo()
      },
      () => {
        saving.value = false
      }
  )
}

// 头像上传
const avatarInput = ref<HTMLInputElement | null>(null)
const triggerAvatarUpload = () => avatarInput.value?.click()

const onAvatarFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  const accept = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  if (!accept.includes(file.type)) {
    ElMessage.warning(locale.value === 'zh-CN' ? '请选择 jpg/png/gif/webp 图片' : 'Please choose jpg/png/gif/webp image')
    target.value = ''
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning(locale.value === 'zh-CN' ? '图片大小不能超过 5MB' : 'Image size must not exceed 5MB')
    target.value = ''
    return
  }
  uploadingAvatar.value = true
  uploadAvatar(
      file,
      (avatarUrl) => {
        uploadingAvatar.value = false
        if (accountInfo.value) accountInfo.value = {...accountInfo.value, avatar: avatarUrl}
        ElMessage.success(locale.value === 'zh-CN' ? '头像已更新' : 'Avatar updated')
        target.value = ''
      },
      () => {
        uploadingAvatar.value = false
        target.value = ''
      }
  )
}

// 当前语言（用于上传/保存的提示）
const locale = computed(() => (t('header.notSet') === '未设置' ? 'zh-CN' : 'en-US'))

// 退出登录
const handleLogout = () => {
  loggingOut.value = true
  logout(
      () => {
        loggingOut.value = false
        router.push('/login')
      },
      () => (loggingOut.value = false)
  )
}

const openGitHub = () => {
  window.open('https://github.com/JackieLinn/MHFL-VS', '_blank', 'noopener,noreferrer')
}

// 系统资源
const systemResources = ref<SystemResources | null>(null)
const loadingResources = ref(false)
let resourceTimer: number | null = null

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

const formatPercent = (value: number | undefined): string => {
  if (value === undefined) return '0%'
  return `${value.toFixed(1)}%`
}

const formatGB = (value: number | undefined): string => {
  if (value === undefined) return '0.0'
  return value.toFixed(1)
}

const getUsageColor = (percent: number | undefined): string => {
  if (percent === undefined) return '#909399'
  if (percent < 50) return '#67c23a'
  if (percent < 80) return '#e6a23c'
  return '#f56c6c'
}

fetchSystemResources()
onMounted(() => {
  loadAccountInfo()
  resourceTimer = window.setInterval(fetchSystemResources, 3000)
})
onUnmounted(() => {
  if (resourceTimer != null) clearInterval(resourceTimer)
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
      <div class="flex items-center gap-5 pr-4 mr-2 border-r border-[var(--home-border)]">
        <div
            v-if="systemResources?.cpu"
            class="flex items-center gap-2 px-2 py-1 rounded-md transition-colors hover:bg-[var(--home-hover-bg)]"
        >
          <el-icon class="text-lg" :color="getUsageColor(systemResources.cpu.usagePercent)">
            <Connection/>
          </el-icon>
          <div class="flex flex-col gap-0.5">
            <span class="text-xs text-[var(--home-text-muted)] leading-none">{{ $t('home.cpu') }}</span>
            <span
                class="text-sm font-semibold leading-none"
                :style="{ color: getUsageColor(systemResources.cpu.usagePercent) }"
            >
              {{ formatPercent(systemResources.cpu.usagePercent) }}
            </span>
            <span class="text-xs text-[var(--home-text-muted)] leading-none">
              {{ systemResources.cpu.cores }} {{ $t('home.cores') }}
              {{
                systemResources.cpu.coresLogical !== systemResources.cpu.cores
                    ? ` / ${systemResources.cpu.coresLogical}${$t('home.threads')}`
                    : ''
              }}
            </span>
          </div>
        </div>
        <div
            v-if="systemResources?.memory"
            class="flex items-center gap-2 px-2 py-1 rounded-md transition-colors hover:bg-[var(--home-hover-bg)]"
        >
          <el-icon class="text-lg" :color="getUsageColor(systemResources.memory.usagePercent)">
            <DataLine/>
          </el-icon>
          <div class="flex flex-col gap-0.5">
            <span class="text-xs text-[var(--home-text-muted)] leading-none">{{ $t('home.memory') }}</span>
            <span
                class="text-sm font-semibold leading-none"
                :style="{ color: getUsageColor(systemResources.memory.usagePercent) }"
            >
              {{ formatPercent(systemResources.memory.usagePercent) }}
            </span>
            <span class="text-xs text-[var(--home-text-muted)] leading-none">
              {{ formatGB(systemResources.memory.used) }} / {{ formatGB(systemResources.memory.total) }} GB
            </span>
          </div>
        </div>
        <div
            v-if="systemResources?.gpu"
            class="flex items-center gap-2 px-2 py-1 rounded-md transition-colors hover:bg-[var(--home-hover-bg)]"
        >
          <el-icon class="text-lg" :color="getUsageColor(systemResources.gpu.usagePercent)">
            <VideoPlay/>
          </el-icon>
          <div class="flex flex-col gap-0.5">
            <span class="text-xs text-[var(--home-text-muted)] leading-none">{{ $t('home.gpu') }}</span>
            <span
                class="text-sm font-semibold leading-none"
                :style="{ color: getUsageColor(systemResources.gpu.usagePercent) }"
            >
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

      <el-dropdown trigger="click" @visible-change="(v: boolean) => v && loadAccountInfo()">
        <div class="user-dropdown">
          <el-avatar :size="32" class="user-avatar" :src="headerAvatarUrl">
            {{ (accountInfo?.username || userInfo?.username || 'U').charAt(0).toUpperCase() }}
          </el-avatar>
          <span class="user-name">{{ accountInfo?.username || userInfo?.username || $t('common.user') }}</span>
          <el-icon class="dropdown-arrow">
            <ArrowDown/>
          </el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu class="header-dropdown-menu">
            <el-dropdown-item @click="openProfile">
              <el-icon class="mr-2">
                <User/>
              </el-icon>
              {{ $t('header.userProfile') }}
            </el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">
              <el-icon class="mr-2">
                <SwitchButton/>
              </el-icon>
              {{ $t('header.logout') }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 用户信息抽屉：挂载到 body，避免被 header 及父级 overflow 裁剪 -->
    <el-drawer
        v-model="profileVisible"
        :title="$t('header.userProfile')"
        direction="rtl"
        size="400px"
        append-to-body
        class="profile-drawer"
    >
      <div v-loading="loadingAccount" class="profile-drawer-content">
        <template v-if="accountInfo">
          <!-- 头像区 -->
          <div class="flex flex-col items-center gap-3 mb-6">
            <div class="avatar-wrap relative" @click="triggerAvatarUpload">
              <img
                  :src="panelAvatarUrl"
                  alt="avatar"
                  class="w-24 h-24 rounded-full object-cover border-2 border-[var(--home-border)] bg-[var(--home-hover-bg)]"
              />
              <div
                  class="absolute inset-0 rounded-full flex items-center justify-center bg-black/40 opacity-0 hover:opacity-100 transition-opacity cursor-pointer"
              >
                <el-icon v-if="!uploadingAvatar" :size="28" color="#fff">
                  <Camera/>
                </el-icon>
                <el-icon v-else class="is-loading" :size="28" color="#fff">
                  <Loading/>
                </el-icon>
              </div>
            </div>
            <input
                ref="avatarInput"
                type="file"
                accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
                class="hidden"
                @change="onAvatarFileChange"
            />
            <span class="text-sm text-[var(--home-text-muted)]">{{ $t('header.changeAvatar') }}</span>
          </div>

          <!-- 查看模式 -->
          <div v-if="!editing" class="profile-fields space-y-4">
            <div class="profile-row">
              <span class="profile-label">{{ $t('header.username') }}</span>
              <span class="profile-value text-[var(--home-text-primary)]">{{ accountInfo.username }}</span>
            </div>
            <div class="profile-row">
              <span class="profile-label">{{ $t('header.gender') }}</span>
              <span class="profile-value text-[var(--home-text-primary)]">{{ genderLabel(accountInfo.gender) }}</span>
            </div>
            <div class="profile-row">
              <span class="profile-label">{{ $t('header.email') }}</span>
              <span class="profile-value text-[var(--home-text-primary)]">{{ accountInfo.email || notSet() }}</span>
            </div>
            <div class="profile-row">
              <span class="profile-label">{{ $t('header.telephone') }}</span>
              <span class="profile-value text-[var(--home-text-primary)]">{{ accountInfo.telephone || notSet() }}</span>
            </div>
            <div class="profile-row">
              <span class="profile-label">{{ $t('header.birthday') }}</span>
              <span class="profile-value text-[var(--home-text-primary)]">{{ accountInfo.birthday || notSet() }}</span>
            </div>
            <div class="profile-row">
              <span class="profile-label">{{ $t('header.age') }}</span>
              <span class="profile-value text-[var(--home-text-primary)]">
                {{ accountInfo.age != null ? accountInfo.age : notSet() }}
              </span>
            </div>
            <el-button type="primary" class="w-full mt-4" :icon="EditPen" @click="startEdit">
              {{ $t('header.edit') }}
            </el-button>
          </div>

          <!-- 编辑模式 -->
          <div v-else class="profile-edit space-y-4">
            <el-form label-position="top" class="profile-form">
              <el-form-item :label="$t('header.username')">
                <el-input v-model="form.username" :placeholder="$t('header.username')" maxlength="30" show-word-limit/>
              </el-form-item>
              <el-form-item :label="$t('header.gender')">
                <el-select v-model="form.gender" :placeholder="$t('header.gender')" class="w-full">
                  <el-option :label="$t('header.unknown')" :value="0"/>
                  <el-option :label="$t('header.male')" :value="1"/>
                  <el-option :label="$t('header.female')" :value="2"/>
                </el-select>
              </el-form-item>
              <el-form-item :label="$t('header.telephone')">
                <el-input v-model="form.telephone" :placeholder="$t('header.telephone')" maxlength="20"/>
              </el-form-item>
              <el-form-item :label="$t('header.birthday')">
                <el-date-picker
                    v-model="form.birthday"
                    type="date"
                    :placeholder="$t('header.birthday')"
                    value-format="YYYY-MM-DD"
                    class="w-full"
                />
              </el-form-item>
            </el-form>
            <div class="flex gap-3 mt-4">
              <el-button class="flex-1" @click="cancelEdit">{{ $t('header.cancel') }}</el-button>
              <el-button type="primary" class="flex-1" :loading="saving" @click="saveProfile">
                {{ $t('header.save') }}
              </el-button>
            </div>
          </div>
        </template>
      </div>
    </el-drawer>
  </header>
</template>

<style scoped>
.home-header {
  height: 64px;
  background: var(--home-header-bg);
  backdrop-filter: blur(16px) saturate(1.5);
  border-bottom: 1px solid var(--home-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
  position: relative;
}

.home-header::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 5%;
  right: 5%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.2), transparent);
  pointer-events: none;
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
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.header-logo:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.35);
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
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid transparent;
}

.user-dropdown:hover {
  background: var(--home-hover-bg);
  border-color: var(--home-border);
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

.avatar-wrap {
  cursor: pointer;
}

.profile-drawer-content {
  padding: 0 4px;
}

.profile-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--home-hover-bg);
}

.profile-label {
  font-size: 13px;
  color: var(--home-text-muted);
  margin-right: 12px;
}

.profile-value {
  font-size: 14px;
  font-weight: 500;
}

.profile-form :deep(.el-form-item__label) {
  color: var(--home-text-secondary);
}

.profile-drawer :deep(.el-drawer__header) {
  color: var(--home-text-primary);
  margin-bottom: 16px;
}

.profile-drawer :deep(.el-drawer__body) {
  padding: 0 20px 20px;
}

:deep(.el-avatar) {
  font-size: 14px;
}
</style>
