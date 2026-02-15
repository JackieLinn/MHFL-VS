<script setup lang="ts">
import {ref, computed, onMounted} from 'vue'
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

// 分页（后端默认 10 一页）
const currentPage = ref(1)
const pageSize = 10
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
        size: pageSize
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
</script>

<template>
  <div class="account-manage">
    <!-- 搜索栏 -->
    <div class="search-bar flex flex-wrap items-end gap-4 mb-4">
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

    <!-- 列表：一行一个用户，网格对齐，角色底色区分 -->
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

    <!-- 分页 -->
    <div class="pagination-wrap flex items-center justify-between mt-4">
      <span class="text-sm text-[var(--home-text-muted)]">{{ totalText }}</span>
      <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="fetchList"
      />
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
.search-bar :deep(.el-input__wrapper),
.search-bar :deep(.el-date-editor) {
  background: var(--home-card-bg);
  border-color: var(--home-border);
}

/* 一行一个用户：网格对齐 */
.account-row {
  display: grid;
  grid-template-columns: 52px 56px 96px 56px 1fr 1fr 88px 96px 48px 150px 150px auto;
  gap: 12px 16px;
  align-items: center;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid var(--home-border);
  transition: background 0.2s, border-color 0.2s;
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

.pagination-wrap :deep(.el-pagination) {
  --el-pagination-bg-color: var(--home-card-bg);
  --el-pagination-button-color: var(--home-text-primary);
  --el-pagination-text-color: var(--home-text-secondary);
}

.create-dialog :deep(.el-dialog__header),
.create-dialog :deep(.el-dialog__body),
.create-dialog :deep(.el-form-item__label) {
  color: var(--home-text-primary);
}

</style>
