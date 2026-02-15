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

    <!-- 列表：一行一个用户 -->
    <div v-loading="loading" class="account-list flex flex-col gap-3">
      <div
          v-for="user in list"
          :key="user.id"
          class="account-row flex items-center gap-4 px-4 py-3 rounded-xl border border-[var(--home-border)] bg-[var(--home-card-bg)]"
      >
        <img
            :src="avatarUrl(user)"
            :alt="user.username"
            class="w-12 h-12 rounded-full object-cover flex-shrink-0 border border-[var(--home-border)]"
        />
        <div class="row-fields flex flex-wrap items-center gap-x-6 gap-y-1 flex-1 min-w-0">
          <span class="field"><span class="field-label">{{ $t('pages.admin.id') }}:</span> {{ user.id }}</span>
          <span class="field"><span class="field-label">{{ $t('header.username') }}:</span> {{ user.username }}</span>
          <span class="field"><span class="field-label">{{ $t('header.gender') }}:</span> {{ genderLabel(user.gender) }}</span>
          <span class="field"><span class="field-label">{{ $t('header.email') }}:</span> {{
              user.email || notSet()
            }}</span>
          <span class="field"><span class="field-label">{{
              $t('header.telephone')
            }}:</span> {{ user.telephone || notSet() }}</span>
          <span class="field"><span class="field-label">{{ $t('pages.admin.role') }}:</span> {{ user.role || notSet() }}</span>
          <span class="field"><span class="field-label">{{ $t('header.birthday') }}:</span> {{
              user.birthday || notSet()
            }}</span>
          <span class="field"><span class="field-label">{{
              $t('header.age')
            }}:</span> {{ user.age != null ? user.age : notSet() }}</span>
          <span class="field"><span class="field-label">{{
              $t('pages.admin.createTime')
            }}:</span> {{ user.createTime || '—' }}</span>
          <span class="field"><span class="field-label">{{
              $t('pages.admin.updateTime')
            }}:</span> {{ user.updateTime || '—' }}</span>
        </div>
        <el-button type="danger" size="small" :icon="Delete" text @click="handleDelete(user)">
          {{ $t('pages.admin.deleteUser') }}
        </el-button>
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
          <el-input v-model="createForm.username" :placeholder="$t('header.username')" maxlength="32" show-word-limit/>
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

.field {
  font-size: 13px;
  color: var(--home-text-primary);
}

.field-label {
  color: var(--home-text-muted);
  margin-right: 4px;
}

.account-row {
  transition: background 0.2s;
}

.account-row:hover {
  background: var(--home-hover-bg) !important;
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
