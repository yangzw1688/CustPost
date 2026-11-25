<template>
  <section>
    <div class="relation-header"><span class="muted">共 {{ total }} 条{{ isPolicy ? '关联保单' : '提交案件' }}</span><el-button :loading="loading" :icon="Refresh" @click="load">刷新</el-button></div>
    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon class="load-error" />
    <el-table v-loading="loading" :data="rows" row-key="id" stripe border :empty-text="loadError || (isPolicy ? '暂无关联保单' : '暂无提交案件')">
      <template v-if="isPolicy">
        <el-table-column prop="policyNo" label="保单号" min-width="170" />
        <el-table-column prop="productName" label="保险产品" min-width="170" show-overflow-tooltip />
        <el-table-column prop="relation" label="与该账号的关系" min-width="155" />
        <el-table-column prop="applicantName" label="投保人" width="120" />
        <el-table-column prop="insuredName" label="被保险人" width="120" />
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="policyStatus(row.status).type">{{ policyStatus(row.status).label }}</el-tag></template></el-table-column>
        <el-table-column label="保障期间" min-width="220"><template #default="{ row }">{{ row.startDate || '—' }} 至 {{ row.endDate || '—' }}</template></el-table-column>
      </template>
      <template v-else>
        <el-table-column prop="id" label="案件ID" width="95" />
        <el-table-column prop="caseTitle" label="病例标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="name" label="患者姓名" width="120" />
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="statusInfo(row.status).type">{{ statusInfo(row.status).label }}</el-tag></template></el-table-column>
        <el-table-column label="关联保单" min-width="170"><template #default="{ row }">{{ row.policyNo || '未绑定' }}</template></el-table-column>
        <el-table-column label="就诊日期" width="130"><template #default="{ row }">{{ row.visitDate || '—' }}</template></el-table-column>
        <el-table-column prop="createTime" label="提交时间" min-width="170" />
      </template>
      <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button type="primary" link @click="openRecord(row.id)">查看详情</el-button></template></el-table-column>
    </el-table>
    <el-pagination :current-page="page" :page-size="size" :total="total" :page-sizes="[5, 10, 20, 50]"
                   layout="total, sizes, prev, pager, next" class="pagination" @current-change="changePage" @size-change="changeSize" />
  </section>
</template>
<script setup>
/* global defineProps */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import request, { errorMessage } from '@/utils/request'
import { statusInfo } from '@/utils/format'

const props = defineProps({ phone: { type: String, required: true }, kind: { type: String, required: true } })
const router = useRouter()
const isPolicy = computed(() => props.kind === 'policies')
const rows = ref([]), total = ref(0), page = ref(1), size = ref(5), loading = ref(false), loadError = ref('')
let controller
const policyStates = {
  ACTIVE: { label: '生效中', type: 'success' }, PENDING: { label: '待生效', type: 'warning' },
  EXPIRED: { label: '已失效', type: 'info' }, CANCELLED: { label: '已取消', type: 'danger' }
}
const policyStatus = value => policyStates[value] || { label: value || '未设置', type: 'info' }
async function load() {
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  loadError.value = ''
  try {
    const { data } = await request.get('/api/users/' + encodeURIComponent(props.phone) + '/' + props.kind,
      { params: { page: page.value, size: size.value }, signal: current.signal })
    if (current.signal.aborted) return
    if (!Array.isArray(data.data?.rows) || !Number.isFinite(data.data.total)) throw new Error('关联记录格式不正确')
    rows.value = data.data.rows; total.value = data.data.total; page.value = data.data.page
  } catch (error) {
    if (!current.signal.aborted) { rows.value = []; total.value = 0; loadError.value = errorMessage(error) }
  } finally { if (controller === current) loading.value = false }
}
function changePage(value) { if (value !== page.value) { page.value = value; load() } }
function changeSize(value) { size.value = value; page.value = 1; load() }
function openRecord(id) { router.push({ name: isPolicy.value ? 'PolicyDetail' : 'CaseDetail', params: { id } }) }
watch(() => [props.phone, props.kind], () => { page.value = 1; load() }, { immediate: true })
onBeforeUnmount(() => controller?.abort())
</script>
<style scoped>
.relation-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; gap: 12px; }
.muted { color: #7a8799; font-size: 13px; }
.load-error { margin-bottom: 16px; }
.pagination { margin-top: 20px; justify-content: flex-end; overflow-x: auto; }
</style>
