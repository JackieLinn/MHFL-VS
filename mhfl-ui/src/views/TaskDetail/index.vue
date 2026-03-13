<script setup lang="ts">
import {ref, computed, onMounted, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {ElMessage} from 'element-plus'
import {ArrowLeft, VideoPause, CircleClose, VideoPlay} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import BackToTop from '@/components/BackToTop.vue'
import TaskDetailContent from './components/TaskDetailContent.vue'
import {getTaskDetail, startTask, stopTask, type TaskVO, type TaskStatusCode} from '@/api/task'

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
const progress = ref<{ current: number; total: number }>({current: 0, total: 0})

const onStatusChange = () => {
  if (Number.isFinite(taskId.value) && taskId.value >= 1) {
    fetchDetail()
  }
}

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

const stopLoading = ref(false)
const startLoading = ref(false)

/** 启动训练 */
const onStartTask = () => {
  if (!task.value || task.value.status !== 0) return
  startLoading.value = true
  startTask(
      task.value.id,
      () => {
        startLoading.value = false
        ElMessage.success(t('pages.task.create.startSuccess'))
        fetchDetail()
      },
      () => {
        startLoading.value = false
      }
  )
}

/** 停止训练 */
const onStopTask = () => {
  if (!task.value || task.value.status !== 1) return
  stopLoading.value = true
  stopTask(
      task.value.id,
      () => {
        stopLoading.value = false
        ElMessage.success(t('pages.taskDetail.stopSuccess'))
        fetchDetail()
      },
      () => {
        stopLoading.value = false
      }
  )
}

watch(taskId, (id) => {
  if (Number.isFinite(id) && id >= 1) {
    fetchDetail()
  }
})

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
      <div v-if="task" class="detail-meta flex flex-wrap items-center gap-4 text-sm">
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
        <div v-if="progress.total > 0" class="detail-meta-item detail-progress-wrap">
          <span class="detail-meta-label">{{ $t('pages.taskDetail.progressLabel') }}</span>
          <div class="detail-progress-inner">
            <el-progress
                :percentage="progress.total > 0 ? Math.min(100, Math.round((progress.current / progress.total) * 100)) : 0"
                :stroke-width="8"
                :show-text="false"
                :status="task.status === 1 ? undefined : (task.status === 2 || task.status === 3 ? 'success' : 'exception')"
            />
            <span class="detail-progress-text">
              {{
                task.status === 1
                    ? $t('pages.taskDetail.progressInProgress', {current: progress.current, total: progress.total})
                    : $t('pages.taskDetail.progressDone', {current: progress.current, total: progress.total})
              }}
            </span>
          </div>
        </div>
        <el-button
            v-if="task.status === 0"
            type="success"
            :icon="VideoPlay"
            :loading="startLoading"
            @click="onStartTask"
        >
          {{ $t('pages.task.start') }}
        </el-button>
        <el-button
            v-else-if="task.status === 1"
            class="detail-stop-btn"
            type="danger"
            :icon="VideoPause"
            :loading="stopLoading"
            @click="onStopTask"
        >
          {{ $t('pages.taskDetail.stopTraining') }}
        </el-button>
        <el-button
            v-else-if="task.status === 5"
            class="detail-stop-btn"
            disabled
            :icon="CircleClose"
        >
          {{ statusLabel(task.status) }}
        </el-button>
      </div>
    </div>

    <div id="task-detail-scroll" v-loading="loading" class="detail-content flex-1 flex flex-col min-w-0">
      <div v-if="error" class="detail-error">
        <p>{{ error }}</p>
      </div>

      <div v-else-if="task" class="flex-1 flex flex-col min-w-0">
        <TaskDetailContent :task="task" @status-change="onStatusChange" @progress="progress = $event"/>
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

.detail-progress-wrap {
  min-width: 180px;
  max-width: 320px;
}

.detail-progress-inner {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.detail-progress-inner .el-progress {
  flex: 1;
  min-width: 80px;
}

.detail-progress-text {
  font-size: 12px;
  font-weight: 500;
  color: var(--home-text-secondary);
  white-space: nowrap;
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

.detail-stop-btn :deep(.el-icon) {
  font-size: 18px;
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
