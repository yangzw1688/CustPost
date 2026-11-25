<template>
  <section class="permission-page">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <h1>权限管理</h1>
          <div v-if="auth.user?.superAdmin">
            <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
            <el-button type="primary" :icon="Plus" @click="edit()">添加管理员</el-button>
          </div>
        </div>
      </template>
      <el-alert v-if="!auth.user?.superAdmin" title="请使用超级管理员账号和密码登录后管理权限" type="warning" show-icon :closable="false" />
      <template v-else>
        <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
        <el-table v-loading="loading" :data="rows" row-key="phone">
          <el-table-column prop="phone" label="登录手机号" min-width="135" />
          <el-table-column label="角色" min-width="125">
            <template #default="{ row }">
              <el-tag :type="row.superAdmin ? 'success' : 'info'">{{ roleLabel(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="已开启模块" min-width="240">
            <template #default="{ row }">
              <span v-if="row.superAdmin">全部模块</span>
              <span v-else>{{ enabledModules(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="权限设置" min-width="150">
            <template #default="{ row }">
              <el-button v-if="row.superAdmin && row.phone === auth.user?.phone" type="primary" link @click="openPasswordDialog">修改密码</el-button>
              <template v-if="row.phone && !row.primaryAdmin && (!row.superAdmin || auth.user?.primaryAdmin)">
                <el-button type="primary" link :disabled="saving" @click="edit(row)">编辑</el-button>
                <el-button type="danger" link :disabled="saving" @click="revoke(row.phone)">撤销</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-card>
    <el-dialog v-model="dialog" :title="editing ? '编辑管理员' : '添加管理员'" width="min(480px, 94vw)"
      :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" @closed="form.adminPassword = ''">
      <el-form :model="form" label-position="top" :disabled="saving" @submit.prevent="save">
        <el-form-item label="登录手机号" :error="phoneError" required>
          <el-input v-model="form.phone" type="tel" inputmode="numeric" maxlength="11" :disabled="editing"
            autocomplete="off" placeholder="请输入 11 位手机号" @input="phoneError = ''" />
        </el-form-item>
        <el-form-item v-if="!editing || auth.user?.primaryAdmin" label="账号角色">
          <el-radio-group v-model="form.superAdmin">
            <el-radio :value="false">管理员</el-radio>
            <el-radio :value="true">超级管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <p v-if="form.superAdmin" class="role-note">超级管理员拥有全部模块及专项权限。</p>
        <el-form-item v-else label="模块权限">
          <div class="module-switches">
            <div v-for="module in moduleOptions" :key="module.key" class="module-switch">
              <span>{{ module.label }}</span>
              <el-switch v-model="form.modules[module.key]" :aria-label="module.label" />
            </div>
          </div>
        </el-form-item>
        <el-form-item v-if="!form.superAdmin" label="专项权限">
          <div class="module-switches">
            <div class="module-switch">
              <span>保单导入与导出</span>
              <el-switch v-model="form.policyTransfer" :disabled="!form.modules.policy" aria-label="保单导入与导出" />
            </div>
            <div class="module-switch">
              <span>用户端页面管理</span>
              <el-switch v-model="form.uiManagement" :disabled="!form.modules.platform" aria-label="用户端页面管理" />
            </div>
          </div>
        </el-form-item>
        <el-form-item :label="editing ? '重设管理员密码' : '管理员密码'" :required="!editing" :error="credentialError">
          <el-input v-model="form.adminPassword" type="password" show-password autocomplete="new-password"
            maxlength="128" :placeholder="editing ? '留空保留现有密码' : '至少 12 个字符'" @input="credentialError = ''" />
        </el-form-item>
        <el-alert v-if="formError" :title="formError" type="error" show-icon :closable="false" />
        <div class="dialog-actions">
          <el-button :disabled="saving" @click="dialog = false">取消</el-button>
          <el-button type="primary" native-type="submit" :loading="saving" :disabled="saving">保存权限</el-button>
        </div>
      </el-form>
    </el-dialog>
    <el-dialog v-model="passwordDialog" title="修改超级管理员密码" width="min(480px, 94vw)"
      :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" @closed="resetPasswords">
      <el-form label-position="top" :disabled="saving" @submit.prevent="changePassword">
        <el-form-item label="当前密码">
          <el-input v-model="passwords.currentPassword" type="password" show-password autocomplete="current-password" maxlength="128" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwords.newPassword" type="password" show-password autocomplete="new-password"
            maxlength="128" :placeholder="`至少 ${minOwnPasswordLength} 个字符`" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="passwords.confirmPassword" type="password" autocomplete="new-password" maxlength="128" />
        </el-form-item>
        <el-alert v-if="passwordError" :title="passwordError" type="error" show-icon :closable="false" />
        <div class="dialog-actions">
          <el-button :disabled="saving" @click="passwordDialog = false">取消</el-button>
          <el-button type="primary" native-type="submit" :loading="saving" :disabled="!validPassword">保存并重新登录</el-button>
        </div>
      </el-form>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { auth, authClient, PHONE_PATTERN } from '@/utils/auth'
import { moduleOptions } from '@/utils/permissions'
import request, { errorMessage, getList } from '@/utils/request'

const path = '/api/admin/permissions'
const router = useRouter()
const route = useRoute()
const rows = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const formError = ref('')
const phoneError = ref('')
const credentialError = ref('')
const dialog = ref(false)
const editing = ref(false)
const passwordDialog = ref(false)
const passwordError = ref('')
const passwords = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const minOwnPasswordLength = computed(() => auth.user?.primaryAdmin ? 8 : 12)
const validPassword = computed(() => passwords.currentPassword.length > 0 && passwords.newPassword.length >= minOwnPasswordLength.value
  && passwords.newPassword === passwords.confirmPassword)
const form = reactive({ phone: '', modules: {}, policyTransfer: false, uiManagement: false, adminPassword: '', superAdmin: false })
let pending

async function load() {
  if (!auth.user?.superAdmin) return
  pending?.abort()
  pending = new AbortController()
  const { signal } = pending
  loading.value = true
  error.value = ''
  try {
    const result = await getList(path, { signal })
    if (!signal.aborted) rows.value = result
  } catch (failure) {
    if (!signal.aborted) error.value = errorMessage(failure)
  } finally {
    if (!signal.aborted) loading.value = false
  }
}

function edit(row) {
  editing.value = Boolean(row)
  Object.assign(form, {
    phone: row?.phone || '',
    modules: Object.fromEntries(moduleOptions.map(module => [module.key, row?.modules?.[module.key] === true])),
    policyTransfer: row?.policyTransfer ?? false,
    uiManagement: row?.uiManagement ?? false,
    adminPassword: '',
    superAdmin: row?.superAdmin === true
  })
  formError.value = ''
  phoneError.value = ''
  credentialError.value = ''
  dialog.value = true
}

function enabledModules(row) {
  return moduleOptions.filter(module => row.modules?.[module.key]).map(module => module.label).join('、') || '暂无授权'
}

function roleLabel(row) {
  if (row.primaryAdmin) return '主超级管理员'
  return row.superAdmin ? '超级管理员' : '管理员'
}

async function save() {
  if (saving.value) return
  formError.value = ''
  const phone = form.phone.trim()
  phoneError.value = PHONE_PATTERN.test(phone) ? '' : '请输入正确的 11 位手机号'
  const password = form.adminPassword
  credentialError.value = (password || !editing.value) && (password.length < 12 || !password.trim())
    ? '请输入至少 12 个字符的管理员密码' : ''
  if (phoneError.value || credentialError.value) return
  saving.value = true
  try {
    await request.put(path + '/' + phone, {
      modules: form.modules,
      policyTransfer: form.policyTransfer,
      uiManagement: form.uiManagement,
      adminPassword: password,
      superAdmin: form.superAdmin,
      createOnly: !editing.value
    })
    dialog.value = false
    ElMessage.success('权限已保存')
    await load()
  } catch (failure) {
    formError.value = errorMessage(failure)
  } finally {
    saving.value = false
  }
}

async function revoke(phone) {
  if (!PHONE_PATTERN.test(phone) || saving.value) return
  try {
    await ElMessageBox.confirm(`撤销 ${phone} 的全部管理权限？`, '撤销权限', {
      type: 'warning',
      confirmButtonText: '撤销',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  saving.value = true
  try {
    await request.delete(path + '/' + phone)
    ElMessage.success('权限已撤销')
    await load()
  } catch (failure) {
    error.value = errorMessage(failure)
  } finally {
    saving.value = false
  }
}

function resetPasswords() {
  Object.assign(passwords, { currentPassword: '', newPassword: '', confirmPassword: '' })
  passwordError.value = ''
}

function openPasswordDialog() {
  resetPasswords()
  passwordDialog.value = true
}

async function changePassword() {
  if (!validPassword.value || saving.value) return
  saving.value = true
  passwordError.value = ''
  try {
    await request.put('/api/admin/auth/password', {
      currentPassword: passwords.currentPassword,
      newPassword: passwords.newPassword
    })
    resetPasswords()
    passwordDialog.value = false
    authClient.clearAuth()
    ElMessage.success('密码已修改，请重新登录')
    await router.replace({ name: 'Login', query: { redirect: route.fullPath } })
  } catch (failure) {
    passwordError.value = errorMessage(failure)
  } finally {
    saving.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => pending?.abort())
</script>

<style scoped>
.permission-page {
  min-width: 0;
}

.toolbar, .dialog-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

h1 {
  margin: 0;
  font-size: 20px;
}

.dialog-actions {
  justify-content: flex-end;
  margin-top: 16px;
}

.module-switches {
  width: 100%;
  display: grid;
  gap: 8px 14px;
  grid-template-columns: repeat(auto-fit, minmax(175px, 1fr));
}

.module-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.role-note {
  margin: 0 0 16px;
  color: var(--el-text-color-secondary);
}
</style>
