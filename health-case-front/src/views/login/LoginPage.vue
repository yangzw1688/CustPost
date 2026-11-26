<template>
  <main class="login-page original-page">
    <header class="logo-area">
      <span class="logo" aria-hidden="true">创</span>
      <h1>健康服务</h1>
      <p>为您提供便捷、专业的健康服务</p>
    </header>

    <form class="login-form" @submit.prevent="submitLogin">
      <div class="form-field">
        <label for="login-phone">手机号</label>
        <div class="form-item">
          <el-icon aria-hidden="true"><Iphone /></el-icon>
          <input id="login-phone" v-model="phone" name="phone" type="tel" inputmode="numeric" maxlength="11"
            autocomplete="tel" placeholder="请输入手机号" :disabled="loading || sending" required
            @input="phone = phone.replace(/\D/g, '').slice(0, 11)" />
        </div>
      </div>

      <div class="form-field">
        <label for="login-code">验证码</label>
        <div class="form-item">
          <el-icon aria-hidden="true"><Key /></el-icon>
          <input id="login-code" v-model="code" name="code" type="text" inputmode="numeric" maxlength="6"
            autocomplete="one-time-code" placeholder="请输入短信中的六位验证码" :disabled="loading || sending" required
            @input="code = code.replace(/\D/g, '').slice(0, 6)" />
          <button type="button" class="code-btn" :disabled="countdown > 0 || sending || loading" @click="getCode">
            {{ sending ? '获取中…' : countdown > 0 ? countdown + ' 秒后重试' : '获取验证码' }}
          </button>
        </div>
      </div>

      <p v-if="message" class="login-message" role="status">{{ message }}</p>
      <p v-if="errorMessage" class="login-error" role="alert">{{ errorMessage }}</p>
      <button type="submit" class="login-btn" :disabled="loading || sending">{{ loading ? '登录中…' : '登录' }}</button>
    </form>
  </main>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Iphone, Key } from '@element-plus/icons-vue'
import { login, safeRedirect, sendCode, PHONE_PATTERN, CODE_PATTERN } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const phone = ref('')
const code = ref('')
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const message = ref('')
const errorMessage = ref(route.query.reason === 'unavailable' ? '暂时无法连接服务，请稍后重试。' : '')
let timer
let disposed = false

watch(phone, () => {
  code.value = ''
  message.value = ''
  errorMessage.value = ''
})

async function getCode() {
  if (sending.value || countdown.value > 0 || loading.value) return
  message.value = ''
  errorMessage.value = ''
  if (!PHONE_PATTERN.test(phone.value)) {
    errorMessage.value = '请输入正确的 11 位手机号'
    return
  }
  sending.value = true
  try {
    const response = await sendCode(phone.value)
    if (disposed) return
    code.value = ''
    message.value = response?.message || '验证码已发送'
    countdown.value = response?.cooldownSeconds ?? 60
    timer = setInterval(() => { if (--countdown.value <= 0) clearInterval(timer) }, 1000)
  } catch (error) {
    if (!disposed) errorMessage.value = error.message || '获取验证码失败，请重试'
  } finally {
    if (!disposed) sending.value = false
  }
}

async function submitLogin() {
  if (loading.value || sending.value) return
  if (!PHONE_PATTERN.test(phone.value) || !CODE_PATTERN.test(code.value)) {
    errorMessage.value = '请填写有效的手机号和六位验证码'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    await login(phone.value, code.value)
    if (!disposed) await router.replace(safeRedirect(route.query.redirect, router))
  } catch (error) {
    if (!disposed) errorMessage.value = error.message || '登录失败，请重试'
  } finally {
    if (!disposed) loading.value = false
  }
}
onBeforeUnmount(() => {
  disposed = true
  clearInterval(timer)
})
</script>

<style scoped>
#app .login-page { padding: clamp(34px, 11vh, 80px) 24px 40px; background: #fff; }
.logo-area { text-align: center; }
.logo { width: 68px; height: 68px; margin: auto; border-radius: 18px; display: grid; place-items: center; color: #fff; font-size: 31px; font-weight: 700; background: linear-gradient(135deg, #1677ff, #69b1ff); box-shadow: 0 8px 20px #1677ff2e; }
.logo-area h1 { margin: 18px 0 0; color: #172b4d; font-size: 24px; }
.logo-area p { margin: 8px 0 0; color: #61728a; font-size: 13px; line-height: 1.6; }
.login-form { margin: 34px 0 0; }
.form-field + .form-field { margin-top: 16px; }
.form-field > label { display: block; margin-bottom: 8px; color: #34465d; font-size: 14px; font-weight: 600; }
.form-item { min-height: 54px; display: flex; align-items: center; gap: 10px; padding: 0 12px; border: 1px solid #e7edf3; border-radius: 10px; background: #f7f8fa; }
.form-item:focus-within { border-color: var(--hc-primary); background: #fff; box-shadow: 0 0 0 3px #1677ff14; }
.form-item > .el-icon { flex-shrink: 0; color: #61728a; font-size: 19px; }
.form-item input { flex: 1; width: 0; padding: 0; border: 0; outline: none; min-height: 52px; background: transparent; color: #1d2129; font-size: 16px; }
.form-item input::placeholder { color: #78869a; }
.code-btn { flex-shrink: 0; min-height: 44px; padding: 0; border: 0; background: transparent; color: #1264df; font-size: 12px; white-space: nowrap; }
.code-btn:disabled { color: #6f7b8c; }
.login-btn { width: 100%; min-height: 50px; margin-top: 22px; border: 0; border-radius: 10px; background: var(--hc-primary); color: #fff; font-size: 16px; font-weight: 600; box-shadow: 0 5px 14px #1677ff2e; }
.login-btn:disabled { opacity: .65; }
.login-message, .login-error { margin: 12px 0 0; padding: 10px 12px; border-radius: 8px; font-size: 13px; line-height: 1.7; overflow-wrap: anywhere; }
.login-message { color: #286442; background: #edf8f1; }
.login-error { color: #a33b3b; background: #fff2f2; }
@media (max-width: 360px) {
  #app .login-page { padding-inline: 16px; }
  .form-item { gap: 8px; }
}
</style>

