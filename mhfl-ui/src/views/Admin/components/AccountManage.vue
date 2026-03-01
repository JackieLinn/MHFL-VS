<script setup lang="ts">
import {ref, computed, onMounted, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Search, Plus, Delete} from '@element-plus/icons-vue'
import {
  listAccountsAdmin,
  createAccountAdmin,
  deleteAccountAdmin,
  type AccountVO,
  type GenderCode,
  type CreateAccountRO
} from '@/api/account'

const {t} = useI18n()

const defaultAvatarUrl = (name: string) => {
  const n = (name || 'U').trim() || 'U'
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(n)}&background=6366f1&color=fff&size=40`
}

const avatarUrl = (user: AccountVO) => (user.avatar ? user.avatar : defaultAvatarUrl(user.username))

const genderLabel = (code: GenderCode | undefined): string => {
  if (code === undefined) return t('header.unknown')
  if (code === 1) return t('header.male')
  if (code === 2) return t('header.female')
  return t('header.unknown')
}

const notSet = () => t('header.notSet')

// 搜索条件
const keyword = ref('')
const startTime = ref('')
const endTime = ref('')

// 分页
const currentPage = ref(1)
const pageSizeOptions = [5, 10, 20, 50] as const
const pageSize = ref(10)
const total = ref(0)
const list = ref<AccountVO[]>([])
const loading = ref(false)

const fetchList = () => {
  loading.value = true
  listAccountsAdmin(
      {
        keyword: keyword.value.trim() || undefined,
        startTime: startTime.value || undefined,
        endTime: endTime.value || undefined,
        current: currentPage.value,
        size: pageSize.value
      },
      (data) => {
        list.value = data.records
        total.value = data.total
        loading.value = false
      },
      () => {
        loading.value = false
      }
  )
}

const onSearch = () => {
  currentPage.value = 1
  fetchList()
}

// 监听关键字变化：清空时自动刷新
watch(keyword, (newVal) => {
  if (!newVal || newVal.trim() === '') {
    // 清空关键字时自动刷新，重置到第一页
    currentPage.value = 1
    fetchList()
  }
})

onMounted(() => {
  fetchList()
})

// 创建用户
const createDialogVisible = ref(false)
const createForm = ref<CreateAccountRO>({username: '', email: '', telephone: ''})
const createSubmitting = ref(false)

const openCreate = () => {
  createForm.value = {username: '', email: '', telephone: ''}
  createDialogVisible.value = true
}

const submitCreate = () => {
  const {username, email, telephone} = createForm.value
  if (!username?.trim()) {
    ElMessage.warning(t('login.validation.usernameRequired'))
    return
  }
  if (!email?.trim()) {
    ElMessage.warning(t('login.validation.emailRequired'))
    return
  }
  if (!telephone?.trim()) {
    ElMessage.warning(t('login.validation.phoneRequired'))
    return
  }
  createSubmitting.value = true
  createAccountAdmin(
      {username: username.trim(), email: email.trim(), telephone: telephone.trim()},
      () => {
        createSubmitting.value = false
        createDialogVisible.value = false
        ElMessage.success(t('pages.admin.createSuccess'))
        fetchList()
      },
      () => {
        createSubmitting.value = false
      }
  )
}

// 删除用户
const handleDelete = (user: AccountVO) => {
  ElMessageBox.confirm(t('pages.admin.confirmDelete'), t('pages.admin.deleteUser'), {
    confirmButtonText: t('pages.admin.confirm'),
    cancelButtonText: t('header.cancel'),
    type: 'warning'
  }).then(() => {
    deleteAccountAdmin(user.id, () => {
      ElMessage.success(t('pages.admin.deleteSuccess'))
      fetchList()
    })
  }).catch(() => {
  })
}

const totalText = computed(() => t('pages.admin.total', {total: total.value}))

const onPageSizeChange = () => {
  currentPage.value = 1
  fetchList()
}
</script>

<template>
  <div class="account-manage">
    <!-- 顶部固定：搜索栏、搜索、导入用户 -->
    <div class="account-manage-header flex flex-wrap items-end gap-4">
      <el-input
          v-model="keyword"
          :placeholder="$t('pages.admin.searchKeyword')"
          clearable
          class="search-keyword w-56"
          @keyup.enter="onSearch"
      />
      <el-date-picker
          v-model="startTime"
          type="date"
          :placeholder="$t('pages.admin.searchStartTime')"
          value-format="YYYY-MM-DD"
          class="w-40"
          clearable
      />
      <el-date-picker
          v-model="endTime"
          type="date"
          :placeholder="$t('pages.admin.searchEndTime')"
          value-format="YYYY-MM-DD"
          class="w-40"
          clearable
      />
      <el-button type="primary" :icon="Search" @click="onSearch">{{ $t('pages.admin.search') }}</el-button>
      <el-button type="success" :icon="Plus" @click="openCreate">{{ $t('pages.admin.createUser') }}</el-button>
    </div>

    <!-- 中间可滚动：列表 -->
    <div v-loading="loading" class="account-list flex flex-col gap-3">
      <div
          v-for="user in list"
          :key="user.id"
          class="account-row"
          :class="user.role === 'admin' ? 'role-admin' : 'role-user'"
      >
        <div class="row-avatar">
          <img
              :src="avatarUrl(user)"
              :alt="user.username"
              class="avatar-img"
          />
        </div>
        <div class="row-cell cell-id">
          <span class="cell-label">{{ $t('pages.admin.id') }}</span>
          <span class="cell-value">{{ user.id }}</span>
        </div>
        <div class="row-cell cell-username">
          <span class="cell-label">{{ $t('header.username') }}</span>
          <span class="cell-value text-ellipsis" :title="user.username">{{ user.username }}</span>
        </div>
        <div class="row-cell cell-gender">
          <span class="cell-label">{{ $t('header.gender') }}</span>
          <span class="cell-value">{{ genderLabel(user.gender) }}</span>
        </div>
        <div class="row-cell cell-email">
          <span class="cell-label">{{ $t('header.email') }}</span>
          <span class="cell-value text-ellipsis" :title="user.email || ''">{{ user.email || notSet() }}</span>
        </div>
        <div class="row-cell cell-telephone">
          <span class="cell-label">{{ $t('header.telephone') }}</span>
          <span class="cell-value text-ellipsis" :title="user.telephone || ''">{{ user.telephone || notSet() }}</span>
        </div>
        <div class="row-cell cell-role">
          <span class="cell-label">{{ $t('pages.admin.role') }}</span>
          <span class="role-badge" :class="user.role === 'admin' ? 'badge-admin' : 'badge-user'">
            {{ user.role === 'admin' ? $t('pages.admin.roleAdmin') : $t('pages.admin.roleUser') }}
          </span>
        </div>
        <div class="row-cell cell-birthday">
          <span class="cell-label">{{ $t('header.birthday') }}</span>
          <span class="cell-value">{{ user.birthday || notSet() }}</span>
        </div>
        <div class="row-cell cell-age">
          <span class="cell-label">{{ $t('header.age') }}</span>
          <span class="cell-value">{{ user.age != null ? user.age : notSet() }}</span>
        </div>
        <div class="row-cell cell-create">
          <span class="cell-label">{{ $t('pages.admin.createTime') }}</span>
          <span class="cell-value text-ellipsis" :title="user.createTime || ''">{{ user.createTime || '—' }}</span>
        </div>
        <div class="row-cell cell-update">
          <span class="cell-label">{{ $t('pages.admin.updateTime') }}</span>
          <span class="cell-value text-ellipsis" :title="user.updateTime || ''">{{ user.updateTime || '—' }}</span>
        </div>
        <div class="row-action">
          <el-button type="danger" size="small" :icon="Delete" text @click="handleDelete(user)">
            {{ $t('pages.admin.deleteUser') }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 底部固定：每页条数、共 x 条、分页（一行不换行） -->
    <div class="account-manage-footer flex items-center justify-between gap-4 flex-nowrap">
      <div class="footer-left flex items-center gap-3 flex-shrink-0">
        <span class="text-sm text-[var(--home-text-muted)] whitespace-nowrap">{{ $t('pages.admin.pageSize') }}</span>
        <el-select v-model="pageSize" class="page-size-select w-24 flex-shrink-0" @change="onPageSizeChange">
          <el-option v-for="n in pageSizeOptions" :key="n" :label="`${n}`" :value="n"/>
        </el-select>
        <span class="text-sm text-[var(--home-text-muted)] whitespace-nowrap">{{ totalText }}</span>
      </div>
      <div class="flex-shrink-0">
        <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            background
            @current-change="fetchList"
        />
      </div>
    </div>

    <!-- 创建用户弹窗 -->
    <el-dialog
        v-model="createDialogVisible"
        :title="$t('pages.admin.createUserTitle')"
        width="420px"
        class="create-dialog"
        @close="createForm = { username: '', email: '', telephone: '' }"
    >
      <el-form label-position="top" :model="createForm">
        <el-form-item :label="$t('header.username')" required>
          <el-input v-model="createForm.username" :placeholder="$t('header.username')" maxlength="30" show-word-limit/>
        </el-form-item>
        <el-form-item :label="$t('header.email')" required>
          <el-input v-model="createForm.email" type="email" :placeholder="$t('header.email')"/>
        </el-form-item>
        <el-form-item :label="$t('header.telephone')" required>
          <el-input v-model="createForm.telephone" :placeholder="$t('header.telephone')" maxlength="20"/>
        </el-form-item>
        <p class="text-xs text-[var(--home-text-muted)] mb-0">{{ $t('pages.admin.defaultPasswordHint') }}</p>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">{{ $t('header.cancel') }}</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">{{ $t('header.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 整体：顶部/底部固定，中间滚动 */
.account-manage {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding-bottom: 3px;
}

.account-manage-header {
  flex-shrink: 0;
  margin-bottom: 16px;
}

.account-manage-header :deep(.el-input__wrapper),
.account-manage-header :deep(.el-date-editor) {
  background: var(--home-card-bg);
  border-color: var(--home-border);
}

.account-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding-right: 4px;
}

.account-manage-footer {
  flex-shrink: 0;
  margin-top: 16px;
  padding-top: 12px;
  padding-bottom: 12px;
  border-top: 1px solid var(--home-border);
}

/* 一行一个用户：网格对齐，右侧列也参与伸缩，侧栏收起时整行均匀变宽 */
.account-row {
  display: grid;
  grid-template-columns: 52px 56px 96px 56px 1fr 1fr minmax(88px, 1fr) minmax(96px, 1fr) 48px minmax(140px, 1fr) minmax(140px, 1fr) auto;
  gap: 12px 16px;
  align-items: center;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid var(--home-border);
  transition: all 0.25s ease;
}

.account-row.role-admin {
  background: var(--admin-role-bg);
  border-color: var(--admin-role-border);
}

.account-row.role-user {
  background: var(--user-role-bg);
  border-color: var(--user-role-border);
}

.account-row:hover {
  filter: brightness(0.97);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.row-avatar {
  grid-column: 1;
}

.avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--home-border);
}

.row-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.cell-label {
  font-size: 11px;
  color: var(--home-text-muted);
  line-height: 1.2;
}

.cell-value {
  font-size: 13px;
  color: var(--home-text-primary);
  line-height: 1.3;
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  width: fit-content;
}

.badge-admin {
  background: var(--admin-badge-bg);
  color: var(--admin-badge-text);
}

.badge-user {
  background: var(--user-badge-bg);
  color: var(--user-badge-text);
}

.row-action {
  justify-self: end;
}

.account-manage-footer :deep(.el-pagination) {
  --el-pagination-bg-color: var(--home-card-bg);
  --el-pagination-button-color: var(--home-text-primary);
  --el-pagination-text-color: var(--home-text-secondary);
}

.account-manage-footer :deep(.page-size-select .el-input__wrapper) {
  background: var(--home-card-bg);
  border-color: var(--home-border);
}

.create-dialog :deep(.el-dialog__header),
.create-dialog :deep(.el-dialog__body),
.create-dialog :deep(.el-form-item__label) {
  color: var(--home-text-primary);
}

</style>
