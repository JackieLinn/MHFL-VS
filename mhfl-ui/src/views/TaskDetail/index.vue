<script setup lang="ts">
import {ref, computed, onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {ArrowLeft} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import BackToTop from '@/components/BackToTop.vue'
import TaskDetailContent from './components/TaskDetailContent.vue'
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

const statusBadgeClass = (code: TaskStatusCode): string => {
  const map: Record<TaskStatusCode, string> = {
    0: 'status-info',
    1: 'status-warning',
    2: 'status-success',
    3: 'status-primary',
    4: 'status-danger',
    5: 'status-cancelled'
  }
  return `status-badge ${map[code] ?? 'status-info'}`
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
  <div class="task-detail-page p-8 pb-4 min-h-full flex flex-col">
    <PageHeader
        class="mb-5"
        :title="$t('pages.taskDetail.title')"
        :desc="$t('pages.taskDetail.desc', {id: taskId})"
    />

    <div class="detail-actions mb-5 flex items-center gap-4">
      <el-button :icon="ArrowLeft" @click="goBack">{{ $t('pages.taskDetail.backToList') }}</el-button>
      <div v-if="task" class="detail-meta flex items-center gap-4 text-sm">
        <span class="detail-meta-item">
          <span class="detail-meta-label">{{ $t('pages.task.status') }}</span>
          <span :class="statusBadgeClass(task.status)">{{ statusLabel(task.status) }}</span>
        </span>
        <span class="detail-meta-item">
          <span class="detail-meta-label">{{ $t('pages.task.algorithmName') }}</span>
          <span class="detail-meta-value">{{ task.algorithmName }}</span>
        </span>
        <span class="detail-meta-item">
          <span class="detail-meta-label">{{ $t('pages.task.dataName') }}</span>
          <span class="detail-meta-value">{{ task.dataName }}</span>
        </span>
      </div>
    </div>

    <div id="task-detail-scroll" v-loading="loading" class="detail-content flex-1 flex flex-col min-w-0">
      <div v-if="error" class="detail-error">
        <p>{{ error }}</p>
      </div>

      <div v-else-if="task" class="flex-1 flex flex-col min-w-0">
        <TaskDetailContent :task="task"/>
      </div>
    </div>

    <BackToTop scroll-target="#task-detail-scroll"/>
  </div>
</template>

<style scoped>
.task-detail-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.detail-content {
  overflow-y: auto;
  min-height: 0;
}

.detail-meta {
  color: var(--home-text-secondary);
}

.detail-meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-meta-label {
  font-size: 12px;
  color: var(--home-text-muted);
}

.detail-meta-value {
  font-weight: 500;
  color: var(--home-text-primary);
}

.detail-error {
  padding: 24px;
  background: var(--user-role-bg);
  border: 1px solid var(--user-role-border);
  border-radius: 12px;
  color: var(--home-text-secondary);
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

.status-cancelled {
  background: rgba(120, 113, 108, 0.25);
  color: #78716c;
}

html.dark .status-cancelled {
  background: rgba(120, 113, 108, 0.3);
  color: #a8a29e;
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

<style src="./taskDetail.css"></style>
