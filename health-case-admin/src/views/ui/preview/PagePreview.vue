<template>
  <template v-if="page">
    <PageContent :content="page.document" :images="page.images" :banners="page.banners" preview />
    <NoticeDialog v-if="page.document.notice" :open="noticeOpen" :notice="page.document.notice" :color="page.document.primaryColor" preview @dismiss="dismissNotice" />
  </template>
  <div v-else class="preview-placeholder">等待管理端发送页面预览</div>
</template>
<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { adminUiImageUrl } from '@/utils/ui-image-url'
import PageContent from './PageContent.vue'
import NoticeDialog from './NoticeDialog.vue'
import './preview-theme.css'
const page = ref(null)
const noticeOpen = ref(false)
function dismissNotice() {
  noticeOpen.value = false
  window.parent.postMessage({ type: 'USER_PAGE_NOTICE_CLOSED' }, window.location.origin)
}
function receive(event) {
  // 只接受同源父窗口的数据，发布内容仍由服务端单独校验。
  if (event.source !== window.parent || event.origin !== window.location.origin || event.data?.type !== 'USER_PAGE_PREVIEW') return
  const value = event.data.page
  if (!value?.document || !Array.isArray(value.document.blocks) || !/^#[0-9a-f]{6}$/i.test(value.document.primaryColor)) return
  noticeOpen.value = value.previewNotice === true
  const banners = value.banners?.map(banner => ({
    ...banner,
    imageUrl: adminUiImageUrl(banner.imageUrl)
  }))
  page.value = { ...value, banners }
}
onMounted(() => {
  document.documentElement.classList.add('mobile-page-preview')
  window.addEventListener('message', receive)
  window.parent.postMessage({ type: 'USER_PAGE_PREVIEW_READY' }, window.location.origin)
})
onBeforeUnmount(() => {
  document.documentElement.classList.remove('mobile-page-preview')
  window.removeEventListener('message', receive)
})
</script>
<style scoped>
.preview-placeholder { padding: 100px 20px; text-align: center; font-size: 13px; color: #8592a5; }
</style>
