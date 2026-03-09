<script setup lang="ts">
import {ref, watch, computed} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {ElMessage} from 'element-plus'
import type {FormInstance, FormRules} from 'element-plus'
import {listDatasetsForSelect, type DatasetVO} from '@/api/dataset'
import {listAlgorithmsForSelect, type AlgorithmVO} from '@/api/algorithm'
import {
  createTask,
  startTask,
  type CreateTaskRO,
  type CreateTaskResultVO
} from '@/api/task'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'created', taskId: number): void
}>()

const router = useRouter()
const {t} = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const formRef = ref<FormInstance | null>(null)
const datasets = ref<DatasetVO[]>([])
const algorithms = ref<AlgorithmVO[]>([])
const loadingDatasets = ref(false)
const loadingAlgorithms = ref(false)
const submitting = ref(false)

/** 每轮比例选项：0.1 ~ 1.0 */
const fractionOptions = [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0]

/** 低概率选项：0.1 ~ 0.9（不含 1.0） */
const lowProbOptions = [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9]

const form = ref<Partial<CreateTaskRO>>({
  did: undefined,
  aid: undefined,
  numNodes: 100,
  fraction: 0.1,
  classesPerNode: 10,
  lowProb: 0.4,
  numSteps: 500,
  epochs: 10
})

/** 整数校验：必须为整数，不能带小数 */
const validateInteger = (min: number, requiredKey: string, minKey: string) =>
    (_rule: unknown, value: number | string | undefined, callback: (err?: Error) => void) => {
      if (value === '' || value == null || value === undefined || (typeof value === 'number' && !Number.isFinite(value))) {
        callback(new Error(t(requiredKey)))
        return
      }
      const n = Number(value)
      if (!Number.isFinite(n) || !Number.isInteger(n) || n !== Math.floor(n)) {
        callback(new Error(t('pages.task.create.mustBeInteger')))
        return
      }
      if (n < min) {
        callback(new Error(t(minKey)))
        return
      }
      callback()
    }

const rules: FormRules = {
  did: [{required: true, message: () => t('pages.task.create.datasetRequired'), trigger: 'change'}],
  aid: [{required: true, message: () => t('pages.task.create.algorithmRequired'), trigger: 'change'}],
  numNodes: [
    {required: true, message: () => t('pages.task.create.numNodesRequired'), trigger: 'blur'},
    {
      validator: validateInteger(1, 'pages.task.create.numNodesRequired', 'pages.task.create.numNodesMin'),
      trigger: 'blur'
    }
  ],
  fraction: [{required: true, message: () => t('pages.task.create.fractionRequired'), trigger: 'change'}],
  classesPerNode: [
    {required: true, message: () => t('pages.task.create.classesPerNodeRequired'), trigger: 'blur'},
    {
      validator: validateInteger(1, 'pages.task.create.classesPerNodeRequired', 'pages.task.create.classesPerNodeMin'),
      trigger: 'blur'
    }
  ],
  lowProb: [{required: true, message: () => t('pages.task.create.lowProbRequired'), trigger: 'change'}],
  numSteps: [
    {required: true, message: () => t('pages.task.create.numStepsRequired'), trigger: 'blur'},
    {
      validator: validateInteger(1, 'pages.task.create.numStepsRequired', 'pages.task.create.numStepsMin'),
      trigger: 'blur'
    }
  ],
  epochs: [
    {required: true, message: () => t('pages.task.create.epochsRequired'), trigger: 'blur'},
    {validator: validateInteger(1, 'pages.task.create.epochsRequired', 'pages.task.create.epochsMin'), trigger: 'blur'}
  ]
}

const fetchDatasets = () => {
  loadingDatasets.value = true
  listDatasetsForSelect(
      (data) => {
        datasets.value = data.records ?? []
        loadingDatasets.value = false
      },
      () => {
        loadingDatasets.value = false
      }
  )
}

const fetchAlgorithms = () => {
  loadingAlgorithms.value = true
  listAlgorithmsForSelect(
      (data) => {
        algorithms.value = data.records ?? []
        loadingAlgorithms.value = false
      },
      () => {
        loadingAlgorithms.value = false
      }
  )
}

watch(visible, (v) => {
  if (v) {
    fetchDatasets()
    fetchAlgorithms()
    form.value = {
      did: undefined,
      aid: undefined,
      numNodes: 100,
      fraction: 0.1,
      classesPerNode: 10,
      lowProb: 0.4,
      numSteps: 500,
      epochs: 10
    }
    formRef.value?.clearValidate()
  }
})

const handleCancel = () => {
  visible.value = false
}

const doCreate = (andStart: boolean) => {
  formRef.value?.validate((valid) => {
    if (!valid) return
    const ro = form.value as CreateTaskRO
    if (ro.did == null || ro.aid == null) return
    const payload: CreateTaskRO = {
      did: Number(ro.did),
      aid: Number(ro.aid),
      numNodes: Math.floor(Number(ro.numNodes)) || 1,
      fraction: Number(ro.fraction),
      classesPerNode: Math.floor(Number(ro.classesPerNode)) || 1,
      lowProb: Number(ro.lowProb),
      numSteps: Math.floor(Number(ro.numSteps)) || 1,
      epochs: Math.floor(Number(ro.epochs)) || 1
    }
    submitting.value = true
    createTask(
        payload,
        (data: CreateTaskResultVO) => {
          submitting.value = false
          visible.value = false
          emit('created', data.taskId)
          if (data.recommendedSameConfig) {
            ElMessage.info(t('pages.task.create.recommendedSameConfigHint'))
          }
          if (data.copied) {
            ElMessage.success(t('pages.task.create.copiedHint'))
          } else {
            ElMessage.success(t('pages.task.create.createSuccess'))
          }
          if (andStart) {
            startTask(
                data.taskId,
                () => {
                  router.push({name: 'TaskDetail', params: {id: String(data.taskId)}})
                },
                () => {
                  ElMessage.warning(t('pages.task.create.startFailed'))
                }
            )
          }
        },
        () => {
          submitting.value = false
        }
    )
  })
}

const handleCreate = () => doCreate(false)
const handleCreateAndStart = () => doCreate(true)

/** 已选择配置（有值的项） */
const selectedConfigText = computed(() => {
  const f = form.value
  const items: string[] = []
  const ds = f.did != null ? datasets.value.find(d => d.id === f.did)?.dataName : null
  const algo = f.aid != null ? algorithms.value.find(a => a.id === f.aid)?.algorithmName : null
  if (ds) items.push(`${t('pages.task.dataName')} ${ds}`)
  if (algo) items.push(`${t('pages.task.algorithmName')} ${algo}`)
  if (f.numNodes != null) items.push(`${t('pages.task.numNodes')} ${f.numNodes}`)
  if (f.fraction != null) items.push(`${t('pages.task.fraction')} ${f.fraction}`)
  if (f.classesPerNode != null) items.push(`${t('pages.task.classesPerNode')} ${f.classesPerNode}`)
  if (f.lowProb != null) items.push(`${t('pages.task.lowProb')} ${f.lowProb}`)
  if (f.numSteps != null) items.push(`${t('pages.task.numSteps')} ${f.numSteps}`)
  if (f.epochs != null) items.push(`${t('pages.task.epochs')} ${f.epochs}`)
  return items.length > 0 ? items.join('，') : t('pages.task.create.none')
})

/** 未选择配置（必填但未选的项） */
const unselectedConfigText = computed(() => {
  const f = form.value
  const items: string[] = []
  if (f.did == null) items.push(t('pages.task.dataName'))
  if (f.aid == null) items.push(t('pages.task.algorithmName'))
  return items.length > 0 ? items.join('、') : t('pages.task.create.none')
})
</script>

<template>
  <el-dialog
      v-model="visible"
      :title="$t('pages.task.create.title')"
      width="760px"
      append-to-body
      align-center
      destroy-on-close
      modal-class="create-task-dialog"
      :close-on-click-modal="false"
      @closed="formRef?.resetFields()"
  >
    <div class="config-summary">
      <div class="summary-row">
        <span class="summary-label">{{ $t('pages.task.create.selectedConfig') }}：</span>
        <span class="summary-value">{{ selectedConfigText }}</span>
      </div>
      <div class="summary-row">
        <span class="summary-label">{{ $t('pages.task.create.unselectedConfig') }}：</span>
        <span class="summary-value">{{ unselectedConfigText }}</span>
      </div>
    </div>

    <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        class="create-task-form"
        label-position="top"
    >
      <div class="form-grid">
        <el-form-item :label="$t('pages.task.dataName')" prop="did" class="form-cell">
          <el-select
              v-model="form.did"
              :placeholder="$t('pages.task.create.selectDataset')"
              filterable
              class="w-full"
              :loading="loadingDatasets"
          >
            <el-option v-for="d in datasets" :key="d.id" :label="d.dataName" :value="d.id"/>
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('pages.task.algorithmName')" prop="aid" class="form-cell">
          <el-select
              v-model="form.aid"
              :placeholder="$t('pages.task.create.selectAlgorithm')"
              filterable
              class="w-full"
              :loading="loadingAlgorithms"
          >
            <el-option v-for="a in algorithms" :key="a.id" :label="a.algorithmName" :value="a.id"/>
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('pages.task.numNodes')" prop="numNodes" class="form-cell">
          <el-input
              v-model.number="form.numNodes"
              type="number"
              :placeholder="$t('pages.task.create.numNodesPlaceholder')"
              class="w-full"
          />
        </el-form-item>
        <el-form-item :label="$t('pages.task.fraction')" prop="fraction" class="form-cell">
          <el-select v-model="form.fraction" :placeholder="$t('pages.task.create.selectFraction')" class="w-full">
            <el-option v-for="v in fractionOptions" :key="v" :label="String(v)" :value="v"/>
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('pages.task.classesPerNode')" prop="classesPerNode" class="form-cell">
          <el-input
              v-model.number="form.classesPerNode"
              type="number"
              :placeholder="$t('pages.task.create.classesPlaceholder')"
              class="w-full"
          />
        </el-form-item>
        <el-form-item :label="$t('pages.task.lowProb')" prop="lowProb" class="form-cell">
          <el-select v-model="form.lowProb" :placeholder="$t('pages.task.create.selectLowProb')" class="w-full">
            <el-option v-for="v in lowProbOptions" :key="v" :label="String(v)" :value="v"/>
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('pages.task.numSteps')" prop="numSteps" class="form-cell">
          <el-input
              v-model.number="form.numSteps"
              type="number"
              :placeholder="$t('pages.task.create.numStepsPlaceholder')"
              class="w-full"
          />
        </el-form-item>
        <el-form-item :label="$t('pages.task.epochs')" prop="epochs" class="form-cell">
          <el-input
              v-model.number="form.epochs"
              type="number"
              :placeholder="$t('pages.task.create.epochsPlaceholder')"
              class="w-full"
          />
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">{{ $t('pages.task.create.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">
          {{ $t('pages.task.create.create') }}
        </el-button>
        <el-button type="success" :loading="submitting" @click="handleCreateAndStart">
          {{ $t('pages.task.create.createAndStart') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.config-summary {
  padding: 12px 16px;
  margin-bottom: 20px;
  background: var(--home-hover-bg);
  border: 1px solid var(--home-border);
  border-radius: 10px;
}

.summary-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-height: 24px;
}

.summary-row + .summary-row {
  margin-top: 8px;
}

.summary-label {
  flex-shrink: 0;
  min-width: 90px;
  font-size: 13px;
  color: var(--home-text-muted);
}

.summary-value {
  flex: 1;
  font-size: 13px;
  color: var(--home-text-primary);
  word-break: break-word;
}

.create-task-form {
  padding: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0 32px;
}

.form-cell {
  margin-bottom: 18px;
}

.form-cell :deep(.el-form-item__label) {
  width: 100% !important;
  text-align: left;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
