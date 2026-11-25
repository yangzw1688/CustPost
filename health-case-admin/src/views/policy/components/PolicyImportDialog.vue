<template>
  <el-dialog
    v-model="visible"
    title="导入 Excel 保单"
    width="min(760px, 94vw)"
    :close-on-click-modal="false"
    :close-on-press-escape="!saving"
    :show-close="!saving"
  >
    <el-form label-position="top" :disabled="saving">
      <el-form-item label="所属项目">
        <el-select v-model="projectId" placeholder="可选，选择运行中的项目" filterable clearable :loading="projectsLoading">
          <el-option v-for="item in projectOptions" :key="item.id" :value="item.id" :label="item.name + ' · ' + item.insurerName" />
        </el-select>
        <div class="field-note">选择后，本批保单及后续案件归属于该项目的保险公司。</div>
        <div v-if="projectError" class="field-note" role="alert">{{ projectError }} <el-button link @click="loadProjects">重试</el-button></div>
      </el-form-item>
      <el-form-item label="Excel 文件">
        <input ref="fileInput" type="file" accept=".xls,.xlsx" :disabled="saving" @change="selectFile" />
        <div class="field-note">支持 .xls 和 .xlsx，每次最多 5 MB、1000 行。保单号和证件号请使用文本单元格。</div>
      </el-form-item>
      <div class="sheet-controls">
        <el-form-item label="工作表">
          <el-select v-model="sheet" :disabled="sheets.length === 0" @change="loadColumns">
            <el-option v-for="(name, index) in sheets" :key="index" :value="index + 1" :label="name" />
          </el-select>
        </el-form-item>
        <el-form-item label="表头行">
          <el-input-number v-model="headerRow" :min="1" :max="1000" :precision="0" @change="invalidateColumns" />
        </el-form-item>
        <el-button :disabled="!file || !headerRow || busy" :loading="action === 'columns'" @click="loadColumns">读取表头</el-button>
      </div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <el-alert
        v-if="sheetReady && !columns.length"
        title="当前行没有列，请选择表头所在行并重新读取。"
        type="warning"
        :closable="false"
        show-icon
      />
      <div v-if="columns.length" class="mapping-grid">
        <el-form-item v-for="field in fields" :key="field.key" :label="field.label" :required="field.required">
          <el-select
            v-model="mapping[field.key]"
            :disabled="busy"
            placeholder="选择 Excel 列"
            clearable
            filterable
            @change="preview = null"
          >
            <el-option
              v-for="column in columns"
              :key="column.index"
              :value="column.index"
              :label="column.label"
              :disabled="columnUsedByOtherField(column.index, field.key)"
            />
          </el-select>
        </el-form-item>
      </div>
    </el-form>

    <template v-if="preview">
      <el-alert
        :title="preview.errorCount ? `共 ${preview.total} 行，${preview.errorCount} 行需修正` : `${preview.total} 行校验通过，可以导入`"
        :type="preview.errorCount ? 'error' : 'success'"
        :closable="false"
        show-icon
      />
      <ul v-if="preview.errors.length" class="import-errors">
        <li v-for="message in preview.errors" :key="message">{{ message }}</li>
        <li v-if="preview.errorCount > preview.errors.length">仅显示前 {{ preview.errors.length }} 条错误。</li>
      </ul>
      <template v-else>
        <p class="field-note">预览前 {{ preview.rows.length }} 行</p>
        <el-table :data="preview.rows" row-key="row" max-height="220" border>
          <el-table-column prop="row" label="Excel 行号" width="100" />
          <el-table-column prop="policyNo" label="保单号" min-width="120" show-overflow-tooltip />
          <el-table-column prop="productName" label="保险产品" min-width="120" show-overflow-tooltip />
          <el-table-column prop="insuredName" label="被保人" min-width="100" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">{{ statusLabels[row.status] }}</template>
          </el-table-column>
        </el-table>
      </template>
    </template>

    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button :disabled="!canPreview || busy" :loading="action === 'preview'" @click="loadPreview">校验并预览</el-button>
      <el-button
        type="primary"
        :disabled="!preview || preview.errorCount > 0 || busy"
        :loading="saving"
        @click="save"
      >确认导入</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request, { errorMessage } from '@/utils/request'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'imported'])
const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})
const file = ref(null)
const fileInput = ref(null)
const sheets = ref([])
const sheet = ref(1)
const headerRow = ref(1)
const sheetReady = ref(false)
const columns = ref([])
const fields = ref([])
const mapping = ref({})
const preview = ref(null)
const error = ref('')
const action = ref('')
let pending
const projectId = ref(null), projectOptions = ref([]), projectsLoading = ref(false), projectError = ref('')
let projectController
async function loadProjects() {
  projectController?.abort(); const current = new AbortController(); projectController = current; projectsLoading.value = true; projectError.value = ''
  try { const {data} = await request.get('/api/policy/project-options', {signal:current.signal}); if(!current.signal.aborted) projectOptions.value = data.data }
  catch(failure) { if(!current.signal.aborted) projectError.value = errorMessage(failure) }
  finally { if(projectController === current) projectsLoading.value = false }
}
onMounted(loadProjects)

const statusLabels = { ACTIVE: '生效中', PENDING: '待生效', EXPIRED: '已到期', CANCELLED: '已取消' }
const busy = computed(() => Boolean(action.value))
const saving = computed(() => action.value === 'save')
const canPreview = computed(() => columns.value.length > 0 && fields.value
  .filter(field => field.required).every(field => Number.isInteger(mapping.value[field.key])))

function columnUsedByOtherField(index, fieldKey) {
  return Object.entries(mapping.value).some(([key, value]) => key !== fieldKey && value === index)
}

function invalidateColumns() {
  pending?.abort()
  action.value = ''
  columns.value = []
  mapping.value = {}
  preview.value = null
  sheetReady.value = false
  error.value = ''
}

function selectFile(event) {
  invalidateColumns()
  const nextFile = event.target.files[0]
  file.value = null
  sheets.value = []
  sheet.value = 1
  headerRow.value = 1
  if (!nextFile) return
  if (!/\.xlsx?$/i.test(nextFile.name) || nextFile.size > 5 * 1024 * 1024) {
    error.value = '请选择不超过 5 MB 的 .xls 或 .xlsx 文件'
    fileInput.value.value = ''
    return
  }
  file.value = nextFile
  loadColumns()
}

async function send(step, path, onSuccess) {
  pending?.abort()
  pending = new AbortController()
  const { signal } = pending
  action.value = step
  error.value = ''
  try {
    const form = new FormData()
    form.append('file', file.value)
    form.append('sheetNumber', sheet.value)
    form.append('headerRow', headerRow.value)
    if (step === 'save' && projectId.value) form.append('projectId', projectId.value)
    if (step !== 'columns') {
      const selected = Object.entries(mapping.value).filter(([, index]) => Number.isInteger(index))
      form.append('mapping', JSON.stringify(Object.fromEntries(selected)))
    }
    const { data } = await request.post('/api/policy/import' + path, form, { signal, timeout: 60000 })
    if (!signal.aborted) onSuccess(data.data)
  } catch (failure) {
    if (!signal.aborted) error.value = errorMessage(failure)
  } finally {
    if (!signal.aborted) action.value = ''
  }
}

function loadColumns() {
  invalidateColumns()
  if (!file.value || !headerRow.value) return
  send('columns', '/columns', result => {
    sheets.value = result.sheets
    columns.value = result.columns
    fields.value = result.fields
    mapping.value = Object.fromEntries(result.columns
      .filter(column => column.suggestedField).map(column => [column.suggestedField, column.index]))
    sheetReady.value = true
  })
}

function loadPreview() {
  preview.value = null
  send('preview', '/preview', result => { preview.value = result })
}

function save() {
  preview.value = null
  send('save', '', count => {
    ElMessage.success(`成功导入 ${count} 条保单`)
    emit('imported')
    visible.value = false
  })
}

onBeforeUnmount(() => { pending?.abort(); projectController?.abort() })
</script>

<style scoped>
.sheet-controls {
  display: grid;
  grid-template-columns: minmax(100px, 1fr) auto auto;
  align-items: end;
  gap: 12px;
  margin-bottom: 16px;
}

.sheet-controls :deep(.el-form-item) {
  margin-bottom: 0;
}

.mapping-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 200px), 1fr));
  gap: 0 12px;
  margin-top: 16px;
}

.field-note {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 6px 0 0;
}

.import-errors {
  max-height: 180px;
  overflow: auto;
  padding-left: 22px;
}

@media (max-width: 600px) {
  .sheet-controls {
    grid-template-columns: 1fr;
  }
}
</style>
