<template>
  <div class="company-page">
    <header class="header"><div><h2>{{ id ? '保险公司详情' : '新增保险公司' }}</h2><p>公司资料、对接环境与关联项目集中维护</p></div><div><el-button :disabled="saving" @click="router.push('/project/insurers')">返回保险公司列表</el-button><el-button v-if="id && loaded && !editing" type="primary" @click="editing = true">编辑资料</el-button><el-button v-if="editing && id" :disabled="saving" @click="cancel">取消修改</el-button><el-button v-if="editing" type="primary" :loading="saving" @click="saveCompany">保存资料</el-button></div></header>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-card v-loading="loading" shadow="never">
      <el-form :model="form" label-position="top" :disabled="!editing || saving" class="form-grid">
        <el-form-item label="保险公司编码" required><el-input v-model="form.code" :disabled="!!id" maxlength="40" placeholder="字母开头的唯一编码" /></el-form-item>
        <el-form-item label="保险公司全称" required><el-input v-model="form.name" maxlength="120" /></el-form-item>
        <el-form-item label="简称"><el-input v-model="form.shortName" maxlength="80" /></el-form-item>
        <el-form-item label="启用状态"><el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactName" maxlength="60" /></el-form-item><el-form-item label="联系电话"><el-input v-model="form.contactPhone" maxlength="32" /></el-form-item>
        <el-form-item label="当前生效环境"><el-select v-model="form.activeEnvironment"><el-option v-for="(label,key) in environments" :key="key" :value="key" :label="label" /></el-select></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" maxlength="240" /></el-form-item>
        <el-form-item label="备注" class="wide"><el-input v-model="form.remarks" type="textarea" :rows="3" maxlength="1000" show-word-limit /></el-form-item>
      </el-form>
    </el-card>
    <el-card v-if="id && loaded" shadow="never">
      <el-tabs>
        <el-tab-pane label="对接配置" lazy>
          <div class="tools"><el-radio-group v-model="environment" :disabled="connectionEditing || connectionSaving"><el-radio-button value="TEST">测试环境</el-radio-button><el-radio-button value="PROD">生产环境</el-radio-button></el-radio-group><div><el-button v-if="!connectionEditing" @click="connectionEditing = true">编辑对接配置</el-button><el-button v-else :disabled="connectionSaving" @click="resetConnection">取消修改</el-button><el-button v-if="connectionEditing" type="primary" :loading="connectionSaving" @click="saveConnection">保存对接配置</el-button></div></div>
          <el-form :model="connection" label-position="top" :disabled="!connectionEditing || connectionSaving" class="form-grid">
            <el-form-item label="对接状态"><el-select v-model="connection.status"><el-option v-for="(label,key) in connectionStates" :key="key" :label="label" :value="key" /></el-select></el-form-item>
            <el-form-item label="鉴权方式"><el-select v-model="connection.authType"><el-option label="无鉴权" value="NONE" /><el-option label="Bearer 令牌" value="BEARER" /></el-select></el-form-item>
            <el-form-item label="HTTPS 通知接收地址" class="wide"><el-input v-model="connection.endpoint" maxlength="512" placeholder="https://保险公司接口地址" /></el-form-item>
            <el-form-item v-if="connection.authType === 'BEARER'" label="接口令牌" class="wide"><el-input v-model="connection.token" type="password" show-password autocomplete="new-password" maxlength="512" :placeholder="connection.tokenConfigured ? '已配置，留空保持原令牌' : '填写保险公司提供的令牌'" /><p class="note">令牌加密保存，不会在详情或日志中回显。首次保存前需部署密钥 INTEGRATION_ENCRYPTION_KEY。</p></el-form-item>
          </el-form>
          <el-alert title="当前支持 HTTPS JSON 赔案通知，内容为 taskId 和 caseId。标记为「已对接」后允许推送；保存配置不会主动向保险公司发送请求。" type="info" :closable="false" show-icon />
          <p class="note">实际推送使用「当前生效环境」的配置。选择无鉴权并保存会清除该环境的旧令牌。</p>
        </el-tab-pane>
        <el-tab-pane label="关联项目" lazy><ManagementList mode="projects" embedded :insurer-id="id" /></el-tab-pane>
        <el-tab-pane label="推送记录" lazy><RelatedBusiness v-if="canAccess('cases')" :id="id" resource="push-records" /><el-empty v-else description="需要案件管理权限才能查看推送记录" /></el-tab-pane>
        <el-tab-pane v-if="canAccess('platform')" label="操作日志" lazy><el-button @click="router.push({name:'OperationLog',query:{target:'保险公司/' + id + '/'}})">查看该保险公司的操作日志</el-button></el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>
<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request, { errorMessage } from '@/utils/request'
import { canAccess } from '@/utils/permissions'
import ManagementList from './components/ManagementList.vue'
import RelatedBusiness from './components/RelatedBusiness.vue'
import { environments, connectionStates, notifyFailure } from './utils/management'
const route = useRoute(), router = useRouter()
const id = computed(() => route.params.id ? Number(route.params.id) : null)
const blank = () => ({code:'',name:'',shortName:'',contactName:'',contactPhone:'',address:'',remarks:'',enabled:true,activeEnvironment:'TEST'})
const form = reactive(blank()), loading = ref(false), loaded = ref(false), saving = ref(false), editing = ref(!id.value), error = ref('')
const environment = ref('TEST'), connections = ref([]), connection = reactive({}), connectionEditing = ref(false), connectionSaving = ref(false)
let original = blank(), controller
function resetConnection() { Object.keys(connection).forEach(key => delete connection[key]); Object.assign(connection, connections.value.find(item => item.environment === environment.value) || {}, {token:''}); connectionEditing.value = false }
async function load() {
  controller?.abort(); error.value = ''; loaded.value = false
  if(!id.value) { Object.assign(form,blank()); loading.value = false; editing.value = true; return }
  const current = new AbortController(); controller = current; loading.value = true; editing.value = false
  try {
    const [profile, configs] = await Promise.all([request.get('/api/insurers/' + id.value,{signal:current.signal}),request.get('/api/insurers/' + id.value + '/connections',{signal:current.signal})])
    if(!current.signal.aborted) { original = profile.data.data; Object.assign(form,original); connections.value = configs.data.data; environment.value = form.activeEnvironment; resetConnection(); loaded.value = true }
  } catch(failure) { if(!current.signal.aborted) error.value = errorMessage(failure) } finally { if(controller === current) loading.value = false }
}
function cancel() { Object.assign(form, original); editing.value = false }
async function saveCompany() {
  if(saving.value) return
  saving.value = true
  try {
    await ElMessageBox.confirm('确认保存保险公司资料及生效环境？停用公司会停止其项目接收新业务及赔案推送。','确认保存',{type:'warning'})
    const {data} = id.value ? await request.put('/api/insurers/' + id.value,form) : await request.post('/api/insurers',form)
    original = data.data; Object.assign(form,original); editing.value = false; ElMessage.success('公司资料已保存')
    if(!id.value) await router.replace({name:'InsuranceCompanyDetail',params:{id:data.data.id}})
  } catch(failure) { notifyFailure(failure) } finally { saving.value = false }
}
async function saveConnection() {
  if(connectionSaving.value) return
  connectionSaving.value = true
  try {
    await ElMessageBox.confirm('确认保存' + environments[environment.value] + '的对接配置？已对接状态允许工作器按此配置发送通知。','确认保存',{type:'warning'})
    const {data} = await request.put('/api/insurers/' + id.value + '/connections/' + environment.value,connection)
    connections.value = [...connections.value.filter(item => item.environment !== environment.value), data.data]; resetConnection()
    ElMessage.success('对接配置已保存')
  } catch(failure) { notifyFailure(failure) } finally { connectionSaving.value = false }
}
onBeforeRouteLeave(() => !saving.value && !connectionSaving.value)
watch(environment, resetConnection)
watch(id, load, {immediate:true})
onBeforeUnmount(() => controller?.abort())
</script>
<style scoped>
.company-page{display:grid;gap:20px}.header,.tools{display:flex;justify-content:space-between;align-items:center;gap:16px;flex-wrap:wrap}.tools{margin-bottom:20px}h2{margin:0 0 8px;font-size:22px}p,.note{margin:0;color:#7a8799;font-size:13px;line-height:1.8}.note{margin-top:12px}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 24px}.wide{grid-column:1/-1}.form-grid :deep(.el-select){width:100%}@media(max-width:760px){.form-grid{grid-template-columns:1fr}}
</style>
