<template>
  <main v-if="loading && !page" class="page-status original-page"><el-skeleton :rows="8" animated /></main>
  <main v-else-if="error && !page" class="page-status original-page" role="alert">
    <el-empty :description="error" />
    <el-button type="primary" @click="load">重新加载</el-button>
    <el-button v-if="slug !== 'home'" @click="router.push('/')">返回首页</el-button>
    <RouterLink v-else to="/mine" class="service-link">个人信息</RouterLink>
  </main>
  <template v-else-if="page">
    <div v-if="error" class="refresh-error" role="alert">{{ error }} <el-button text @click="load">重试</el-button></div>
    <PageContent :content="page.document" :images="page.images" :banners="page.banners" :home-active="slug === 'home'" @navigate="navigate" @refresh="load" />
    <NoticeDialog v-if="activeNotice" :open="noticeOpen" :notice="activeNotice" :color="page.document.primaryColor" @dismiss="dismissNotice" />
  </template>
</template>
<script setup>
/* global defineProps */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageContent from '@/components/ui/PageContent.vue'
import NoticeDialog from '@/components/ui/NoticeDialog.vue'
import { defaultHome } from '@/views/home/default-home'
import request from '@/utils/request'
import { auth } from '@/utils/auth'

const props = defineProps({ slug: { type: String, default: '' } })
const route = useRoute()
const router = useRouter()
const slug = computed(() => props.slug || String(route.params.slug || 'home'))
const page = ref(null)
const loading = ref(false)
const error = ref('')
const activeNotice = computed(() => {
  const notice = page.value?.document?.notice
  return notice?.enabled && notice.title && notice.message ? notice : null
})
const noticeOpen = ref(false)
const dismissalKey = computed(() => auth.user?.phone ? 'health-case:page-notice:' + auth.user.phone + ':' + slug.value : '')
const noticeSignature = computed(() => activeNotice.value ? JSON.stringify([activeNotice.value.title, activeNotice.value.message]) : '')
const dismissed = new Map()
let controller
async function load() {
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = ''
  try {
    const { data } = await request.get(`/api/ui/pages/${encodeURIComponent(slug.value)}/content`, { signal: current.signal })
    if (controller !== current || current.signal.aborted) return
    if (!data.data?.document || !Array.isArray(data.data.document.blocks)) throw new Error('页面配置格式不正确')
    page.value = data.data
    document.title = `${page.value.document.title} · 健康服务平台`
  } catch (err) {
    if (current.signal.aborted || controller !== current) return
    const missing = err.response?.status === 404 || err.response?.data?.code === 404
    if (slug.value === 'home' && missing) {
      if (!page.value) page.value = defaultHome
      document.title = `${page.value.document.title} · 健康服务平台`
    } else {
      error.value = err.response?.data?.message || err.message || '页面暂时无法加载'
    }
  } finally {
    if (controller === current) loading.value = false
  }
}
function dismissNotice() {
  noticeOpen.value = false
  if (!dismissalKey.value || !noticeSignature.value) return
  dismissed.set(dismissalKey.value, noticeSignature.value)
  try { sessionStorage.setItem(dismissalKey.value, noticeSignature.value) } catch { /* 浏览器禁用存储时保留当前页面的关闭状态。 */ }
}
watch([dismissalKey, noticeSignature], ([key, signature]) => {
  if (!key) {
    noticeOpen.value = false
    return
  }
  let previous = dismissed.get(key)
  try { previous = sessionStorage.getItem(key) || previous } catch { /* 浏览器禁用存储时使用内存状态。 */ }
  noticeOpen.value = !!signature && signature !== previous
})
function navigate(url, linkType = 'auto') {
  const target = url.trim()
  if (linkType === 'url') {
    if (/^https?:\/\//i.test(target) || target.startsWith('/') && !target.startsWith('//') && !/[\\\r\n\t]/.test(target)) {
      window.location.assign(target)
    } else ElMessage.info('跳转地址格式不正确，请联系管理员')
    return
  }
  if (linkType === 'page') {
    if (target.startsWith('/') && !target.startsWith('//') && !/[\\\r\n\t]/.test(target) && router.resolve(target).name !== 'NotFound') {
      router.push(target)
    } else ElMessage.info('所选页面暂时无法打开，请联系管理员')
    return
  }
  if (/^https?:\/\//i.test(target)) window.open(target, '_blank', 'noopener,noreferrer')
  else if (target.startsWith('/') && !target.startsWith('//') && !/[\\\r\n\t]/.test(target)) {
    if (router.resolve(target).name === 'NotFound') window.location.assign(target)
    else router.push(target)
  } else ElMessage.info('跳转地址格式不正确，请联系管理员')
}
watch(slug, () => { page.value = null; load() }, { immediate: true })
onBeforeUnmount(() => controller?.abort())
</script>
<style scoped>
.page-status { padding: 36px 20px; text-align: center; }
.service-link { display: inline-block; margin-left: 12px; color: var(--hc-primary); }
.refresh-error { max-width: 430px; margin: 0 auto; padding: 8px 15px; color: #8e3a32; background: #fff3f1; font-size: 13px; }
</style>
