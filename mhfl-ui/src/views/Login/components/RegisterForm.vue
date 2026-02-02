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
  <div class="w-full">
    <!-- 标题 -->
    <div class="text-center mb-5">
      <h2 class="text-2xl font-semibold text-gray-800 dark:text-white mb-1">创建账号</h2>
      <p class="text-sm text-gray-500 dark:text-gray-400">加入 MHFL 可视化仿真平台</p>
    </div>

    <!-- 表单 - 紧凑布局 -->
    <el-form ref="formRef" :model="form" :rules="rules" size="default" class="compact-form">
      <div class="grid grid-cols-2 gap-x-3">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User"/>
        </el-form-item>
        <el-form-item prop="telephone">
          <el-input v-model="form.telephone" placeholder="手机号" :prefix-icon="Phone"/>
        </el-form-item>
      </div>

      <el-form-item prop="email">
        <el-input v-model="form.email" placeholder="邮箱地址" :prefix-icon="Message"/>
      </el-form-item>

      <el-form-item prop="code">
        <div class="flex gap-2 w-full">
          <el-input v-model="form.code" placeholder="邮箱验证码" :prefix-icon="Key" class="flex-1"/>
          <el-button :disabled="!canSendCode" :loading="sendingCode" @click="handleSendCode" class="w-24 flex-shrink-0">
            {{ countdown > 0 ? `${countdown}s` : '发送' }}
          </el-button>
        </div>
      </el-form-item>

      <div class="grid grid-cols-2 gap-x-3">
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock"/>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password
                    :prefix-icon="Lock"/>
        </el-form-item>
      </div>

      <el-form-item class="mb-2">
        <el-button type="primary" class="w-full h-10 rounded-lg" :loading="loading" @click="handleSubmit">
          注 册
        </el-button>
      </el-form-item>

      <div class="text-center text-sm text-gray-500 dark:text-gray-400">
        已有账号？
        <span class="text-indigo-500 hover:text-indigo-600 cursor-pointer transition-colors ml-1"
              @click="emit('switch', 'login')">
          返回登录
        </span>
      </div>
    </el-form>
  </div>
</template>

<style scoped>
.compact-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.compact-form :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.compact-form :deep(.el-button) {
  border-radius: 8px;
}
</style>
