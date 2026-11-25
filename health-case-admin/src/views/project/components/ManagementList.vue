<template>
  <div class="management-list">
    <header class="page-header"><div><h2 v-if="!embedded">{{ isCompany ? '保险公司管理' : '项目列表' }}</h2><p>{{ isCompany ? '维护合作保险公司资料及对接环境' : '管理合作项目及关联保单、案件' }}</p></div><el-button type="primary" @click="create">{{ isCompany ? '新增保险公司' : '新增项目' }}</el-button></header>
    <el-card shadow="never">
      <el-form inline @submit.prevent="search">
        <el-form-item label="名称或编码"><el-input v-model="form.keyword" maxlength="120" placeholder="搜索名称或编码" clearable @keyup.enter="search" /></el-form-item>
        <el-form-item v-if="!isCompany && !embedded" label="保险公司"><CompanySelect v-model="form.insurerId" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status" placeholder="全部状态" clearable><el-option v-for="(label, key) in states" :key="key" :label="label" :value="key" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="reset">重置</el-button><el-button :loading="loading" @click="load">刷新</el-button></el-form-item>
      </el-form>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <el-table v-loading="loading" :data="rows" row-key="id" border stripe :empty-text="error || '暂无数据'">
        <el-table-column prop="code" label="编码" min-width="140" />
        <el-table-column prop="name" :label="isCompany ? '保险公司' : '项目名称'" min-width="190" />
        <template v-if="isCompany">
          <el-table-column prop="contactName" label="联系人" width="110" />
          <el-table-column prop="contactPhone" label="联系电话" min-width="150" />
          <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
          <el-table-column label="当前环境" width="110"><template #default="{ row }">{{ environments[row.activeEnvironment] }}</template></el-table-column>
          <el-table-column label="对接状态" width="110"><template #default="{ row }">{{ connectionStates[row.connectionStatus] || '未配置' }}</template></el-table-column>
          <el-table-column prop="projectCount" label="项目数" width="90" />
        </template>
        <template v-else>
          <el-table-column prop="insurerName" label="保险公司" min-width="170" />
          <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ projectStates[row.status] }}</el-tag></template></el-table-column>
          <el-table-column prop="ownerName" label="负责人" width="110" />
          <el-table-column prop="policyCount" label="保单数" width="90" />
          <el-table-column prop="caseCount" label="案件数" width="90" />
        </template>
        <el-table-column label="操作" width="115" fixed="right"><template #default="{ row }"><el-button type="primary" link @click="open(row.id)">详情 / 编辑</el-button></template></el-table-column>
      </el-table>
      <el-pagination :current-page="page" :page-size="size" :total="total" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next" class="pagination" @current-change="changePage" @size-change="changeSize" />
    </el-card>
  </div>
</template>
<script setup>
/* global defineProps */
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request, { errorMessage } from '@/utils/request'
import CompanySelect from './CompanySelect.vue'
import { projectStates, connectionStates, environments } from '../utils/management'
const props = defineProps({ mode: { type: String, required: true }, embedded: Boolean, insurerId: Number })
const route = useRoute(), router = useRouter()
const isCompany = computed(() => props.mode === 'insurers')
const states = computed(() => isCompany.value ? { ENABLED: '启用', DISABLED: '停用' } : projectStates)
const form = reactive({ keyword: '', status: '', insurerId: undefined })
const rows = ref([]), page = ref(1), size = ref(10), total = ref(0), loading = ref(false), error = ref('')
let controller, applied = {}
async function load() {
  controller?.abort(); const current = new AbortController(); controller = current; loading.value = true; error.value = ''
  try {
    const { data } = await request.get('/api/' + props.mode, { params: { ...applied, insurerId: props.insurerId || applied.insurerId, page: page.value, size: size.value }, signal: current.signal })
    if (!current.signal.aborted) { rows.value = data.data.rows; total.value = data.data.total; page.value = data.data.page }
  } catch (failure) { if (!current.signal.aborted) { rows.value = []; total.value = 0; error.value = errorMessage(failure) } }
  finally { if (controller === current) loading.value = false }
}
function search() { applied = { ...form }; page.value = 1; load() }
function reset() { Object.assign(form, { keyword: '', status: '', insurerId: undefined }); search() }
function changePage(value) { if (value !== page.value) { page.value = value; load() } }
function changeSize(value) { size.value = value; page.value = 1; load() }
function open(id) { router.push({ name: isCompany.value ? 'InsuranceCompanyDetail' : 'ProjectConfig', params: { id } }) }
function create() { router.push({ name: isCompany.value ? 'InsuranceCompanyDetail' : 'ProjectConfig', query: props.insurerId ? { insurerId: props.insurerId } : {} }) }
watch(() => [props.insurerId, route.query.insurerId], () => {
  const id = Number(props.insurerId || route.query.insurerId)
  form.insurerId = Number.isSafeInteger(id) && id > 0 ? id : undefined
  search()
}, { immediate: true })
onBeforeUnmount(() => controller?.abort())
</script>
<style scoped>
.management-list { display: grid; gap: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
h2 { margin: 0 0 8px; font-size: 22px; } p { margin: 0; font-size: 13px; color: #7a8799; }
.el-form :deep(.el-input), .el-form :deep(.el-select) { width: 200px; }
.pagination { margin-top: 20px; justify-content: flex-end; overflow-x: auto; }
@media(max-width:760px) { .el-form :deep(.el-form-item) { display: flex; margin-right: 0; } }
</style>
