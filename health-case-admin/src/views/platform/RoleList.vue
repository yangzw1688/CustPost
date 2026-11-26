<template>
  <section class="role-overview" aria-labelledby="role-title">
    <header class="role-heading">
      <div>
        <h1 id="role-title">角色管理</h1>
        <p>查看当前账号的授权，比较不同管理员角色的操作范围。</p>
      </div>
      <RouterLink v-if="canAccess('superAdmin')" :to="{ name: 'PermissionList' }" class="permission-link">
        权限管理 <el-icon aria-hidden="true"><ArrowRight /></el-icon>
      </RouterLink>
    </header>

    <section v-if="currentRole" class="role-summary" aria-labelledby="current-role-heading">
      <div class="role-identity">
        <el-icon class="role-icon" aria-hidden="true"><component :is="roles[currentRole].icon" /></el-icon>
        <div>
          <h2 id="current-role-heading">当前账号</h2>
          <strong>{{ roles[currentRole].title }}</strong>
        </div>
      </div>
      <dl class="role-grants">
        <div>
          <dt>已授权模块</dt>
          <dd>
            <el-tag v-for="module in grantedModules" :key="module.key" effect="plain" disable-transitions>
              {{ module.label }}
            </el-tag>
            <span v-if="!grantedModules.length">暂无模块授权</span>
          </dd>
        </div>
        <div>
          <dt>专项权限</dt>
          <dd>
            <el-tag v-for="grant in grantedExtras" :key="grant.key" effect="plain" disable-transitions>
              {{ grant.label }}
            </el-tag>
            <span v-if="!grantedExtras.length">暂无专项权限</span>
          </dd>
        </div>
      </dl>
    </section>

    <section class="role-comparison" aria-labelledby="role-comparison-heading">
      <h2 id="role-comparison-heading">角色权限对照</h2>
      <table aria-labelledby="role-comparison-heading">
        <thead>
          <tr>
            <th scope="col">权限项目</th>
            <th v-for="(role, key) in roles" :key="key" scope="col" :class="{ 'active-role': key === currentRole }">
              {{ role.title }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in permissionRows" :key="row.label">
            <th scope="row">{{ row.label }}</th>
            <td v-for="(_, key) in roles" :key="key" :class="{ 'active-role': key === currentRole }">{{ row[key] }}</td>
          </tr>
        </tbody>
      </table>
      <p class="role-note">主超级管理员账号由系统配置指定。</p>
    </section>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowRight, Key, Lock, UserFilled } from '@element-plus/icons-vue'
import { auth } from '@/utils/auth'
import { canAccess, moduleOptions } from '@/utils/permissions'

const roles = {
  primary: { title: '主超级管理员', icon: Key },
  super: { title: '超级管理员', icon: Lock },
  admin: { title: '管理员', icon: UserFilled }
}

const permissionRows = [
  { label: '业务模块', primary: '全部模块', super: '全部模块', admin: '按账号授权' },
  { label: '专项权限', primary: '全部专项权限', super: '全部专项权限', admin: '开通对应模块后另行授权' },
  { label: '新增管理员或超级管理员', primary: '可新增', super: '可新增', admin: '无权限' },
  { label: '编辑或撤销普通管理员', primary: '可操作', super: '可操作', admin: '无权限' },
  { label: '调整或撤销现有超级管理员', primary: '可操作', super: '无权限', admin: '无权限' }
]

const extraPermissions = [
  { key: 'policyTransfer', label: '保单导入与导出' },
  { key: 'uiManagement', label: '用户端页面管理' }
]

const currentRole = computed(() => {
  if (!auth.user) return null
  if (!canAccess('superAdmin')) return 'admin'
  return auth.user.primaryAdmin ? 'primary' : 'super'
})
const grantedModules = computed(() => moduleOptions.filter(module => canAccess(module.key)))
const grantedExtras = computed(() => extraPermissions.filter(grant => canAccess(grant.key)))
</script>

<style scoped>
.role-overview {
  display: grid;
  gap: 20px;
  max-width: 1320px;
  min-width: 0;
  margin-inline: auto;
}

.role-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  padding: clamp(20px, 3vw, 32px);
  border-radius: 14px;
  background: linear-gradient(110deg, #154c47, #18675d);
  color: #fff;
}

.role-heading h1 {
  margin: 0;
  font-size: clamp(24px, 2.5vw, 32px);
  line-height: 1.3;
}

.role-heading p {
  margin: 8px 0 0;
  color: #e1f0ee;
  font-size: 14px;
  line-height: 1.6;
}

.permission-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 14px;
  border-radius: 8px;
  background: #fff;
  color: var(--el-color-primary-dark-2);
  font-size: 14px;
  font-weight: 650;
  text-decoration: none;
}

.permission-link:hover {
  background: #e6f1ef;
}

.permission-link:focus-visible {
  outline: 2px solid #fff;
  outline-offset: 3px;
}

.role-summary,
.role-comparison {
  min-width: 0;
  border: 1px solid var(--management-border);
  border-radius: 12px;
  background: var(--el-bg-color);
}

.role-summary {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 20px 30px;
  padding: 20px;
}

.role-identity {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 178px;
}

.role-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: #e7f3ef;
  color: var(--el-color-primary-dark-2);
  font-size: 22px;
}

.role-identity h2,
.role-grants dt {
  margin: 0 0 6px;
  color: var(--management-muted);
  font-size: 13px;
  font-weight: 550;
}

.role-identity strong {
  color: var(--management-ink);
  font-size: 17px;
}

.role-grants {
  display: flex;
  flex: 1 1 290px;
  flex-wrap: wrap;
  gap: 14px 26px;
  min-width: 0;
  margin: 0;
}

.role-grants dd {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0;
  color: var(--management-muted);
  font-size: 13px;
}

.role-comparison {
  overflow: hidden;
}

.role-comparison h2 {
  margin: 0;
  padding: 18px 20px;
  color: var(--management-ink);
  font-size: 17px;
  font-weight: 650;
}

.role-comparison table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  color: var(--management-ink);
  font-size: 14px;
  line-height: 1.5;
}

.role-comparison th,
.role-comparison td {
  padding: 13px 15px;
  border-block-start: 1px solid var(--management-border);
  text-align: center;
  overflow-wrap: anywhere;
}

.role-comparison th:first-child {
  width: 31%;
  padding-inline-start: 20px;
  text-align: left;
}

.role-comparison thead th {
  background: #f2f6f8;
}

.role-comparison tbody th {
  font-weight: 550;
}

.role-comparison .active-role {
  background: #eef7f4;
  color: var(--el-color-primary-dark-2);
}

.role-comparison thead .active-role {
  background: #deefeb;
}

.role-note {
  margin: 0;
  padding: 13px 20px;
  border-top: 1px solid var(--management-border);
  color: var(--management-muted);
  font-size: 13px;
  line-height: 1.5;
}

@media (max-width: 760px) {
  .role-overview {
    gap: 14px;
  }

  .role-summary {
    gap: 14px;
    padding: 16px;
  }

  .role-comparison h2,
  .role-note {
    padding-inline: 12px;
  }

  .role-comparison table {
    font-size: 13px;
  }

  .role-comparison th,
  .role-comparison td {
    padding: 10px 4px;
  }

  .role-comparison thead th {
    text-wrap: balance;
  }

  .role-comparison th:first-child {
    width: 29%;
    padding-inline-start: 12px;
  }
}
</style>
