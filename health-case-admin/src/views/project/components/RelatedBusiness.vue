<template>
  <section>
    <div class="tools"><span>共 {{ total }} 条记录</span><div><el-button v-if="resource === 'policies'" type="primary" :disabled="!assignable" @click="openAssign">关联已有保单</el-button><el-button :loading="loading" @click="load">刷新</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-table v-loading="loading" :data="rows" :row-key="row => row.taskId || row.id || row.caseId" border stripe :empty-text="error || '暂无关联记录'">
      <template v-if="resource === 'policies'">
        <el-table-column prop="policyNo" label="保单号" min-width="170" /><el-table-column prop="productName" label="产品" min-width="160" /><el-table-column prop="insuredName" label="被保险人" width="120" />
      </template>
      <template v-else-if="resource === 'cases'"><el-table-column prop="id" label="案件ID" width="90" /><el-table-column prop="caseTitle" label="病例标题" min-width="170" /><el-table-column prop="name" label="患者" width="120" /><el-table-column prop="policyNo" label="关联保单" min-width="170" /></template>
      <template v-else><el-table-column prop="caseId" label="案件ID" width="90" /><el-table-column prop="projectName" label="所属项目" min-width="160" /><el-table-column prop="attempts" label="尝试次数" width="100" /><el-table-column prop="lastError" label="最近错误" min-width="200" show-overflow-tooltip /></template>
      <el-table-column label="状态" width="120"><template #default="{ row }">{{ statusText(row.status) }}</template></el-table-column>
      <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openBusiness(row)">查看详情</el-button></template></el-table-column>
    </el-table>
    <el-pagination :current-page="page" :page-size="10" :total="total" layout="total, prev, pager, next" class="pagination" @current-change="changePage" />
    <el-dialog v-model="assignVisible" title="关联已有保单" width="min(850px, 94vw)" :close-on-click-modal="false" :show-close="!saving">
      <p class="note">可关联尚未分配项目、尚未提交案件的保单。已有案件的保单保持原归属。</p>
      <div class="tools"><el-input v-model="keyword" placeholder="搜索保单号或产品" clearable @keyup.enter="searchAvailable" /><el-button @click="searchAvailable">查询</el-button></div>
      <el-alert v-if="assignError" :title="assignError" type="error" :closable="false" />
      <el-table :key="assignRevision" v-loading="availableLoading" :data="availableRows" row-key="id" border @selection-change="selection = $event.map(item => item.id)">
        <el-table-column type="selection" :reserve-selection="true" width="50" />
        <el-table-column prop="policyNo" label="保单号" min-width="160" /><el-table-column prop="productName" label="产品" min-width="160" /><el-table-column prop="insuredName" label="被保险人" width="120" />
      </el-table>
      <el-pagination :current-page="availablePage" :page-size="10" :total="availableTotal" layout="total, prev, pager, next" class="pagination" @current-change="value => { availablePage = value; loadAvailable() }" />
      <template #footer><span>已选 {{ selection.length }} 份</span><el-button :disabled="saving" @click="assignVisible = false">取消</el-button><el-button type="primary" :disabled="!selection.length || selection.length > 100" :loading="saving" @click="assign">保存关联</el-button></template>
    </el-dialog>
  </section>
</template>
<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request, { errorMessage } from '@/utils/request'
import { notifyFailure } from '../utils/management'
const props = defineProps({ id: { type: Number, required: true }, resource: { type: String, required: true }, assignable: Boolean })
const emit = defineEmits(['changed'])
const router = useRouter()
const rows = ref([]), page = ref(1), total = ref(0), loading = ref(false), error = ref('')
const assignVisible = ref(false), keyword = ref(''), availableRows = ref([]), availablePage = ref(1), availableTotal = ref(0), selection = ref([]), assignError = ref(''), availableLoading = ref(false), saving = ref(false)
const assignRevision = ref(0)
let controller, availableController
const endpoint = () => '/api/' + (props.resource === 'push-records' ? 'insurers/' : 'projects/') + props.id + '/' + props.resource
const statuses = { ACTIVE: '生效中', PENDING: '待处理 / 待生效', PROCESSING: '处理中', COMPLETED: '已完成', EXPIRED: '已失效', CANCELLED: '已取消', QUEUED: '排队中', RUNNING: '推送中', RETRY_WAIT: '等待重试', SUCCEEDED: '推送成功', FAILED: '推送失败' }
const statusText = value => value === 'PENDING' ? (props.resource === 'policies' ? '待生效' : '待处理') : statuses[value] || value || '—'
async function load() {
  controller?.abort(); const current = new AbortController(); controller = current; loading.value = true; error.value = ''
  try { const { data } = await request.get(endpoint(), { params: { page: page.value, size: 10 }, signal: current.signal }); if (!current.signal.aborted) { rows.value = data.data.rows; total.value = data.data.total; page.value = data.data.page } }
  catch (failure) { if (!current.signal.aborted) { rows.value = []; total.value = 0; error.value = errorMessage(failure) } }
  finally { if (controller === current) loading.value = false }
}
function changePage(value) { if (page.value !== value) { page.value = value; load() } }
function openBusiness(row) { router.push({ name: props.resource === 'policies' ? 'PolicyDetail' : 'CaseDetail', params: { id: row.caseId || row.id } }) }
function openAssign() { assignRevision.value++; selection.value = []; availableRows.value = []; keyword.value = ''; availablePage.value = 1; assignVisible.value = true; loadAvailable() }
function searchAvailable() { availablePage.value = 1; loadAvailable() }
async function loadAvailable() {
  availableController?.abort(); const current = new AbortController(); availableController = current; availableLoading.value = true; assignError.value = ''
  try { const { data } = await request.get(endpoint(), { params: { available: true, keyword: keyword.value, page: availablePage.value, size: 10 }, signal: current.signal }); if (!current.signal.aborted) { availableRows.value = data.data.rows; availableTotal.value = data.data.total; availablePage.value = data.data.page } }
  catch (failure) { if (!current.signal.aborted) assignError.value = errorMessage(failure) }
  finally { if (availableController === current) availableLoading.value = false }
}
async function assign() {
  if (saving.value) return
  saving.value = true
  try { await ElMessageBox.confirm('确认将所选 ' + selection.value.length + ' 份保单关联到当前项目？', '确认保存', { type: 'warning' }); await request.put(endpoint(), { policyIds: selection.value }); ElMessage.success('关联已保存'); assignVisible.value = false; emit('changed'); load() }
  catch (failure) { notifyFailure(failure) } finally { saving.value = false }
}
watch(() => [props.id, props.resource], () => { page.value = 1; load() }, { immediate: true })
onBeforeUnmount(() => { controller?.abort(); availableController?.abort() })
</script>
<style scoped>.tools { display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:16px; }.tools .el-input{max-width:300px}.pagination{margin-top:18px;justify-content:flex-end;overflow-x:auto}.note{font-size:13px;color:#7a8799;line-height:1.8}</style>
