<template>
  <section>
    <h2>保单操作记录</h2>
    <el-input v-model.trim="keyword" placeholder="搜索保单号或操作人" clearable style="width: 260px; margin-bottom: 16px" />
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-table v-loading="loading" :data="visible" stripe>
      <el-table-column prop="policyNo" label="保单号" min-width="160" />
      <el-table-column label="操作" width="145"><template #default="{ row }">{{ policyOperationLabel(row.operationType) }}</template></el-table-column>
      <el-table-column prop="operationContent" label="内容" min-width="200" />
      <el-table-column prop="operator" label="操作人" min-width="110" />
      <el-table-column label="时间" width="170"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="15" :total="filtered.length" layout="total, prev, pager, next" />
  </section>
</template>
<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { errorMessage, getList } from '@/utils/request'
import { formatDateTime, policyOperationLabel } from '@/utils/format'
const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const filtered = computed(() => rows.value.filter(row => [row.policyNo, row.operator].some(value => String(value || '').includes(keyword.value))))
const visible = computed(() => filtered.value.slice((page.value - 1) * 15, page.value * 15))
watch(keyword, () => { page.value = 1 })
onMounted(async () => {
  loading.value = true
  try { rows.value = await getList('/api/policy/operationLogList') }
  catch (failure) { error.value = errorMessage(failure) }
  finally { loading.value = false }
})
</script>
