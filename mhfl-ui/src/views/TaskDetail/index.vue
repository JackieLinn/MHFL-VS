<script setup lang="ts">
import {ref, computed, onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {ArrowLeft} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {getTaskDetail, type TaskVO, type TaskStatusCode} from '@/api/task'

const route = useRoute()
const router = useRouter()
const {t} = useI18n()

const taskId = computed(() => {
  const id = route.params.id
  if (typeof id === 'string') return parseInt(id, 10)
  return NaN
})

const task = ref<TaskVO | null>(null)
const loading = ref(false)
const error = ref('')

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

/** 百分数展示；-1 表示占位，显示 — */
const formatPercent = (v: number | null | undefined): string => {
  if (v == null || v === -1) return '—'
  return Number.isFinite(v) ? `${(v * 100).toFixed(2)}%` : '—'
}

/** Loss 展示；-1 表示占位，显示 — */
const formatLoss = (v: number | null | undefined): string => {
  if (v == null || v === -1) return '—'
  return Number.isFinite(v) ? v.toFixed(4) : '—'
}

const fetchDetail = () => {
  const id = taskId.value
  if (!Number.isFinite(id) || id < 1) {
    error.value = t('pages.taskDetail.invalidId')
    return
  }
  loading.value = true
  error.value = ''
  getTaskDetail(
      id,
      (data) => {
        task.value = data
        loading.value = false
      },
      (msg) => {
        error.value = msg || t('pages.taskDetail.loadFailed')
        loading.value = false
      }
  )
}

const goBack = () => {
  router.push({name: 'Task'})
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="task-detail-page">
    <PageHeader
        class="mb-5"
        :title="$t('pages.taskDetail.title')"
        :desc="$t('pages.taskDetail.desc', {id: taskId})"
    />

    <div class="detail-actions mb-5">
      <el-button :icon="ArrowLeft" @click="goBack">{{ $t('pages.taskDetail.backToList') }}</el-button>
    </div>

    <div v-loading="loading" class="detail-content">
      <div v-if="error" class="detail-error">
        <p>{{ error }}</p>
      </div>

      <div v-else-if="task" class="detail-card">
        <h3 class="detail-section-title">{{ $t('pages.taskDetail.basicInfo') }}</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.id') }}</span>
            <span class="detail-value">{{ task.id }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.dataName') }}</span>
            <span class="detail-value">{{ task.dataName }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.algorithmName') }}</span>
            <span class="detail-value">{{ task.algorithmName }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.username') }}</span>
            <span class="detail-value">{{
                $t('pages.task.creatorWithUid', {
                  uid: task.uid,
                  username: task.username || '—'
                })
              }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.status') }}</span>
            <span :class="statusBadgeClass(task.status)">{{ statusLabel(task.status) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.numNodes') }}</span>
            <span class="detail-value">{{ task.numNodes }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.fraction') }}</span>
            <span class="detail-value">{{ task.fraction }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.classesPerNode') }}</span>
            <span class="detail-value">{{ task.classesPerNode }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.lowProb') }}</span>
            <span class="detail-value">{{ task.lowProb }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.numSteps') }}</span>
            <span class="detail-value">{{ task.numSteps }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.epochs') }}</span>
            <span class="detail-value">{{ task.epochs }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.loss') }}</span>
            <span class="detail-value">{{ formatLoss(task.loss) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.accuracy') }}</span>
            <span class="detail-value">{{ formatPercent(task.accuracy) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.precision') }}</span>
            <span class="detail-value">{{ formatPercent(task.precision) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.recall') }}</span>
            <span class="detail-value">{{ formatPercent(task.recall) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.f1Score') }}</span>
            <span class="detail-value">{{ formatPercent(task.f1Score) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.createTime') }}</span>
            <span class="detail-value">{{ task.createTime || '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('pages.task.updateTime') }}</span>
            <span class="detail-value">{{ task.updateTime || '—' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.task-detail-page {
  padding: 32px 24px 16px 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.detail-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.detail-error {
  padding: 24px;
  background: var(--user-role-bg);
  border: 1px solid var(--user-role-border);
  border-radius: 12px;
  color: var(--home-text-secondary);
}

.detail-card {
  padding: 24px;
  background: var(--user-role-bg);
  border: 1px solid var(--user-role-border);
  border-radius: 14px;
}

.detail-section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--home-text-primary);
  margin: 0 0 20px 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px 32px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  font-size: 11px;
  color: var(--home-text-muted);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.detail-value {
  font-size: 14px;
  color: var(--home-text-primary);
}

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
</style>
