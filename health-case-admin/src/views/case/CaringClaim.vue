<template>
  <section class="paid-cases">
    <header class="page-header">
      <div>
        <h2>已赔付报案</h2>
        <p>查看已在线下赔付并确认完成的报案。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </header>

    <el-card shadow="never">
      <el-form inline @submit.prevent="search">
        <el-form-item label="报案人 / 患者">
          <el-input v-model="form.name" maxlength="20" clearable placeholder="输入报案人或历史患者姓名" />
        </el-form-item>
        <el-form-item label="保单号">
          <el-input v-model="form.policyNo" maxlength="80" clearable placeholder="输入保单号" />
        </el-form-item>
        <el-form-item label="赔付确认日期">
          <el-date-picker v-model="form.dates" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" native-type="submit">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="list-error" />
      <el-table v-loading="loading" :data="rows" row-key="id" stripe border :empty-text="error ? '加载失败' : '暂无已赔付报案'">
        <el-table-column prop="id" label="案件号" min-width="92" />
        <el-table-column prop="caseTitle" label="报案标题" min-width="174" show-overflow-tooltip />
        <el-table-column label="报案人" min-width="118">
          <template #default="{ row }">
            <div v-if="row.reporterName">{{ row.reporterName }}</div>
            <div v-else>未采集<small v-if="row.patientName"> · 患者：{{ row.patientName }}</small></div>
          </template>
        </el-table-column>
        <el-table-column label="保单号" min-width="145" show-overflow-tooltip>
          <template #default="{ row }">{{ row.policyNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="核定赔付金额" min-width="145" align="right">
          <template #default="{ row }">{{ formatPayout(row.payoutAmount) }}</template>
        </el-table-column>
        <el-table-column label="赔付确认人" min-width="132">
          <template #default="{ row }">{{ maskPhone(row.paymentConfirmedBy) || '—' }}</template>
        </el-table-column>
        <el-table-column prop="paymentConfirmedAt" label="赔付确认时间" min-width="174" />
        <el-table-column label="处理备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.processRemark || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="95" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push({ name: 'CaseDetail', params: { id: row.id }, query: { from: 'CaringClaim' } })">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total" :current-page="page" :page-size="size" :total="total" :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper" class="pagination" @current-change="changePage" @size-change="changeSize" />
    </el-card>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import request, { errorMessage } from '@/utils/request'
import { formatPayout, maskPhone } from '@/utils/format'

const router = useRouter()
const emptyForm = () => ({ name: '', policyNo: '', dates: [] })
const form = reactive(emptyForm())
const rows = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)
const error = ref('')
let filters = {}
let controller

async function load() {
  controller?.abort()
  controller = new AbortController()
  const { signal } = controller
  loading.value = true
  error.value = ''
  try {
    const { data } = await request.get('/api/service-record/paid', { params: { ...filters, page: page.value, size: size.value }, signal })
    if (signal.aborted) return
    rows.value = data.data.rows
    page.value = data.data.page
    total.value = data.data.total
  } catch (failure) {
    if (signal.aborted) return
    error.value = errorMessage(failure)
    rows.value = []
    total.value = 0
  } finally {
    if (!signal.aborted) loading.value = false
  }
}

function search() {
  filters = { name: form.name.trim(), policyNo: form.policyNo.trim(), from: form.dates?.[0], to: form.dates?.[1] }
  page.value = 1
  load()
}
function reset() { Object.assign(form, emptyForm()); search() }
function changePage(value) { if (value !== page.value) { page.value = value; load() } }
function changeSize(value) { if (value !== size.value) { size.value = value; page.value = 1; load() } }
onMounted(load)
onBeforeUnmount(() => controller?.abort())
</script>

<style scoped>
.paid-cases { display: grid; gap: 18px; min-width: 0; }
.page-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-header h2 { margin: 0 0 6px; font-size: 22px; }
.page-header p { margin: 0; color: #718592; font-size: 13px; }
.list-error { margin-bottom: 14px; }
.pagination { margin-top: 16px; justify-content: flex-end; overflow-x: auto; }
</style>
