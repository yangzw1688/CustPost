<template>
  <el-dialog :model-value="open" :title="notice.title || '通知标题'" width="min(420px, 100%)"
    class="page-notice-dialog" modal-class="page-notice-overlay" align-center append-to-body
    :show-close="!preview" :close-on-click-modal="false"
    :close-on-press-escape="!preview" @update:model-value="value => { if (!value) $emit('dismiss') }">
    <p class="notice-message">{{ notice.message || '在通知正文中填写要展示的内容。' }}</p>
    <template #footer>
      <el-button class="notice-action" type="primary" :style="{ '--el-button-bg-color': color, '--el-button-border-color': color }"
        @click="$emit('dismiss')">我知道了</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
/* global defineProps, defineEmits */
defineProps({
  open: Boolean,
  notice: { type: Object, required: true },
  color: { type: String, required: true },
  preview: Boolean
})
defineEmits(['dismiss'])
</script>

<style>
.page-notice-overlay {
  --notice-gutter: 16px;
  --notice-top: max(var(--notice-gutter), env(safe-area-inset-top, 0px));
  --notice-bottom: max(var(--notice-gutter), env(safe-area-inset-bottom, 0px));
}
.page-notice-overlay .el-overlay-dialog {
  padding: var(--notice-top) max(var(--notice-gutter), env(safe-area-inset-right, 0px))
    var(--notice-bottom) max(var(--notice-gutter), env(safe-area-inset-left, 0px));
}
.page-notice-overlay .page-notice-dialog {
  --el-dialog-padding-primary: 20px;
  display: flex;
  flex-direction: column;
  max-width: 100%;
  max-height: calc(100vh - var(--notice-top) - var(--notice-bottom));
  max-height: calc(100dvh - var(--notice-top) - var(--notice-bottom));
  overflow: auto;
}
.page-notice-dialog .el-dialog__header,
.page-notice-dialog .el-dialog__footer { flex: none; padding-block: 0 12px; }
.page-notice-dialog .el-dialog__footer { padding-block: 12px 0; }
.page-notice-dialog .el-dialog__title { overflow-wrap: anywhere; line-height: 1.4; font-weight: 600; }
.page-notice-dialog .el-dialog__body { min-height: 1.75em; overflow: auto; }
.page-notice-dialog .notice-message { margin: 0; white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.75; }
.page-notice-dialog .notice-action { min-height: 44px; }
@media (max-width: 480px) {
  .page-notice-overlay .page-notice-dialog { --el-dialog-padding-primary: 16px; --el-dialog-content-font-size: 15px; }
  .page-notice-dialog .notice-action { width: 100%; }
}
@media (max-height: 420px) {
  .page-notice-overlay { --notice-gutter: 10px; }
  .page-notice-overlay .page-notice-dialog { --el-dialog-padding-primary: 12px; }
}
</style>
