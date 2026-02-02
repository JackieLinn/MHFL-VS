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
  <div class="reset-form">
    <!-- 标题 -->
    <div class="form-header">
      <h2 class="form-title">重置密码</h2>
      <p class="form-subtitle">
        {{ step === 1 ? '输入邮箱验证身份' : '设置您的新密码' }}
      </p>
    </div>

    <!-- 步骤指示器 -->
    <div class="steps">
      <div :class="['step', { active: step >= 1 }]">
        <span class="step-num">1</span>
        <span class="step-text">验证邮箱</span>
      </div>
      <div :class="['step-line', { active: step >= 2 }]"></div>
      <div :class="['step', { active: step >= 2 }]">
        <span class="step-num">2</span>
        <span class="step-text">设置密码</span>
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
            <el-input v-model="form.email" placeholder="注册时的邮箱地址" class="custom-input"/>
          </div>
        </el-form-item>

        <el-form-item prop="code">
          <div class="code-row">
            <div class="input-wrapper flex-1">
              <el-icon class="input-icon">
                <Key/>
              </el-icon>
              <el-input v-model="form.code" placeholder="邮箱验证码" class="custom-input"/>
            </div>
            <button type="button" class="code-btn" :disabled="!canSendCode || sendingCode" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s` : '发送' }}
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
            <el-input v-model="form.password" type="password" placeholder="新密码" show-password class="custom-input"/>
          </div>
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <div class="input-wrapper">
            <el-icon class="input-icon">
              <Lock/>
            </el-icon>
            <el-input v-model="form.confirmPassword" type="password" placeholder="确认新密码" show-password
                      class="custom-input"/>
          </div>
        </el-form-item>
      </template>

      <el-form-item class="btn-group-item">
        <div class="btn-group">
          <button type="button" class="back-btn" @click="handleBack">
            {{ step === 1 ? '返回登录' : '上一步' }}
          </button>
          <button type="button" class="submit-btn" :disabled="loading" @click="handleSubmit">
            {{ step === 1 ? '下一步' : '重置密码' }}
          </button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
/* 使用全局主题变量 */
.reset-form {
  width: 100%;
}

.form-header {
  text-align: center;
  margin-bottom: 24px;
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--login-form-title);
  margin-bottom: 8px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--login-form-subtitle);
}

/* 步骤指示器 */
.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32px;
}

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

.step-text {
  font-size: 13px;
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

/* 输入框 */
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

/* 验证码行 */
.code-row {
  display: flex;
  gap: 12px;
}

.code-btn {
  width: 90px;
  height: 42px;
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

/* 按钮组 */
.btn-group-item {
  margin-top: 32px;
}

.btn-group {
  display: flex;
  gap: 12px;
  width: 100%;
}

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
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(99, 102, 241, 0.3);
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
