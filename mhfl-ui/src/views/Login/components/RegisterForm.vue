<script setup lang="ts">
import {ref, reactive, computed} from 'vue'
import {User, Lock, Message, Phone, Key} from '@element-plus/icons-vue'
import {register, sendEmailCode, type RegisterParams} from '@/api/auth'
import type {FormInstance, FormRules} from 'element-plus'

const emit = defineEmits<{
  (e: 'switch', panel: 'login'): void
}>()

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
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {min: 3, max: 20, message: '3-20个字符', trigger: 'blur'},
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, max: 20, message: '6-20个字符', trigger: 'blur'},
  ],
  confirmPassword: [
    {required: true, message: '请确认密码', trigger: 'blur'},
    {validator: validateConfirmPassword, trigger: 'blur'},
  ],
  email: [
    {required: true, message: '请输入邮箱', trigger: 'blur'},
    {type: 'email', message: '邮箱格式不正确', trigger: 'blur'},
  ],
  telephone: [
    {required: true, message: '请输入手机号', trigger: 'blur'},
    {pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur'},
  ],
  code: [
    {required: true, message: '请输入验证码', trigger: 'blur'},
    {len: 6, message: '6位数字', trigger: 'blur'},
  ],
}

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
  <div class="register-form">
    <!-- 标题 -->
    <div class="form-header">
      <h2 class="form-title">创建账号</h2>
      <p class="form-subtitle">加入 MHFL-VS 可视化仿真平台</p>
    </div>

    <!-- 表单 -->
    <el-form ref="formRef" :model="form" :rules="rules" size="default" class="compact-form">
      <div class="form-row">
        <el-form-item prop="username" class="form-col">
          <div class="input-wrapper">
            <el-icon class="input-icon">
              <User/>
            </el-icon>
            <el-input v-model="form.username" placeholder="用户名" class="custom-input"/>
          </div>
        </el-form-item>
        <el-form-item prop="telephone" class="form-col">
          <div class="input-wrapper">
            <el-icon class="input-icon">
              <Phone/>
            </el-icon>
            <el-input v-model="form.telephone" placeholder="手机号" class="custom-input"/>
          </div>
        </el-form-item>
      </div>

      <el-form-item prop="email">
        <div class="input-wrapper">
          <el-icon class="input-icon">
            <Message/>
          </el-icon>
          <el-input v-model="form.email" placeholder="邮箱地址" class="custom-input"/>
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

      <div class="form-row">
        <el-form-item prop="password" class="form-col">
          <div class="input-wrapper">
            <el-icon class="input-icon">
              <Lock/>
            </el-icon>
            <el-input v-model="form.password" type="password" placeholder="密码" show-password class="custom-input"/>
          </div>
        </el-form-item>
        <el-form-item prop="confirmPassword" class="form-col">
          <div class="input-wrapper">
            <el-icon class="input-icon">
              <Lock/>
            </el-icon>
            <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password
                      class="custom-input"/>
          </div>
        </el-form-item>
      </div>

      <el-form-item class="submit-item">
        <button type="button" class="submit-btn" :disabled="loading" @click="handleSubmit">
          <span v-if="!loading">注 册</span>
          <span v-else>注册中...</span>
        </button>
      </el-form-item>

      <div class="form-footer">
        <span class="footer-text">已有账号？</span>
        <span class="link-text" @click="emit('switch', 'login')">返回登录</span>
      </div>
    </el-form>
  </div>
</template>

<style scoped>
/* 使用全局主题变量 */
.register-form {
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

/* 紧凑表单 */
.compact-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

/* 双列布局 */
.form-row {
  display: flex;
  gap: 12px;
}

.form-col {
  flex: 1;
}

/* 输入框 */
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

/* 验证码行 */
.code-row {
  display: flex;
  gap: 10px;
}

.code-btn {
  width: 80px;
  height: 34px;
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
.submit-item {
  margin-top: 24px;
  margin-bottom: 16px;
}

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
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(99, 102, 241, 0.3);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 底部 */
.form-footer {
  text-align: center;
}

.footer-text {
  font-size: 14px;
  color: var(--login-footer-text);
  margin-right: 4px;
}

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
