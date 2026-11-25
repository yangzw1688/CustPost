<template>
  <el-dialog :model-value="true" :title="confirmingPayment ? '确认线下赔付完成' : '更新案件进度'" width="min(520px, calc(100vw - 32px))"
    :show-close="!saving" :close-on-click-modal="false" :close-on-press-escape="!saving" @close="$emit('close')">
    <p>案件 #{{ record.id }} · {{ record.caseTitle }}</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-form label-position="top" :disabled="saving || conflict" @submit.prevent="save">
      <el-form-item v-if="confirmingPayment" label="核定赔付金额">
        <strong>{{ formatPayout(record.payoutAmount) }}</strong>
      </el-form-item>
      <el-form-item v-else label="案件状态" required>
        <el-select v-model="status" style="width: 100%">
          <el-option v-for="option in availableStatuses" :key="option" :label="labels[option] || option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="处理结果（用户可见）" :required="needsReason">
        <el-input v-model="remark" type="textarea" :rows="4" maxlength="200" show-word-limit />
      </el-form-item>
      <el-checkbox v-if="confirmingPayment" v-model="paymentConfirmed">我确认已在线下完成赔付</el-checkbox>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="$emit('close')">取消</el-button>
      <el-button v-if="conflict" type="primary" @click="$emit('reload')">刷新列表</el-button>
      <el-button v-else type="primary" :loading="saving" :disabled="!canSave" @click="save">{{ confirmingPayment ? '确认完成' : '保存' }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request, { errorMessage } from '@/utils/request'
import { formatPayout } from '@/utils/format'

const props = defineProps({ record: { type: Object, required: true }, confirmingPayment: Boolean })
const emit = defineEmits(['close', 'reload'])
const labels = { PENDING: '待审核', PROCESSING: props.record.reviewDecision === 'APPROVED' ? '待线下赔付' : '处理中', COMPLETED: '已完成', REJECTED: '已驳回', CANCELLED: '已取消' }
const expectedStatus = props.record.status
const expectedRemark = props.record.processRemark || ''
const status = ref(props.confirmingPayment ? 'COMPLETED' : expectedStatus)
const paymentConfirmed = ref(false)
const remark = ref(expectedRemark)
const availableStatuses = expectedStatus === 'PENDING' ? ['PENDING', 'CANCELLED'] : [expectedStatus]
const saving = ref(false)
const error = ref('')
const conflict = ref(false)
const needsReason = computed(() => ['REJECTED', 'CANCELLED'].includes(status.value))
const canSave = computed(() => props.confirmingPayment ? paymentConfirmed.value : availableStatuses.includes(status.value)
  && (!needsReason.value || remark.value.trim())
  && (status.value !== expectedStatus || remark.value.trim() !== expectedRemark))

async function save() {
  if (saving.value || conflict.value || !canSave.value) return
  saving.value = true
  error.value = ''
  try {
    try {
      const message = props.confirmingPayment
        ? '确认实际赔付已完成，并将该案件标记为「已完成」？'
        : status.value !== expectedStatus
        ? '保存后，用户端会显示新状态，并自动创建赔案推送任务。'
        : '保存后，用户端会显示更新后的处理结果。'
      await ElMessageBox.confirm(message, '确认更新案件进度', { type: 'warning' })
    } catch { return }
    await request.put('/api/service-record/manage/' + props.record.id + '/progress', {
      expectedStatus, expectedRemark, status: status.value, processRemark: remark.value.trim(),
      paymentConfirmed: props.confirmingPayment && paymentConfirmed.value
    })
    ElMessage.success('案件进度已更新')
    emit('reload')
  } catch (failure) {
    conflict.value = failure.response?.status === 409
    error.value = errorMessage(failure)
  } finally {
    saving.value = false
  }
}
</script>
