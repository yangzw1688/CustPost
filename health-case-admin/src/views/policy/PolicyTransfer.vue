<template>
  <div class="policy-transfer">
    <h1>保单导入与导出</h1>

    <el-card shadow="never">
      <template #header><h2>导入保单</h2></template>
      <p>上传 .xls 或 .xlsx，匹配 Excel 列并校验后批量写入保单。</p>
      <el-button type="primary" :icon="Upload" @click="importVisible = true">导入 Excel</el-button>
      <router-link v-if="imported" class="policy-link" :to="{ name: 'PolicyList' }">查看保单列表</router-link>
    </el-card>

    <el-card shadow="never">
      <template #header><h2>导出保单</h2></template>
      <el-form :model="filters" label-position="top" :disabled="exporting" @submit.prevent="download">
        <div class="export-filters">
          <el-form-item label="保单号">
            <el-input v-model="filters.policyNo" maxlength="64" placeholder="按保单号筛选" clearable />
          </el-form-item>
          <el-form-item label="被保人">
            <el-input v-model="filters.insuredName" maxlength="100" placeholder="按姓名筛选" clearable />
          </el-form-item>
          <el-form-item label="保单状态">
            <el-select v-model="filters.status" placeholder="全部状态" clearable>
              <el-option v-for="status in statuses" :key="status.value" v-bind="status" />
            </el-select>
          </el-form-item>
          <el-form-item label="创建日期">
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
        </div>
        <div class="export-options">
          <p>留空导出全部保单，单次最多 10,000 条。手机号和证件号自动脱敏。</p>
        </div>
        <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="export-error" />
        <el-button type="primary" native-type="submit" :icon="Download" :loading="exporting">导出 Excel</el-button>
      </el-form>
    </el-card>

    <PolicyImportDialog v-if="importVisible" v-model="importVisible" @imported="imported = true" />
  </div>
</template>

<script setup>
import { onBeforeUnmount, reactive, ref } from 'vue'
import { Download, Upload } from '@element-plus/icons-vue'
import request, { errorMessage } from '@/utils/request'
import PolicyImportDialog from './components/PolicyImportDialog.vue'

const statuses = [
  { value: 'PENDING', label: '待生效' },
  { value: 'ACTIVE', label: '生效中' },
  { value: 'EXPIRED', label: '已到期' },
  { value: 'CANCELLED', label: '已取消' }
]
const filters = reactive({ policyNo: '', insuredName: '', status: '', dateRange: null })
const importVisible = ref(false)
const imported = ref(false)
const exporting = ref(false)
const error = ref('')
let exportRequest

async function download() {
  if (exporting.value) return
  exportRequest = new AbortController()
  const { signal } = exportRequest
  exporting.value = true
  error.value = ''
  try {
    const [from, to] = filters.dateRange || []
    const { data, headers } = await request.get('/api/policy/export', {
      params: {
        policyNo: filters.policyNo.trim() || undefined,
        insuredName: filters.insuredName.trim() || undefined,
        status: filters.status || undefined,
        from,
        to
      },
      responseType: 'blob',
      signal,
      timeout: 60000
    })
    if (signal.aborted) return
    if (!data.type.includes('spreadsheetml.sheet')) throw new Error('导出响应不是 Excel 文件')
    const url = URL.createObjectURL(data)
    const link = document.createElement('a')
    link.href = url
    link.download = /filename="?([\w-]+\.xlsx)"?/i.exec(headers['content-disposition'] || '')?.[1] || 'policies.xlsx'
    document.body.appendChild(link)
    link.click()
    link.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (failure) {
    if (signal.aborted) return
    const body = failure.response?.data
    if (body instanceof Blob && body.type.includes('json')) {
      const result = await body.text().then(JSON.parse).catch(() => null)
      if (result?.message) {
        error.value = result.message
        return
      }
    }
    error.value = errorMessage(failure)
  } finally {
    if (!signal.aborted) exporting.value = false
  }
}

onBeforeUnmount(() => exportRequest?.abort())
</script>

<style scoped>
.policy-transfer {
  display: grid;
  min-width: 0;
  gap: 16px;
}

h1, h2 {
  margin: 0;
}

h1 {
  font-size: 22px;
}

h2 {
  font-size: 16px;
}

p {
  color: var(--el-text-color-secondary);
  margin: 0 0 14px;
}

.policy-link {
  margin-left: 12px;
  color: var(--el-color-primary);
}

.export-filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 245px), 1fr));
  gap: 0 16px;
}

.export-filters :deep(.el-date-editor) {
  width: 100%;
}

.export-options p {
  font-size: 13px;
}

.export-error {
  margin-bottom: 12px;
}
</style>
