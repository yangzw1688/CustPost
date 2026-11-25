<template>
  <main class="detail-page">
    <header class="page-header">
      <button type="button" class="back-button" aria-label="返回服务记录" @click="goBack"><el-icon><ArrowLeft /></el-icon></button><h1 class="page-title">服务详情</h1>
      <button type="button" class="refresh-button" :disabled="loading || refreshing" @click="loadDetail(false)"><el-icon :class="{ 'is-loading': refreshing }"><Refresh /></el-icon>刷新</button>
    </header>
    <div v-if="loading" class="loading-state" role="status" aria-live="polite"><el-skeleton :rows="6" animated /><p>正在获取服务详情…</p></div>
    <div v-else-if="!detail" class="empty-state">
      <span class="empty-icon"><el-icon><Document /></el-icon></span><h1>{{ errorMessage ? '暂时无法加载' : '未找到这条服务' }}</h1>
      <p role="alert">{{ errorMessage || '请检查服务记录，或返回后重新选择。' }}</p>
      <div><el-button @click="goBack">返回记录</el-button><el-button type="primary" @click="loadDetail()">重新加载</el-button></div>
    </div>
    <div v-else class="content">
      <div v-if="errorMessage" class="error-banner" role="alert"><span>{{ errorMessage }}</span><button type="button" @click="loadDetail(false)">重试</button></div>
      <section class="status-card">
        <div class="status-heading">
          <div class="status-icon" :class="`status-${detail.status}`"><el-icon><component :is="statusInfo.icon" /></el-icon></div>
          <h2 class="status-title">{{ statusInfo.text }}</h2>
        </div>
        <p class="status-description">{{ statusInfo.description }}</p>
        <ol v-if="progressIndex >= 0" class="progress-steps" aria-label="服务处理进度">
          <li v-for="(step, index) in progressSteps" :key="step.status" :class="{ finished: index < progressIndex || detail.status === 'COMPLETED', current: index === progressIndex }" :aria-current="index === progressIndex ? 'step' : undefined">
            <span class="step-dot"><el-icon v-if="index < progressIndex || detail.status === 'COMPLETED'"><Check /></el-icon><span v-else>{{ index + 1 }}</span></span><span>{{ step.title }}</span>
          </li>
        </ol>
      </section>
      <section class="section-card">
        <div class="section-heading"><span class="section-icon"><el-icon><Tickets /></el-icon></span><h2>服务信息</h2></div>
        <dl class="info-grid"><div><dt>服务编号</dt><dd>{{ detail.id || '—' }}</dd></div><div><dt>提交时间</dt><dd>{{ detail.createTime || '—' }}</dd></div><div><dt>更新时间</dt><dd>{{ detail.updateTime || '—' }}</dd></div></dl>
      </section>
      <section class="section-card">
        <div class="section-heading"><span class="section-icon"><el-icon><User /></el-icon></span><h2>患者信息</h2></div>
        <dl class="info-grid">
          <div><dt>患者姓名</dt><dd>{{ detail.name || '—' }}</dd></div>
          <div v-if="detail.reporterName"><dt>报案人</dt><dd>{{ detail.reporterName }}</dd></div>
          <div><dt>报案联系号码</dt><dd>{{ detail.phone || '—' }}</dd></div>
        </dl>
      </section>
      <section class="section-card">
        <div class="section-heading"><span class="section-icon"><el-icon><Document /></el-icon></span><h2>病例信息</h2></div>
        <dl class="info-grid"><div><dt>病例名称</dt><dd>{{ detail.caseTitle || '—' }}</dd></div><div><dt>就诊日期</dt><dd>{{ detail.visitDate || '未填写' }}</dd></div></dl>
        <div class="description-block"><h3>病例描述</h3><p>{{ detail.caseDescription || '暂无病例描述' }}</p></div>
      </section>
      <section class="section-card">
        <div class="section-heading"><span class="section-icon"><el-icon><Picture /></el-icon></span><h2>病例资料</h2><span class="section-note">{{ attachments.length }} 个文件</span></div>
        <template v-if="imagePreviewList.length">
          <p class="image-hint">点击图片可放大查看。</p>
          <div class="image-list">
            <div v-for="(image, index) in availableImages" :key="`${image.id || index}-${image.fileUrl}`" class="image-item">
              <el-image :src="image.fileUrl" :alt="`病例资料 ${index + 1}`" fit="cover" :preview-src-list="imagePreviewList" :initial-index="index" preview-teleported lazy @error="imageFailed = true">
                <template #error><div class="image-error"><el-icon><Picture /></el-icon><span>图片加载失败</span></div></template>
                <template #placeholder><div class="image-placeholder"><el-icon class="is-loading"><Loading /></el-icon></div></template>
              </el-image><span class="image-index">{{ String(index + 1).padStart(2, '0') }}</span>
            </div>
          </div>
          <div v-if="imageFailed" class="image-retry" role="status"><span>图片链接可能已失效，请刷新后重试。</span><button type="button" :disabled="refreshing" @click="loadDetail(false)">{{ refreshing ? '刷新中…' : '重新获取图片' }}</button></div>
        </template>
        <p v-else-if="!pdfFiles.length" class="no-images">暂无病例资料</p>
        <div v-if="pdfFiles.length" class="pdf-list">
          <a v-for="file in pdfFiles" :key="file.id || file.fileUrl" :href="file.fileUrl" target="_blank" rel="noopener noreferrer">
            <el-icon><Document /></el-icon><span>{{ file.originalName || file.fileName || 'PDF 病例资料' }}</span><strong>查看 PDF</strong>
          </a>
          <button type="button" class="pdf-refresh" :disabled="refreshing" @click="loadDetail(false)">重新获取 PDF 链接</button>
        </div>
      </section>
      <section v-if="detail.processRemark" class="section-card result-card"><div class="section-heading"><span class="section-icon"><el-icon><CircleCheck /></el-icon></span><h2>处理结果</h2></div><p class="remark">{{ detail.processRemark }}</p></section>
      <p class="footer-note">最近更新 {{ detail.updateTime || detail.createTime || '—' }}<br />处理状态以当前服务记录为准。</p>
    </div>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Check, CircleCheck, CircleClose, Clock, Document, Loading, Minus, Picture, Refresh, Setting, Tickets, User } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { isPdfAttachment } from '@/shared/files/case-files.mjs'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const refreshing = ref(false)
const detail = ref(null)
const errorMessage = ref('')
const imageFailed = ref(false)
let controller
const progressSteps = [{ status: 'PENDING', title: '审核中' }, { status: 'PROCESSING', title: '处理中' }, { status: 'COMPLETED', title: '已完成' }]
const progressIndex = computed(() => progressSteps.findIndex(step => step.status === detail.value?.status))
const statuses = {
  PENDING: { text: '资料审核中', description: '您的资料已提交，我们会在审核后更新处理进度。', icon: Clock },
  PROCESSING: { text: '服务处理中', description: '工作人员正在处理您的服务，请留意后续进度。', icon: Setting },
  COMPLETED: { text: '服务已完成', description: '本次服务已处理完成，感谢您的耐心等待。', icon: CircleCheck },
  REJECTED: { text: '审核未通过', description: '请查看下方处理结果，了解具体原因。', icon: CircleClose },
  CANCELLED: { text: '服务已取消', description: '本次服务已取消，您仍可查看提交的资料。', icon: Minus }
}
const statusInfo = computed(() => {
  if (detail.value?.status === 'PROCESSING' && detail.value.reviewDecision === 'APPROVED') {
    return { text: '审核通过，待赔付', description: '工作人员将办理线下赔付，请留意案件进度。', icon: Setting }
  }
  return statuses[detail.value?.status] || { text: '状态待确认', description: '请刷新页面获取最新服务状态。', icon: Clock }
})
const attachments = computed(() => Array.isArray(detail.value?.images)
  ? detail.value.images.map(file => ({ ...file, fileUrl: file.fileUrl || file.imageUrl })).filter(file => file.fileUrl)
  : [])
const pdfFiles = computed(() => attachments.value.filter(isPdfAttachment))
const availableImages = computed(() => attachments.value.filter(file => !isPdfAttachment(file)))
const imagePreviewList = computed(() => availableImages.value.map(image => image.fileUrl))
const goBack = () => router.push('/service-record')
const loadDetail = async (initial = true) => {
  controller?.abort()
  const current = new AbortController()
  controller = current
  if (initial) { loading.value = true; detail.value = null }
  else refreshing.value = true
  errorMessage.value = ''
  const id = String(route.params.id || '')
  try {
    if (!/^\d+$/.test(id) || /^0+$/.test(id)) throw new Error('服务编号不正确，请返回记录后重新选择。')
    const response = await request.get(`/api/service-record/${id}`, { signal: current.signal })
    if (controller !== current || current.signal.aborted) return
    if (response.data?.code !== 200) throw new Error(response.data?.message || '获取服务详情失败')
    detail.value = response.data.data || null
    imageFailed.value = false
  } catch (error) {
    if (controller !== current || current.signal.aborted) return
    errorMessage.value = error.response?.data?.message || error.message || '网络繁忙，请稍后重新加载。'
  } finally {
    if (controller === current) {
      loading.value = false
      refreshing.value = false
    }
  }
}
watch(() => route.params.id, () => loadDetail(), { immediate: true })
onBeforeUnmount(() => controller?.abort())
</script>

<style scoped>
#app .detail-page { min-height: 100dvh; }
.detail-page { width: 100%; max-width: 430px; margin: 0 auto; min-height: 100vh; background: #f5f7fa; color: var(--hc-ink, #243850); padding-bottom: calc(24px + env(safe-area-inset-bottom)); }
.page-header { position: sticky; top: 0; z-index: 10; height: 56px; display: flex; align-items: center; justify-content: space-between; background: #fffffff5; border-bottom: 1px solid #edf0f5; backdrop-filter: blur(12px); }
.back-button { display: grid; place-items: center; width: 52px; height: 52px; border: 0; color: var(--hc-ink, #243850); background: none; cursor: pointer; font-size: 21px; }
.page-title { margin: 0; font-size: 17px; font-weight: 600; }
.refresh-button { width: 52px; border: 0; background: none; color: var(--hc-primary, #1677ff); display: inline-flex; flex-direction: column; gap: 2px; align-items: center; justify-content: center; cursor: pointer; padding: 7px; font: inherit; font-size: 12px; min-height: 44px; }
.refresh-button:disabled { color: #aab9ce; cursor: default; }
.content { padding: 14px 12px 24px; }
.loading-state { padding: 50px 24px; color: #96a4b7; font-size: 12px; text-align: center; }
.loading-state p { margin-top: 30px; }
.empty-state { padding: 70px 24px; text-align: center; }
.empty-icon { display: grid; place-items: center; width: 64px; height: 64px; border-radius: 50%; background: #edf3fb; color: #96afce; margin: auto auto 22px; font-size: 28px; }
.empty-state h1 { font-size: 18px; margin: 0; }
.empty-state p { color: #95a3b6; font-size: 12px; line-height: 1.9; margin: 16px 0 24px; }
.error-banner { display: flex; align-items: center; gap: 12px; color: #b05542; background: #fff7f3; border: 1px solid #f3dbd3; padding: 12px; border-radius: 9px; font-size: 12px; line-height: 1.7; margin-bottom: 14px; }
.error-banner button { flex-shrink: 0; margin-left: auto; border: 0; color: inherit; text-decoration: underline; background: none; font: inherit; cursor: pointer; }
.status-card { border: 1px solid #edf1f7; border-radius: 15px; background: #fff; padding: 25px 18px 22px; margin-bottom: 14px; text-align: center; box-shadow: 0 3px 14px #28456b04; }
.status-heading { display: flex; flex-direction: column; align-items: center; }
.status-icon { display: grid; place-items: center; width: 62px; height: 62px; border-radius: 50%; color: var(--hc-primary, #1677ff); background: #edf5ff; font-size: 30px; margin-bottom: 12px; }
.status-title { margin: 0; font-size: 19px; line-height: 1.5; font-weight: 600; }
.status-PENDING { background: #fff7e6; color: #d49b2a; }
.status-COMPLETED { background: #f0f9eb; color: #67a943; }
.status-REJECTED { background: #fff1f0; color: #de6a61; }
.status-CANCELLED { background: #f2f4f7; color: #98a1af; }
.status-description { margin: 8px 0 10px; color: #929fb1; font-size: 12px; line-height: 1.8; }
.progress-steps { display: flex; margin: 25px 0 0; padding: 0; list-style: none; }
.progress-steps li { position: relative; flex: 1; display: flex; flex-direction: column; align-items: center; gap: 8px; color: #a8b4c3; font-size: 11px; }
.progress-steps li::before { content: ''; position: absolute; height: 1px; background: #e0e8f2; left: -50%; right: 50%; top: 11px; }
.progress-steps li:first-child::before { display: none; }
.step-dot { position: relative; z-index: 1; display: grid; place-items: center; width: 23px; height: 23px; background: #f5f8fc; border: 1px solid #dce5f0; border-radius: 50%; font-size: 10px; }
.progress-steps li.current { color: var(--hc-primary, #1677ff); }
.progress-steps .current .step-dot { border-color: var(--hc-primary, #1677ff); background: var(--hc-primary, #1677ff); color: #fff; }
.progress-steps li.finished { color: #67a943; }.progress-steps .finished .step-dot { background: #f0f9eb; border-color: #c4e4b1; color: #67a943; }
.section-card { margin-bottom: 14px; border: 1px solid #edf1f7; border-radius: 13px; padding: 16px; background: #fff; box-shadow: 0 3px 14px #28456b04; }
.section-heading { display: flex; align-items: center; gap: 8px; margin: -2px -16px 15px; padding: 0 16px 13px; border-bottom: 1px solid #f0f3f7; }
.section-icon { width: 26px; height: 26px; display: grid; place-items: center; border-radius: 8px; background: #edf5ff; color: var(--hc-primary, #1677ff); font-size: 15px; }
.section-heading h2 { margin: 0; font-size: 16px; font-weight: 600; }
.section-note { margin-left: auto; font-size: 11px; color: #8a9cb2; border: 1px solid #e3ebf6; border-radius: 5px; padding: 3px 7px; }
.info-grid { display: block; margin: 0; border: 1px solid #e9eef6; border-radius: 7px; overflow: hidden; }
.info-grid > div { min-width: 0; display: flex; border-top: 1px solid #e9eef6; }
.info-grid > div:first-child { border-top: 0; }
dt { display: flex; align-items: center; width: 90px; flex-shrink: 0; background: #fafbfd; color: #93a1b4; font-size: 12px; padding: 11px 10px; border-right: 1px solid #e9eef6; }
dd { flex: 1; min-width: 0; font-size: 13px; color: #52647a; margin: 0; padding: 11px 12px; line-height: 1.7; overflow-wrap: anywhere; }
.description-block { margin-top: 17px; }
h3 { font-size: 12px; color: #93a1b4; font-weight: 400; margin: 0 0 9px; }
.description-block p, .remark { font-size: 13px; line-height: 1.85; white-space: pre-wrap; overflow-wrap: anywhere; color: #65758a; margin: 0; }
.description-block p, .remark { background: #f7f9fc; border-radius: 8px; padding: 12px; }
.image-hint { font-size: 11px; color: #9aa8ba; margin: -3px 0 13px; }
.image-list { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.image-item { min-width: 0; position: relative; aspect-ratio: 1; border: 1px solid #e7edf6; background: #f5f8fc; border-radius: 8px; overflow: hidden; }
.image-item .el-image { width: 100%; height: 100%; display: block; cursor: zoom-in; }
.image-index { position: absolute; bottom: 5px; left: 5px; background: #24385080; color: white; font-size: 9px; padding: 2px 5px; border-radius: 4px; pointer-events: none; }
.image-error, .image-placeholder { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 7px; color: #a5b4c7; font-size: 10px; }
.image-error .el-icon, .image-placeholder .el-icon { font-size: 23px; }
.image-retry { margin-top: 14px; padding: 10px; border-radius: 8px; background: #f5f8fd; color: #8c9eb5; font-size: 11px; line-height: 1.8; display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.image-retry button { border: 0; color: var(--hc-primary, #1677ff); font: inherit; cursor: pointer; background: none; padding: 0; margin-left: auto; }
.image-retry button:disabled { opacity: .5; }
.no-images { text-align: center; color: #a5b2c4; font-size: 12px; padding: 20px 0; }
.result-card .section-icon { background: #f0f9eb; color: #67a943; }
.footer-note { color: #a5b1c2; font-size: 10px; line-height: 1.9; text-align: center; margin: 22px 0 0; }
button:focus-visible { outline: 3px solid #81b4fa; outline-offset: 3px; }
@media (min-width: 768px) { .detail-page { box-shadow: 0 0 32px #29446a0c; } }
@media (max-width: 350px) { .content { padding-left: 10px; padding-right: 10px; } .section-card { padding: 14px; } .section-heading { margin-left: -14px; margin-right: -14px; padding-left: 14px; padding-right: 14px; } }
.pdf-list { display: grid; gap: 10px; margin-top: 12px; }
.pdf-list a { display: flex; align-items: center; gap: 10px; padding: 12px; min-height: 48px; border-radius: 8px; background: #fff5f2; color: #8c4838; font-size: 13px; }
.pdf-list a .el-icon { flex-shrink: 0; font-size: 21px; }
.pdf-list a span { flex: 1; min-width: 0; overflow-wrap: anywhere; }
.pdf-list a strong { flex-shrink: 0; font-size: 12px; }
.pdf-refresh { border: 0; background: transparent; color: var(--hc-primary); font-size: 13px; min-height: 44px; }
</style>
