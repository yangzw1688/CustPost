<template>
  <el-dialog :model-value="true" title="审核报案" width="min(520px, calc(100vw - 32px))"
    :show-close="!saving" :close-on-click-modal="false" :close-on-press-escape="!saving" @close="$emit('close')">
    <p>案件 #{{ record.id }} · {{ record.caseTitle }}</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-form label-position="top" :disabled="saving || conflict" @submit.prevent="save">
      <el-form-item label="审核结果" required>
        <el-radio-group v-model="decision">
          <el-radio value="APPROVED">通过</el-radio>
          <el-radio value="REJECTED">驳回</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="decision === 'APPROVED'" label="赔付金额（元）" required>
        <el-input v-model.trim="amount" inputmode="decimal" maxlength="13" placeholder="请输入金额，最多两位小数" />
      </el-form-item>
      <el-form-item :label="decision === 'REJECTED' ? '驳回原因（用户可见）' : '审核备注（用户可见）'" :required="decision === 'REJECTED'">
        <el-input v-model="remark" type="textarea" :rows="3" maxlength="200" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="$emit('close')">取消</el-button>
      <el-button v-if="conflict" type="primary" @click="$emit('reload')">刷新列表</el-button>
      <el-button v-else type="primary" :loading="saving" :disabled="!canSave" @click="save">提交审核</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request, { errorMessage } from '@/utils/request'
import { formatPayout } from '@/utils/format'

const props = defineProps({ record: { type: Object, required: true } })
const emit = defineEmits(['close', 'reload'])
const decision = ref('')
const amount = ref('')
const remark = ref('')
const saving = ref(false)
const error = ref('')
const conflict = ref(false)
const canSave = computed(() => decision.value === 'APPROVED'
  ? /^[0-9]{1,10}(\.[0-9]{1,2})?$/.test(amount.value) && Number(amount.value) > 0
  : decision.value === 'REJECTED' && remark.value.trim().length > 0)

async function save() {
  if (saving.value || conflict.value || !canSave.value) return
  saving.value = true
  error.value = ''
  const approved = decision.value === 'APPROVED'
  try {
    try {
      const message = approved
        ? '确认审核通过，核定赔付金额为 ' + formatPayout(amount.value) + '？保存后进入待线下赔付。'
        : '确认驳回该报案？用户将看到驳回原因。'
      await ElMessageBox.confirm(message, '确认审核结果', { type: 'warning' })
    } catch { return }
    await request.put('/api/service-record/manage/' + props.record.id + '/review', {
      expectedStatus: props.record.status, expectedRemark: props.record.processRemark || '',
      decision: decision.value, payoutAmount: approved ? amount.value : null, remark: remark.value.trim()
    })
    ElMessage.success(approved ? '审核通过，待线下赔付' : '已驳回报案')
    emit('reload')
  } catch (failure) {
    conflict.value = failure.response?.status === 409
    error.value = errorMessage(failure)
  } finally {
    saving.value = false
  }
}
</script>
