<script setup lang="ts">
import {ref, computed, onMounted, watch} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {Search, Plus, View} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import CreateTaskDialog from './components/CreateTaskDialog.vue'
import {listTasks, type TaskVO, type TaskStatusCode} from '@/api/task'

const router = useRouter()
const {t} = useI18n()

/** 状态码转显示文案 */
const statusLabel = (code: TaskStatusCode): string => {
  const map: Record<TaskStatusCode, string> = {
    0: t('pages.dashboard.statusNotStarted'),
    1: t('pages.dashboard.statusInProgress'),
    2: t('pages.dashboard.statusSuccess'),
    3: t('pages.dashboard.statusRecommended'),
    4: t('pages.dashboard.statusFailed'),
    5: t('pages.dashboard.statusCancelled')
  }
  return map[code] ?? '—'
}

/** 状态徽章样式类 */
const statusBadgeClass = (code: TaskStatusCode): string => {
  const map: Record<TaskStatusCode, string> = {
    0: 'status-info',
    1: 'status-warning',
    2: 'status-success',
    3: 'status-primary',
    4: 'status-danger',
    5: 'status-info'
  }
  return `status-badge ${map[code] ?? 'status-info'}`
}

/** Loss 展示：4 位小数；-1 表示占位，显示 — */
const formatLoss = (v: number | null | undefined): string => {
  if (v == null || v === -1) return '—'
  return Number.isFinite(v) ? v.toFixed(4) : '—'
}

/** 百分数展示：accuracy/precision/recall/f1Score，保留 2 位小数；-1 表示占位，显示 — */
const formatPercent = (v: number | null | undefined): string => {
  if (v == null || v === -1) return '—'
  return Number.isFinite(v) ? `${(v * 100).toFixed(2)}%` : '—'
}

/** 训练参数紧凑展示：节点数 · 比例 · 类别数 · lowProb · epochs · steps */
const paramsText = (row: TaskVO): string => {
  const n = row.numNodes ?? '—'
  const f = row.fraction != null ? row.fraction : '—'
  const c = row.classesPerNode ?? '—'
  const l = row.lowProb != null ? row.lowProb : '—'
  const e = row.epochs ?? '—'
  const s = row.numSteps ?? '—'
  return `${n}${t('pages.task.paramNodes')} · ${f} · ${c}${t('pages.task.paramClasses')} · ${l} · ${e}${t('pages.task.paramEpochs')} · ${s}${t('pages.task.paramSteps')}`
}

/** 指标紧凑展示：Loss 4 位小数，Acc/P/R/F1 百分数 2 位小数 */
const metricsText = (row: TaskVO): string => {
  const parts = [
    `Loss: ${formatLoss(row.loss)}`,
    `Acc: ${formatPercent(row.accuracy)}`,
    `P: ${formatPercent(row.precision)}`,
    `R: ${formatPercent(row.recall)}`,
    `F1: ${formatPercent(row.f1Score)}`
  ]
  return parts.join(' · ')
}

/** 创建者展示：UID + 用户名 */
const creatorText = (task: TaskVO): string => t('pages.task.creatorWithUid', {
  uid: task.uid,
  username: task.username || '—'
})

/** 时间一行展示 */
const timeText = (row: TaskVO): string => {
  const c = row.createTime || '—'
  const u = row.updateTime || '—'
  return `${t('pages.task.createTime')}: ${c}  |  ${t('pages.task.updateTime')}: ${u}`
}

// 搜索条件
const keyword = ref('')
const startTime = ref('')
const endTime = ref('')

// 分页
const currentPage = ref(1)
const pageSizeOptions = [5, 10, 20, 50] as const
const pageSize = ref(10)
const total = ref(0)
const list = ref<TaskVO[]>([])
const loading = ref(false)

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
</script>

<template>
  <div class="task-manage">
    <PageHeader
        class="mb-5"
        :title="$t('pages.task.title')"
        :desc="$t('pages.task.desc')"
    />

    <!-- 搜索栏 -->
    <div class="task-manage-header flex flex-wrap items-end gap-4">
      <el-input
          v-model="keyword"
          :placeholder="$t('pages.task.searchKeyword')"
          clearable
          class="search-keyword w-56"
          @keyup.enter="onSearch"
      />
      <el-date-picker
          v-model="startTime"
          type="date"
          :placeholder="$t('pages.task.searchStartTime')"
          value-format="YYYY-MM-DD"
          class="w-40"
          clearable
      />
      <el-date-picker
          v-model="endTime"
          type="date"
          :placeholder="$t('pages.task.searchEndTime')"
          value-format="YYYY-MM-DD"
          class="w-40"
          clearable
      />
      <el-button type="primary" :icon="Search" @click="onSearch">{{ $t('pages.task.search') }}</el-button>
      <el-button type="success" :icon="Plus" @click="handleCreateTask">{{ $t('pages.task.createTask') }}</el-button>
    </div>

    <!-- 任务列表（卡片式） -->
    <div v-loading="loading" class="task-list flex flex-col gap-3">
      <div v-if="isEmpty" class="task-empty">
        <div class="empty-icon"></div>
        <p class="empty-title">{{ $t('pages.task.noTasks') }}</p>
        <p class="empty-hint">{{ $t('pages.task.noTasksHint') }}</p>
      </div>

      <div
          v-for="task in list"
          :key="task.id"
          class="task-row"
      >
        <!-- 第一行：核心信息 + 查看详情 -->
        <div class="row-primary">
          <div class="row-cell cell-id">
            <span class="cell-label">{{ $t('pages.task.id') }}</span>
            <span class="cell-value">{{ task.id }}</span>
          </div>
          <div class="row-cell cell-dataName">
            <span class="cell-label">{{ $t('pages.task.dataName') }}</span>
            <span class="cell-value text-ellipsis font-semibold" :title="task.dataName">{{ task.dataName }}</span>
          </div>
          <div class="row-cell cell-algorithmName">
            <span class="cell-label">{{ $t('pages.task.algorithmName') }}</span>
            <span class="cell-value text-ellipsis" :title="task.algorithmName">{{ task.algorithmName }}</span>
          </div>
          <div class="row-cell cell-creator">
            <span class="cell-label">{{ $t('pages.task.username') }}</span>
            <span class="cell-value text-ellipsis" :title="creatorText(task)">{{ creatorText(task) }}</span>
          </div>
          <div class="row-cell cell-status">
            <span class="cell-label">{{ $t('pages.task.status') }}</span>
            <span :class="statusBadgeClass(task.status)">{{ statusLabel(task.status) }}</span>
          </div>
          <div class="row-action">
            <el-button type="primary" size="small" :icon="View" @click="handleViewDetail(task)">
              {{ $t('pages.task.viewDetail') }}
            </el-button>
          </div>
        </div>

        <!-- 第二行：训练参数、指标、时间 -->
        <div class="row-secondary">
          <div class="secondary-group params-group">
            <span class="group-label">{{ $t('pages.task.paramsLabel') }}</span>
            <span class="group-value text-ellipsis" :title="paramsText(task)">{{ paramsText(task) }}</span>
          </div>
          <div class="secondary-group metrics-group">
            <span class="group-label">{{ $t('pages.task.metricsLabel') }}</span>
            <span class="group-value text-ellipsis" :title="metricsText(task)">{{ metricsText(task) }}</span>
          </div>
          <div class="secondary-group time-group">
            <span class="group-label">{{ $t('pages.task.timeLabel') }}</span>
            <span class="group-value time-value" :title="timeText(task)">{{ timeText(task) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
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
  </div>
</template>

<style scoped>
/* 整体：顶部/底部固定，中间滚动（与 AccountManage 一致） */
.task-manage {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 32px 24px 16px 24px;
  padding-bottom: 3px;
}

.task-manage-header {
  flex-shrink: 0;
  margin-bottom: 16px;
}

.task-manage-header :deep(.el-input__wrapper),
.task-manage-header :deep(.el-date-editor) {
  background: var(--home-card-bg);
  border-color: var(--home-border);
}

.task-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
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

/* 任务卡片行（参考 AccountManage，增强视觉效果） */
.task-row {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 22px;
  border-radius: 14px;
  border: 1px solid var(--user-role-border);
  background: var(--user-role-bg);
  transition: all 0.25s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

html.dark .task-row {
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
}

.task-row:hover {
  filter: brightness(0.98);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.12);
  border-color: rgba(99, 102, 241, 0.25);
}

html.dark .task-row:hover {
  filter: brightness(1.06);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
  border-color: rgba(165, 180, 252, 0.2);
}

/* 第一行：核心信息网格 + 操作按钮 */
.row-primary {
  display: grid;
  grid-template-columns: 56px minmax(140px, 1.5fr) minmax(120px, 1.2fr) minmax(140px, 1.2fr) 100px auto;
  gap: 16px 24px;
  align-items: center;
}

.row-action {
  justify-self: end;
}

.row-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  align-items: center;
  text-align: center;
}

.row-cell .cell-value {
  width: 100%;
}

.cell-label {
  font-size: 11px;
  color: var(--home-text-muted);
  line-height: 1.2;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.cell-value {
  font-size: 14px;
  color: var(--home-text-primary);
  line-height: 1.4;
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
  width: 100%;
}

.cell-dataName .cell-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--home-text-primary);
}

/* 状态徽章（自定义，适配深色模式） */
.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  width: fit-content;
}

.status-info {
  background: rgba(100, 116, 139, 0.2);
  color: var(--user-badge-text);
}

.status-warning {
  background: rgba(245, 158, 11, 0.2);
  color: #d97706;
}

html.dark .status-warning {
  background: rgba(245, 158, 11, 0.25);
  color: #fbbf24;
}

.status-success {
  background: rgba(34, 197, 94, 0.2);
  color: #16a34a;
}

html.dark .status-success {
  background: rgba(34, 197, 94, 0.25);
  color: #4ade80;
}

.status-primary {
  background: var(--admin-badge-bg);
  color: var(--admin-badge-text);
}

.status-danger {
  background: rgba(239, 68, 68, 0.2);
  color: #dc2626;
}

html.dark .status-danger {
  background: rgba(239, 68, 68, 0.25);
  color: #f87171;
}

/* 第二行：训练参数、指标、时间（时间列略宽，整体左移） */
.row-secondary {
  display: grid;
  grid-template-columns: 1fr 1.2fr minmax(340px, 1.4fr);
  gap: 20px 24px;
  padding-top: 14px;
  margin-top: 2px;
  border-top: 1px solid var(--home-border);
}

.secondary-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  align-items: center;
  text-align: center;
}

.secondary-group .group-value {
  width: 100%;
  min-width: 0;
}

.group-label {
  font-size: 11px;
  color: var(--home-text-muted);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.group-value {
  font-size: 14px;
  color: var(--home-text-secondary);
  line-height: 1.5;
}

.time-value {
  white-space: nowrap;
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
