<template>
  <div class="user-list-page">
    <header class="page-header">
      <div><h2>用户列表</h2><p>按登录手机号查看用户及关联业务信息</p></div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </header>
    <el-card shadow="never">
      <el-form :model="form" inline @submit.prevent="search">
        <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="完整手机号或手机号尾号" maxlength="11" clearable @keyup.enter="search" /></el-form-item>
        <el-form-item label="用户来源">
          <el-select v-model="form.source" placeholder="全部来源" clearable>
            <el-option label="已登录用户" value="LOGIN" /><el-option label="业务关联用户" value="BUSINESS" />
          </el-select>
        </el-form-item>
        <el-form-item label="最近登录日期">
          <el-date-picker v-model="form.dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
                          start-placeholder="开始日期" end-placeholder="结束日期" clearable />
        </el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="search">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
      <p class="source-note">业务关联用户来自保单或案件中的手机号，尚无登录记录。</p>
    </el-card>
    <el-card shadow="never">
      <template #header><div class="table-header"><span>用户档案</span><span class="muted">共 {{ total }} 位用户</span></div></template>
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon class="load-error" />
      <el-table v-loading="loading" :data="rows" row-key="phone" stripe border :empty-text="loadError || '暂无符合条件的用户'">
        <el-table-column label="登录手机号" min-width="155"><template #default="{ row }">{{ maskPhone(row.phone) }}</template></el-table-column>
        <el-table-column label="用户来源" width="135"><template #default="{ row }"><el-tag :type="row.registered ? 'success' : 'info'">{{ row.registered ? '已登录用户' : '业务关联用户' }}</el-tag></template></el-table-column>
        <el-table-column prop="policyCount" label="关联保单" width="100" align="center" />
        <el-table-column prop="caseCount" label="提交案件" width="100" align="center" />
        <el-table-column prop="loginCount" label="登录次数" width="100" align="center" />
        <el-table-column label="首次登录" min-width="170"><template #default="{ row }">{{ row.firstLoginAt || '暂无登录记录' }}</template></el-table-column>
        <el-table-column label="最近登录" min-width="170"><template #default="{ row }">{{ row.lastLoginAt || '暂无登录记录' }}</template></el-table-column>
        <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button type="primary" link @click="showDetail(row.phone)">查看详情</el-button></template></el-table-column>
      </el-table>
      <el-pagination :current-page="page" :page-size="size" :total="total" :page-sizes="[10, 20, 50, 100]"
                     layout="total, sizes, prev, pager, next, jumper" class="pagination"
                     @current-change="changePage" @size-change="changeSize" />
    </el-card>
  </div>
</template>
<script setup>
import { onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import request, { errorMessage } from '@/utils/request'
import { maskPhone } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const form = reactive({ phone: '', source: '', dateRange: [] })
const page = ref(1), size = ref(10), total = ref(0)
const rows = ref([]), loading = ref(false), loadError = ref('')
let controller
const text = value => typeof value === 'string' ? value : ''
const number = (value, fallback, max) => /^\d+$/.test(String(value)) ? Math.max(1, Math.min(Number(value), max)) : fallback

async function load() {
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  loadError.value = ''
  try {
    const { data } = await request.get('/api/users', { params: { ...route.query, page: page.value, size: size.value }, signal: current.signal })
    if (current.signal.aborted) return
    if (!Array.isArray(data.data?.rows) || !Number.isFinite(data.data.total)) throw new Error('用户数据格式不正确')
    rows.value = data.data.rows
    total.value = data.data.total
    page.value = data.data.page
  } catch (error) {
    if (!current.signal.aborted) { rows.value = []; total.value = 0; loadError.value = errorMessage(error) }
  } finally { if (controller === current) loading.value = false }
}

function navigateQuery(query) {
  const target = { name: 'UserList', query: Object.fromEntries(Object.entries(query).filter(([, value]) => value !== '' && value != null)) }
  if (router.resolve(target).fullPath === route.fullPath) load()
  else router.replace(target)
}
function search() {
  navigateQuery({ phone: form.phone.trim(), source: form.source, from: form.dateRange?.[0], to: form.dateRange?.[1], page: 1, size: size.value })
}
function reset() { navigateQuery({}) }
function changePage(value) { if (value !== page.value) navigateQuery({ ...route.query, page: value, size: size.value }) }
function changeSize(value) { navigateQuery({ ...route.query, page: 1, size: value }) }
function showDetail(phone) { router.push({ name: 'UserDetail', params: { id: phone }, query: { ...route.query } }) }
watch(() => route.query, query => {
  form.phone = text(query.phone)
  form.source = text(query.source)
  form.dateRange = query.from || query.to ? [text(query.from), text(query.to)] : []
  page.value = number(query.page, 1, 2147483647)
  size.value = number(query.size, 10, 100)
  load()
}, { immediate: true })
onBeforeUnmount(() => controller?.abort())
</script>
<style scoped>
.user-list-page { display: grid; gap: 20px; }
.page-header, .table-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-header h2 { margin: 0 0 8px; font-size: 22px; }
.page-header p, .muted, .source-note { margin: 0; color: #7a8799; font-size: 13px; }
.source-note { line-height: 1.7; }
.el-form :deep(.el-input), .el-form :deep(.el-select) { width: 210px; }
.el-form :deep(.el-date-editor) { max-width: 320px; }
.load-error { margin-bottom: 16px; }
.pagination { margin-top: 20px; justify-content: flex-end; overflow-x: auto; }
@media (max-width: 760px) {
  .el-form :deep(.el-form-item) { display: flex; margin-right: 0; }
  .el-form :deep(.el-form-item__content) { min-width: 0; }
  .el-form :deep(.el-input), .el-form :deep(.el-select), .el-form :deep(.el-date-editor) { width: 100%; }
}
</style>
