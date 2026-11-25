<template>
  <div class="claim-push-page">
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" class="search-form" label-width="90px" @submit.prevent="handleSearch">
        <div class="search-row">
          <el-form-item label="案件号">
            <el-input v-model="searchForm.caseNo" placeholder="请输入案件号" clearable />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="searchForm.personName" placeholder="请输入被保人或患者姓名" clearable />
          </el-form-item>
          <el-form-item label="案件状态">
            <el-select v-model="searchForm.status" placeholder="请选择" clearable>
              <el-option v-for="status in caseStatuses" :key="status.value" :label="status.label" :value="status.value" />
            </el-select>
          </el-form-item>
        </div>
        <div class="search-row search-row-bottom">
          <el-form-item label="提交时间">
            <el-date-picker
              v-model="searchForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" native-type="submit">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </div>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <div>
          <span class="table-title">赔案推送列表</span>
          <span v-if="!loadError" class="table-count">共 {{ filteredList.length }} 条</span>
          <div class="push-note">案件状态变更后自动排队，也可手动推送。</div>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" :disabled="!configured || !selected.length" :loading="sending"
            @click="enqueue(selected)">手动批量推送</el-button>
          <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        </div>
      </div>
      <el-alert v-if="!configured && !loading && !loadError" title="推送配置尚未补齐，请检查接收方和队列连接参数。"
        type="info" :closable="false" show-icon class="list-alert" />
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
      <el-table
        v-else
        v-loading="loading"
        :data="pagedList"
        :empty-text="loading ? '加载中…' : '暂无符合条件的案件'"
        row-key="id"
        @selection-change="selected = $event"
        border
        stripe
      >
        <el-table-column type="selection" width="44" :selectable="canEnqueue" />
        <el-table-column prop="id" label="案件号" min-width="100" />
        <el-table-column prop="caseTitle" label="案件名称" min-width="170" show-overflow-tooltip />
        <el-table-column label="保单号" min-width="165" show-overflow-tooltip>
          <template #default="{ row }">{{ row.policyNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="被保人 / 患者" min-width="115" show-overflow-tooltip>
          <template #default="{ row }">{{ row.insuredName || row.patientName || '—' }}</template>
        </el-table-column>
        <el-table-column label="案件状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusInfo(row.status).type">{{ statusInfo(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="155">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="推送状态" min-width="150">
          <template #default="{ row }">
            <el-tag :type="row.pushStatus === 'FAILED' ? 'danger' : row.pushStatus === 'SUCCEEDED' ? 'success' : 'info'">
              {{ pushLabels[row.pushStatus] || '未推送' }}
            </el-tag>
            <div v-if="row.pushTrigger" class="push-note">{{ triggerLabels[row.pushTrigger] }}<template v-if="row.pushCaseStatus"> · {{ statusInfo(row.pushCaseStatus).label }}</template></div>
            <div v-if="row.pushAttempts || row.pushError" class="push-note">
              {{ row.pushAttempts || 0 }} 次尝试<span v-if="row.pushError"> · {{ row.pushError }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="215">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push({ name: 'CaseDetail', params: { id: row.id } })">详情</el-button>
            <el-button v-if="row.pushStatus" type="primary" link @click="showHistory(row.id)">推送记录</el-button>
            <el-button v-if="canEnqueue(row)" type="primary" link :disabled="sending" @click="enqueue([row])">{{ row.pushStatus ? '再次推送' : '手动推送' }}</el-button>
            <el-button v-if="configured && row.pushStatus === 'FAILED'" type="warning" link :disabled="sending"
              @click="retry(row.id, row.pushTaskId)">重试</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loadError && filteredList.length" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredList.length"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="page = 1"
        />
      </div>
    </el-card>
    <el-dialog v-model="historyVisible" :title="'案件 #' + historyCaseId + ' 推送记录'" width="min(860px, calc(100vw - 32px))">
      <el-alert v-if="historyError" :title="historyError" type="error" :closable="false" show-icon />
      <el-table v-loading="historyLoading" :data="historyRows" row-key="taskId" empty-text="暂无推送记录">
        <el-table-column label="触发方式" min-width="100"><template #default="{ row }">{{ triggerLabels[row.triggerType] }}</template></el-table-column>
        <el-table-column label="任务创建时状态" min-width="130"><template #default="{ row }">{{ row.caseStatus ? statusInfo(row.caseStatus).label : '—' }}</template></el-table-column>
        <el-table-column label="推送状态" min-width="110"><template #default="{ row }">{{ pushLabels[row.status] }}</template></el-table-column>
        <el-table-column prop="attempts" label="尝试次数" width="85" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="150" />
        <el-table-column prop="lastError" label="错误原因" min-width="135" show-overflow-tooltip />
        <el-table-column label="操作" width="72">
          <template #default="{ row }"><el-button v-if="configured && row.status === 'FAILED'" type="warning" link :disabled="sending" @click="retry(historyCaseId, row.taskId)">重试</el-button></template>
        </el-table-column>
      </el-table>
      <template #footer><el-button :loading="historyLoading" @click="loadHistory">刷新记录</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { caseStatuses, formatDate, formatDateTime, statusInfo } from '@/utils/format'
import request, { errorMessage } from '@/utils/request'

const router = useRouter()
const emptySearch = () => ({ caseNo: '', personName: '', status: '', dateRange: [] })
const searchForm = ref(emptySearch())
const filters = ref(emptySearch())
const cases = ref([])
const page = ref(1)
const pageSize = ref(10)
const loading = ref(true)
const loadError = ref('')
const configured = ref(false)
const selected = ref([])
const sending = ref(false)
const historyVisible = ref(false)
const historyCaseId = ref(null)
const historyRows = ref([])
const historyError = ref('')
const historyLoading = ref(false)
const pushPath = '/api/service-record/claim-push'
const pushLabels = { QUEUED: '已排队', RUNNING: '发送中', RETRY_WAIT: '等待重试', SUCCEEDED: '接收方已受理', FAILED: '失败' }
const triggerLabels = { MANUAL: '手动推送', STATUS_CHANGE: '状态变更' }
const canEnqueue = row => configured.value && !row.activePush && (!row.pushStatus || row.pushStatus === 'SUCCEEDED')
let listRequest
let historyRequest
let refreshTimer

const filteredList = computed(() => {
  const { caseNo, personName, status, dateRange } = filters.value
  return cases.value.filter(row => {
    if (!String(row.id).includes(caseNo) || !(row.insuredName || row.patientName || '').includes(personName)) return false
    if (status && row.status !== status) return false
    if (dateRange?.length !== 2) return true
    const date = formatDate(row.createTime)
    return date >= dateRange[0] && date <= dateRange[1]
  })
})
const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

function handleSearch() {
  filters.value = {
    ...searchForm.value,
    caseNo: searchForm.value.caseNo.trim(),
    personName: searchForm.value.personName.trim()
  }
  page.value = 1
}

function handleReset() {
  searchForm.value = emptySearch()
  handleSearch()
}

async function loadData() {
  listRequest?.abort()
  listRequest = new AbortController()
  const { signal } = listRequest
  loading.value = true
  loadError.value = ''
  try {
    const { data: { data } } = await request.get(pushPath, { signal })
    if (signal.aborted) return
    cases.value = data.rows
    configured.value = data.configured
    page.value = Math.min(page.value, Math.max(1, Math.ceil(filteredList.value.length / pageSize.value)))
  } catch (error) {
    if (!signal.aborted) loadError.value = errorMessage(error)
  } finally {
    if (!signal.aborted) loading.value = false
  }
}

async function submitTask(path, ids) {
  if (sending.value) return
  sending.value = true
  try {
    const { data } = await request.post(path, ids)
    if (ids && !data.data) ElMessage.info('未新增任务，请查看最新推送记录')
    else ElMessage.success(ids ? '新增 ' + data.data + ' 个推送任务' : '任务已重新排队')
    await loadData()
    if (historyVisible.value) await loadHistory()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    sending.value = false
  }
}

async function enqueue(rows) {
  if (sending.value || !rows.length) return
  if (rows.some(row => row.pushStatus === 'SUCCEEDED')) {
    try { await ElMessageBox.confirm('所选案件中有通知已被受理。确认创建新的推送任务？', '确认再次推送', { type: 'warning' }) }
    catch { return }
  }
  await submitTask(pushPath, rows.map(row => row.id))
}
const retry = (id, taskId) => submitTask(pushPath + '/' + id + (taskId ? '/tasks/' + encodeURIComponent(taskId) : '') + '/retry')

function showHistory(id) {
  historyCaseId.value = id
  historyRows.value = []
  historyVisible.value = true
  loadHistory()
}
async function loadHistory() {
  historyRequest?.abort()
  historyRequest = new AbortController()
  const { signal } = historyRequest
  historyLoading.value = true
  historyError.value = ''
  try {
    const { data } = await request.get(pushPath + '/' + historyCaseId.value + '/tasks', { signal })
    if (!signal.aborted) historyRows.value = data.data
  } catch (error) {
    if (!signal.aborted) historyError.value = errorMessage(error)
  } finally {
    if (!signal.aborted) historyLoading.value = false
  }
}

onMounted(() => {
  loadData()
  refreshTimer = setInterval(() => {
    if (!document.hidden && !loading.value && !sending.value && !selected.value.length
      && cases.value.some(row => row.activePush)) loadData()
  }, 5000)
})
onBeforeUnmount(() => {
  clearInterval(refreshTimer)
  listRequest?.abort()
  historyRequest?.abort()
})
</script>

<style scoped lang="scss">
.claim-push-page {
  width: 100%;
  min-width: 0;
}

.search-card,
.table-card {
  border: none;
}

.search-card {
  margin-bottom: 20px;
}

.search-form {
  padding: 4px 4px 0;
}

.search-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 18px 36px;
  min-height: 42px;
}

.search-row-bottom {
  margin-top: 22px;
}

.search-form :deep(.el-form-item) {
  margin: 0;
}

.search-form :deep(.el-input) {
  width: 220px;
}

.search-form :deep(.el-select) {
  width: 160px;
}

.search-row-bottom :deep(.el-date-editor) {
  width: 280px;
}

.table-toolbar,
.toolbar-right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.table-toolbar {
  justify-content: space-between;
  margin-bottom: 16px;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
}

.table-count {
  margin-left: 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.list-alert {
  margin-bottom: 16px;
}

.push-note { font-size: 12px; color: var(--el-text-color-secondary); }

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  overflow-x: auto;
}
</style>
