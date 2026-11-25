<template>
  <main class="policies-view original-page">
    <header><RouterLink to="/" aria-label="返回首页"><el-icon><ArrowLeft /></el-icon></RouterLink><strong>我的保单</strong><span class="header-spacer"></span></header>
    <div class="toolbar"><el-input v-model="keyword" :prefix-icon="Search" placeholder="搜索产品名称或保单号" clearable aria-label="搜索保单" /><el-button :loading="loading" :icon="Refresh" aria-label="刷新保单" @click="loadPolicies" /></div>
    <section v-if="loading" class="panel"><el-skeleton :rows="6" animated /></section>
    <section v-else-if="error" class="panel error-panel" role="alert"><el-icon><Warning /></el-icon><h2>保单加载失败</h2><p>{{ error }}</p><el-button type="primary" @click="loadPolicies">重试</el-button></section>
    <el-empty v-else-if="!filtered.length" :description="policies.length ? '未找到符合条件的保单' : '暂无可查看的保单信息'"><el-button v-if="keyword" @click="keyword = ''">清除搜索</el-button></el-empty>
    <div v-else class="policy-grid"><button v-for="policy in visible" :key="policy.id" class="policy-item" @click="selected = policy"><div class="policy-top"><span class="policy-icon"><el-icon><Tickets /></el-icon></span><el-tag :type="statusType(policy.status)" round>{{ statusText(policy.status) }}</el-tag></div><h2>{{ policy.productName || '保单信息' }}</h2><dl><div><dt>保单号</dt><dd>{{ policy.policyNo || '—' }}</dd></div><div><dt>被保险人</dt><dd>{{ policy.insuredName || '—' }}</dd></div><div><dt>保险期间</dt><dd>{{ policy.startDate || '—' }} 至 {{ policy.endDate || '—' }}</dd></div></dl><footer>查看保单详情<el-icon><ArrowRight /></el-icon></footer></button></div>
    <el-pagination v-if="filtered.length > pageSize" v-model:current-page="page" :page-size="pageSize" :total="filtered.length" layout="prev, pager, next" class="pagination" />
    <el-dialog :model-value="!!selected" title="保单详情" width="560px" @update:model-value="value => { if (!value) selected = null }">
      <template v-if="selected"><div class="dialog-title"><span>{{ selected.productName || '保单信息' }}</span><el-tag :type="statusType(selected.status)">{{ statusText(selected.status) }}</el-tag></div><dl class="detail-list"><div v-for="field in detailFields" :key="field.key"><dt>{{ field.label }}</dt><dd>{{ field.key === 'premium' ? money(selected[field.key]) : selected[field.key] || '—' }}</dd></div></dl><el-alert title="保障范围及责任以正式保单条款为准" type="info" :closable="false" show-icon /></template>
    </el-dialog>
  <nav class="tab-bar" aria-label="底部导航"><RouterLink to="/" class="tab-item"><el-icon :size="23"><House /></el-icon><span>首页</span></RouterLink><RouterLink to="/mine" class="tab-item"><el-icon :size="23"><User /></el-icon><span>个人信息</span></RouterLink></nav></main>
</template>
<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { ArrowLeft, ArrowRight, Search, Refresh, Warning, Tickets, House, User } from '@element-plus/icons-vue'
import request from '@/utils/request'
const policies = ref([])
const loading = ref(false)
const error = ref('')
const keyword = ref('')
const selected = ref(null)
const page = ref(1)
const pageSize = 8
let controller
const filtered = computed(() => policies.value.filter(item => `${item.productName || ''} ${item.policyNo || ''}`.toLowerCase().includes(keyword.value.trim().toLowerCase())))
const visible = computed(() => filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize))
watch(keyword, () => { page.value = 1 })
const detailFields = [{ key: 'policyNo', label: '保单号' }, { key: 'applicantName', label: '投保人' }, { key: 'insuredName', label: '被保险人' }, { key: 'startDate', label: '生效日期' }, { key: 'endDate', label: '到期日期' }, { key: 'premium', label: '保费' }]
const statusText = status => ({ VALID: '保障中', ACTIVE: '保障中', EXPIRED: '已到期', CANCELLED: '已失效', PENDING: '待生效' }[status] || status || '未标注')
const statusType = status => ({ VALID: 'success', ACTIVE: 'success', EXPIRED: 'info', CANCELLED: 'danger', PENDING: 'warning' }[status] || 'info')
const money = value => value === null || value === undefined ? '—' : new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(Number(value))
async function loadPolicies() {
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = ''
  selected.value = null
  try {
    const response = await request.get('/api/policy/mine', { signal: current.signal })
    if (controller !== current || current.signal.aborted) return
    if (!Array.isArray(response.data.data)) throw new Error('保单数据格式异常')
    policies.value = response.data.data
    page.value = 1
  } catch (err) {
    if (controller === current && !current.signal.aborted && err.code !== 'ERR_CANCELED') {
      error.value = err.message || '请稍后重试'
    }
  } finally {
    if (controller === current) loading.value = false
  }
}
onMounted(loadPolicies)
onBeforeUnmount(() => controller?.abort())
</script>
<style scoped>
header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

header a {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar {
  display: flex;
  gap: 10px;
  background: #fff;
  border: 1px solid var(--hc-border);
}

.panel {
  padding: 32px;
  border: 1px solid var(--hc-border);
  border-radius: 18px;
  background: #fff;
}

.error-panel {
  text-align: center;
  padding: 48px 24px;
}

.error-panel > .el-icon {
  font-size: 36px;
  color: #b58d48;
}

.error-panel h2 {
  font-size: 18px;
}

.error-panel p {
  color: var(--hc-muted);
  font-size: 13px;
  margin: 15px 0 24px;
}

.policy-grid {
  display: grid;
}

.policy-item {
  text-align: left;
  border: 1px solid var(--hc-border);
  color: var(--hc-ink);
  background: #fff;
  transition: border-color .2s, transform .2s;
}

.policy-item:hover {
  transform: translateY(-2px);
}

.policy-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.policy-icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  font-size: 23px;
}

.policy-item h2 {
  line-height: 1.6;
  margin: 20px 0;
}

.policy-item dl {
  font-size: 12px;
}

.policy-item dl > div {
  display: flex;
  gap: 15px;
}

.policy-item dt {
  color: var(--hc-muted);
  min-width: 48px;
}

.policy-item dd {
  margin: 0;
  overflow-wrap: anywhere;
}

.policy-item footer {
  padding-top: 18px;
  margin-top: 22px;
  border-top: 1px solid #edf1ee;
  display: flex;
  justify-content: space-between;
  color: var(--hc-primary);
  font-size: 12px;
}

.dialog-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  gap: 12px;
  padding: 12px 0 20px;
  color: var(--hc-ink);
}

.detail-list {
  margin: 0 0 24px;
}

.detail-list > div {
  display: flex;
  gap: 20px;
  padding: 15px 0;
  border-bottom: 1px solid var(--hc-border);
  font-size: 13px;
}

.detail-list dt {
  color: var(--hc-muted);
  min-width: 70px;
}

.detail-list dd {
  margin: 0;
  overflow-wrap: anywhere;
}

.pagination {
  justify-content: center;
  margin-top: 24px;
}

.policies-view {
  padding: 0 14px 90px;
}

header {
  margin: 0 -14px 16px;
  padding: 0 16px;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #edf0f6;
  color: #243850;
  font-size: 17px;
}

header a {
  width: 36px;
  color: #546379;
  font-size: 22px;
}

.header-spacer {
  width: 36px;
}

.policy-grid {
  grid-template-columns: 1fr;
  gap: 14px;
}

.policy-item {
  padding: 20px 18px;
  border-radius: 14px;
  border-color: #e6edf5;
}

.policy-item:hover {
  border-color: #b7d3f6;
}

.toolbar {
  padding: 12px;
  border-radius: 12px;
  margin-bottom: 14px;
}

.policy-icon {
  background: #edf5ff;
  color: #4489dc;
}

.policy-item h2 {
  font-size: 17px;
}

.policy-item dl > div {
  margin: 16px 0;
}

.dialog-title {
  font-size: 17px;
}
</style>
