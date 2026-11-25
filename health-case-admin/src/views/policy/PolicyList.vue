<template>
  <section class="management-page">
    <header class="management-heading">
      <div><h1>保单管理</h1><p>查询保单、被保险人和保单有效期。</p></div>
      <el-button v-if="canAccess('policyTransfer')" @click="router.push({ name: 'PolicyTransfer' })">保单导入与导出</el-button>
    </header>

    <el-card class="management-surface" shadow="never">
      <el-form class="management-filters" label-position="top" @submit.prevent="search">
        <el-form-item label="保单号"><el-input v-model.trim="filters.policyNo" clearable placeholder="输入保单号" /></el-form-item>
        <el-form-item label="被保险人"><el-input v-model.trim="filters.insuredName" clearable placeholder="输入姓名" /></el-form-item>
        <el-form-item label="保单状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态">
            <el-option v-for="(item, key) in statuses" :key="key" :label="item.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="登记日期" class="management-date-filter">
          <el-date-picker v-model="filters.dates" type="daterange" value-format="YYYY-MM-DD"
                          range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
        </el-form-item>
        <div class="management-filter-actions">
          <el-button type="primary" native-type="submit" :loading="loading">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card class="management-surface" shadow="never">
      <div class="management-table-heading"><h2>保单列表</h2><span v-if="!error">共 {{ total }} 份</span></div>
      <el-alert v-if="error" class="management-alert" :title="error" type="error" :closable="false" show-icon />
      <el-table v-loading="loading" class="management-table desktop-records" :data="rows" stripe row-key="id"
                :empty-text="error ? '保单加载失败' : '暂无符合条件的保单'">
        <el-table-column prop="policyNo" label="保单号" min-width="145" show-overflow-tooltip />
        <el-table-column prop="productName" label="保险产品" min-width="160" show-overflow-tooltip />
        <el-table-column prop="insuredName" label="被保险人" min-width="115" />
        <el-table-column label="状态" width="105">
          <template #default="{ row }"><el-tag :type="statusInfo(row.status).type">{{ statusInfo(row.status).label }}</el-tag></template>
        </el-table-column>
        <el-table-column label="有效期" min-width="210">
          <template #default="{ row }">{{ formatDate(row.startDate) }} 至 {{ formatDate(row.endDate) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="102" fixed="right">
          <template #default="{ row }"><el-button type="primary" link @click="openPolicy(row)">查看详情</el-button></template>
        </el-table-column>
      </el-table>

      <div v-loading="loading" class="compact-records">
        <el-empty v-if="!rows.length" :image-size="60" :description="error ? '保单加载失败' : '暂无符合条件的保单'" />
        <ul v-else class="policy-cards">
          <li v-for="row in rows" :key="row.id">
            <div class="policy-card-title">
              <div><strong>{{ row.productName || '未填写保险产品' }}</strong><small>{{ row.policyNo || '—' }}</small></div>
              <el-tag :type="statusInfo(row.status).type">{{ statusInfo(row.status).label }}</el-tag>
            </div>
            <dl>
              <dt>被保险人</dt><dd>{{ row.insuredName || '—' }}</dd>
              <dt>有效期</dt><dd>{{ formatDate(row.startDate) }} 至 {{ formatDate(row.endDate) }}</dd>
            </dl>
            <el-button type="primary" link @click="openPolicy(row)">查看详情</el-button>
          </li>
        </ul>
      </div>

      <el-pagination v-if="total" v-model:current-page="page" class="management-pagination"
                     :total="total" :page-size="size" layout="total, prev, pager, next"
                     :pager-count="5" @current-change="load" />
    </el-card>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import request, { errorMessage } from '@/utils/request'
import { formatDate } from '@/utils/format'
import { canAccess } from '@/utils/permissions'

const statuses = {
  PENDING: { label: '待生效', type: 'warning' },
  ACTIVE: { label: '生效中', type: 'success' },
  EXPIRED: { label: '已失效', type: 'info' },
  CANCELLED: { label: '已取消', type: 'danger' }
}
const router = useRouter()
const defaults = () => ({ policyNo: '', insuredName: '', status: '', dates: null })
const filters = reactive(defaults())
const rows = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const error = ref('')
const size = 15
let applied = {}
let controller

const statusInfo = status => statuses[status] || { label: status || '—', type: 'info' }

function openPolicy(row) {
  router.push({ name: 'PolicyDetail', params: { id: row.id } })
}

async function load() {
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = ''
  try {
    const { data } = await request.get('/api/policy/page', {
      params: { ...applied, page: page.value, size },
      signal: current.signal
    })
    if (current.signal.aborted) return
    rows.value = data.data.rows
    total.value = data.data.total
    page.value = data.data.page
  } catch (failure) {
    if (!current.signal.aborted) {
      rows.value = []
      total.value = 0
      error.value = errorMessage(failure)
    }
  } finally {
    if (!current.signal.aborted) loading.value = false
  }
}

function search() {
  const { policyNo, insuredName, status, dates } = filters
  applied = { policyNo, insuredName, status, from: dates?.[0], to: dates?.[1] }
  page.value = 1
  load()
}

function reset() {
  Object.assign(filters, defaults())
  search()
}

onMounted(load)
onBeforeUnmount(() => controller?.abort())
</script>

<style scoped>
.policy-cards { display: grid; gap: 12px; list-style: none; padding: 0; margin: 0; }
.policy-cards li { border: 1px solid var(--management-border); border-radius: 10px; padding: 13px; }
.policy-card-title { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.policy-card-title > div { display: grid; gap: 5px; overflow-wrap: anywhere; }
.policy-card-title small, dt { color: var(--management-muted); }
dl { display: grid; grid-template-columns: auto 1fr; gap: 8px 12px; margin: 13px 0; font-size: 13px; }
dd { margin: 0; overflow-wrap: anywhere; }
</style>
