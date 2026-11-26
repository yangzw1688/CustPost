<template>
  <section class="platform-overview" aria-labelledby="platform-title">
    <header class="platform-header">
      <div>
        <h1 id="platform-title">平台列表</h1>
        <p>管理用户端内容，查看角色、权限与操作记录。</p>
      </div>
      <el-input
        ref="searchInput"
        v-model="keyword"
        class="platform-search"
        :prefix-icon="Search"
        aria-label="搜索平台功能"
        placeholder="搜索平台功能"
        size="large"
        clearable
      />
    </header>

    <div v-if="visibleGroups.length" class="platform-groups">
      <section
        v-for="group in visibleGroups"
        :key="group.id"
        class="platform-group"
        :aria-labelledby="group.id"
      >
        <h2 :id="group.id">{{ group.title }}</h2>
        <ul>
          <li v-for="entry in group.entries" :key="entry.route">
            <RouterLink :to="{ name: entry.route }" class="platform-link" :class="entry.tone">
              <el-icon class="entry-icon" aria-hidden="true"><component :is="entry.icon" /></el-icon>
              <div class="entry-copy">
                <h3>{{ entry.title }}</h3>
                <p>{{ entry.description }}</p>
              </div>
              <el-icon class="entry-arrow" aria-hidden="true"><ArrowRight /></el-icon>
            </RouterLink>
          </li>
        </ul>
      </section>
    </div>

    <div v-else class="platform-empty">
      <p role="status">{{ searchTerm ? '没有找到匹配的功能' : '当前账号没有可访问的平台功能' }}</p>
      <el-button v-if="searchTerm" @click="clearSearch">清空搜索</el-button>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ArrowRight, Document, Lock, Monitor, Picture, Search, UserFilled } from '@element-plus/icons-vue'
import { canAccess } from '@/utils/permissions'

const groups = [
  {
    id: 'platform-content',
    title: '用户端内容',
    entries: [
      {
        route: 'UserPages', permission: 'uiManagement', icon: Monitor, tone: 'teal',
        title: '用户端页面', description: '编辑页面模块、预览内容和发布更新。'
      },
      {
        route: 'UiImages', permission: 'uiManagement', icon: Picture, tone: 'blue',
        title: '首页轮播', description: '维护轮播图片、跳转地址和显示顺序。'
      }
    ]
  },
  {
    id: 'platform-administration',
    title: '系统管理',
    entries: [
      {
        route: 'RoleList', permission: 'platform', icon: UserFilled, tone: 'blue',
        title: '角色管理', description: '查看管理员角色及对应的授权范围。'
      },
      {
        route: 'PermissionList', permission: 'superAdmin', icon: Lock, tone: 'amber',
        title: '权限管理', description: '为管理员分配业务模块和专项权限。'
      },
      {
        route: 'OperationLog', permission: 'platform', icon: Document, tone: 'teal',
        title: '平台操作日志', description: '按操作人、业务模块和执行结果查询记录。'
      }
    ]
  }
]

const keyword = ref('')
const searchInput = ref(null)
const searchTerm = computed(() => keyword.value.trim().toLowerCase())
const visibleGroups = computed(() => groups.map(group => ({
  ...group,
  entries: group.entries.filter(entry => {
    const text = [group.title, entry.title, entry.description].join(' ').toLowerCase()
    return canAccess(entry.permission) && text.includes(searchTerm.value)
  })
})).filter(group => group.entries.length > 0))

function clearSearch() {
  keyword.value = ''
  searchInput.value?.focus()
}
</script>

<style scoped>
.platform-overview {
  display: grid;
  gap: 22px;
  width: 100%;
  max-width: 1320px;
  min-width: 0;
  margin-inline: auto;
}

.platform-header {
  position: relative;
  isolation: isolate;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  overflow: hidden;
  padding: clamp(20px, 3vw, 32px);
  background: linear-gradient(110deg, #154c47, #18675d);
  border-radius: 14px;
  color: #fff;
}

.platform-header::after {
  position: absolute;
  z-index: -1;
  top: -125px;
  right: 9%;
  width: 290px;
  height: 290px;
  border: 1px solid rgb(255 255 255 / 14%);
  border-radius: 50%;
  box-shadow: 0 0 0 26px rgb(255 255 255 / 5%), 0 0 0 52px rgb(255 255 255 / 4%);
  content: '';
  pointer-events: none;
}

.platform-header h1 {
  margin: 0;
  font-size: clamp(24px, 2.5vw, 32px);
  line-height: 1.3;
}

.platform-header p {
  margin: 8px 0 0;
  color: #e1f0ee;
  font-size: 14px;
  line-height: 1.6;
}

.platform-search {
  flex: 0 1 290px;
  --el-input-focus-border-color: #78cfba;
  --el-input-placeholder-color: #5c6f77;
}

.platform-groups {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(365px, 100%), 1fr));
  align-items: start;
  gap: 18px;
}

.platform-group,
.platform-empty {
  min-width: 0;
  background: var(--el-bg-color);
  border: 1px solid var(--management-border);
  border-radius: 12px;
}

.platform-group h2 {
  margin: 0;
  padding: 17px 19px;
  color: var(--management-ink);
  font-size: 16px;
  font-weight: 650;
  border-bottom: 1px solid var(--management-border);
}

.platform-group ul {
  margin: 0;
  padding: 5px 10px;
  list-style: none;
}

.platform-group li + li {
  border-top: 1px solid var(--el-border-color-lighter);
}

.platform-link {
  --entry-color: #216b63;
  --entry-background: #e7f3ef;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  min-height: 79px;
  padding: 13px 9px;
  border-radius: 8px;
  color: var(--management-ink);
  text-decoration: none;
}

.platform-link.blue {
  --entry-color: #285c9c;
  --entry-background: #eaf1fc;
}

.platform-link.amber {
  --entry-color: #875315;
  --entry-background: #fbf0da;
}

.platform-link:hover,
.platform-link:focus-visible {
  background: #f3f8f7;
}

.platform-link:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: -2px;
}

.entry-icon {
  width: 42px;
  height: 42px;
  border-radius: 9px;
  color: var(--entry-color);
  background: var(--entry-background);
  font-size: 20px;
}

.entry-copy h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 650;
  line-height: 1.4;
}

.entry-copy p {
  margin: 4px 0 0;
  color: var(--management-muted);
  font-size: 13px;
  line-height: 1.55;
}

.entry-arrow {
  color: var(--management-muted);
}

.platform-link:hover .entry-arrow,
.platform-link:focus-visible .entry-arrow {
  color: var(--el-color-primary-dark-2);
}

.platform-empty {
  padding: 32px 16px;
  text-align: center;
}

.platform-empty p {
  margin: 0;
  color: var(--management-muted);
}

.platform-empty .el-button {
  margin-top: 12px;
}

@media (max-width: 760px) {
  .platform-overview {
    gap: 15px;
  }

  .platform-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .platform-search {
    flex: none;
  }

  .platform-groups {
    gap: 12px;
  }

  .platform-link {
    gap: 10px;
  }
}
</style>
