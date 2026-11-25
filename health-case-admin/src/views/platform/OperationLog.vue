<template>
  <div class="operation-log-page">
    <div class="page-header">
      <div><h2>平台操作日志</h2><p>查看管理员操作及执行结果，日志仅供查询</p></div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-alert v-if="optionsError" :title="optionsError" type="warning" :closable="false" show-icon class="status-alert">
      <el-button link @click="loadOptions">重新加载筛选选项</el-button>
    </el-alert>
    <el-card shadow="never" class="search-card">
      <el-form :model="form" inline @submit.prevent="search">
        <el-form-item label="操作模块">
          <el-select v-model="form.module" placeholder="全部模块" clearable @change="form.action = ''">
            <el-option v-for="item in modules" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="form.action" placeholder="全部操作" clearable filterable>
            <el-option v-for="item in actions" :key="item.action" :label="item.actionName" :value="item.action" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="form.operator" placeholder="手机号或脱敏号码" maxlength="32" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="操作目标">
          <el-input v-model="form.target" placeholder="页面标识、账号或目标" maxlength="160" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="操作结果">
          <el-select v-model="form.result" placeholder="全部结果" clearable>
            <el-option label="成功" value="SUCCESS" /><el-option label="失败" value="FAILURE" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作日期">
          <el-date-picker v-model="form.dateRange" type="daterange" value-format="YYYY-MM-DD"
                          range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header><div class="table-header"><span>操作记录</span><span class="record-total">共 {{ total }} 条</span></div></template>
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon class="status-alert" />
      <el-table v-loading="loading" :data="rows" row-key="id" stripe border :empty-text="loadError || '暂无操作日志'">
        <el-table-column prop="id" label="编号" width="85" />
        <el-table-column prop="createdAt" label="操作时间" width="170" />
        <el-table-column prop="moduleName" label="模块" width="125" />
        <el-table-column prop="actionName" label="操作" min-width="150" />
        <el-table-column prop="operator" label="操作人" width="145" />
        <el-table-column label="网络 IP" min-width="195" show-overflow-tooltip>
          <template #default="{ row }"><div>{{ row.publicIp || row.clientIp || '未获取' }}</div><small class="ip-source">{{ row.publicIp ? ipSourceLabel(row.ipSource) : '原始连接地址' }}</small></template>
        </el-table-column>
        <el-table-column prop="target" label="操作目标" min-width="180" show-overflow-tooltip />
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.successful ? 'success' : 'danger'">{{ row.successful ? '成功' : '失败' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="耗时" width="100" align="right"><template #default="{ row }">{{ row.durationMs }} ms</template></el-table-column>
        <el-table-column label="详情" width="85" fixed="right">
          <template #default="{ row }"><el-button type="primary" link @click="showDetail(row.id)">查看</el-button></template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50, 100]"
                     layout="total, sizes, prev, pager, next, jumper" class="pagination"
                     @current-change="load" @size-change="changeSize" />
    </el-card>

    <el-dialog v-model="detailVisible" title="操作日志详情" width="min(760px, calc(100vw - 32px))" destroy-on-close @closed="closeDetail">
      <div v-loading="detailLoading" class="detail-content">
        <el-alert v-if="detailError" :title="detailError" type="error" :closable="false" show-icon>
          <el-button link @click="showDetail(detailId)">重新加载</el-button>
        </el-alert>
        <el-descriptions v-else-if="detail" :column="2" border>
          <el-descriptions-item label="日志编号">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="模块">{{ detail.moduleName }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">{{ detail.actionName }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detail.operator }}</el-descriptions-item>
          <el-descriptions-item :label="detail.action === 'ADMIN_LOGIN' ? '登录网络 IP' : '网络 IP'">{{ detail.publicIp || detail.clientIp || '未获取' }}</el-descriptions-item>
          <el-descriptions-item label="公网 IP 来源">{{ ipSourceLabel(detail.ipSource) }}</el-descriptions-item>
          <el-descriptions-item label="原始连接 IP" :span="2">{{ detail.clientIp || '—' }}</el-descriptions-item>
          <el-descriptions-item label="操作目标" :span="2">{{ detail.target || '—' }}</el-descriptions-item>
          <el-descriptions-item label="操作结果"><el-tag :type="detail.successful ? 'success' : 'danger'">{{ detail.successful ? '成功' : '失败' }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="HTTP 状态">{{ detail.httpStatus }}</el-descriptions-item>
          <el-descriptions-item label="当前版本">{{ detail.version ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="发布版本">{{ detail.publishedVersion ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="请求方式">{{ detail.requestMethod }}</el-descriptions-item>
          <el-descriptions-item label="处理耗时">{{ detail.durationMs }} ms</el-descriptions-item>
          <el-descriptions-item label="请求路径" :span="2">{{ detail.requestPath }}</el-descriptions-item>
          <el-descriptions-item label="操作摘要" :span="2"><span class="detail-text">{{ detail.details || '—' }}</span></el-descriptions-item>
          <el-descriptions-item label="失败原因" :span="2"><span class="detail-text">{{ detail.errorMessage || '—' }}</span></el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import request, { errorMessage } from '@/utils/request'

const endpoint = '/api/platform/operation-logs'
const ipSources = { REQUEST: '服务端识别', BROWSER: '浏览器查询（参考）', BROWSER_LOGIN: '登录时浏览器查询（参考）', UNAVAILABLE: '未获取公网 IP' }
const ipSourceLabel = value => ipSources[value] || '未获取公网 IP'
const emptyForm = () => ({ module: '', action: '', operator: '', target: '', result: '', dateRange: [] })
const form = reactive(emptyForm())
const applied = ref({})
const initialTarget = useRoute().query.target
if (typeof initialTarget === 'string') { form.target = initialTarget; applied.value = { target: initialTarget } }
const options = ref([])
const optionsError = ref('')
const modules = computed(() => [...new Map(options.value.map(item => [item.module, { value: item.module, label: item.moduleName }])).values()])
const actions = computed(() => options.value.filter(item => !form.module || item.module === form.module))
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const loadError = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref(null)
const detailId = ref(null)
let listController, detailController, optionsController

async function loadOptions() {
  optionsController?.abort()
  const current = new AbortController()
  optionsController = current
  optionsError.value = ''
  try {
    const { data } = await request.get(endpoint + '/options', { signal: current.signal })
    if (!Array.isArray(data.data)) throw new Error('筛选选项格式不正确')
    options.value = data.data
  } catch (error) {
    if (error.code !== 'ERR_CANCELED') optionsError.value = errorMessage(error)
  }
}

async function load() {
  listController?.abort()
  const current = new AbortController()
  listController = current
  loading.value = true
  loadError.value = ''
  try {
    const { data } = await request.get(endpoint, { params: { ...applied.value, page: page.value, size: size.value }, signal: current.signal })
    if (!Array.isArray(data.data?.rows) || !Number.isFinite(data.data.total)) throw new Error('操作日志数据格式不正确')
    rows.value = data.data.rows
    total.value = data.data.total
    page.value = data.data.page
  } catch (error) {
    if (error.code !== 'ERR_CANCELED') {
      rows.value = []
      total.value = 0
      loadError.value = errorMessage(error)
    }
  } finally {
    if (listController === current) loading.value = false
  }
}

function search() {
  const { dateRange, ...filters } = form
  applied.value = { ...filters, from: dateRange?.[0] || undefined, to: dateRange?.[1] || undefined }
  page.value = 1
  load()
}
function reset() { Object.assign(form, emptyForm()); search() }
function changeSize() { page.value = 1; load() }

async function showDetail(id) {
  detailController?.abort()
  const current = new AbortController()
  detailController = current
  detailId.value = id
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  detailError.value = ''
  try {
    const { data } = await request.get(endpoint + '/' + id, { signal: current.signal })
    if (!data.data?.id) throw new Error('操作日志详情格式不正确')
    detail.value = data.data
  } catch (error) {
    if (error.code !== 'ERR_CANCELED') detailError.value = errorMessage(error)
  } finally {
    if (detailController === current) detailLoading.value = false
  }
}
function closeDetail() { detailController?.abort(); detail.value = null }
onMounted(() => { loadOptions(); load() })
onBeforeUnmount(() => { listController?.abort(); detailController?.abort(); optionsController?.abort() })
</script>

<style scoped>
.operation-log-page { display: grid; gap: 20px; }
.page-header, .table-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-header h2 { margin: 0 0 8px; font-size: 22px; }
.page-header p, .record-total { margin: 0; color: #7a8799; font-size: 13px; }
.search-card :deep(.el-select), .search-card :deep(.el-input) { width: 190px; }
.search-card :deep(.el-date-editor) { max-width: 320px; }
.status-alert { margin-bottom: 16px; }
.pagination { margin-top: 20px; justify-content: flex-end; overflow-x: auto; }
.detail-content { min-height: 120px; }
.detail-content :deep(.el-descriptions__content) { overflow-wrap: anywhere; }
.detail-text { white-space: pre-wrap; }
.ip-source { color: #7a8799; font-size: 12px; }
@media (max-width: 760px) {
  .page-header { align-items: flex-start; }
  .search-card :deep(.el-form-item) { display: flex; margin-right: 0; }
  .search-card :deep(.el-form-item__content) { min-width: 0; }
  .search-card :deep(.el-select), .search-card :deep(.el-input), .search-card :deep(.el-date-editor) { width: 100%; }
}
</style>
