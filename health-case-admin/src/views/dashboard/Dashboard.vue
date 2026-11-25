<template>
  <section class="management-page">
    <header class="management-heading">
      <div><h1>数据统计</h1><p>查看案件处理和保单登记情况，进入日常工作。</p></div>
      <el-button :icon="Refresh" :loading="loading || loadingRecent" @click="load">刷新</el-button>
    </header>
    <el-alert v-if="summaryError" :title="summaryError" type="error" :closable="false" show-icon />
    <ul v-loading="loading" class="dashboard-totals">
      <li v-for="item in counts" :key="item.key" :class="item.tone">
        <div class="metric-icon"><el-icon :size="20"><component :is="item.icon" /></el-icon></div>
        <span>{{ item.title }}</span>
        <strong>{{ totals[item.key] == null ? '—' : totals[item.key].toLocaleString('zh-CN') }}</strong>
        <router-link v-if="canAccess(item.permission)" :to="item.target" :aria-label="item.action + ' · ' + item.title">
          {{ item.action }} <el-icon><ArrowRight /></el-icon>
        </router-link>
      </li>
    </ul>

    <el-card v-if="canAccess('cases')" class="management-surface" shadow="never">
      <div class="management-table-heading">
        <h2>最近提交</h2>
        <el-button type="primary" link @click="router.push({ name: 'CaseList' })">查看全部</el-button>
      </div>
      <el-alert v-if="recentError" :title="recentError" type="error" :closable="false" show-icon />
      <div v-loading="loadingRecent" class="dashboard-recent">
        <el-empty v-if="!recent.length && !recentError" :image-size="66" description="暂无案件记录" />
        <ul v-else>
          <li v-for="record in recent" :key="record.id">
            <router-link :to="{ name: 'CaseDetail', params: { id: record.id } }">
              <span><strong>{{ record.caseTitle || '未填写病例名称' }}</strong>
                <small>案件号 {{ record.id }} · {{ formatDateTime(record.createTime) }}</small></span>
              <el-tag :type="caseStatusInfo(record).type" effect="light">{{ caseStatusInfo(record).label }}</el-tag>
            </router-link>
          </li>
        </ul>
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, CircleCheck, Clock, Document, Loading, Refresh, Tickets } from '@element-plus/icons-vue'
import request, { errorMessage } from '@/utils/request'
import { canAccess } from '@/utils/permissions'
import { caseStatusInfo, formatDateTime } from '@/utils/format'
const router = useRouter()
const counts = [
  { key: 'total', title: '全部案件', icon: Document, tone: 'teal', permission: 'cases', action: '查看案件', target: { name: 'CaseList' } },
  { key: 'pending', title: '待审核', icon: Clock, tone: 'amber', permission: 'cases', action: '进入审核', target: { name: 'PayeeReview' } },
  { key: 'processing', title: '处理中', icon: Loading, tone: 'blue', permission: 'cases', action: '维护进度', target: { name: 'CaseMaintain', query: { status: 'PROCESSING' } } },
  { key: 'completed', title: '已完成', icon: CircleCheck, tone: 'green', permission: 'cases', action: '查看完成记录', target: { name: 'CaseList', query: { status: 'COMPLETED' } } },
  { key: 'policies', title: '登记保单', icon: Tickets, tone: 'blue', permission: 'policy', action: '查看保单', target: { name: 'PolicyList' } }
]
const totals = ref({})
const recent = ref([])
const loading = ref(false)
const loadingRecent = ref(false)
const summaryError = ref('')
const recentError = ref('')
let controller

async function loadSummary(signal) {
  loading.value = true
  summaryError.value = ''
  try {
    const { data } = await request.get('/api/dashboard/summary', { signal })
    if (!signal.aborted) totals.value = data.data
  } catch (failure) {
    if (!signal.aborted) summaryError.value = errorMessage(failure)
  } finally {
    if (!signal.aborted) loading.value = false
  }
}

async function loadRecent(signal) {
  loadingRecent.value = true
  recentError.value = ''
  try {
    const { data } = await request.get('/api/service-record/page', { params: { page: 1, size: 6 }, signal })
    if (!signal.aborted) recent.value = data.data.rows
  } catch (failure) {
    if (!signal.aborted) recentError.value = errorMessage(failure)
  } finally {
    if (!signal.aborted) loadingRecent.value = false
  }
}

function load() {
  controller?.abort()
  controller = new AbortController()
  const { signal } = controller
  loadSummary(signal)
  if (canAccess('cases')) loadRecent(signal)
  else loadingRecent.value = false
}
onMounted(load)
onBeforeUnmount(() => controller?.abort())
</script>

<style scoped>
.dashboard-totals { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 13px; margin: 0; padding: 0; list-style: none; }
.dashboard-totals li {
  --metric-color: #216b63;
  --metric-background: #e7f3ef;
  display: grid; justify-items: start; align-content: start; gap: 8px; padding: 16px;
  border: 1px solid var(--management-border); border-radius: 12px; background: #fff; min-width: 0;
}
.dashboard-totals li.amber { --metric-color: #8e5915; --metric-background: #fbf0da; }
.dashboard-totals li.blue { --metric-color: #285c9c; --metric-background: #eaf1fc; }
.dashboard-totals li.green { --metric-color: #397243; --metric-background: #e9f4ea; }
.metric-icon { display: grid; place-content: center; width: 37px; height: 37px; border-radius: 8px; color: var(--metric-color); background: var(--metric-background); }
.dashboard-totals li > span { color: var(--management-muted); font-size: 13px; }
.dashboard-totals strong { font-size: clamp(24px, 3.2vw, 32px); font-variant-numeric: tabular-nums; line-height: 1.1; overflow-wrap: anywhere; }
.dashboard-totals a { display: inline-flex; align-items: center; gap: 3px; color: var(--el-color-primary-dark-2); text-decoration: none; font-size: 13px; }
.dashboard-totals a:hover { text-decoration: underline; }
.dashboard-recent ul { list-style: none; padding: 0; margin: 0; }
.dashboard-recent li + li { border-top: 1px solid var(--management-border); }
.dashboard-recent a { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 13px 0; color: var(--management-ink); text-decoration: none; }
.dashboard-recent a:hover strong { color: var(--el-color-primary-dark-2); text-decoration: underline; }
.dashboard-recent a > span:first-child { min-width: 0; overflow-wrap: anywhere; }
.dashboard-recent small { display: block; margin-top: 4px; color: var(--management-muted); }
@media (max-width: 760px) {
  .dashboard-totals { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 9px; }
  .dashboard-totals li { padding: 12px; }
}
</style>
