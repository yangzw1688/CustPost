<template>
  <main class="submit-page">
    <header class="page-header">
      <button type="button" class="back-button" aria-label="返回首页" :disabled="submitting" @click="goBack"><el-icon><ArrowLeft /></el-icon></button>
      <h1 class="page-title">报案资料提交</h1><span class="header-placeholder" aria-hidden="true"></span>
    </header>
    <div class="content">
      <form @submit.prevent="submit">
        <fieldset :disabled="submitting || submitted">
          <section class="form-card">
            <div class="section-heading"><span class="title-line"></span><h2>患者信息</h2></div>
            <div class="field-grid">
              <div class="field"><label for="patient-name"><span>*</span> 姓名</label><input id="patient-name" v-model.trim="form.name" type="text" maxlength="20" autocomplete="name" placeholder="请输入患者姓名" required /></div>
            </div>
          </section>
          <section class="form-card">
            <div class="section-heading"><span class="title-line"></span><h2>报案人信息</h2></div>
            <div class="field-grid">
              <div class="field"><label for="reporter-name"><span>*</span> 报案人姓名</label><input id="reporter-name" v-model.trim="form.reporterName" type="text" maxlength="20" autocomplete="name" required /></div>
              <div class="field"><label for="reporter-phone">报案人联系电话</label><input id="reporter-phone" :value="auth.user?.phone" type="tel" readonly /></div>
            </div>
          </section>
          <section class="form-card">
            <div class="section-heading"><span class="title-line"></span><h2>关联保单</h2></div>
            <p v-if="policiesLoading" class="policy-hint" role="status">正在读取我的保单…</p>
            <div v-else-if="policiesError" class="policy-hint" role="alert">
              {{ policiesError }} <button type="button" class="policy-retry" @click="loadPolicies">重试</button>
            </div>
            <p v-else-if="!policies.length" class="policy-hint" role="status">当前手机号没有可关联的保单。</p>
            <div v-else class="field policy-field">
              <label for="case-policy"><span>*</span> 选择保单</label>
              <select id="case-policy" v-model="form.policyId" required>
                <option value="" disabled>请选择保单</option>
                <option v-for="policy in policies" :key="policy.id" :value="policy.id">
                  {{ policy.policyNo }} · {{ policy.productName }}{{ policy.insuredName ? ' · ' + policy.insuredName : '' }}
                </option>
              </select>
            </div>
          </section>
          <section class="form-card">
            <div class="section-heading"><span class="title-line"></span><h2>病例信息</h2></div>
            <div class="field"><label for="case-title"><span>*</span> 病例名称</label><input id="case-title" v-model.trim="form.caseTitle" type="text" maxlength="100" placeholder="请输入病例名称" required /></div>
            <div class="field textarea-field">
              <label for="case-description"><span>*</span> 病例描述</label>
              <div class="textarea-wrapper"><textarea id="case-description" v-model.trim="form.caseDescription" rows="5" minlength="5" maxlength="2000" placeholder="请输入就诊情况、检查结果或目前症状等，至少填写5个字" aria-describedby="description-count" required></textarea>
              <div id="description-count" class="field-count">{{ form.caseDescription.length }} / 2000</div></div>
            </div>
          </section>
          <section class="form-card">
            <div class="section-heading"><span class="title-line"></span><h2>病例资料</h2></div>
            <p class="upload-hint">请上传病例、检查报告、处方等相关 PDF 或图片，最多上传9个文件</p>
            <div class="image-list">
              <div v-for="(image, index) in images" :key="image.id" class="image-item">
                <a v-if="image.isPdf" :href="image.url" target="_blank" rel="noopener noreferrer" class="pdf-tile"><el-icon><Document /></el-icon><strong>PDF</strong><span>{{ image.file.name }}</span></a><el-image v-else :src="image.url" :alt="`已选择资料 ${index + 1}`" fit="cover" :preview-src-list="previewUrls" :initial-index="imagePreviewIndex(image)" preview-teleported />
                <button type="button" class="delete-image" :aria-label="`移除第 ${index + 1} 张资料`" @click="deleteImage(index)"><el-icon><Close /></el-icon></button>
                <span class="image-index">{{ String(index + 1).padStart(2, '0') }}</span>
              </div>
              <button v-if="images.length < MAX_CASE_FILES" type="button" class="upload-button" @click="fileInput?.click()"><el-icon><Plus /></el-icon><span>添加资料</span></button>
            </div>
            <p class="image-count">已选择 {{ images.length }}/{{ MAX_CASE_FILES }} 个</p><p class="file-hint">支持 PDF、JPG、PNG、GIF、WebP，单个文件不超过 10MB。</p>
            <input ref="fileInput" type="file" class="file-input" aria-label="选择病例资料文件" accept="application/pdf,.pdf,image/jpeg,image/png,image/webp,image/gif" multiple @change="handleFileChange" />
          </section>
        </fieldset>
        <div v-if="errorMessage" class="feedback error-feedback" role="alert"><el-icon><Warning /></el-icon><span>{{ errorMessage }}</span></div>
        <div v-if="submitted" class="feedback success-feedback" role="status"><el-icon><CircleCheck /></el-icon><span>资料提交成功，正在前往服务记录。</span></div>
        <div v-if="submitting" class="upload-progress" role="status" aria-live="polite">
          <div><span>{{ uploadProgress < 100 ? '正在上传资料' : '上传完成，正在保存资料' }}</span><strong>{{ uploadProgress }}%</strong></div>
          <progress :value="uploadProgress" max="100" aria-label="资料上传进度"></progress>
          <p>请保持页面打开，文件较多时可能需要一些时间。</p>
        </div>
        <button type="submit" class="submit-button" :disabled="submitting || submitted || policiesLoading || !policies.length">
          <el-icon v-if="submitting" class="is-loading"><Loading /></el-icon><el-icon v-else-if="submitted"><CircleCheck /></el-icon>
          <span>{{ submitted ? '提交成功' : submitting ? '提交中…' : '提交报案' }}</span>
        </button>
        <p class="submit-note">提交后可在服务记录中查看审核进度</p>
      </form>
    </div>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, CircleCheck, Close, Document, Loading, Plus, Warning } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { auth } from '@/utils/auth'
import { MAX_CASE_FILES, MAX_CASE_FILE_SIZE, isPdfAttachment, canUploadCaseFile } from '@/shared/files/case-files.mjs'

const router = useRouter()
const form = reactive({ name: '', reporterName: '', policyId: '', caseTitle: '', caseDescription: '' })
const policies = ref([])
const policiesLoading = ref(true)
const policiesError = ref('')
let policyRequest
const fileInput = ref(null)
const images = ref([])
const previewUrls = computed(() => images.value.filter(image => !image.isPdf).map(image => image.url))
const imagePreviewIndex = image => previewUrls.value.indexOf(image.url)
const submitting = ref(false)
const submitted = ref(false)
const uploadProgress = ref(0)
const errorMessage = ref('')
let redirectTimer
let disposed = false
let requestId
watch([form, images], () => { requestId = undefined }, { deep: true })

const goBack = () => router.push('/')
async function loadPolicies() {
  policyRequest?.abort()
  policyRequest = new AbortController()
  const { signal } = policyRequest
  policiesLoading.value = true
  policiesError.value = ''
  try {
    const { data } = await request.get('/api/policy/mine', { signal })
    if (signal.aborted) return
    if (!Array.isArray(data?.data)) throw new Error('保单列表格式不正确')
    policies.value = data.data
    if (!policies.value.some(policy => policy.id === form.policyId)) form.policyId = ''
  } catch {
    if (!signal.aborted) policiesError.value = '我的保单加载失败，请重试。'
  } finally {
    if (!signal.aborted) policiesLoading.value = false
  }
}
const handleFileChange = event => {
  if (submitting.value || submitted.value) return
  const selected = Array.from(event.target.files || [])
  const remaining = MAX_CASE_FILES - images.value.length
  const issues = []
  if (selected.length > remaining) issues.push(`本次最多还能添加 ${remaining} 个文件`)
  for (const file of selected.slice(0, remaining)) {
    if (!canUploadCaseFile(file)) issues.push(`${file.name} 格式不支持，请选择 PDF、JPG、PNG、GIF 或 WebP`)
    else if (!file.size || file.size > MAX_CASE_FILE_SIZE) issues.push(`${file.name} 为空或超过 10MB`)
    else if (!images.value.some(item => item.file.name === file.name && item.file.size === file.size && item.file.lastModified === file.lastModified)) {
      images.value.push({ id: `${Date.now()}-${Math.random()}`, file, isPdf: isPdfAttachment(file), url: URL.createObjectURL(file) })
    }
  }
  errorMessage.value = issues.join('；')
  event.target.value = ''
}
const deleteImage = index => {
  if (submitting.value || submitted.value) return
  URL.revokeObjectURL(images.value[index].url)
  images.value.splice(index, 1)
}
const validateForm = () => {
  if (!form.name || form.name.length > 20) return '请填写患者姓名，最多 20 个字符'
  if (!form.reporterName || form.reporterName.length > 20) return '请填写报案人姓名，最多 20 个字符'
  if (!policies.value.some(policy => policy.id === form.policyId)) return '请选择本人的保单'
  if (!form.caseTitle || form.caseTitle.length > 100) return '请填写病例名称，最多 100 个字符'
  if (form.caseDescription.length < 5 || form.caseDescription.length > 2000) return '病例描述请填写 5 至 2000 个字符'
  if (!images.value.length) return '请至少添加一份 PDF 或图片资料'
  return ''
}
const submit = async () => {
  if (submitting.value || submitted.value || policiesLoading.value || !policies.value.length) return
  errorMessage.value = validateForm()
  if (errorMessage.value) return
  submitting.value = true
  uploadProgress.value = 0
  // 文件以 multipart 原样提交，后端校验后写入 OBS，表单只保存业务字段。
  // 文件以 multipart 原样提交，后端校验后写入 OBS，表单只保存业务字段。
  const data = new FormData()
  if (!requestId) requestId = Array.from(crypto.getRandomValues(new Uint8Array(16)),
    byte => byte.toString(16).padStart(2, '0')).join('')
  data.append('requestId', requestId)
  Object.entries(form).forEach(([key, value]) => { if (value) data.append(key, value) })
  images.value.forEach(image => data.append('files', image.file))
  try {
    const response = await request.post('/api/service-record/save', data, {
      timeout: 120000,
      onUploadProgress: event => {
        if (!disposed && event.total) uploadProgress.value = Math.min(100, Math.round(event.loaded / event.total * 100))
      }
    })
    if (disposed) return
    if (response.data?.code !== 200 || !response.data?.data) throw new Error(response.data?.message || '提交未成功，请检查资料后重试')
    submitted.value = true
    redirectTimer = setTimeout(() => router.replace('/service-record'), 900)
  } catch (error) {
    if (disposed) return
    // 超时不等于保存失败，提示先查询记录，避免再次提交产生重复病例。
    // 超时不等于保存失败，提示先查询记录，避免再次提交产生重复病例。
    const uncertain = error.code === 'ECONNABORTED' || error.code === 'ERR_NETWORK'
    errorMessage.value = uncertain
      ? '网络连接中断或请求超时，提交结果尚未确认。请先到服务记录查看，避免重复提交。'
      : error.response?.data?.message || error.message || '提交失败，请稍后重试'
  } finally {
    if (!disposed) submitting.value = false
  }
}
onMounted(loadPolicies)
onBeforeUnmount(() => {
  disposed = true
  policyRequest?.abort()
  clearTimeout(redirectTimer)
  // 本地预览 URL 持有文件内存，离开页面时统一释放。
  // 本地预览 URL 持有文件内存，离开页面时统一释放。
  images.value.forEach(image => URL.revokeObjectURL(image.url))
})
</script>

<style scoped>
.submit-page { width: 100%; max-width: 430px; margin: 0 auto; min-height: 100vh; color: var(--hc-ink, #243850); background: #f5f7fa; padding-bottom: calc(20px + env(safe-area-inset-bottom)); }
.page-header { position: sticky; top: 0; z-index: 10; height: 56px; display: flex; align-items: center; justify-content: space-between; background: #fffffff5; border-bottom: 1px solid #edf0f5; backdrop-filter: blur(12px); }
.back-button, .header-placeholder { display: grid; place-items: center; width: 52px; height: 52px; flex-shrink: 0; border: 0; background: none; color: var(--hc-ink, #243850); cursor: pointer; font-size: 21px; }
.page-title { margin: 0; font-size: 17px; font-weight: 600; }
.content { padding: 14px 12px 24px; }
fieldset { margin: 0; padding: 0; border: 0; min-width: 0; }
.form-card { margin-bottom: 14px; background: #fff; border: 1px solid #ecf0f6; padding: 17px 16px; border-radius: 13px; box-shadow: 0 3px 14px #28456b04; }
.section-heading { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.title-line { width: 3px; height: 16px; border-radius: 4px; background: var(--hc-primary, #1677ff); } .section-heading h2 { font-size: 16px; margin: 0; font-weight: 600; }
.field-grid { display: block; }
.field { min-height: 54px; display: flex; align-items: center; gap: 8px; min-width: 0; border-bottom: 1px solid #f0f3f7; }
.field:last-child { border-bottom: 0; }
.field label { width: 80px; flex-shrink: 0; font-size: 14px; color: #526277; }
.field label > span { color: #ef6b64; }
.policy-field select { flex: 1; min-width: 0; min-height: 44px; padding: 6px; border: 1px solid #dce3ed; border-radius: 7px; background: #fff; color: inherit; font: inherit; font-size: 14px; }
.policy-hint { margin: 12px 0 0; color: #526277; font-size: 14px; }
.policy-retry { border: 0; background: none; color: var(--hc-primary, #1677ff); cursor: pointer; font: inherit; }
.field input, .field textarea { width: 100%; min-width: 0; border: 1px solid transparent; background: #fff; border-radius: 7px; padding: 12px 6px; color: var(--hc-ink, #243850); font: inherit; font-size: 14px; transition: border-color .2s, box-shadow .2s; }
.field input { min-height: 44px; }
.field input::placeholder, .field textarea::placeholder { color: #a1acbb; font-size: 13px; }
.field input:focus, .field textarea:focus { outline: 0; border-color: #b7d6ff; background: #fafcff; box-shadow: 0 0 0 2px #1677ff0a; }
.textarea-field { align-items: flex-start; padding-top: 12px; }
.textarea-field label { padding-top: 10px; }
.textarea-wrapper { flex: 1; min-width: 0; }
.field textarea { resize: vertical; min-height: 120px; line-height: 1.8; padding-top: 6px; }
.field-count { text-align: right; margin-top: 3px; font-size: 11px; color: #9aa8b9; }
.upload-hint { color: #8996a8; font-size: 12px; line-height: 1.8; margin: 13px 0 16px; }
.image-list { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.image-item { position: relative; aspect-ratio: 1; min-width: 0; border: 1px solid #e8edf5; border-radius: 9px; background: #f5f8fc; }
.image-item .el-image { width: 100%; height: 100%; border-radius: 8px; display: block; }
.delete-image { position: absolute; right: -5px; top: -5px; width: 25px; height: 25px; padding: 0; display: grid; place-items: center; background: #e96161; color: white; border: 2px solid #fff; border-radius: 50%; cursor: pointer; z-index: 1; }
.image-index { position: absolute; left: 5px; bottom: 5px; padding: 2px 5px; font-size: 10px; color: #fff; background: #24385080; border-radius: 4px; pointer-events: none; }
.upload-button { aspect-ratio: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; border: 1px dashed #cedcee; background: #f8faff; border-radius: 9px; color: #91a4bf; cursor: pointer; font: inherit; font-size: 12px; }
.upload-button .el-icon { font-size: 27px; }
.upload-button:hover { background: #eef5ff; border-color: var(--hc-primary, #1677ff); color: var(--hc-primary, #1677ff); }
.image-count { font-size: 12px; margin: 12px 0 0; text-align: right; color: #8c99ab; }
.file-hint { font-size: 10px; color: #a0adbd; line-height: 1.8; margin: 10px 0 0; }
.file-input { display: none; }
.feedback { display: flex; gap: 8px; align-items: flex-start; border-radius: 9px; padding: 13px; font-size: 12px; line-height: 1.8; margin: 14px 0; }
.feedback .el-icon { flex-shrink: 0; margin-top: 4px; font-size: 15px; }
.error-feedback { color: #b05542; border: 1px solid #f3dbd3; background: #fff7f3; }
.success-feedback { color: #428a60; background: #f0faf2; border: 1px solid #d6ebdb; }
.upload-progress { margin: 16px 0; padding: 15px; background: #edf5ff; border-radius: 10px; font-size: 12px; }
.upload-progress > div { display: flex; justify-content: space-between; color: #4382c5; }
.upload-progress progress { display: block; width: 100%; height: 5px; margin: 12px 0; border: 0; border-radius: 6px; accent-color: var(--hc-primary, #1677ff); }
.upload-progress progress::-webkit-progress-bar { background: #d9e8fb; border-radius: 6px; }
.upload-progress progress::-webkit-progress-value { background: var(--hc-primary, #1677ff); border-radius: 6px; }
.upload-progress p { color: #8ba6c5; margin: 0; font-size: 11px; }
.submit-button { margin-top: 24px; width: 100%; min-height: 48px; border: 0; border-radius: 10px; background: var(--hc-primary, #1677ff); color: white; display: flex; align-items: center; justify-content: center; gap: 10px; font: inherit; font-size: 15px; font-weight: 600; cursor: pointer; box-shadow: 0 5px 12px #1677ff14; }
.submit-button:hover:not(:disabled) { background: #1264df; }
button:disabled { opacity: .65; cursor: default; }
.submit-note { font-size: 11px; text-align: center; line-height: 1.9; color: #99a6b8; margin: 13px 0 0; }
button:focus-visible { outline: 3px solid #81b4fa; outline-offset: 3px; }
@media (min-width: 768px) { .submit-page { box-shadow: 0 0 32px #29446a0c; } }
@media (max-width: 350px) { .content { padding-left: 10px; padding-right: 10px; } .form-card { padding: 15px 13px; } .field label { width: 72px; font-size: 13px; } }
.pdf-tile { display: flex; flex-direction: column; justify-content: center; align-items: center; gap: 4px; min-height: 90px; color: #b96a53; background: #fff5f2; }
</style>
