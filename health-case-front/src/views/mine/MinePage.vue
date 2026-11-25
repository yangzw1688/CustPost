<template>
  <main class="mine-view original-page"><header><RouterLink to="/" aria-label="返回首页"><el-icon><ArrowLeft /></el-icon></RouterLink><strong>个人信息</strong><span class="header-spacer"></span></header><section class="welcome-card"><span class="avatar"><el-icon><User /></el-icon></span><div><h1>{{ maskedPhone }}</h1><p>服务办理与进度查询，一处轻松管理。</p></div></section><h2>服务管理</h2><section class="mine-links"><RouterLink v-for="item in entries" :key="item.to" :to="item.to"><span class="entry-icon"><el-icon><component :is="item.icon" /></el-icon></span><div><strong>{{ item.title }}</strong><p>{{ item.description }}</p></div><el-icon><ArrowRight /></el-icon></RouterLink></section><h2>服务帮助</h2><a class="support-link" href="https://work.weixin.qq.com/kfid/kfcbca36d8d108da2db" target="_blank" rel="noopener noreferrer"><el-icon><Service /></el-icon><div><strong>在线客服</strong><p>咨询资料提交、保单和服务相关问题</p></div><el-icon><ArrowRight /></el-icon></a><el-button class="logout-button" :loading="loggingOut" @click="signOut">退出登录</el-button><p v-if="logoutError" class="logout-error" role="alert">{{ logoutError }}</p><p class="footnote">感谢您的信任，愿每一份关怀都及时抵达。</p><nav class="tab-bar" aria-label="底部导航"><RouterLink to="/" class="tab-item"><el-icon :size="23"><House /></el-icon><span>首页</span></RouterLink><RouterLink to="/mine" class="tab-item active"><el-icon :size="23"><User /></el-icon><span>个人信息</span></RouterLink></nav></main>
</template>
<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { auth, logout } from '@/utils/auth'
const router = useRouter()
const loggingOut = ref(false)
const logoutError = ref('')
const maskedPhone = computed(() => auth.user?.phone ? auth.user.phone.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2') : '健康服务用户')
async function signOut() {
  if (loggingOut.value) return
  loggingOut.value = true
  logoutError.value = ''
  try { await logout(); await router.replace('/login') }
  catch (error) { logoutError.value = error.message || '退出失败，请重试' }
  finally { loggingOut.value = false }
}
import { ArrowLeft, ArrowRight, User, Document, Tickets, UploadFilled, Service, House } from '@element-plus/icons-vue'
const entries = [
  { to: '/service-record', title: '服务记录', description: '查看提交资料与处理状态', icon: Document },
  { to: '/policy', title: '保单查询', description: '了解保单信息与保险期间', icon: Tickets },
  { to: '/submit', title: '资料提交', description: '准备清晰图片，在线提交病例', icon: UploadFilled }
]
</script>
<style scoped>
.mine-view { min-height: 100vh; width: min(100%, 720px); margin: auto; padding: 0 16px 85px; }
header { display: flex; align-items: center; justify-content: space-between; min-height: 57px; }
header a, .header-spacer { display: grid; place-items: center; width: 32px; }
.welcome-card, .mine-links, .support-link { border: 1px solid #e6edf5; border-radius: 14px; background: white; }
.welcome-card { display: flex; gap: 16px; align-items: center; padding: 20px; background: #edf5ff; }
.avatar, .entry-icon { display: grid; place-items: center; color: #1677ff; background: #e5f0ff; width: 40px; height: 40px; border-radius: 12px; }
h1 { font-size: 20px; margin: 0; } h2 { font-size: 16px; margin-top: 24px; }
p { margin: 5px 0 0; color: #60738a; font-size: 13px; }
.mine-links a, .support-link { display: flex; align-items: center; gap: 12px; padding: 14px; }
.mine-links a + a { border-top: 1px solid #edf1f6; }
.mine-links a > div, .support-link > div { flex: 1; }
.logout-button { width: 100%; margin-top: 22px; }
.logout-error { color: #b93838; } .footnote { margin-top: 14px; text-align: center; }
.tab-bar { position: fixed; inset: auto 0 0; display: flex; justify-content: center; gap: 60px; padding: 10px 0 calc(10px + env(safe-area-inset-bottom)); background: white; border-top: 1px solid #e6edf5; }
.tab-item { display: grid; justify-items: center; gap: 4px; font-size: 12px; color: #60738a; }
.tab-item.active { color: #1677ff; }
</style>
