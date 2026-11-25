export const caseStatuses = [
  { value: 'PENDING', label: '待处理', type: 'warning' },
  { value: 'PROCESSING', label: '处理中', type: 'primary' },
  { value: 'COMPLETED', label: '已完成', type: 'success' },
  { value: 'CANCELLED', label: '已取消', type: 'info' },
  { value: 'REJECTED', label: '已驳回', type: 'danger' }
]
export function statusInfo(value) {
  return caseStatuses.find(item => item.value === value) || { label: value || '未设置', type: 'info' }
}
export function canReviewCase(record) {
  return Boolean(record && !record.reviewDecision && ['PENDING', 'PROCESSING'].includes(record.status))
}
export function canConfirmPayout(record) {
  return Boolean(record?.status === 'PROCESSING' && record.reviewDecision === 'APPROVED' && !record.paymentConfirmedAt)
}
export function caseStatusInfo(record) {
  if (canConfirmPayout(record)) return { label: '待线下赔付', type: 'warning' }
  return statusInfo(record?.status)
}
export function formatDate(value) { return value ? String(value).substring(0, 10) : '—' }
const payoutFormat = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' })
export function formatPayout(value) { return payoutFormat.format(Number(value)) }
export function maskPhone(value) { return value ? String(value).replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2') : '—' }
export function formatDateTime(value) { return value ? String(value).replace('T', ' ').substring(0, 16) : '—' }
const policyOperationLabels = {
  CREATE: '新增保单',
  UPDATE: '修改保单',
  STATUS_CHANGE: '状态变更',
  AUDIT: '审核',
  DELETE: '删除保单'
}

export function policyOperationLabel(type) {
  return policyOperationLabels[type] || '其他操作'
}

export function dateKey(date) {
  return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-')
}
