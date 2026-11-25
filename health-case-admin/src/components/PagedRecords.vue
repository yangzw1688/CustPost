<template>
  <section>
    <h2>{{ title }}</h2>
    <div class="tools">
      <el-input v-model.trim="keyword" :placeholder="searchHint" clearable @keyup.enter="search" />
      <el-button type="primary" @click="search">查询</el-button>
      <slot name="actions" />
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column v-for="column in columns" :key="column.prop" v-bind="column" />
      <el-table-column v-if="$slots.rowActions" label="操作" width="120">
        <template #default="{ row }"><slot name="rowActions" :row="row" /></template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="size" layout="total, prev, pager, next" @current-change="load" />
  </section>
</template>
<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import request, { errorMessage } from '@/utils/request'
const props = defineProps({
  title: { type: String, required: true },
  endpoint: { type: String, required: true },
  columns: { type: Array, required: true },
  searchKey: { type: String, default: 'keyword' },
  searchHint: { type: String, default: '输入关键词' }
})
const keyword = ref('')
const rows = ref([])
const page = ref(1)
const size = 15
const total = ref(0)
const loading = ref(false)
const error = ref('')
let controller

async function load() {
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = ''
  try {
    const { data } = await request.get(props.endpoint, { params: { page: page.value, size, [props.searchKey]: keyword.value }, signal: current.signal })
    if (current.signal.aborted) return
    rows.value = data.data.rows
    total.value = data.data.total
    page.value = data.data.page
  } catch (failure) {
    if (!current.signal.aborted) error.value = errorMessage(failure)
  } finally {
    if (!current.signal.aborted) loading.value = false
  }
}
function search() { page.value = 1; load() }
watch(() => props.endpoint, search, { immediate: true })
onBeforeUnmount(() => controller?.abort())
</script>
<style scoped>
.tools { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.tools .el-input { width: 240px; }
.el-pagination { margin-top: 16px; }
</style>
