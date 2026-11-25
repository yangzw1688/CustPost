<template>
  <section class="role-overview">
    <header class="role-heading">
      <div><h1>角色管理</h1><p>查看管理员角色和对应的授权范围。</p></div>
      <el-button v-if="canAccess('superAdmin')" type="primary" @click="$router.push({ name: 'PermissionList' })">前往权限管理</el-button>
    </header>
    <div class="roles">
      <el-card v-for="role in roles" :key="role.title" shadow="never">
        <h2>{{ role.title }}</h2>
        <p>{{ role.scope }}</p>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { canAccess } from '@/utils/permissions'

const roles = [
  { title: '主超级管理员', scope: '拥有全部管理模块和专项权限，可管理超级管理员和普通管理员的授权。' },
  { title: '超级管理员', scope: '拥有全部管理模块和专项权限，可管理普通管理员的授权。' },
  { title: '管理员', scope: '按账号已获授权访问相应模块。保单导入与导出、用户端页面管理需要单独授权。' }
]
</script>

<style scoped>
.role-heading { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; margin-bottom: 20px; }
.role-heading h1 { margin: 0 0 8px; font-size: 23px; }
.roles { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(245px, 100%), 1fr)); gap: 16px; }
.roles h2 { margin: 0 0 8px; font-size: 17px; }
.role-overview p { margin: 0; font-size: 13px; color: var(--el-text-color-regular); line-height: 1.7; }
</style>
