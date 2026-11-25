<template>
  <div class="user-detail-page">
    <header class="page-header">
      <div><h2>用户详情</h2><p>查看账号登录记录及按手机号关联的业务信息</p></div>
      <div class="header-actions"><el-button @click="back">返回用户列表</el-button><el-button v-if="phone" :icon="Refresh" :loading="loading" @click="refresh">刷新</el-button></div>
    </header>
    <el-card v-if="!phone" shadow="never"><el-empty description="请从用户列表选择要查看的用户"><el-button type="primary" @click="back">前往用户列表</el-button></el-empty></el-card>
    <el-card v-else-if="loading && !user" shadow="never"><el-skeleton :rows="6" animated /></el-card>
    <el-card v-else-if="loadError" shadow="never"><el-result icon="warning" title="用户详情加载失败" :sub-title="loadError"><template #extra><el-button type="primary" @click="load">重新加载</el-button></template></el-result></el-card>
    <template v-else-if="user">
      <el-card v-loading="loading" shadow="never">
        <template #header>账号资料</template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="登录手机号"><span class="phone-number">{{ revealPhone ? user.phone : maskPhone(user.phone) }}</span><el-button type="primary" link @click="revealPhone = !revealPhone">{{ revealPhone ? '隐藏号码' : '显示完整号码' }}</el-button></el-descriptions-item>
          <el-descriptions-item label="用户来源"><el-tag :type="user.registered ? 'success' : 'info'">{{ user.registered ? '已登录用户' : '业务关联用户' }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="首次登录">{{ user.firstLoginAt || '暂无登录记录' }}</el-descriptions-item>
          <el-descriptions-item label="最近登录">{{ user.lastLoginAt || '暂无登录记录' }}</el-descriptions-item>
          <el-descriptions-item label="成功登录次数">{{ user.loginCount }} 次</el-descriptions-item>
          <el-descriptions-item label="最近提交案件">{{ user.latestCaseAt || '暂无案件' }}</el-descriptions-item>
        </el-descriptions>
        <p class="identity-note">一个手机号可以关联多位患者或被保险人，其姓名以对应案件、保单资料为准。</p>
      </el-card>
      <section class="summary-grid" aria-label="用户业务概览">
        <el-card shadow="never"><span>关联保单</span><strong>{{ user.policyCount }}</strong></el-card>
        <el-card shadow="never"><span>提交案件</span><strong>{{ user.caseCount }}</strong></el-card>
        <el-card shadow="never"><span>待完成案件</span><strong>{{ user.pendingCaseCount }}</strong></el-card>
        <el-card shadow="never"><span>已完成案件</span><strong>{{ user.completedCaseCount }}</strong></el-card>
      </section>
      <el-card shadow="never">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="关联保单" name="policies" lazy>
            <UserRelatedRecords v-if="canAccess('policy')" :key="'policies-' + relationRevision" :phone="phone" kind="policies" />
            <el-empty v-else description="需要保单管理权限才能查看关联保单" />
          </el-tab-pane>
          <el-tab-pane label="提交案件" name="cases" lazy>
            <UserRelatedRecords v-if="canAccess('cases')" :key="'cases-' + relationRevision" :phone="phone" kind="cases" />
            <el-empty v-else description="需要案件管理权限才能查看提交案件" />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>
  </div>
</template>
<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import request, { errorMessage } from '@/utils/request'
import { maskPhone } from '@/utils/format'
import { canAccess } from '@/utils/permissions'
import UserRelatedRecords from './components/UserRelatedRecords.vue'

const route = useRoute(), router = useRouter()
const phone = computed(() => String(route.params.id || ''))
const user = ref(null), loading = ref(false), loadError = ref(''), revealPhone = ref(false)
const activeTab = ref(canAccess('policy') ? 'policies' : 'cases')
const relationRevision = ref(0)
let controller
async function load() {
  controller?.abort()
  if (!phone.value) { user.value = null; return }
  const current = new AbortController()
  controller = current
  loading.value = true
  loadError.value = ''
  try {
    const { data } = await request.get('/api/users/' + encodeURIComponent(phone.value), { signal: current.signal })
    if (current.signal.aborted) return
    if (!data.data?.phone) throw new Error('用户资料格式不正确')
    user.value = data.data
  } catch (error) {
    if (!current.signal.aborted) { user.value = null; loadError.value = errorMessage(error) }
  } finally { if (controller === current) loading.value = false }
}
function refresh() { relationRevision.value++; load() }
function back() { router.push({ name: 'UserList', query: { ...route.query } }) }
watch(phone, () => { revealPhone.value = false; user.value = null; load() }, { immediate: true })
onBeforeUnmount(() => controller?.abort())
</script>
<style scoped>
.user-detail-page { display: grid; gap: 20px; }
.page-header, .header-actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.page-header h2 { margin: 0 0 8px; font-size: 22px; }
.page-header p, .identity-note { margin: 0; color: #7a8799; font-size: 13px; line-height: 1.8; }
.identity-note { margin-top: 16px; }
.phone-number { margin-right: 16px; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.summary-grid span { display: block; color: #7a8799; font-size: 13px; }
.summary-grid strong { display: block; margin-top: 12px; color: #087f7b; font-size: 28px; }
.user-detail-page :deep(.el-descriptions__content) { overflow-wrap: anywhere; }
@media (max-width: 760px) { .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
