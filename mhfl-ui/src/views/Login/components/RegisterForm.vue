<script setup lang="ts">
import {ref, reactive, computed} from 'vue'
import {User, Lock, Message, Phone, Key} from '@element-plus/icons-vue'
import {register, sendEmailCode, type RegisterParams} from '@/api/auth'
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

const form = reactive<RegisterParams & { confirmPassword: string }>({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  telephone: '',
  code: '',
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error(t('login.validation.passwordMismatch')))
  } else {
    callback()
  }
}

const rules = computed<FormRules>(() => ({
  username: [
    {required: true, message: t('login.validation.usernameRequired'), trigger: 'blur'},
    {min: 3, max: 20, message: t('login.validation.usernameLength'), trigger: 'blur'},
  ],
  password: [
    {required: true, message: t('login.validation.passwordRequired'), trigger: 'blur'},
    {min: 6, max: 20, message: t('login.validation.passwordLength'), trigger: 'blur'},
  ],
  confirmPassword: [
    {required: true, message: t('login.validation.confirmPasswordRequired'), trigger: 'blur'},
    {validator: validateConfirmPassword, trigger: 'blur'},
  ],
  email: [
    {required: true, message: t('login.validation.emailRequired'), trigger: 'blur'},
    {type: 'email', message: t('login.validation.emailInvalid'), trigger: 'blur'},
  ],
  telephone: [
    {required: true, message: t('login.validation.phoneRequired'), trigger: 'blur'},
    {pattern: /^1[3-9]\d{9}$/, message: t('login.validation.phoneInvalid'), trigger: 'blur'},
  ],
  code: [
    {required: true, message: t('login.validation.codeRequired'), trigger: 'blur'},
    {len: 6, message: t('login.validation.codeLength'), trigger: 'blur'},
  ],
}))

// 响应式placeholder
const placeholders = computed(() => ({
  username: t('register.username'),
  telephone: t('register.telephone'),
  email: t('register.email'),
  code: t('register.code'),
  password: t('register.password'),
  confirmPassword: t('register.confirmPassword'),
}))

const canSendCode = computed(() => {
  return form.email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email) && countdown.value === 0
})

const handleSendCode = () => {
  if (!canSendCode.value) return
  sendingCode.value = true
  sendEmailCode(form.email, 'register',
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

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (!valid) return
    loading.value = true
    register({
          username: form.username,
          password: form.password,
          email: form.email,
          telephone: form.telephone,
          code: form.code,
        },
        () => {
          loading.value = false
          emit('switch', 'login')
        },
        () => loading.value = false
    )
  })
}
</script>

<template>
  <div class="w-full">
    <!-- 标题 -->
    <div class="text-center mb-6">
      <h2 class="text-2xl font-bold mb-2 form-title">{{ $t('register.title') }}</h2>
      <p class="text-sm form-subtitle">{{ $t('register.subtitle') }}</p>
    </div>

    <!-- 表单 -->
    <el-form ref="formRef" :model="form" :rules="rules" size="default" class="compact-form">
      <div class="flex gap-3">
        <el-form-item prop="username" class="flex-1">
          <div class="input-wrapper">
            <el-icon class="input-icon">
              <User/>
            </el-icon>
            <el-input v-model="form.username" :placeholder="placeholders.username" class="custom-input"/>
          </div>
        </el-form-item>
        <el-form-item prop="telephone" class="flex-1">
          <div class="input-wrapper">
            <el-icon class="input-icon">
              <Phone/>
            </el-icon>
            <el-input v-model="form.telephone" :placeholder="placeholders.telephone" class="custom-input"/>
          </div>
        </el-form-item>
      </div>

      <el-form-item prop="email">
        <div class="input-wrapper">
          <el-icon class="input-icon">
            <Message/>
          </el-icon>
          <el-input v-model="form.email" :placeholder="placeholders.email" class="custom-input"/>
        </div>
      </el-form-item>

      <el-form-item prop="code">
        <div class="flex gap-2.5 items-stretch w-full">
          <div class="input-wrapper flex-1 min-w-0">
            <el-icon class="input-icon">
              <Key/>
            </el-icon>
            <el-input v-model="form.code" :placeholder="placeholders.code" class="custom-input"/>
          </div>
          <button type="button" class="code-btn" :disabled="!canSendCode || sendingCode" @click="handleSendCode">
            {{ countdown > 0 ? `${countdown}s` : $t('register.sendCode') }}
          </button>
        </div>
      </el-form-item>

      <div class="flex gap-3">
        <el-form-item prop="password" class="flex-1">
          <div class="input-wrapper">
            <el-icon class="input-icon">
              <Lock/>
            </el-icon>
            <el-input v-model="form.password" type="password" :placeholder="placeholders.password" show-password class="custom-input"/>
          </div>
        </el-form-item>
        <el-form-item prop="confirmPassword" class="flex-1">
          <div class="input-wrapper">
            <el-icon class="input-icon">
              <Lock/>
            </el-icon>
            <el-input v-model="form.confirmPassword" type="password" :placeholder="placeholders.confirmPassword" show-password
                      class="custom-input"/>
          </div>
        </el-form-item>
      </div>

      <el-form-item class="mt-6 mb-4">
        <button type="button" class="submit-btn" :disabled="loading" @click="handleSubmit">
          <span v-if="!loading">{{ $t('register.register') }}</span>
          <span v-else>{{ $t('register.registering') }}</span>
        </button>
      </el-form-item>

      <div class="text-center">
        <span class="text-sm mr-1 footer-text">{{ $t('register.hasAccount') }}</span>
        <span class="link-text" @click="emit('switch', 'login')">{{ $t('register.backToLogin') }}</span>
      </div>
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

.footer-text {
  color: var(--login-footer-text);
}

/* 紧凑表单 */
.compact-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

/* 输入框图标 */
.input-wrapper {
  position: relative;
  width: 100%;
}

.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  color: var(--login-input-icon);
  font-size: 16px;
  transition: color 0.3s;
}

.input-wrapper:focus-within .input-icon {
  color: #6366f1;
}

/* 自定义输入框 */
.custom-input :deep(.el-input__wrapper) {
  background: var(--login-input-bg);
  border: 1px solid var(--login-input-border);
  border-radius: 10px;
  padding: 2px 12px 2px 38px;
  box-shadow: none;
  transition: all 0.3s;
}

.custom-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(99, 102, 241, 0.5);
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.15);
}

.custom-input :deep(.el-input__inner) {
  color: var(--login-input-text);
  font-size: 14px;
}

.custom-input :deep(.el-input__inner::placeholder) {
  color: var(--login-input-placeholder);
}

/* 发送按钮 */
.code-btn {
  width: 80px;
  background: var(--login-code-btn-bg);
  border: 1px solid var(--login-code-btn-border);
  border-radius: 10px;
  color: var(--login-code-btn-text);
  font-size: 13px;
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

/* 提交按钮 */
.submit-btn {
  width: 100%;
  height: 44px;
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

.submit-btn span {
  position: relative;
  z-index: 1;
}

/* 链接 */
.link-text {
  font-size: 14px;
  color: var(--login-link-color);
  cursor: pointer;
  transition: color 0.3s;
}

.link-text:hover {
  color: var(--login-link-hover);
}

:deep(.el-form-item__error) {
  font-size: 11px;
  padding-top: 2px;
}
</style>
