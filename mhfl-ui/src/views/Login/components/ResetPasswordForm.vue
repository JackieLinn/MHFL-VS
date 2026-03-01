<script setup lang="ts">
import {ref, reactive, computed} from 'vue'
import {Message, Key, Lock} from '@element-plus/icons-vue'
import {sendEmailCode, confirmReset, resetPassword} from '@/api/auth'
import type {FormInstance, FormRules} from 'element-plus'
import {useI18n} from 'vue-i18n'

const emit = defineEmits<{
  (e: 'switch', panel: 'login'): void
}>()

const {t} = useI18n()
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
    callback(new Error(t('login.validation.passwordMismatch')))
  } else {
    callback()
  }
}

const rulesStep1 = computed<FormRules>(() => ({
  email: [
    {required: true, message: t('login.validation.emailRequired'), trigger: 'blur'},
    {type: 'email', message: t('login.validation.emailInvalid'), trigger: 'blur'},
  ],
  code: [
    {required: true, message: t('login.validation.codeRequired'), trigger: 'blur'},
    {len: 6, message: t('login.validation.codeLength'), trigger: 'blur'},
  ],
}))

const rulesStep2 = computed<FormRules>(() => ({
  password: [
    {required: true, message: t('login.validation.newPasswordRequired'), trigger: 'blur'},
    {min: 6, max: 20, message: t('login.validation.passwordLength'), trigger: 'blur'},
  ],
  confirmPassword: [
    {required: true, message: t('login.validation.confirmPasswordRequired'), trigger: 'blur'},
    {validator: validateConfirmPassword, trigger: 'blur'},
  ],
}))

const currentRules = computed(() => step.value === 1 ? rulesStep1.value : rulesStep2.value)

// 响应式placeholder
const placeholders = computed(() => ({
  email: t('resetPassword.email'),
  code: t('resetPassword.code'),
  password: t('resetPassword.password'),
  confirmPassword: t('resetPassword.confirmPassword'),
}))

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
    <div class="text-center mb-6">
      <h2 class="text-2xl font-bold mb-2 form-title">{{ $t('resetPassword.title') }}</h2>
      <p class="text-sm form-subtitle">{{ step === 1 ? $t('resetPassword.subtitle1') : $t('resetPassword.subtitle2') }}</p>
    </div>

    <!-- 步骤指示器 -->
    <div class="flex items-center justify-center mb-8">
      <div :class="['step', { active: step >= 1 }]">
        <span class="step-num">1</span>
        <span class="text-sm">{{ $t('resetPassword.step1Label') }}</span>
      </div>
      <div :class="['step-line', { active: step >= 2 }]"></div>
      <div :class="['step', { active: step >= 2 }]">
        <span class="step-num">2</span>
        <span class="text-sm">{{ $t('resetPassword.step2Label') }}</span>
      </div>
    </div>

    <!-- 表单 -->
    <el-form ref="formRef" :model="form" :rules="currentRules" size="large">
      <template v-if="step === 1">
        <el-form-item prop="email">
          <div class="input-wrapper">
            <el-icon class="input-icon">
              <Message/>
            </el-icon>
            <el-input v-model="form.email" :placeholder="placeholders.email" class="custom-input"/>
          </div>
        </el-form-item>

        <el-form-item prop="code">
          <div class="flex gap-3 items-stretch w-full">
            <div class="input-wrapper flex-1 min-w-0">
              <el-icon class="input-icon">
                <Key/>
              </el-icon>
              <el-input v-model="form.code" :placeholder="placeholders.code" class="custom-input"/>
            </div>
            <button type="button" class="code-btn" :disabled="!canSendCode || sendingCode" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s` : $t('resetPassword.sendCode') }}
            </button>
          </div>
        </el-form-item>
      </template>

      <template v-else>
        <el-form-item prop="password">
          <div class="input-wrapper">
            <el-icon class="input-icon">
              <Lock/>
            </el-icon>
            <el-input v-model="form.password" type="password" :placeholder="placeholders.password" show-password class="custom-input"/>
          </div>
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <div class="input-wrapper">
            <el-icon class="input-icon">
              <Lock/>
            </el-icon>
            <el-input v-model="form.confirmPassword" type="password" :placeholder="placeholders.confirmPassword" show-password
                      class="custom-input"/>
          </div>
        </el-form-item>
      </template>

      <el-form-item class="mt-8">
        <div class="flex gap-3 w-full">
          <button type="button" class="back-btn" @click="handleBack">
            {{ step === 1 ? $t('resetPassword.backToLogin') : $t('resetPassword.prevStep') }}
          </button>
          <button type="button" class="submit-btn" :disabled="loading" @click="handleSubmit">
            {{ step === 1 ? $t('resetPassword.next') : $t('resetPassword.reset') }}
          </button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
/* 主题色 */
.form-title {
  color: var(--login-form-title);
}

.form-subtitle {
  color: var(--login-form-subtitle);
}

/* 步骤指示器 */
.step {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--login-step-color);
  transition: color 0.3s;
}

.step.active {
  color: var(--login-step-active);
}

.step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--login-step-num-bg);
  border: 1px solid var(--login-step-num-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.3s;
}

.step.active .step-num {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.step-line {
  width: 50px;
  height: 2px;
  background: var(--login-step-line-bg);
  margin: 0 12px;
  border-radius: 1px;
  transition: background 0.3s;
}

.step-line.active {
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
}

/* 输入框图标 */
.input-wrapper {
  position: relative;
  width: 100%;
}

.input-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  color: var(--login-input-icon);
  font-size: 18px;
  transition: color 0.3s;
}

.input-wrapper:focus-within .input-icon {
  color: #6366f1;
}

/* 自定义输入框 */
.custom-input :deep(.el-input__wrapper) {
  background: var(--login-input-bg);
  border: 1px solid var(--login-input-border);
  border-radius: 12px;
  padding: 4px 16px 4px 44px;
  box-shadow: none;
  transition: all 0.3s;
}

.custom-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(99, 102, 241, 0.5);
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
}

.custom-input :deep(.el-input__inner) {
  color: var(--login-input-text);
  font-size: 15px;
}

.custom-input :deep(.el-input__inner::placeholder) {
  color: var(--login-input-placeholder);
}

/* 发送按钮 */
.code-btn {
  width: 90px;
  background: var(--login-code-btn-bg);
  border: 1px solid var(--login-code-btn-border);
  border-radius: 12px;
  color: var(--login-code-btn-text);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  flex-shrink: 0;
}

.code-btn:hover:not(:disabled) {
  background: rgba(99, 102, 241, 0.3);
  border-color: #6366f1;
}

.code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 按钮 */
.back-btn {
  flex: 1;
  height: 48px;
  background: var(--login-back-btn-bg);
  border: 1px solid var(--login-back-btn-border);
  border-radius: 12px;
  color: var(--login-back-btn-text);
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s;
}

.back-btn:hover {
  background: rgba(99, 102, 241, 0.15);
  border-color: rgba(99, 102, 241, 0.3);
  transform: translateY(-1px);
}

.submit-btn {
  flex: 2;
  height: 48px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  border-radius: 12px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.submit-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 50%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transform: translateX(-100%);
  border-radius: inherit;
}

.submit-btn:hover:not(:disabled)::after {
  animation: shimmer 0.8s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(99, 102, 241, 0.35);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__error) {
  color: #f87171;
  font-size: 12px;
}
</style>
