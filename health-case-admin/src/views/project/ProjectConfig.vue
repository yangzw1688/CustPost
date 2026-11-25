<template>
  <div class="project-page">
    <header class="header"><div><h2>{{ id ? '项目详情' : '新增项目' }}</h2><p>维护项目资料、状态与业务归属</p></div><div><el-button :disabled="saving" @click="router.push('/project/list')">返回项目列表</el-button><el-button v-if="id && loaded && !editing" type="primary" @click="editing = true">编辑项目</el-button><el-button v-if="editing && id" :disabled="saving" @click="cancel">取消修改</el-button><el-button v-if="editing" type="primary" :loading="saving" @click="save">保存项目</el-button></div></header>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-card v-loading="loading" shadow="never">
      <el-form :model="form" label-position="top" :disabled="!editing || saving" class="form-grid">
        <el-form-item label="项目编码" required><el-input v-model="form.code" :disabled="!!id" maxlength="40" placeholder="字母开头的唯一编码" /></el-form-item>
        <el-form-item label="项目名称" required><el-input v-model="form.name" maxlength="120" /></el-form-item>
        <el-form-item label="保险公司" required><CompanySelect v-model="form.insurerId" :disabled="!editing || saving || !!form.policyCount" /></el-form-item>
        <el-form-item label="项目状态"><el-select v-model="form.status"><el-option v-for="(label,key) in projectStates" :key="key" :value="key" :label="label" /></el-select></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.ownerName" maxlength="60" /></el-form-item><el-form-item label="联系电话"><el-input v-model="form.ownerPhone" maxlength="32" /></el-form-item>
        <el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" clearable /></el-form-item><el-form-item label="结束日期"><el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" clearable /></el-form-item>
        <el-form-item label="项目说明" class="wide"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="2000" show-word-limit /></el-form-item>
        <el-form-item label="资料清单与提交说明" class="wide"><el-input v-model="form.documentRequirements" type="textarea" :rows="3" maxlength="2000" show-word-limit /></el-form-item>
      </el-form>
      <p class="note">运行中且在有效期内的项目可接收新保单和案件。暂停、结束后保留历史资料。已有关联保单的项目不能更换保险公司。</p>
    </el-card>
    <template v-if="id && loaded">
      <div class="stats"><el-card shadow="never">关联保单 <strong>{{ form.policyCount || 0 }}</strong></el-card><el-card shadow="never">关联案件 <strong>{{ form.caseCount || 0 }}</strong></el-card></div>
      <el-card shadow="never"><el-tabs>
        <el-tab-pane label="关联保单" lazy><RelatedBusiness v-if="canAccess('policy')" :id="id" resource="policies" :assignable="original.status === 'ACTIVE'" @changed="refreshCounts" /><el-empty v-else description="需要保单管理权限" /></el-tab-pane>
        <el-tab-pane label="关联案件" lazy><RelatedBusiness v-if="canAccess('cases')" :id="id" resource="cases" /><el-empty v-else description="需要案件管理权限" /></el-tab-pane>
      </el-tabs></el-card>
    </template>
  </div>
</template>
<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request, { errorMessage } from '@/utils/request'
import { canAccess } from '@/utils/permissions'
import CompanySelect from './components/CompanySelect.vue'
import RelatedBusiness from './components/RelatedBusiness.vue'
import { projectStates, notifyFailure } from './utils/management'
const route = useRoute(), router = useRouter()
const id = computed(() => route.params.id ? Number(route.params.id) : null)
const blank = () => ({ code:'', name:'', insurerId: undefined, status:'DRAFT', ownerName:'', ownerPhone:'', startDate:null, endDate:null, description:'', documentRequirements:'' })
const form = reactive(blank()), editing = ref(!id.value), saving = ref(false), loading = ref(false), loaded = ref(false), error = ref('')
const original = ref(blank())
let controller
async function load() {
  controller?.abort(); loaded.value = false; error.value = ''
  if (!id.value) { Object.assign(form, blank()); loading.value = false; const selected = Number(route.query.insurerId); if(selected > 0) form.insurerId = selected; editing.value = true; return }
  const current = new AbortController(); controller = current; loading.value = true; editing.value = false
  try { const { data } = await request.get('/api/projects/' + id.value, { signal: current.signal }); if(!current.signal.aborted) { original.value = data.data; Object.assign(form, original.value); loaded.value = true } }
  catch (failure) { if(!current.signal.aborted) error.value = errorMessage(failure) } finally { if(controller === current) loading.value = false }
}
async function refreshCounts() {
  const projectId = id.value
  try {
    const { data } = await request.get('/api/projects/' + projectId)
    if (id.value !== projectId) return
    const { policyCount, caseCount } = data.data
    Object.assign(original.value, { policyCount, caseCount })
    Object.assign(form, { policyCount, caseCount })
  } catch (failure) { if (id.value === projectId) error.value = errorMessage(failure) }
}
function cancel() { Object.assign(form, original.value); editing.value = false }
async function save() {
  if(saving.value) return
  saving.value = true
  try {
    await ElMessageBox.confirm('确认保存项目资料及当前状态？暂停或结束项目将停止接收新业务。', '确认保存', {type:'warning'})
    const { data } = id.value ? await request.put('/api/projects/' + id.value, form) : await request.post('/api/projects', form)
    original.value = data.data; Object.assign(form, original.value); editing.value = false; loaded.value = true; ElMessage.success('项目已保存')
    if(!id.value) await router.replace({name:'ProjectConfig',params:{id:data.data.id}})
  } catch(failure) { notifyFailure(failure) } finally { saving.value = false }
}
onBeforeRouteLeave(() => !saving.value)
watch(id, load, {immediate:true})
onBeforeUnmount(() => controller?.abort())
</script>
<style scoped>
.project-page{display:grid;gap:20px}.header{display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap}h2{margin:0 0 8px;font-size:22px}p,.note{color:#7a8799;font-size:13px;line-height:1.8;margin:0}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 24px}.wide{grid-column:1/-1}.form-grid :deep(.el-select),.form-grid :deep(.el-date-editor){width:100%}.stats{display:grid;grid-template-columns:1fr 1fr;gap:20px}.stats strong{display:block;font-size:28px;color:#087f7b;margin-top:10px}@media(max-width:760px){.form-grid{grid-template-columns:1fr}}
</style>
