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
          
          // 只有在验证码过期或用完时才刷新，普通错误让用户继续尝试
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
  <div class="w-full">
    <!-- 标题 -->
    <div class="text-center mb-6">
      <h2 class="text-2xl font-semibold text-gray-800 dark:text-white mb-1">欢迎登录</h2>
      <p class="text-sm text-gray-500 dark:text-gray-400">模型异构联邦学习全链路可视化与仿真平台</p>
    </div>

    <!-- 表单 -->
    <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleSubmit">
      <el-form-item prop="username">
        <el-input v-model="form.username" placeholder="用户名 / 邮箱 / 手机号" :prefix-icon="User"/>
      </el-form-item>

      <el-form-item prop="password">
        <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock"/>
      </el-form-item>

      <el-form-item prop="captchaCode">
        <div class="flex gap-3 w-full">
          <el-input v-model="form.captchaCode" placeholder="图形验证码" :prefix-icon="Picture" class="flex-1"/>
          <div
              class="w-28 h-10 rounded-lg cursor-pointer overflow-hidden bg-gray-100 dark:bg-gray-700 flex-center border border-gray-200 dark:border-gray-600 hover:border-indigo-400 dark:hover:border-indigo-500 transition-colors flex-shrink-0"
              @click="refreshCaptcha"
          >
            <img v-if="captchaImage && !captchaLoading" :src="captchaImage" alt="验证码"
                 class="w-full h-full object-cover"/>
            <el-icon v-else-if="captchaLoading" class="animate-spin text-indigo-500">
              <Loading/>
            </el-icon>
            <span v-else class="text-xs text-gray-400">点击获取</span>
          </div>
        </div>
      </el-form-item>

      <div class="flex-between mb-4">
        <el-checkbox v-model="form.remember">记住我</el-checkbox>
        <span class="text-sm text-indigo-500 hover:text-indigo-600 cursor-pointer transition-colors"
              @click="emit('switch', 'reset')">
          忘记密码？
        </span>
      </div>

      <el-form-item>
        <el-button type="primary" class="btn-primary" :loading="loading" @click="handleSubmit">
          登 录
        </el-button>
      </el-form-item>

      <div class="text-center text-sm text-gray-500 dark:text-gray-400">
        还没有账号？
        <span class="text-indigo-500 hover:text-indigo-600 cursor-pointer transition-colors ml-1"
              @click="emit('switch', 'register')">
          立即注册
        </span>
      </div>
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

:deep(.el-checkbox__label) {
  color: var(--el-text-color-regular);
}
</style>
