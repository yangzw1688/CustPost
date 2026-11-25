<template>
  <section class="management-page">
    <header class="detail-header">
      <el-button :icon="Back" text @click="goBack">返回{{ backLabel }}</el-button>
      <div class="management-heading">
        <div>
          <h1>{{ record?.caseTitle || '案件详情' }}</h1>
          <p v-if="record">案件号 {{ record.id }} · 提交于 {{ formatDateTime(record.createTime) }}</p>
        </div>
        <el-tag v-if="record" :type="caseStatusInfo(record).type" effect="light" size="large">{{ caseStatusInfo(record).label }}</el-tag>
      </div>
    </header>

    <el-skeleton v-if="loading && !record" :rows="7" animated class="detail-loading" />
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon>
      <el-button size="small" @click="load">重新加载</el-button>
    </el-alert>

    <template v-if="record">
      <el-card class="management-surface" shadow="never">
        <template #header><div class="detail-card-heading"><h2>案件进度</h2><CaseActions
          v-if="fromPage === 'PayeeReview' || fromPage === 'CaseMaintain'" :record="record"
          :reviewing="fromPage === 'PayeeReview'" :maintaining="fromPage === 'CaseMaintain'" :show-detail="false"
          @edit="edit" /></div></template>
        <dl class="detail-grid">
          <div><dt>当前状态</dt><dd>{{ caseStatusInfo(record).label }}</dd></div>
          <div v-if="record.visitDate"><dt>就诊日期</dt><dd>{{ formatDate(record.visitDate) }}</dd></div>
          <div v-if="record.reviewedAt"><dt>审核时间</dt><dd>{{ formatDateTime(record.reviewedAt) }}</dd></div>
          <div v-if="record.reviewerPhone"><dt>审核人</dt><dd>{{ maskPhone(record.reviewerPhone) }}</dd></div>
          <div v-if="record.payoutAmount != null"><dt>核定赔付金额</dt><dd class="detail-amount">{{ formatPayout(record.payoutAmount) }}</dd></div>
          <div v-if="record.paymentConfirmedAt"><dt>赔付确认时间</dt><dd>{{ formatDateTime(record.paymentConfirmedAt) }}</dd></div>
          <div v-if="record.paymentConfirmedBy"><dt>赔付确认人</dt><dd>{{ maskPhone(record.paymentConfirmedBy) }}</dd></div>
          <div v-if="record.reviewRemark && record.reviewRemark !== record.processRemark" class="detail-wide"><dt>审核备注</dt><dd class="detail-note">{{ record.reviewRemark }}</dd></div>
          <div class="detail-wide"><dt>处理结果</dt><dd class="detail-note">{{ record.processRemark || '暂无处理结果' }}</dd></div>
        </dl>
      </el-card>

      <el-card class="management-surface" shadow="never">
        <template #header><div class="detail-card-heading"><h2>报案与就诊资料</h2></div></template>
        <dl class="detail-grid">
          <div><dt>报案人姓名</dt><dd>{{ record.reporterName || '未采集' }}</dd></div>
          <div><dt>报案人手机</dt><dd>{{ record.reporterPhone || '未采集' }}</dd></div>
          <div><dt>患者姓名</dt><dd>{{ record.name || '未采集' }}</dd></div>
          <div><dt>提交联系电话</dt><dd>{{ record.phone || '未采集' }}</dd></div>
          <div class="detail-wide"><dt>病例描述</dt><dd class="detail-note">{{ record.caseDescription || '未填写' }}</dd></div>
        </dl>
      </el-card>

      <el-card class="management-surface" shadow="never">
        <template #header><div class="detail-card-heading"><h2>病例附件</h2><span>{{ attachments.length }} 份</span></div></template>
        <el-empty v-if="!attachments.length" :image-size="66" description="暂无可查看的附件" />
        <ul v-else class="attachments">
          <li v-for="file in attachments" :key="file.id || file.fileUrl">
            <a v-if="isPdfAttachment(file)" class="attachment-pdf" :href="file.fileUrl" target="_blank" rel="noopener noreferrer">
              <el-icon :size="24"><Document /></el-icon>
              <span>{{ file.originalName || '病例附件.pdf' }}</span>
              <small>在新标签页打开</small>
            </a>
            <template v-else>
              <el-image :src="file.fileUrl" :alt="file.originalName || '病例图片'" :preview-src-list="imageUrls"
                :initial-index="imageUrls.indexOf(file.fileUrl)" preview-teleported fit="cover">
                <template #error><span class="attachment-error">图片暂不可用</span></template>
              </el-image>
              <span class="attachment-name" :title="file.originalName">{{ file.originalName || '病例图片' }}</span>
            </template>
          </li>
        </ul>
      </el-card>

      <el-card class="management-surface" shadow="never">
        <template #header><div class="detail-card-heading"><h2>关联保单</h2><router-link v-if="record.policyId && canAccess('policy')"
          class="policy-link" :to="{ name: 'PolicyDetail', params: { id: record.policyId } }">查看保单详情</router-link></div></template>
        <p v-if="!record.policyNo && !record.policyId" class="detail-empty">这条报案没有关联保单。</p>
        <dl v-else class="detail-grid">
          <div><dt>保单号</dt><dd>{{ record.policyNo || '—' }}</dd></div>
          <div><dt>保险产品</dt><dd>{{ record.productName || '—' }}</dd></div>
          <div><dt>所属项目</dt><dd>{{ record.projectName || '—' }}</dd></div>
          <div><dt>保险公司</dt><dd>{{ record.insurerName || '—' }}</dd></div>
          <div><dt>投保人</dt><dd>{{ record.applicantName || '—' }}</dd></div>
          <div><dt>被保险人</dt><dd>{{ record.policyInsuredName || record.insuredName || '—' }}</dd></div>
          <div><dt>保险期间</dt><dd>{{ formatDate(record.policyStartDate) }} 至 {{ formatDate(record.policyEndDate) }}</dd></div>
        </dl>
      </el-card>
    </template>

    <CaseReviewDialog v-if="editing?.kind === 'review'" :record="editing.record" @close="editing = null" @reload="reloadAfterEdit" />
    <CaseProgressDialog v-else-if="editing" :record="editing.record" :confirming-payment="editing.kind === 'payout'"
      @close="editing = null" @reload="reloadAfterEdit" />
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Back, Document } from '@element-plus/icons-vue'
import request, { errorMessage } from '@/utils/request'
import { caseStatusInfo, formatDate, formatDateTime, formatPayout, maskPhone } from '@/utils/format'
import { canAccess } from '@/utils/permissions'
import { isPdfAttachment } from '@/shared/files/case-files.mjs'
import CaseActions from './components/CaseActions.vue'
import CaseReviewDialog from './components/CaseReviewDialog.vue'
import CaseProgressDialog from './components/CaseProgressDialog.vue'

const route = useRoute()
const router = useRouter()
const record = ref(null)
const loading = ref(false)
const error = ref('')
const editing = ref(null)
let controller
const origins = {
  CaseList: '案件列表', CaseQuery: '综合查询', CaseMaintain: '案件维护',
  PayeeReview: '领款人审核', CaringClaim: '已赔付报案', ClaimPush: '赔案推送', ClaimImage: '理赔影像'
}
const fromPage = computed(() => {
  const source = route.query.fromPage || route.query.from
  return typeof source === 'string' && Object.hasOwn(origins, source) ? source : 'CaseList'
})
const backLabel = computed(() => origins[fromPage.value])
const attachments = computed(() => (record.value?.images || []).filter(file => file.fileUrl))
const imageUrls = computed(() => attachments.value.filter(file => !isPdfAttachment(file)).map(file => file.fileUrl))

function goBack() {
  const { fromPage: source, from: legacy, ...filters } = route.query
  if (source && legacy && !filters.from) filters.from = legacy
  router.push({ name: fromPage.value, query: filters })
}

async function load() {
  controller?.abort()
  const id = route.params.id
  if (!id) return
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = ''
  try {
    const { data } = await request.get('/api/service-record/manage/' + encodeURIComponent(id), { signal: current.signal })
    if (!current.signal.aborted) record.value = data.data
  } catch (failure) {
    if (!current.signal.aborted) error.value = errorMessage(failure)
  } finally {
    if (!current.signal.aborted) loading.value = false
  }
}
function edit(kind, record) { editing.value = { kind, record } }
function reloadAfterEdit() { editing.value = null; return load() }
watch(() => route.params.id, () => { record.value = null; editing.value = null; load() }, { immediate: true })
onBeforeUnmount(() => controller?.abort())
</script>

<style scoped>
.detail-header > .el-button { padding-left: 0; margin-bottom: 8px; }
.detail-header .el-tag { flex-shrink: 0; }
.detail-loading { padding: 18px; border-radius: 12px; background: #fff; }
.detail-card-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.detail-card-heading h2 { margin: 0; font-size: 16px; }
.detail-card-heading > span { color: var(--management-muted); font-size: 13px; font-weight: 400; }
.detail-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 17px 20px; margin: 0; }
.detail-grid > div { min-width: 0; }
.detail-grid dt { color: var(--management-muted); margin-bottom: 5px; font-size: 13px; }
.detail-grid dd { margin: 0; line-height: 1.55; overflow-wrap: anywhere; }
.detail-wide { grid-column: 1 / -1; }
.detail-note { white-space: pre-wrap; }
.detail-amount { color: var(--el-color-primary-dark-2); font-weight: 650; }
.attachments { display: grid; grid-template-columns: repeat(auto-fill, minmax(148px, 1fr)); gap: 12px; list-style: none; margin: 0; padding: 0; }
.attachments li { min-width: 0; overflow: hidden; min-height: 130px; border: 1px solid var(--management-border); border-radius: 9px; background: #f8fbfc; }
.attachments .el-image { display: grid; width: 100%; height: 108px; }
.attachment-error { display: grid; place-content: center; color: var(--management-muted); font-size: 13px; }
.attachment-name { display: block; overflow: hidden; padding: 6px 8px; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }
.attachment-pdf { display: grid; min-height: 130px; align-content: center; padding: 12px; gap: 6px; justify-items: start; color: var(--el-color-primary-dark-2); text-decoration: none; overflow-wrap: anywhere; }
.attachment-pdf:hover, .policy-link:hover { text-decoration: underline; }
.attachment-pdf small, .detail-empty { color: var(--management-muted); }
.policy-link { color: var(--el-color-primary-dark-2); font-size: 13px; text-decoration: none; white-space: nowrap; }
.detail-empty { margin: 0; }
@media (max-width: 760px) {
  .detail-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .attachments { grid-template-columns: repeat(auto-fill, minmax(122px, 1fr)); }
  .detail-card-heading { align-items: flex-start; }
}
</style>
