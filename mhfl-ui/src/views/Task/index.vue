<script setup lang="ts">
import {ref, computed, onMounted, watch} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Search, Plus} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import BackToTop from '@/components/BackToTop.vue'
import CreateTaskDialog from './components/CreateTaskDialog.vue'
import TaskCard from './components/TaskCard.vue'
import {getUserInfo} from '@/api/user'
import {listTasks, deleteTask, type TaskVO} from '@/api/task'
import {usePageSize} from '@/composables/usePageSize'
import {useTaskSortPreference, type TimeSortOrder} from '@/composables/useTaskSortPreference'

const router = useRouter()
const {t} = useI18n()

const isAdmin = computed(() => getUserInfo()?.role === 'admin')

// 搜索条件
const keyword = ref('')
const startTime = ref('')
const endTime = ref('')
const {createTimeSort, updateTimeSort} = useTaskSortPreference('task')

// 分页
const currentPage = ref(1)
const {pageSize, pageSizeOptions} = usePageSize('task')
const total = ref(0)
const list = ref<TaskVO[]>([])
const loading = ref(false)

const baseSortOptions = computed(() => [
  {label: t('pages.task.sortDefault'), value: 'DEFAULT' as TimeSortOrder},
  {label: t('pages.task.sortAsc'), value: 'ASC' as TimeSortOrder},
  {label: t('pages.task.sortDesc'), value: 'DESC' as TimeSortOrder}
])

const createSortOptions = computed(() =>
    baseSortOptions.value.map((option) => ({
      value: option.value,
      label: `${t('pages.task.createTimeSort')} - ${option.label}`
    }))
)

const updateSortOptions = computed(() =>
    baseSortOptions.value.map((option) => ({
      value: option.value,
      label: `${t('pages.task.updateTimeSort')} - ${option.label}`
    }))
)

const parseTimeToStamp = (value?: string | null) => {
  if (!value) return 0
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const stamp = new Date(normalized).getTime()
  return Number.isNaN(stamp) ? 0 : stamp
}

const sortByTimeField = (
    field: 'createTime' | 'updateTime',
    order: Exclude<TimeSortOrder, 'DEFAULT'>
) => {
  const direction = order === 'ASC' ? 1 : -1
  return [...list.value].sort((a, b) => {
    const diff = parseTimeToStamp(a[field]) - parseTimeToStamp(b[field])
    if (diff !== 0) return diff * direction
    return a.id - b.id
  })
}

const sortedList = computed(() => {
  if (createTimeSort.value !== 'DEFAULT') {
    return sortByTimeField('createTime', createTimeSort.value)
  }
  if (updateTimeSort.value !== 'DEFAULT') {
    return sortByTimeField('updateTime', updateTimeSort.value)
  }
  return list.value
})

const fetchList = () => {
  loading.value = true
  listTasks(
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

watch(keyword, (newVal) => {
  if (!newVal || newVal.trim() === '') {
    currentPage.value = 1
    fetchList()
  }
})

onMounted(() => {
  fetchList()
})

const totalText = computed(() => t('pages.task.total', {total: total.value}))

const onPageSizeChange = () => {
  currentPage.value = 1
  fetchList()
}

const isEmpty = computed(() => !loading.value && list.value.length === 0)

/** 查看详情：跳转任务详情页 */
const handleViewDetail = (task: TaskVO) => {
  router.push({name: 'TaskDetail', params: {id: String(task.id)}})
}

const createDialogVisible = ref(false)

const handleCreateTask = () => {
  createDialogVisible.value = true
}

const onTaskCreated = () => {
  fetchList()
}

const onRecommendSuccess = () => {
  ElMessage.success(t('pages.task.recommendSuccess'))
  fetchList()
}

const handleDelete = (task: TaskVO) => {
  ElMessageBox.confirm(t('pages.task.deleteConfirm'), t('common.confirm'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning'
  }).then(() => {
    deleteTask(
        task.id,
        () => {
          ElMessage.success(t('pages.task.deleteSuccess'))
          fetchList()
        },
        () => {
        }
    )
  }).catch(() => {
  })
}
</script>

<template>
  <div class="task-manage">
    <div id="task-scroll" class="task-scroll">
      <PageHeader
          class="mb-5"
          :title="$t('pages.task.title')"
          :desc="$t('pages.task.desc')"
      />

      <!-- 搜索栏：滚动到顶部后 sticky -->
      <div class="task-manage-header">
        <div class="task-header-row task-header-row-keyword">
          <el-input
              v-model="keyword"
              :placeholder="$t('pages.task.searchKeyword')"
              clearable
              class="search-keyword w-56"
              @keyup.enter="onSearch"
          />
        </div>
        <div class="task-header-row task-header-row-actions">
          <el-date-picker
              v-model="startTime"
              type="date"
              :placeholder="$t('pages.task.searchStartTime')"
              value-format="YYYY-MM-DD"
              class="task-filter-control"
              clearable
          />
          <el-date-picker
              v-model="endTime"
              type="date"
              :placeholder="$t('pages.task.searchEndTime')"
              value-format="YYYY-MM-DD"
              class="task-filter-control"
              clearable
          />
          <el-select
              v-model="createTimeSort"
              class="task-filter-control"
              :placeholder="$t('pages.task.createTimeSort')"
          >
            <el-option
                v-for="option in createSortOptions"
                :key="`create-${option.value}`"
                :label="option.label"
                :value="option.value"
            />
          </el-select>
          <el-select
              v-model="updateTimeSort"
              class="task-filter-control"
              :placeholder="$t('pages.task.updateTimeSort')"
          >
            <el-option
                v-for="option in updateSortOptions"
                :key="`update-${option.value}`"
                :label="option.label"
                :value="option.value"
            />
          </el-select>
          <el-button class="task-action-btn" type="primary" :icon="Search" @click="onSearch">
            {{ $t('pages.task.search') }}
          </el-button>
          <el-button class="task-action-btn" type="success" :icon="Plus" @click="handleCreateTask">
            {{ $t('pages.task.createTask') }}
          </el-button>
        </div>
      </div>

      <!-- 任务列表（卡片式） -->
      <div v-loading="loading" class="task-list flex flex-col gap-3">
        <div v-if="isEmpty" class="task-empty">
          <div class="empty-icon"></div>
          <p class="empty-title">{{ $t('pages.task.noTasks') }}</p>
          <p class="empty-hint">{{ $t('pages.task.noTasksHint') }}</p>
        </div>

        <TaskCard
            v-for="task in sortedList"
            :key="task.id"
            :task="task"
            :is-admin="isAdmin"
            @view-detail="handleViewDetail"
            @delete="handleDelete"
            @recommend-success="onRecommendSuccess"
            @start-success="onTaskCreated"
        />
      </div>
    </div>

    <!-- 分页（固定在底部） -->
    <div v-if="!isEmpty" class="task-manage-footer flex items-center justify-between gap-4 flex-nowrap">
      <div class="footer-left flex items-center gap-3 flex-shrink-0">
        <span class="text-sm text-[var(--home-text-muted)] whitespace-nowrap">{{ $t('pages.task.pageSize') }}</span>
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

    <CreateTaskDialog v-model="createDialogVisible" @created="onTaskCreated"/>
    <BackToTop scroll-target="#task-scroll"/>
  </div>
</template>

<style scoped>
/* PageHeader 滚走，搜索栏 sticky，列表滚动，分页固定底部 */
.task-manage {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 32px 24px 16px 24px;
  padding-bottom: 3px;
}

.task-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding-right: 4px;
}

.task-manage-header {
  position: sticky;
  top: 0;
  z-index: 10;
  margin-bottom: 16px;
  padding-bottom: 4px;
  background: var(--home-bg);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-header-row {
  display: flex;
  align-items: flex-end;
  gap: 16px;
}

.task-header-row-actions {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) auto auto;
  align-items: end;
  width: 100%;
}

.task-filter-control {
  min-width: 0;
  width: 100%;
}

.task-header-row-actions :deep(.el-date-editor),
.task-header-row-actions :deep(.el-select) {
  width: 100%;
  min-width: 0;
}

.task-action-btn {
  width: 120px;
}

.task-manage-header :deep(.el-input__wrapper),
.task-manage-header :deep(.el-date-editor),
.task-manage-header :deep(.el-select__wrapper) {
  background: var(--home-card-bg);
  border-color: var(--home-border);
}

.task-list {
  padding-right: 4px;
}

.task-manage-footer {
  flex-shrink: 0;
  margin-top: 16px;
  padding-top: 12px;
  padding-bottom: 12px;
  border-top: 1px solid var(--home-border);
}

/* 空状态 */
.task-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 280px;
  color: var(--home-text-muted);
}

.empty-icon {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.08), rgba(139, 92, 246, 0.08));
  border: 1px solid rgba(99, 102, 241, 0.1);
  margin-bottom: 20px;
}

.empty-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--home-text-secondary);
  margin: 0 0 8px 0;
}

.empty-hint {
  font-size: 13px;
  color: var(--home-text-muted);
  margin: 0;
}

/* 分页样式 */
.task-manage-footer :deep(.el-pagination) {
  --el-pagination-bg-color: var(--home-card-bg);
  --el-pagination-button-color: var(--home-text-primary);
  --el-pagination-text-color: var(--home-text-secondary);
}

.task-manage-footer :deep(.page-size-select .el-input__wrapper) {
  background: var(--home-card-bg);
  border-color: var(--home-border);
}
</style>
