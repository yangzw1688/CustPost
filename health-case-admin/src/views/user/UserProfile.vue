<template>
  <el-card shadow="never">
    <template #header>个人资料</template>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="登录手机号">{{ auth.user?.phone || '—' }}</el-descriptions-item>
      <el-descriptions-item label="账号类型">{{ auth.user?.superAdmin ? '超级管理员' : '管理员' }}</el-descriptions-item>
      <el-descriptions-item label="模块权限">{{ modules || '—' }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>
<script setup>
import { computed } from 'vue'
import { auth } from '@/utils/auth'
import { canAccess, moduleOptions } from '@/utils/permissions'
const modules = computed(() => moduleOptions.filter(module => canAccess(module.key)).map(module => module.label).join('、'))
</script>
