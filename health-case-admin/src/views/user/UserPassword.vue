<template>
  <el-card shadow="never" style="max-width: 480px">
    <template #header>修改密码</template>
    <el-alert v-if="message" :title="message" :type="ok ? 'success' : 'error'" :closable="false" />
    <el-form v-if="auth.user?.superAdmin" label-position="top" @submit.prevent="save">
      <el-form-item label="当前密码"><el-input v-model="currentPassword" type="password" autocomplete="current-password" show-password /></el-form-item>
      <el-form-item label="新密码"><el-input v-model="newPassword" type="password" autocomplete="new-password" show-password /></el-form-item>
      <el-button type="primary" :loading="saving" native-type="submit">保存</el-button>
    </el-form>
    <p v-else>密码由超级管理员管理。</p>
  </el-card>
</template>
<script setup>
import { ref } from 'vue'
import { auth } from '@/utils/auth'
import request, { errorMessage } from '@/utils/request'
const currentPassword = ref('')
const newPassword = ref('')
const saving = ref(false)
const message = ref('')
const ok = ref(false)
async function save() {
  if (saving.value) return
  ok.value = false
  if (!currentPassword.value || !newPassword.value) { message.value = '请填写当前密码和新密码'; return }
  saving.value = true
  try {
    await request.put('/api/admin/auth/password', { currentPassword: currentPassword.value, newPassword: newPassword.value })
    currentPassword.value = ''
    newPassword.value = ''
    ok.value = true
    message.value = '密码已更新'
  } catch (failure) { message.value = errorMessage(failure) }
  finally { saving.value = false }
}
</script>
