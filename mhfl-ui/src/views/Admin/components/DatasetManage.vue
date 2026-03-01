<script setup lang="ts">
import {ref, computed, onMounted, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Search, Plus, Delete, Edit} from '@element-plus/icons-vue'
import {
  listDatasetsAdmin,
  createDatasetAdmin,
  updateDatasetAdmin,
  deleteDatasetAdmin,
  type DatasetVO
} from '@/api/dataset'

const {t} = useI18n()

// 搜索条件
const keyword = ref('')
const startTime = ref('')
const endTime = ref('')

// 分页
const currentPage = ref(1)
const pageSizeOptions = [5, 10, 20, 50] as const
const pageSize = ref(10)
const total = ref(0)
const list = ref<DatasetVO[]>([])
const loading = ref(false)

const fetchList = () => {
  loading.value = true
  listDatasetsAdmin(
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
    currentPage.value = 1
    fetchList()
  }
})

onMounted(() => {
  fetchList()
})

// 创建数据集
const createDialogVisible = ref(false)
const createForm = ref({dataName: ''})
const createSubmitting = ref(false)

const openCreate = () => {
  createForm.value = {dataName: ''}
  createDialogVisible.value = true
}

const submitCreate = () => {
  const {dataName} = createForm.value
  if (!dataName?.trim()) {
    ElMessage.warning(t('pages.admin.datasetNameRequired'))
    return
  }
  createSubmitting.value = true
  createDatasetAdmin(
      dataName.trim(),
      () => {
        createSubmitting.value = false
        createDialogVisible.value = false
        ElMessage.success(t('pages.admin.createDatasetSuccess'))
        fetchList()
      },
      () => {
        createSubmitting.value = false
      }
  )
}

// 编辑数据集
const editDialogVisible = ref(false)
const editingDataset = ref<DatasetVO | null>(null)
const editForm = ref({dataName: ''})
const editSubmitting = ref(false)

const openEdit = (dataset: DatasetVO) => {
  editingDataset.value = dataset
  editForm.value = {dataName: dataset.dataName}
  editDialogVisible.value = true
}

const submitEdit = () => {
  if (!editingDataset.value) return
  const {dataName} = editForm.value
  if (!dataName?.trim()) {
    ElMessage.warning(t('pages.admin.datasetNameRequired'))
    return
  }
  editSubmitting.value = true
  updateDatasetAdmin(
      editingDataset.value.id,
      dataName.trim(),
      () => {
        editSubmitting.value = false
        editDialogVisible.value = false
        editingDataset.value = null
        ElMessage.success(t('pages.admin.updateDatasetSuccess'))
        fetchList()
      },
      () => {
        editSubmitting.value = false
      }
  )
}

// 删除数据集
const handleDelete = (dataset: DatasetVO) => {
  ElMessageBox.confirm(
      t('pages.admin.confirmDeleteDataset', {name: dataset.dataName}),
      t('pages.admin.deleteDataset'),
      {
        confirmButtonText: t('pages.admin.confirm'),
        cancelButtonText: t('header.cancel'),
        type: 'warning'
      }
  ).then(() => {
    deleteDatasetAdmin(dataset.id, () => {
      ElMessage.success(t('pages.admin.deleteDatasetSuccess'))
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
  <div class="dataset-manage">
    <!-- 顶部固定：搜索栏、搜索、创建数据集 -->
    <div class="dataset-manage-header flex flex-wrap items-end gap-4">
      <el-input
          v-model="keyword"
          :placeholder="$t('pages.admin.searchDatasetKeyword')"
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
      <el-button type="success" :icon="Plus" @click="openCreate">{{ $t('pages.admin.createDataset') }}</el-button>
    </div>

    <!-- 中间可滚动：列表 -->
    <div v-loading="loading" class="dataset-list flex flex-col gap-3">
      <div
          v-for="dataset in list"
          :key="dataset.id"
          class="dataset-row"
      >
        <div class="row-cell cell-id">
          <span class="cell-label">{{ $t('pages.admin.id') }}</span>
          <span class="cell-value">{{ dataset.id }}</span>
        </div>
        <div class="row-cell cell-name">
          <span class="cell-label">{{ $t('pages.admin.datasetName') }}</span>
          <span class="cell-value text-ellipsis font-semibold" :title="dataset.dataName">{{ dataset.dataName }}</span>
        </div>
        <div class="row-cell cell-create">
          <span class="cell-label">{{ $t('pages.admin.createTime') }}</span>
          <span class="cell-value text-ellipsis" :title="dataset.createTime || ''">{{
              dataset.createTime || '—'
            }}</span>
        </div>
        <div class="row-cell cell-update">
          <span class="cell-label">{{ $t('pages.admin.updateTime') }}</span>
          <span class="cell-value text-ellipsis" :title="dataset.updateTime || ''">{{
              dataset.updateTime || '—'
            }}</span>
        </div>
        <div class="row-action flex items-center gap-2">
          <el-button type="primary" size="small" :icon="Edit" text @click="openEdit(dataset)">
            {{ $t('header.edit') }}
          </el-button>
          <el-button type="danger" size="small" :icon="Delete" text @click="handleDelete(dataset)">
            {{ $t('pages.admin.deleteDataset') }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 底部固定：每页条数、共 x 条、分页 -->
    <div class="dataset-manage-footer flex items-center justify-between gap-4 flex-nowrap">
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

    <!-- 创建数据集弹窗 -->
    <el-dialog
        v-model="createDialogVisible"
        :title="$t('pages.admin.createDatasetTitle')"
        width="420px"
        class="create-dialog"
        @close="createForm = { dataName: '' }"
    >
      <el-form label-position="top" :model="createForm">
        <el-form-item :label="$t('pages.admin.datasetName')" required>
          <el-input
              v-model="createForm.dataName"
              :placeholder="$t('pages.admin.datasetNamePlaceholder')"
              maxlength="100"
              show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">{{ $t('header.cancel') }}</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">{{ $t('header.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 编辑数据集弹窗 -->
    <el-dialog
        v-model="editDialogVisible"
        :title="$t('pages.admin.editDatasetTitle')"
        width="420px"
        class="edit-dialog"
        @close="editingDataset = null; editForm = { dataName: '' }"
    >
      <el-form label-position="top" :model="editForm">
        <el-form-item :label="$t('pages.admin.datasetName')" required>
          <el-input
              v-model="editForm.dataName"
              :placeholder="$t('pages.admin.datasetNamePlaceholder')"
              maxlength="100"
              show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">{{ $t('header.cancel') }}</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">{{ $t('header.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 整体：顶部/底部固定，中间滚动 */
.dataset-manage {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding-bottom: 3px;
}

.dataset-manage-header {
  flex-shrink: 0;
  margin-bottom: 16px;
}

.dataset-manage-header :deep(.el-input__wrapper),
.dataset-manage-header :deep(.el-date-editor) {
  background: var(--home-card-bg);
  border-color: var(--home-border);
}

.dataset-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding-right: 4px;
}

.dataset-manage-footer {
  flex-shrink: 0;
  margin-top: 16px;
  padding-top: 12px;
  padding-bottom: 12px;
  border-top: 1px solid var(--home-border);
}

/* 一行一个数据集：网格对齐，优化布局使其不显得空 */
.dataset-row {
  display: grid;
  grid-template-columns: 80px minmax(200px, 2fr) minmax(160px, 1.5fr) minmax(160px, 1.5fr) auto;
  gap: 16px 24px;
  align-items: center;
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid var(--home-border);
  background: var(--home-card-bg);
  transition: all 0.25s ease;
}

.dataset-row:hover {
  background: var(--home-hover-bg);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.row-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
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
}

.cell-name .cell-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--home-text-primary);
}

.row-action {
  justify-self: end;
}

.dataset-manage-footer :deep(.el-pagination) {
  --el-pagination-bg-color: var(--home-card-bg);
  --el-pagination-button-color: var(--home-text-primary);
  --el-pagination-text-color: var(--home-text-secondary);
}

.dataset-manage-footer :deep(.page-size-select .el-input__wrapper) {
  background: var(--home-card-bg);
  border-color: var(--home-border);
}

.create-dialog :deep(.el-dialog__header),
.create-dialog :deep(.el-dialog__body),
.create-dialog :deep(.el-form-item__label),
.edit-dialog :deep(.el-dialog__header),
.edit-dialog :deep(.el-dialog__body),
.edit-dialog :deep(.el-form-item__label) {
  color: var(--home-text-primary);
}
</style>
