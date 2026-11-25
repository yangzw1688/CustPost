<template>
  <section class="platform-overview">
    <h1>平台管理</h1>
    <div class="platform-links">
      <RouterLink v-for="entry in visibleEntries" :key="entry.route" :to="{ name: entry.route }" class="platform-link">
        <el-icon class="entry-icon" aria-hidden="true"><component :is="entry.icon" /></el-icon>
        <div>
          <h2>{{ entry.title }}</h2>
          <p>{{ entry.description }}</p>
        </div>
        <el-icon class="entry-arrow" aria-hidden="true"><ArrowRight /></el-icon>
      </RouterLink>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowRight, Document, Monitor, Picture, UserFilled } from '@element-plus/icons-vue'
import { canAccess } from '@/utils/permissions'

const entries = [
  { route: 'UserPages', permission: 'uiManagement', icon: Monitor, title: '用户端页面', description: '编辑页面模块，实时预览，保存草稿和发布。' },
  { route: 'UiImages', permission: 'uiManagement', icon: Picture, title: '首页轮播', description: '维护首页轮播的图片、说明和显示顺序。' },
  { route: 'PermissionList', permission: 'superAdmin', icon: UserFilled, title: '权限管理', description: '设置管理员可访问的模块和专项权限。' },
  { route: 'OperationLog', permission: 'platform', icon: Document, title: '平台操作日志', description: '按模块、操作人和执行结果查询记录。' }
]
const visibleEntries = computed(() => entries.filter(entry => canAccess(entry.permission)))
</script>

<style scoped>
.platform-overview h1 { margin: 0 0 20px; font-size: 23px; }
.platform-links { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(290px, 100%), 1fr)); gap: 16px; }
.platform-link {
  display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: 14px;
  min-height: 100px; padding: 18px; color: inherit; text-decoration: none; background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light); border-radius: 12px;
}
.platform-link:hover, .platform-link:focus-visible { border-color: var(--el-color-primary); }
.platform-link h2 { margin: 0 0 5px; font-size: 16px; }
.platform-link p { margin: 0; color: var(--el-text-color-regular); font-size: 13px; line-height: 1.6; }
.entry-icon { width: 40px; height: 40px; border-radius: 9px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.entry-arrow { color: var(--el-text-color-secondary); }
</style>
