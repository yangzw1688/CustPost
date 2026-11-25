<template>
  <main class="records-view original-page">
    <header class="records-header"><RouterLink to="/" class="back-link" aria-label="返回首页"><el-icon><ArrowLeft /></el-icon></RouterLink><strong>服务记录</strong><span class="header-spacer"></span></header>
    <div v-if="loading" class="state-panel" aria-live="polite"><el-skeleton :rows="5" animated /></div>
    <div v-else-if="error" class="state-panel error-panel" role="alert"><el-icon><Warning /></el-icon><h2>暂时无法加载记录</h2><p>{{ error }}</p><el-button type="primary" @click="loadRecords">重新加载</el-button></div>
    <el-empty v-else-if="!filtered.length" :description="records.length ? '没有符合条件的服务记录' : '提交资料后，您可以在这里查看服务进度'"><el-button v-if="records.length" @click="resetFilters">清除筛选</el-button><el-button v-else type="primary" @click="router.push('/submit')">提交第一份资料</el-button></el-empty>
    <template v-else><p class="result-count">共 {{ filtered.length }} 条记录</p><div class="records-grid"><RouterLink v-for="item in visibleRecords" :key="item.id" :to="`/service-detail/${item.id}`" class="record-item"><div class="record-top"><h2>{{ item.caseTitle || '未命名病例' }}</h2><el-tag :type="statusTypes[item.status] || 'info'" round effect="light">{{ statuses[item.status] || '未知状态' }}</el-tag></div><dl><div><dt>患者姓名</dt><dd>{{ item.name || '—' }}</dd></div><div><dt>提交时间</dt><dd>{{ formatTime(item.createTime) }}</dd></div></dl><footer><span>服务编号 {{ item.id }}</span><strong>查看详情 <el-icon><ArrowRight /></el-icon></strong></footer></RouterLink></div><el-pagination v-if="filtered.length > pageSize" v-model:current-page="page" :page-size="pageSize" :total="filtered.length" layout="prev, pager, next" class="records-pagination" /></template>
  </main>
</template>
<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Warning } from '@element-plus/icons-vue'
import request from '@/utils/request'
const router = useRouter()
const records = ref([])
const loading = ref(false)
const error = ref('')
const keyword = ref('')
const status = ref('')
const page = ref(1)
const pageSize = 8
const statuses = { PENDING: '审核中', PROCESSING: '处理中', COMPLETED: '已完成', REJECTED: '已驳回', CANCELLED: '已取消' }
const statusTypes = { PENDING: 'warning', PROCESSING: 'primary', COMPLETED: 'success', REJECTED: 'danger', CANCELLED: 'info' }
let controller
const filtered = computed(() => records.value.filter(item => (!status.value || item.status === status.value) && (!keyword.value.trim() || `${item.caseTitle || ''} ${item.id}`.toLowerCase().includes(keyword.value.trim().toLowerCase()))))
const visibleRecords = computed(() => filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize))
watch([keyword, status], () => { page.value = 1 })
const resetFilters = () => { keyword.value = ''; status.value = '' }
const formatTime = value => value ? String(value).replace('T', ' ').slice(0, 16) : '—'
async function loadRecords() {
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = ''
  try {
    const res = await request.get('/api/service-record/list', { signal: current.signal })
    if (!Array.isArray(res.data.data)) throw new Error('服务记录数据格式异常')
    records.value = res.data.data
    page.value = 1
  } catch (err) {
    if (err.code !== 'ERR_CANCELED') error.value = err.message || '请检查网络连接后重试'
  } finally {
    if (controller === current) loading.value = false
  }
}
onMounted(loadRecords)
onBeforeUnmount(() => controller?.abort())
</script>
<style scoped>
.records-view { max-width: 960px; margin: auto; padding: 0 32px 48px; }.records-header { height: 80px; display: flex; align-items: center; justify-content: space-between; font-size: 12px; color: var(--hc-muted); }.back-link { display: flex; align-items: center; gap: 8px; color: var(--hc-primary); padding: 10px 0; }.records-intro { display: flex; align-items: center; justify-content: space-between; padding: 28px 0; gap: 15px; }.eyebrow { color: #688d7c; font-size: 10px; letter-spacing: 2px; }h1 { font-size: 30px; margin: 10px 0; letter-spacing: -.7px; }.records-intro p { font-size: 13px; color: var(--hc-muted); line-height: 1.7; margin: 0; }.new-record { background: var(--hc-primary); color: white; padding: 12px 16px; display: flex; align-items: center; gap: 8px; font-size: 13px; border-radius: 10px; white-space: nowrap; }.records-toolbar { display: flex; gap: 10px; padding: 16px; background: #fff; border: 1px solid var(--hc-border); border-radius: 14px; margin: 6px 0 24px; }.records-toolbar > .el-input { flex: 1; }.status-filter { width: 135px; }.result-count { color: var(--hc-muted); font-size: 12px; margin: 0 0 14px; }.records-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }.record-item { background: #fff; padding: 24px; border: 1px solid var(--hc-border); border-radius: 18px; color: var(--hc-ink); transition: transform .2s, border-color .2s; }.record-item:hover { transform: translateY(-2px); border-color: #95bca7; }.record-top { display: flex; justify-content: space-between; align-items: center; }.record-symbol { width: 38px; height: 38px; background: #edf5ef; color: #52866c; border-radius: 11px; font-size: 22px; display: grid; place-items: center; }.record-item h2 { margin: 18px 0; font-size: 17px; overflow-wrap: anywhere; }.record-item dl { font-size: 12px; margin: 0 0 20px; }.record-item dl > div { display: flex; gap: 16px; margin: 12px 0; }.record-item dt { color: var(--hc-muted); flex-shrink: 0; }.record-item dd { margin: 0; overflow-wrap: anywhere; }.record-item footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; border-top: 1px solid #edf1ee; padding-top: 16px; font-size: 11px; color: #8b9c91; }.record-item footer strong { color: var(--hc-primary); font-weight: 500; display: flex; align-items: center; gap: 5px; }.state-panel { background: #fff; border: 1px solid var(--hc-border); border-radius: 18px; padding: 32px; }.error-panel { text-align: center; padding: 50px 24px; }.error-panel > .el-icon { font-size: 36px; color: #b58d48; }.error-panel h2 { font-size: 18px; }.error-panel p { color: var(--hc-muted); font-size: 13px; margin: 12px 0 22px; }.records-pagination { justify-content: center; margin-top: 26px; }
@media (max-width: 600px) { .records-view { padding: 0 20px 36px; }.records-header { height: 64px; }.records-intro { padding: 16px 0 24px; }h1 { font-size: 27px; }.records-intro p { font-size: 11px; }.new-record { padding: 11px; font-size: 12px; }.records-toolbar { padding: 12px; flex-wrap: wrap; }.records-toolbar > .el-input { flex: 1 0 100%; }.status-filter { flex: 1; }.records-grid { grid-template-columns: 1fr; gap: 14px; }.record-item { padding: 22px; } }
.records-view { padding: 0 14px 36px; }.records-header { height: 56px; margin: 0 -14px 16px; padding: 0 16px; background: #fff; border-bottom: 1px solid #edf0f6; color: #243850; font-size: 17px; }.records-header .back-link { color: #546379; width: 36px; font-size: 22px; }.header-spacer { width: 36px; }.records-grid { grid-template-columns: 1fr; gap: 14px; }.record-item { padding: 20px 18px; border-radius: 14px; border-color: #e6edf5; }.record-top { gap: 12px; }.record-top h2 { margin: 0; font-size: 16px; min-width: 0; }.record-item dl { margin: 18px 0; padding: 8px 0; border-top: 1px solid #f0f3f8; border-bottom: 1px solid #f0f3f8; }.record-item footer { border: 0; padding-top: 0; }.record-item:hover { border-color: #b7d3f6; }.result-count { font-size: 12px; margin: 0 4px 12px; }
</style>
