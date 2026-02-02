<script setup lang="ts">
import {ref, reactive, computed} from 'vue'
import {Message, Key, Lock} from '@element-plus/icons-vue'
import {sendEmailCode, confirmReset, resetPassword} from '@/api/auth'
import type {FormInstance, FormRules} from 'element-plus'

const emit = defineEmits<{
  (e: 'switch', panel: 'login'): void
}>()

const formRef = ref<FormInstance>()
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const step = ref(1)

const form = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rulesStep1: FormRules = {
  email: [
    {required: true, message: '请输入邮箱', trigger: 'blur'},
    {type: 'email', message: '邮箱格式不正确', trigger: 'blur'},
  ],
  code: [
    {required: true, message: '请输入验证码', trigger: 'blur'},
    {len: 6, message: '6位数字', trigger: 'blur'},
  ],
}

const rulesStep2: FormRules = {
  password: [
    {required: true, message: '请输入新密码', trigger: 'blur'},
    {min: 6, max: 20, message: '6-20个字符', trigger: 'blur'},
  ],
  confirmPassword: [
    {required: true, message: '请确认密码', trigger: 'blur'},
    {validator: validateConfirmPassword, trigger: 'blur'},
  ],
}

const currentRules = computed(() => step.value === 1 ? rulesStep1 : rulesStep2)

const canSendCode = computed(() => {
  return form.email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email) && countdown.value === 0
})

const handleSendCode = () => {
  if (!canSendCode.value) return
  sendingCode.value = true
  sendEmailCode(form.email, 'reset',
      () => {
        sendingCode.value = false
        countdown.value = 60
        const timer = setInterval(() => {
          countdown.value--
          if (countdown.value <= 0) clearInterval(timer)
        }, 1000)
      },
      () => sendingCode.value = false
  )
}

const handleNextStep = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (!valid) return
    loading.value = true
    confirmReset(form.email, form.code,
        () => {
          loading.value = false
          step.value = 2
        },
        () => loading.value = false
    )
  })
}

const handleResetPassword = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (!valid) return
    loading.value = true
    resetPassword({email: form.email, code: form.code, password: form.password},
        () => {
          loading.value = false
          emit('switch', 'login')
        },
        () => loading.value = false
    )
  })
}

const handleSubmit = () => step.value === 1 ? handleNextStep() : handleResetPassword()

const handleBack = () => {
  if (step.value === 2) {
    step.value = 1
  } else {
    emit('switch', 'login')
  }
}
</script>

<template>
  <div class="w-full">
    <!-- 标题 -->
    <div class="text-center mb-5">
      <h2 class="text-2xl font-semibold text-gray-800 dark:text-white mb-1">重置密码</h2>
      <p class="text-sm text-gray-500 dark:text-gray-400">
        {{ step === 1 ? '输入邮箱接收验证码' : '设置新密码' }}
      </p>
    </div>

    <!-- 步骤指示器 -->
    <div class="flex-center gap-3 mb-6">
      <div class="flex items-center gap-2">
        <div
            :class="['w-7 h-7 rounded-full flex-center text-xs font-semibold transition-colors', step >= 1 ? 'bg-indigo-500 text-white' : 'bg-gray-200 dark:bg-gray-700 text-gray-500']">
          1
        </div>
        <span :class="['text-sm transition-colors', step >= 1 ? 'text-indigo-500' : 'text-gray-400']">验证邮箱</span>
      </div>
      <div :class="['w-8 h-0.5 transition-colors', step >= 2 ? 'bg-indigo-500' : 'bg-gray-200 dark:bg-gray-700']"></div>
      <div class="flex items-center gap-2">
        <div
            :class="['w-7 h-7 rounded-full flex-center text-xs font-semibold transition-colors', step >= 2 ? 'bg-indigo-500 text-white' : 'bg-gray-200 dark:bg-gray-700 text-gray-500']">
          2
        </div>
        <span :class="['text-sm transition-colors', step >= 2 ? 'text-indigo-500' : 'text-gray-400']">设置密码</span>
      </div>
    </div>

    <!-- 表单 -->
    <el-form ref="formRef" :model="form" :rules="currentRules" size="large">
      <template v-if="step === 1">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="注册时的邮箱地址" :prefix-icon="Message"/>
        </el-form-item>

        <el-form-item prop="code">
          <div class="flex gap-2 w-full">
            <el-input v-model="form.code" placeholder="邮箱验证码" :prefix-icon="Key" class="flex-1"/>
            <el-button :disabled="!canSendCode" :loading="sendingCode" @click="handleSendCode"
                       class="w-24 flex-shrink-0">
              {{ countdown > 0 ? `${countdown}s` : '发送' }}
            </el-button>
          </div>
        </el-form-item>
      </template>

      <template v-else>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="新密码" show-password :prefix-icon="Lock"/>
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认新密码" show-password
                    :prefix-icon="Lock"/>
        </el-form-item>
      </template>

      <el-form-item>
        <div class="flex gap-3 w-full">
          <el-button class="flex-1 h-11 rounded-lg" @click="handleBack">
            {{ step === 1 ? '返回登录' : '上一步' }}
          </el-button>
          <el-button type="primary" class="flex-2 h-11 rounded-lg" :loading="loading" @click="handleSubmit">
            {{ step === 1 ? '下一步' : '重置密码' }}
          </el-button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-button) {
  border-radius: 8px;
}

.flex-2 {
  flex: 2;
}
</style>
