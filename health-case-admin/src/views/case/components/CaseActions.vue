<template>
  <div class="case-actions">
    <el-button v-if="reviewing && canReviewCase(record)" type="primary" link @click="$emit('edit', 'review', record)">审核</el-button>
    <template v-if="maintaining">
      <el-button v-if="canConfirmPayout(record)" type="primary" link @click="$emit('edit', 'payout', record)">确认已赔付</el-button>
      <el-button type="primary" link @click="$emit('edit', 'progress', record)">{{ canConfirmPayout(record) ? '进度' : '维护进度' }}</el-button>
    </template>
    <el-button v-if="!maintaining || !canConfirmPayout(record)" type="primary" link @click="$emit('open', record)">详情</el-button>
  </div>
</template>

<script setup>
import { canConfirmPayout, canReviewCase } from '@/utils/format'
defineProps({ record: { type: Object, required: true }, reviewing: Boolean, maintaining: Boolean })
defineEmits(['edit', 'open'])
</script>

<style scoped>
.case-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 3px 8px; }
.case-actions .el-button { margin-left: 0; }
@media (max-width: 760px) {
  .case-actions .el-button { min-height: 40px; padding: 8px 9px; }
  .case-actions .el-button:first-child { background: var(--el-color-primary-light-9); }
}
</style>
