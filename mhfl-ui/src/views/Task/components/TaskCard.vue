<script setup lang="ts">
import {computed} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {View, Delete, VideoPlay, Star, StarFilled} from '@element-plus/icons-vue'
import type {TaskVO, TaskStatusCode} from '@/api/task'

const props = defineProps<{
  task: TaskVO
  isAdmin?: boolean
}>()

const emit = defineEmits<{
  (e: 'view-detail', task: TaskVO): void
  (e: 'delete', task: TaskVO): void
}>()

const router = useRouter()
const {t} = useI18n()

/** 状态码：0 未开始 1 进行中 2 已完成 3 推荐 4 失败 5 已取消 */
const NOT_STARTED = 0
const SUCCESS = 2
const RECOMMENDED = 3

const canDelete = computed(() => props.task.status !== RECOMMENDED)
const canStart = computed(() => props.task.status === NOT_STARTED)
const canSetRecommend = computed(() => props.isAdmin && props.task.status === SUCCESS)
const canUnsetRecommend = computed(() => props.isAdmin && props.task.status === RECOMMENDED)
const canRecommend = computed(() => canSetRecommend.value || canUnsetRecommend.value)

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
    5: 'status-cancelled'
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

const handleViewDetail = () => {
  emit('view-detail', props.task)
}

const handleStart = () => {
  router.push({name: 'TaskDetail', params: {id: String(props.task.id)}})
}

const handleDelete = () => {
  emit('delete', props.task)
}

/** 推荐按钮：先不连后端，占位 */
const handleSetRecommend = () => {
  // TODO: 对接 setRecommend API
}

const handleUnsetRecommend = () => {
  // TODO: 对接 setRecommend API
}
</script>

<template>
  <div class="task-row">
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
      <div class="row-action task-card-actions">
        <el-tooltip :content="$t('pages.task.startDisabledHint')" :disabled="canStart" placement="top">
          <el-button
              type="success"
              size="small"
              :icon="VideoPlay"
              :disabled="!canStart"
              class="task-action-btn"
              @click="handleStart"
          >
            {{ $t('pages.task.start') }}
          </el-button>
        </el-tooltip>
        <el-tooltip :content="$t('pages.task.recommendDisabledHint')" :disabled="canRecommend" placement="top">
          <el-button
              :type="canUnsetRecommend ? 'info' : 'warning'"
              size="small"
              :icon="canUnsetRecommend ? StarFilled : Star"
              :disabled="!canRecommend"
              class="task-action-btn"
              @click="canSetRecommend ? handleSetRecommend() : handleUnsetRecommend()"
          >
            {{ canUnsetRecommend ? $t('pages.task.unsetRecommend') : $t('pages.task.setRecommend') }}
          </el-button>
        </el-tooltip>
        <el-tooltip :content="$t('pages.task.deleteDisabledHint')" :disabled="canDelete" placement="top">
          <el-button
              type="danger"
              size="small"
              :icon="Delete"
              :disabled="!canDelete"
              class="task-action-btn"
              @click="handleDelete"
          >
            {{ $t('pages.task.delete') }}
          </el-button>
        </el-tooltip>
        <el-button type="primary" size="small" :icon="View" class="task-action-btn" @click="handleViewDetail">
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
</template>

<style scoped>
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

/* 操作按钮区：固定宽度保证对齐 */
.task-card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  justify-content: flex-end;
}

.task-action-btn {
  min-width: 88px;
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
</style>
