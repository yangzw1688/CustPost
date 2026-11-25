<template>
  <div class="admin-layout">

    <button v-if="isCompact && mobileMenuOpen" class="mobile-menu-scrim" type="button"
        aria-label="关闭导航菜单" tabindex="-1" @click="closeMobileMenu" />
    <!-- ================= 左侧菜单 ================= -->
    <aside
        id="admin-navigation"
        class="sidebar"
        aria-label="管理端导航"
        :class="{ 'sidebar-collapsed': !isCompact && isCollapse, 'sidebar-open': isCompact && mobileMenuOpen }"
        :inert="isCompact && !mobileMenuOpen"
    >

      <!-- Logo -->
      <div class="logo-wrapper">
        <div class="logo-icon">
          <span>创</span>
        </div>

        <transition name="fade">
          <span v-if="isCompact || !isCollapse" class="logo-title">
            创新支付健康管理平台
          </span>
        </transition>
        <button v-if="isCompact" ref="mobileCloseButton" class="mobile-close-button" type="button"
            aria-label="收起导航菜单" @click="closeMobileMenu"><el-icon :size="19"><Close /></el-icon></button>
      </div>

      <!-- 菜单 -->
      <el-menu
          ref="navigationMenu"
          :default-active="activeMenu"
          :default-openeds="activeMenuGroups"
          :unique-opened="true"
          @open="handleMenuOpen"
          router
          class="side-menu"
      >

        <!-- 数据统计 -->
        <el-menu-item v-if="canAccess('dashboard')" index="/dashboard">
          <el-icon>
            <DataAnalysis />
          </el-icon>
          <template #title>
            <span>数据统计</span>
          </template>
        </el-menu-item>


        <!-- 案件管理 -->
        <el-sub-menu v-if="canAccess('cases')" index="case">
          <template #title>
            <el-icon>
              <Document />
            </el-icon>
            <span>案件管理</span>
          </template>

          <el-menu-item index="/case/list">
            案件列表
          </el-menu-item>

          <el-menu-item index="/case/query">
            综合查询
          </el-menu-item>

          <el-menu-item index="/case/maintain">
            案件维护
          </el-menu-item>

          <el-menu-item index="/case/payee-review">
            领款人审核
          </el-menu-item>

          <el-menu-item index="/case/claim-push">
            赔案推送
          </el-menu-item>

          <el-menu-item index="/case/claim-image">
            理赔影像
          </el-menu-item>

          <el-menu-item index="/case/caring-claim">
            理赔
          </el-menu-item>

          <!-- 案件详情保留在路由中，但不显示在左侧导航 -->
        </el-sub-menu>



        <!-- 保单管理 -->
        <el-sub-menu v-if="canAccess('policy')" index="policy">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>保单管理</span>
          </template>

          <el-menu-item index="/policy/list">
            保单列表
          </el-menu-item>

          <el-menu-item v-if="canAccess('policyTransfer')" index="/policy/transfer">
            保单导入与导出
          </el-menu-item>

          <el-menu-item index="/policy/operation-log">
            保单操作记录
          </el-menu-item>
        </el-sub-menu>


        <!-- 项目管理 -->
        <el-sub-menu v-if="canAccess('project')" index="project">
          <template #title>
            <el-icon>
              <Folder />
            </el-icon>
            <span>项目管理</span>
          </template>

          <el-menu-item index="/project/list">
            项目列表
          </el-menu-item>

          <el-menu-item index="/project/insurers">
            保险公司管理
          </el-menu-item>
        </el-sub-menu>

        <!-- 平台管理 -->
        <el-sub-menu v-if="canAccess('platform')" index="platform">
          <template #title>
            <el-icon>
              <Setting />
            </el-icon>
            <span>平台管理</span>
          </template>

          <el-menu-item index="/platform/list">
            平台列表
          </el-menu-item>

          <el-menu-item index="/platform/role">
            角色管理
          </el-menu-item>

          <el-menu-item v-if="canAccess('superAdmin')" index="/platform/permission">
            权限管理
          </el-menu-item>
          <el-menu-item v-if="canAccess('uiManagement')" index="/platform/ui">用户端页面</el-menu-item>
          <el-menu-item v-if="canAccess('uiManagement')" index="/platform/ui-images">素材管理</el-menu-item>
          <el-menu-item index="/platform/operation-log">平台操作日志</el-menu-item>
        </el-sub-menu>

        <!-- 用户中心 -->
        <el-sub-menu v-if="canAccess('users')" index="user">
          <template #title>
            <el-icon>
              <User />
            </el-icon>
            <span>用户中心</span>
          </template>

          <el-menu-item index="/user/list">
            用户列表
          </el-menu-item>

        </el-sub-menu>

      </el-menu>

      <!-- 底部版本信息 -->
      <div v-if="!isCollapse" class="sidebar-footer">
        健康服务管理平台
        <span>v1.0.0</span>
      </div>

    </aside>


    <!-- ================= 右侧主体 ================= -->
    <section class="main-container">

      <!-- 顶部 Header -->
      <header class="header">

        <!-- 左侧 -->
        <div class="header-left">

          <!-- 折叠菜单 -->
          <button
              type="button"
              ref="navigationToggle"
              class="collapse-button"
              aria-controls="admin-navigation"
              :aria-label="navigationExpanded ? '收起导航菜单' : '展开导航菜单'"
              :aria-expanded="navigationExpanded"
              @click="toggleCollapse"
          >
            <el-icon :size="20">
              <Expand v-if="!navigationExpanded" />
              <Fold v-else />
            </el-icon>
          </button>

          <!-- 面包屑 -->
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>
              管理后台
            </el-breadcrumb-item>

            <el-breadcrumb-item>
              {{ currentTitle }}
            </el-breadcrumb-item>
          </el-breadcrumb>

        </div>


        <!-- 右侧 -->
        <div class="header-right">

          <!-- 通知 -->
          <button type="button" class="header-action" aria-label="通知" @click="$message.info('暂无可用的通知服务')">
            <el-badge :value="0" :hidden="true">
              <el-icon :size="20">
                <Bell />
              </el-icon>
            </el-badge>
          </button>

          <!-- 用户 -->
          <el-dropdown
              trigger="click"
              @command="handleCommand"
          >

            <div class="user-info">

              <el-avatar :size="34">
                管
              </el-avatar>

              <div class="user-detail">
                <span class="username">
                  管理员
                </span>

                <span class="user-role">
                  系统管理员
                </span>
              </div>

              <el-icon class="arrow">
                <ArrowDown />
              </el-icon>

            </div>

            <template #dropdown>

              <el-dropdown-menu>

                <el-dropdown-item v-if="canAccess('users')" command="profile">
                  <el-icon>
                    <User />
                  </el-icon>
                  个人信息
                </el-dropdown-item>

                <el-dropdown-item v-if="canAccess('users')" command="password">
                  <el-icon>
                    <Lock />
                  </el-icon>
                  修改密码
                </el-dropdown-item>

                <el-dropdown-item divided command="logout">
                  <el-icon>
                    <SwitchButton />
                  </el-icon>
                  退出登录
                </el-dropdown-item>

              </el-dropdown-menu>

            </template>

          </el-dropdown>

        </div>

      </header>


      <!-- ================= 页面内容 ================= -->
      <main class="content">

        <router-view />

      </main>

    </section>

  </div>
</template>


<script>
import { authClient, logout } from '@/utils/auth'
import { canAccess } from '@/utils/permissions'
import {
  DataAnalysis,
  Document,
  Folder,
  Setting,
  User,
  Expand,
  Fold,
  Bell,
  ArrowDown,
  Lock,
  Close,
  SwitchButton
} from '@element-plus/icons-vue'

export default {
  name: 'AdminLayout',

  components: {
    DataAnalysis,
    Document,
    Folder,
    Setting,
    User,
    Expand,
    Fold,
    Bell,
    ArrowDown,
    Lock,
    Close,
    SwitchButton
  },

  data() {
    return {
      isCollapse: false,
      isCompact: false,
      mobileMenuOpen: false,
      viewportQuery: null
    }
  },

  mounted() {
    this.viewportQuery = window.matchMedia('(max-width: 768px)')
    this.updateViewport()
    this.viewportQuery.addEventListener('change', this.updateViewport)
    window.addEventListener('keydown', this.handleNavigationKey)
    window.addEventListener('focus', this.refreshPermissions)
  },

  beforeUnmount() {
    this.viewportQuery?.removeEventListener('change', this.updateViewport)
    window.removeEventListener('keydown', this.handleNavigationKey)
    window.removeEventListener('focus', this.refreshPermissions)
  },

  watch: {
    '$route.path'() {
      if (this.mobileMenuOpen) this.closeMobileMenu()
      this.$nextTick(() => {
        const group = this.activeMenuGroups[0]
        if (group) this.$refs.navigationMenu?.open(group)
      })
    },
    isCollapse(value) {
      if (value) this.$nextTick(() => {
        for (const group of ['case', 'policy', 'project', 'platform', 'user']) this.$refs.navigationMenu?.close(group)
      })
    }
  },

  computed: {
    navigationExpanded() {
      return this.isCompact ? this.mobileMenuOpen : !this.isCollapse
    },
    // 始终使用纵向内联菜单；只收窄侧栏，不启用会弹出子菜单的 collapse 模式。
    activeMenuGroups() {
      const group = this.$route.path.split('/')[1]
      return ['case', 'policy', 'project', 'platform', 'user'].includes(group) ? [group] : []
    },

    /**
     * 当前菜单
     */
    activeMenu() {
      const path = this.$route.path
      return path.startsWith('/case/detail/') ? '/case/list'
          : path.startsWith('/policy/detail/') ? '/policy/list'
          : path.startsWith('/user/detail/') || path === '/user/detail' ? '/user/list'
          : path.startsWith('/project/insurer/') || path === '/project/insurer' ? '/project/insurers'
          : path.startsWith('/project/config') ? '/project/list' : path
    },

    /**
     * 当前页面标题
     */
    currentTitle() {
      const titleMap = {
        // 数据统计
        '/dashboard': '数据统计',

        // 案件管理
        '/case/list': '案件列表',
        '/case/query': '综合查询',
        '/case/maintain': '案件维护',
        '/case/payee-review': '领款人审核',
        '/case/claim-push': '赔案推送',
        '/case/claim-image': '理赔影像',
        '/case/caring-claim': '理赔',

        // 保单管理
        '/policy/list': '保单管理',
        '/policy/transfer': '保单导入与导出',
        '/policy/operation-log': '保单操作记录',

        // 项目管理
        '/project/list': '项目列表',
        '/project/config': '项目配置',

        // 平台管理
        '/platform/list': '平台列表',
        '/platform/role': '角色管理',
        '/platform/permission': '权限管理',
        '/platform/ui': '用户端页面',
        '/platform/ui-images': '素材管理',
        '/platform/operation-log': '平台操作日志',

        // 用户中心
        '/user/list': '用户列表',
        '/user/detail': '用户详情',
        '/user/profile': '个人信息',
        '/user/password': '修改密码'
      }

      // 处理详情页这种带 :id 的动态路由
      const path = this.$route.path

      if (path.startsWith('/case/detail/')) {
        return '案件详情'
      }

      if (path.startsWith('/policy/detail/')) {
        return '保单详情'
      }

      if (path.startsWith('/user/detail/')) return '用户详情'
      if (path.startsWith('/project/insurer')) return path === '/project/insurers' ? '保险公司管理' : '保险公司详情'
      if (path.startsWith('/project/config')) return '项目资料'

      if (path.startsWith('/user/detail/')) return '用户详情'

      return titleMap[path] || '数据统计'
    }
  },

  methods: {
    canAccess,
    updateViewport() {
      this.isCompact = this.viewportQuery.matches
      this.mobileMenuOpen = false
    },
    closeMobileMenu() {
      this.mobileMenuOpen = false
      this.$nextTick(() => this.$refs.navigationToggle?.focus())
    },
    handleNavigationKey(event) {
      if (event.key === 'Escape' && this.mobileMenuOpen) this.closeMobileMenu()
    },
    async refreshPermissions() {
      try {
        const loggedIn = await authClient.currentUser()
        const route = this.$route
        if (!loggedIn) this.$router.replace({ name: 'Login', query: { redirect: route.fullPath } })
        else if (!canAccess(route.meta.permission)) this.$router.replace({ name: 'NoAccess' })
      } catch {
        // 保留当前页面，下一次导航或请求时继续由服务端校验。
      }
    },
    handleMenuOpen() {
      if (!this.isCompact) this.isCollapse = false
    },

    /**
     * 折叠菜单
     */
    toggleCollapse() {
      if (!this.isCompact) {
        this.isCollapse = !this.isCollapse
        return
      }
      if (this.mobileMenuOpen) this.closeMobileMenu()
      else {
        this.mobileMenuOpen = true
        this.$nextTick(() => this.$refs.mobileCloseButton?.focus())
      }
    },

    /**
     * 用户菜单
     */
    handleCommand(command) {

      switch (command) {

        case 'profile':

          this.$router.push('/user/profile')
          break

        case 'password':

          this.$router.push('/user/password')
          break

        case 'logout':

          this.handleLogout()
          break

      }
    },

    /**
     * 退出登录
     */
    handleLogout() {

      this.$confirm(
          '确定要退出当前账号吗？',
          '退出登录',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
      )
          .then(async () => {

            await logout()

            this.$router.replace('/login')

          })
          .catch(error => {
            if (error !== 'cancel' && error !== 'close') this.$message.error(error.message || '退出失败，请重试')
          })
    }

  }
}
</script>


<style scoped lang="scss">

/* ==============================
   整体布局
============================== */

.admin-layout {
  width: 100%;
  height: 100vh;
  display: flex;
  overflow: hidden;
  background: #f2f6f8;
}


/* ==============================
   左侧菜单
============================== */

.sidebar {
  width: 230px;
  height: 100vh;
  flex-shrink: 0;

  display: flex;
  flex-direction: column;

  background: #163440;
  color: #fff;

  transition: width 0.3s;

  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.08);
}

.sidebar-collapsed {
  width: 64px;
}

.sidebar-collapsed :deep(.el-sub-menu__title > span),
.sidebar-collapsed :deep(.el-menu-item > span),
.sidebar-collapsed :deep(.el-sub-menu__icon-arrow),
.sidebar-collapsed :deep(.el-sub-menu > .el-menu) { display: none; }


/* Logo */

.logo-wrapper {
  height: 64px;

  display: flex;
  align-items: center;

  padding: 0 18px;

  box-sizing: border-box;

  border-bottom: 1px solid rgba(255, 255, 255, 0.06);

  overflow: hidden;
}

.logo-icon {
  width: 34px;
  height: 34px;

  flex-shrink: 0;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: 8px;

  background: #16837c;

  font-size: 17px;
  font-weight: bold;
}

.logo-title {
  margin-left: 12px;

  white-space: nowrap;

  font-size: 16px;
  font-weight: 600;
}


/* 菜单 */

.side-menu {
  flex: 1;

  border-right: none;

  background: #163440;
}


/* Element Plus 菜单样式 */

:deep(.el-menu) {
  background: #163440;
  border-right: none;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 52px;
  line-height: 52px;

  color: #b8c1d1;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background: #244955;
  color: #fff;
}

:deep(.el-menu-item.is-active) {
  background: #16837c;
  color: #fff;
}

:deep(.el-sub-menu .el-menu-item) {
  min-width: 230px;
  background: #122e39;
}

:deep(.el-sub-menu .el-menu-item:hover) {
  background: #244955;
}


/* 底部 */

.sidebar-footer {
  padding: 15px;

  text-align: center;

  font-size: 11px;

  color: #697586;

  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.sidebar-footer span {
  display: block;

  margin-top: 4px;
}


/* ==============================
   右侧主体
============================== */

.main-container {
  flex: 1;

  min-width: 0;

  display: flex;
  flex-direction: column;
}


/* ==============================
   Header
============================== */

.header {
  height: 64px;
  flex-shrink: 0;

  display: flex;
  align-items: center;
  justify-content: space-between;

  padding: 0 24px;

  background: #fff;

  border-bottom: 1px solid #ebeef5;

  box-sizing: border-box;
}


/* Header 左侧 */

.header-left {
  display: flex;
  align-items: center;

  gap: 20px;
}

.collapse-button {
  width: 32px;
  height: 32px;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: 6px;

  cursor: pointer;

  color: #606266;
}

.collapse-button:hover {
  background: #f2f3f5;
}


/* Header 右侧 */

.header-right {
  display: flex;
  align-items: center;

  gap: 22px;
}

.header-action {
  display: flex;
  align-items: center;

  cursor: pointer;

  color: #606266;
}


/* 用户 */

.user-info {
  display: flex;
  align-items: center;

  cursor: pointer;

  user-select: none;
}

.user-detail {
  margin-left: 10px;

  display: flex;
  flex-direction: column;
}

.username {
  font-size: 14px;
  color: #303133;
}

.user-role {
  margin-top: 3px;

  font-size: 11px;
  color: #909399;
}

.arrow {
  margin-left: 8px;

  color: #909399;
}


/* ==============================
   内容区域
============================== */

.content {
  flex: 1;

  overflow-y: auto;

  padding: 20px;

  box-sizing: border-box;

  background: #f2f6f8;
}


/* ==============================
   动画
============================== */

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}


/* ==============================
   响应式
============================== */

@media screen and (max-width: 768px) {

  .admin-layout { height: 100dvh; }
  .sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 1000;
    width: min(286px, calc(100vw - 40px));
    height: 100dvh;
    visibility: hidden;
    pointer-events: none;
    transform: translateX(-100%);
    transition: transform .2s ease, visibility .2s;
  }
  .sidebar.sidebar-open {
    visibility: visible;
    pointer-events: auto;
    transform: translateX(0);
  }
  .mobile-menu-scrim {
    position: fixed;
    inset: 0;
    z-index: 999;
    display: block;
    width: 100%;
    border: 0;
    background: #132e4580;
  }
  .mobile-close-button {
    display: inline-grid;
    flex: 0 0 32px;
    place-items: center;
    width: 32px;
    height: 32px;
    margin-left: auto;
    padding: 0;
    border: 0;
    border-radius: 5px;
    background: transparent;
    color: #fff;
    cursor: pointer;
  }
  .mobile-close-button:focus-visible { outline: 2px solid #83d5c4; }
  .logo-title { min-width: 0; overflow: hidden; text-overflow: ellipsis; }
  .header-left { min-width: 0; gap: 9px; }
  .header-left :deep(.el-breadcrumb) { display: flex; align-items: center; }
  .header-left :deep(.el-breadcrumb__inner) { white-space: nowrap; }

  .sidebar-footer {
    display: none;
  }

  .user-detail {
    display: none;
  }

  .header {
    padding: 0 12px;
  }

  .content {
    padding: 12px;
  }

}


.side-menu { overflow-y: auto; overflow-x: hidden; scrollbar-width: thin; scrollbar-color: #47626d transparent; }
.logo-title { font-size: 13px; letter-spacing: .3px; }
.logo-icon { border-radius: 10px; box-shadow: 0 4px 10px #0001; }
.collapse-button, .header-action { border: 0; padding: 0; font: inherit; background: transparent; }
.collapse-button:focus-visible, .header-action:focus-visible { outline: 3px solid #68bab1; outline-offset: 4px; }
.side-menu :deep(.el-menu-item), .side-menu :deep(.el-sub-menu__title) { font-size: 13px; }
.side-menu :deep(.el-menu-item.is-active) { box-shadow: inset 3px 0 #83d5c4; }
.header { box-shadow: 0 1px 4px #28445103; }
.user-info :deep(.el-avatar) { background: #e5f2ef; color: #2b776e; }
</style>
