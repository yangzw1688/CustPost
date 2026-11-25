<template>
  <div class="login-page">
    <div class="login-box">
      <h1>创新支付健康管理平台</h1>
      <form @submit.prevent="submitLogin">
        <el-form-item>
          <el-input v-model="phone" placeholder="请输入手机号" aria-label="手机号" type="tel"
            maxlength="11" autocomplete="username" :disabled="loading"
            @input="phone = phone.replace(/\D/g, '').slice(0, 11)" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" placeholder="请输入管理员密码" aria-label="管理员密码" type="password"
            show-password maxlength="128" autocomplete="current-password" :disabled="loading" />
        </el-form-item>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <el-button type="primary" native-type="submit" class="login-button" :loading="loading" :disabled="!canLogin">{{ loading ? '正在登录…' : '登录' }}</el-button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, PHONE_PATTERN, safeRedirect } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const phone = ref('')
const password = ref('')
const loading = ref(false)
const error = ref(route.query.reason === 'unavailable' ? '暂时无法连接服务，请稍后重试' : '')
const canLogin = computed(() => PHONE_PATTERN.test(phone.value) && password.value.length > 0)
let disposed = false

async function submitLogin() {
  if (!canLogin.value || loading.value) return
  loading.value = true
  error.value = ''
  try {
    await login(phone.value, password.value)
    if (!disposed) await router.replace(safeRedirect(route.query.redirect, router, '/dashboard'))
  } catch (failure) {
    if (!disposed) error.value = failure.message || '登录失败，请重试'
  } finally {
    if (!disposed) loading.value = false
  }
}

onBeforeUnmount(() => { disposed = true })
</script>

<style scoped>
.login-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  padding: 24px;
}
.login-box {
  width: 400px;
  max-width: 100%;
  padding: 40px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 4px 20px #00000014;
}
h1 {
  margin: 0 0 30px;
  text-align: center;
  font-size: 22px;
  line-height: 1.5;
}
.error {
  color: var(--el-color-danger);
  font-size: 13px;
  line-height: 1.7;
}
.login-button {
  width: 100%;
}
@media (max-width: 480px) {
  .login-box {
    padding: 28px 20px;
  }
}
</style>
