<script setup lang="ts">
import {ref, reactive, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {User, Lock, Picture, Loading} from '@element-plus/icons-vue'
import {getCaptcha, login, type LoginParams, type CaptchaData} from '@/api/auth'
import type {FormInstance, FormRules} from 'element-plus'

const emit = defineEmits<{
  (e: 'switch', panel: 'register' | 'reset'): void
}>()

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaLoading = ref(false)
const captchaImage = ref('')

const form = reactive<LoginParams>({
  username: '',
  password: '',
  captchaId: '',
  captchaCode: '',
  remember: false,
})

const rules: FormRules<LoginParams> = {
  username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码至少6位', trigger: 'blur'},
  ],
  captchaCode: [{required: true, message: '请输入验证码', trigger: 'blur'}],
}

const refreshCaptcha = () => {
  captchaLoading.value = true
  getCaptcha(
      (data: CaptchaData) => {
        form.captchaId = data.captchaId
        captchaImage.value = data.captchaImage
        captchaLoading.value = false
      },
      () => {
        captchaLoading.value = false
      }
  )
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (!valid) return
    loading.value = true
    login(form,
        () => {
          loading.value = false
          router.push('/home')
        },
        (message) => {
          loading.value = false
          form.captchaCode = ''

          const needRefresh = message.includes('过期') ||
              message.includes('无效') ||
              message.includes('请刷新')
          if (needRefresh) {
            refreshCaptcha()
          }
        }
    )
  })
}

onMounted(() => refreshCaptcha())
</script>

<template>
  <div class="login-form">
    <!-- 标题 -->
    <div class="form-header">
      <h2 class="form-title">欢迎回来</h2>
      <p class="form-subtitle">MHFL-VS 可视化仿真平台</p>
    </div>

    <!-- 表单 -->
    <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleSubmit">
      <el-form-item prop="username">
        <div class="input-wrapper">
          <el-icon class="input-icon">
            <User/>
          </el-icon>
          <el-input
              v-model="form.username"
              placeholder="用户名 / 邮箱 / 手机号"
              class="custom-input"
          />
        </div>
      </el-form-item>

      <el-form-item prop="password">
        <div class="input-wrapper">
          <el-icon class="input-icon">
            <Lock/>
          </el-icon>
          <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              show-password
              class="custom-input"
          />
        </div>
      </el-form-item>

      <el-form-item prop="captchaCode">
        <div class="captcha-row">
          <div class="input-wrapper">
            <el-icon class="input-icon">
              <Picture/>
            </el-icon>
            <el-input
                v-model="form.captchaCode"
                placeholder="图形验证码"
                class="custom-input"
            />
          </div>
          <div class="captcha-box" @click="refreshCaptcha">
            <img v-if="captchaImage && !captchaLoading" :src="captchaImage" alt="验证码"/>
            <el-icon v-else-if="captchaLoading" class="captcha-loading">
              <Loading/>
            </el-icon>
            <span v-else class="captcha-placeholder">点击获取</span>
          </div>
        </div>
      </el-form-item>

      <div class="form-options">
        <el-checkbox v-model="form.remember" class="custom-checkbox">记住我</el-checkbox>
        <span class="link-text" @click="emit('switch', 'reset')">忘记密码？</span>
      </div>

      <el-form-item>
        <button type="button" class="submit-btn" :disabled="loading" @click="handleSubmit">
          <span v-if="!loading">登 录</span>
          <el-icon v-else class="btn-loading">
            <Loading/>
          </el-icon>
        </button>
      </el-form-item>

      <div class="form-footer">
        <span class="footer-text">还没有账号？</span>
        <span class="link-text" @click="emit('switch', 'register')">立即注册</span>
      </div>
    </el-form>
  </div>
</template>

<style scoped>
/* 使用全局主题变量 */
.login-form {
  width: 100%;
}

/* 标题 */
.form-header {
  text-align: center;
  margin-bottom: 32px;
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

/* 输入框容器 */
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

/* 自定义输入框样式 */
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
.captcha-row {
  display: flex;
  gap: 12px;
  align-items: stretch;
  width: 100%;
}

.captcha-row .input-wrapper {
  flex: 1;
  min-width: 0;
}

.captcha-box {
  width: 120px;
  border-radius: 12px;
  background: var(--login-input-bg);
  border: 1px solid var(--login-input-border);
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s;
}

.captcha-box:hover {
  border-color: rgba(99, 102, 241, 0.5);
}

.captcha-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-loading {
  color: #6366f1;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.captcha-placeholder {
  font-size: 12px;
  color: var(--login-input-placeholder);
}

/* 表单选项 */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.custom-checkbox :deep(.el-checkbox__label) {
  color: var(--login-checkbox-label);
  font-size: 14px;
}

.custom-checkbox :deep(.el-checkbox__inner) {
  background: var(--login-checkbox-bg);
  border-color: var(--login-checkbox-border);
}

.custom-checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: #6366f1;
  border-color: #6366f1;
}

/* 链接文字 */
.link-text {
  font-size: 14px;
  color: var(--login-link-color);
  cursor: pointer;
  transition: color 0.3s;
}

.link-text:hover {
  color: var(--login-link-hover);
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  border-radius: 12px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
}

.submit-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #818cf8 0%, #a78bfa 100%);
  opacity: 0;
  transition: opacity 0.3s;
}

.submit-btn:hover::before {
  opacity: 1;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(99, 102, 241, 0.3);
}

.submit-btn:active {
  transform: translateY(0);
}

.submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.submit-btn span,
.submit-btn .el-icon {
  position: relative;
  z-index: 1;
}

.btn-loading {
  animation: spin 1s linear infinite;
}

/* 底部 */
.form-footer {
  text-align: center;
  margin-top: 24px;
}

.footer-text {
  font-size: 14px;
  color: var(--login-footer-text);
  margin-right: 4px;
}

/* 表单项间距 */
:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__error) {
  color: #f87171;
  font-size: 12px;
  padding-top: 4px;
}
</style>
