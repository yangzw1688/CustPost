<template>
  <section class="management-page">
    <header class="management-heading">
      <div>
        <h1>{{ route.meta.title }}</h1>
        <p>{{ description }}</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </header>

    <el-card class="management-surface" shadow="never">
      <el-form class="management-filters" label-position="top" @submit.prevent="search">
        <el-form-item label="病例名称">
          <el-input v-model.trim="form.caseTitle" maxlength="100" clearable placeholder="输入病例名称" />
        </el-form-item>
        <el-form-item label="患者姓名">
          <el-input v-model.trim="form.name" maxlength="20" clearable placeholder="输入患者姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model.trim="form.phone" inputmode="tel" maxlength="20" clearable placeholder="输入提交时的电话" />
        </el-form-item>
        <el-form-item label="保单号">
          <el-input v-model.trim="form.policyNo" maxlength="80" clearable placeholder="输入保单号" />
        </el-form-item>
        <el-form-item v-if="!reviewing" label="案件状态">
          <el-select v-model="form.status" clearable placeholder="全部状态">
            <el-option v-for="state in caseStatuses" :key="state.value" :label="state.label" :value="state.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="提交日期" class="management-date-filter">
          <el-date-picker v-model="form.dates" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" />
        </el-form-item>
        <div class="management-filter-actions">
          <el-button type="primary" :icon="Search" native-type="submit">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card class="management-surface" shadow="never">
      <div class="management-table-heading">
        <h2>{{ reviewing ? '待审核报案' : '案件记录' }}</h2>
        <span v-if="!loading && !error">共 {{ total }} 条</span>
      </div>
      <el-alert v-if="error" class="management-alert" :title="error" type="error" :closable="false" show-icon />
      <el-table v-loading="loading" :data="rows" row-key="id" stripe class="management-table desktop-records"
        :empty-text="error ? '加载失败' : loading ? '加载中' : reviewing ? '暂无待审核报案' : '没有符合条件的案件'">
        <el-table-column label="案件" min-width="215" show-overflow-tooltip>
          <template #default="{ row }">
            <button type="button" class="record-link" @click="open(row)">{{ row.caseTitle || '未填写病例名称' }}</button>
            <span class="record-secondary">案件号 {{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="报案人 / 患者" min-width="132">
          <template #default="{ row }">
            <span>{{ row.reporterName || '报案人未采集' }}</span>
            <span v-if="row.name && row.name !== row.reporterName" class="record-secondary">患者 {{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="联系电话" min-width="117">
          <template #default="{ row }">{{ maskPhone(row.reporterPhone || row.phone) }}</template>
        </el-table-column>
        <el-table-column prop="policyNo" label="保单号" min-width="132" show-overflow-tooltip>
          <template #default="{ row }">{{ row.policyNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="119">
          <template #default="{ row }">
            <el-tag :type="caseStatusInfo(row).type" effect="light">{{ caseStatusInfo(row).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="149">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" :width="maintaining ? 164 : 125" fixed="right">
          <template #default="{ row }">
            <CaseActions :record="row" :reviewing="reviewing" :maintaining="maintaining" @edit="edit" @open="open" />
          </template>
        </el-table-column>
      </el-table>
      <div v-loading="loading" class="compact-records">
        <el-empty v-if="!rows.length" :image-size="58"
          :description="error ? '加载失败' : loading ? '加载中' : reviewing ? '暂无待审核报案' : '没有符合条件的案件'" />
        <ul v-else>
          <li v-for="row in rows" :key="row.id" class="compact-case">
            <div class="compact-case-title">
              <div>
                <button type="button" class="record-link" @click="open(row)">{{ row.caseTitle || '未填写病例名称' }}</button>
                <span class="record-secondary">案件号 {{ row.id }} · {{ formatDateTime(row.createTime) }}</span>
              </div>
              <el-tag :type="caseStatusInfo(row).type" effect="light">{{ caseStatusInfo(row).label }}</el-tag>
            </div>
            <dl>
              <div><dt>报案人</dt><dd>{{ row.reporterName || '未采集' }}</dd></div>
              <div><dt>患者</dt><dd>{{ row.name || '—' }}</dd></div>
              <div><dt>保单号</dt><dd>{{ row.policyNo || '—' }}</dd></div>
            </dl>
            <CaseActions :record="row" :reviewing="reviewing" :maintaining="maintaining" @edit="edit" @open="open" />
          </li>
        </ul>
      </div>

      <div v-if="total > 0" class="management-pagination">
        <el-pagination :current-page="page" :page-size="size" :total="total" :page-sizes="pageSizes"
          layout="sizes, prev, pager, next" @current-change="changePage" @size-change="changeSize" />
      </div>
    </el-card>

    <CaseReviewDialog v-if="editing?.kind === 'review'" :record="editing.record" @close="editing = null" @reload="reloadAfterEdit" />
    <CaseProgressDialog v-else-if="editing" :record="editing.record" :confirming-payment="editing.kind === 'payout'"
      @close="editing = null" @reload="reloadAfterEdit" />
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import request, { errorMessage } from '@/utils/request'
import { caseStatuses, caseStatusInfo, formatDateTime, maskPhone } from '@/utils/format'
import { listQuery, readListFilters, readPage, readText } from '@/utils/list-filters'
import CaseActions from './components/CaseActions.vue'
import CaseReviewDialog from './components/CaseReviewDialog.vue'
import CaseProgressDialog from './components/CaseProgressDialog.vue'

const route = useRoute()
const router = useRouter()
const pageSizes = [10, 20, 50, 100]
const listRoutes = ['CaseList', 'CaseQuery', 'CaseMaintain', 'PayeeReview']
const caseStates = caseStatuses.map(item => item.value)
const reviewing = computed(() => Boolean(route.meta.reviewCases))
const maintaining = computed(() => Boolean(route.meta.editProgress))
const description = computed(() => ({
  CaseList: '查看提交记录、关联保单和当前处理状态。',
  CaseQuery: '按病例、患者、联系电话、保单或日期查找案件。',
  CaseMaintain: '更新处理结果，并登记已经完成的线下赔付。',
  PayeeReview: '审核待处理的报案。审核通过后登记核定赔付金额。'
})[route.name])

const form = reactive({ caseTitle: '', name: '', phone: '', policyNo: '', status: '', dates: undefined })
const rows = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)
const error = ref('')
const editing = ref(null)
let controller
let applied = {}

async function load() {
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = ''
  try {
    const { data } = await request.get('/api/service-record/page', {
      params: { ...applied, page: page.value, size: size.value, reviewPending: reviewing.value },
      signal: current.signal
    })
    if (current.signal.aborted) return
    const result = data.data
    rows.value = result.rows
    total.value = result.total
    page.value = result.page
  } catch (failure) {
    if (current.signal.aborted) return
    rows.value = []
    total.value = 0
    error.value = errorMessage(failure)
  } finally {
    if (!current.signal.aborted) loading.value = false
  }
}

function changeQuery({ page, size, ...filters }) {
  const target = { name: route.name, query: listQuery(filters, page, size) }
  if (router.resolve(target).fullPath === route.fullPath) return load()
  return router.push(target)
}

function search() {
  return changeQuery({
    caseTitle: form.caseTitle, name: form.name, phone: form.phone, policyNo: form.policyNo,
    status: reviewing.value ? '' : form.status, from: form.dates?.[0], to: form.dates?.[1],
    page: 1, size: size.value
  })
}

function reset() { return changeQuery({ page: 1, size: size.value }) }
function changePage(value) { if (value !== page.value) return changeQuery({ ...applied, page: value, size: size.value }) }
function changeSize(value) { if (value !== size.value) return changeQuery({ ...applied, page: 1, size: value }) }
function open(row) {
  router.push({ name: 'CaseDetail', params: { id: row.id }, query: { ...route.query, fromPage: route.name } })
}
function edit(kind, record) { editing.value = { kind, record } }
function reloadAfterEdit() { editing.value = null; return load() }

watch(() => [route.name, route.query], ([name, query]) => {
  controller?.abort()
  if (!listRoutes.includes(name)) return
  const { status, from, to } = readListFilters(query, caseStates)
  applied = {
    caseTitle: readText(query.caseTitle, 100), name: readText(query.name, 20),
    phone: readText(query.phone, 20), policyNo: readText(query.policyNo, 80),
    status: reviewing.value ? '' : status, from, to
  }
  Object.assign(form, applied, { dates: from && to ? [from, to] : undefined })
  page.value = readPage(query.page)
  const requestedSize = readPage(query.size)
  size.value = pageSizes.includes(requestedSize) ? requestedSize : 10
  rows.value = []
  total.value = 0
  editing.value = null
  load()
}, { immediate: true })
onBeforeUnmount(() => controller?.abort())
</script>

<style scoped>
.record-link {
  display: block;
  max-width: 100%;
  padding: 0;
  overflow: hidden;
  border: 0;
  background: none;
  color: var(--el-color-primary-dark-2);
  font: inherit;
  font-weight: 650;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}
.record-link:hover { text-decoration: underline; }
.record-secondary { display: block; margin-top: 2px; color: #637583; font-size: 12px; }
.compact-records ul { margin: 0; padding: 0; list-style: none; }
.compact-case { padding: 13px 0; border-top: 1px solid var(--management-border); }
.compact-case:last-child { padding-bottom: 0; }
.compact-case-title { display: flex; align-items: flex-start; justify-content: space-between; gap: 9px; }
.compact-case-title > div { min-width: 0; }
.compact-case-title .el-tag { flex-shrink: 0; }
.compact-case dl { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin: 11px 0 6px; }
.compact-case dl div { min-width: 0; }
.compact-case dt { font-size: 12px; color: var(--management-muted); }
.compact-case dd { margin: 3px 0 0; font-size: 13px; overflow-wrap: anywhere; }
</style>
