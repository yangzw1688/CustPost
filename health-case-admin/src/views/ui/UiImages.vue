<template>
  <div class="ui-page">
    <div class="page-heading"><div><h2>{{ isBanner ? '首页轮播' : '页面素材' }}</h2><p>{{ isBanner ? '轮播内容保存后直接更新，页面模块布局需单独发布。' : '上传用于内容页的图片，在页面编辑中选择素材。' }}</p></div><div class="actions"><el-button :loading="loading" @click="loadItems">刷新</el-button><el-button v-if="isBanner" :disabled="!!error || loading" :loading="importing" @click="importDefaults">导入原有首页图片</el-button><el-button type="primary" :disabled="!!error || loading" @click="openEditor()">新增{{ isBanner ? '轮播' : '素材' }}</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="load-error" />
    <el-card shadow="never"><el-table v-loading="loading" :data="items" :empty-text="isBanner ? '暂无轮播配置' : '暂无图片素材'"><el-table-column label="图片" width="150"><template #default="{ row }"><el-image v-if="row.imageUrl" :src="row.imageUrl" fit="cover" class="thumbnail" :preview-src-list="[row.imageUrl]" preview-teleported><template #error><span class="image-error">加载失败，请刷新</span></template></el-image><span v-else class="gradient-label">渐变背景</span></template></el-table-column><el-table-column prop="title" label="标题" min-width="140" /><el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip /><el-table-column v-if="isBanner" prop="linkUrl" label="跳转地址" min-width="160" show-overflow-tooltip /><el-table-column v-if="isBanner" prop="sortOrder" label="排序" width="80" /><el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="150" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openEditor(row)">编辑</el-button><el-button link type="danger" @click="deleteItem(row)">删除</el-button></template></el-table-column></el-table></el-card>
    <el-dialog v-model="editing" :title="(form.id ? '编辑' : '新增') + (isBanner ? '轮播' : '素材')" width="min(600px, 94vw)" :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving" @closed="resetFile">
      <el-form label-width="90px" :disabled="saving" @submit.prevent="save">
        <el-form-item label="配置标识" required><el-input v-model.trim="form.assetKey" :disabled="!!form.id" maxlength="64" placeholder="例如 home-notice，仅小写字母、数字、横线" /></el-form-item>
        <el-form-item label="标题" required><el-input v-model.trim="form.title" maxlength="80" show-word-limit /></el-form-item>
        <el-form-item label="说明"><el-input v-model.trim="form.description" maxlength="200" type="textarea" :rows="2" show-word-limit /></el-form-item>
        <el-form-item v-if="isBanner" label="跳转地址"><el-input v-model.trim="form.linkUrl" maxlength="512" placeholder="站内路径 /submit 或 https:// 开头的地址" /></el-form-item>
        <el-form-item v-if="isBanner" label="背景样式"><el-select v-model="form.tone"><el-option label="默认" value="" /><el-option label="蓝色" value="blue" /><el-option label="绿色" value="green" /></el-select></el-form-item>
        <el-form-item label="图片"><div class="file-field"><input ref="fileInput" type="file" accept="image/png,image/jpeg,image/gif,image/webp" :disabled="saving" aria-label="选择图片" @change="chooseFile" /><el-image v-if="preview || form.imageUrl" :src="preview || form.imageUrl" class="preview" fit="cover" /><p>支持 JPG、PNG、GIF、WebP，单张不超过 10MB。编辑时不选文件会保留原图片。{{ isBanner ? '新增时可使用渐变背景。' : '新增素材时请选择图片。' }}</p></div></el-form-item>
        <el-form-item v-if="isBanner" label="排序"><el-input-number v-model="form.sortOrder" :min="0" :max="9999" :precision="0" /></el-form-item>
        <el-form-item label="显示状态"><el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
        <el-alert v-if="saveError" :title="saveError" type="error" :closable="false" show-icon />
      </el-form>
      <template #footer><el-button :disabled="saving" @click="editing = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { computed, ref, reactive, watch, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { adminUiImageUrl } from '@/utils/ui-image-url'
const props = defineProps({ slotName: { type: String, default: 'home_banner' } })
const emit = defineEmits(['changed'])
const isBanner = computed(() => props.slotName === 'home_banner')
const items = ref([]), loading = ref(false), importing = ref(false), error = ref('')
const editing = ref(false), saving = ref(false), saveError = ref('')
const fileInput = ref(null), selectedFile = ref(null), preview = ref('')
const defaults = () => ({ id: null, assetKey: '', slot: props.slotName, title: '', description: '', linkUrl: '', tone: '', sortOrder: 0, enabled: true, imageUrl: '' })
const form = reactive(defaults())
let controller
let disposed = false
function resetFile() { if (preview.value) URL.revokeObjectURL(preview.value); preview.value = ''; selectedFile.value = null; if (fileInput.value) fileInput.value.value = '' }
function openEditor(row) { resetFile(); Object.assign(form, defaults(), row || {}); saveError.value = ''; editing.value = true }
function chooseFile(event) {
  const file = event.target.files?.[0]
  resetFile()
  if (!file) return
  if (!['image/png', 'image/jpeg', 'image/gif', 'image/webp'].includes(file.type) || file.size > 10 * 1024 * 1024 || !file.size) { ElMessage.error('请选择不超过10MB的有效图片'); return }
  selectedFile.value = file
  preview.value = URL.createObjectURL(file)
}
async function loadItems() {
  controller?.abort()
  const current = new AbortController(); controller = current
  loading.value = true; error.value = ''
  try {
    const response = await request.get('/api/ui/manage', { params: { slot: props.slotName }, signal: current.signal })
    if (!Array.isArray(response.data.data)) throw new Error('图片配置数据格式异常')
    if (!current.signal.aborted) {
      items.value = response.data.data.map(row => ({ ...row, imageUrl: adminUiImageUrl(row.imageUrl) }))
    }
  } catch (err) { if (!current.signal.aborted && err.code !== 'ERR_CANCELED') { error.value = err.response?.data?.message || err.message || '加载失败，请重试'; items.value = [] } }
  finally { if (controller === current) loading.value = false }
}
async function save() {
  if (saving.value) return
  saveError.value = ''
  if (!/^[a-z][a-z0-9_-]{0,63}$/.test(form.assetKey) || !form.title) { saveError.value = '请填写有效配置标识和标题'; return }
  if (!isBanner.value && !form.id && !selectedFile.value) { saveError.value = '新增素材时请选择图片'; return }
  saving.value = true
  const data = new FormData()
  for (const key of ['id', 'assetKey', 'slot', 'title', 'description', 'linkUrl', 'tone', 'sortOrder', 'enabled']) if (form[key] !== null) data.append(key, form[key])
  if (selectedFile.value) data.append('file', selectedFile.value)
  try {
    try {
      const message = isBanner.value ? '确认保存当前轮播修改吗？保存后会直接更新用户端轮播。' : '确认保存当前页面素材修改吗？引用该素材的页面将使用更新后的内容。'
      await ElMessageBox.confirm(message, '确认保存', { confirmButtonText: '确认保存', cancelButtonText: '继续编辑', type: 'warning', closeOnClickModal: false })
    } catch { return }
    if (disposed) return
    await request.post('/api/ui/save', data, { timeout: 120000 })
    if (disposed) return
    editing.value = false
    ElMessage.success('配置已保存')
    emit('changed')
    await loadItems()
  }
  catch (err) { if (!disposed) saveError.value = err.response?.data?.message || err.message || '保存失败，请重试' }
  finally { if (!disposed) saving.value = false }
}
async function importDefaults() {
  if (importing.value) return
  importing.value = true
  try { const response = await request.post('/api/ui/import-defaults', null, { timeout: 120000 }); if (disposed) return; ElMessage.success(response.data.data.imported ? '原有首页图片已导入' : '原有图片已导入，无需重复操作'); emit('changed'); await loadItems() }
  catch (err) { if (!disposed) ElMessage.error(err.response?.data?.message || err.message || '导入失败，请重试') }
  finally { if (!disposed) importing.value = false }
}
async function deleteItem(row) {
  try { await ElMessageBox.confirm(`确定删除“${row.title}”吗？`, '删除图片', { type: 'warning' }) }
  catch { return }
  try { await request.delete(`/api/ui/${row.id}`); if (!disposed) { ElMessage.success('已删除'); emit('changed'); await loadItems() } }
  catch (err) { if (!disposed) ElMessage.error(err.response?.data?.message || err.message || '删除失败') }
}
watch(() => props.slotName, loadItems, { immediate: true })
onBeforeUnmount(() => { disposed = true; controller?.abort(); resetFile() })
</script>
<style scoped>
.page-heading { display: flex; justify-content: space-between; align-items: center; gap: 20px; margin-bottom: 22px; }.page-heading h2 { margin: 0 0 8px; font-size: 23px; }.page-heading p { margin: 0; font-size: 13px; color: #8592a5; }.actions { display: flex; flex-wrap: wrap; gap: 10px; }.actions .el-button { margin: 0; }.load-error { margin-bottom: 18px; }.thumbnail { width: 120px; height: 62px; border-radius: 8px; }.gradient-label,.image-error { font-size: 12px; color: #8592a5; }.file-field { width: 100%; }.file-field input { max-width: 100%; }.preview { display: block; width: 240px; max-width: 100%; height: 120px; margin-top: 12px; border-radius: 8px; }.file-field p { font-size: 12px; color: #8592a5; line-height: 1.8; margin-bottom: 0; }@media(max-width:800px) { .page-heading { align-items: flex-start; flex-direction: column; } }
</style>
