import { createRouter, createWebHistory } from 'vue-router'
import { canAccess } from '@/utils/permissions'
import { authClient } from '@/utils/auth'
import { onUnauthorized } from '@/utils/request'
import { installAuthGuard } from '@/shared/auth/auth-client.mjs'

import AdminLayout from '@/layout/AdminLayout.vue'

// ============================
// 数据统计
// ============================
import Dashboard from '@/views/dashboard/Dashboard.vue'

// ============================
// 案件管理
// ============================
import CaseList from '@/views/case/CaseList.vue'
import CaseDetail from '@/views/case/CaseDetail.vue'

// ============================
// 保单管理
// ============================
import PolicyList from '@/views/policy/PolicyList.vue'
import PolicyDetail from '@/views/policy/PolicyDetail.vue'

// ============================
// 项目管理
// ============================
import ProjectList from '@/views/project/ProjectList.vue'
import ProjectConfig from '@/views/project/ProjectConfig.vue'

// ============================
// 平台管理
// ============================
import PlatformList from '@/views/platform/PlatformList.vue'
import RoleList from '@/views/platform/RoleList.vue'
import PermissionList from '@/views/platform/PermissionList.vue'

// ============================
// 用户中心
// ============================
import UserList from '@/views/user/UserList.vue'
import UserDetail from '@/views/user/UserDetail.vue'


const routes = [

  // ============================
  // 登录
  // ============================
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: {
      title: '登录'
    }
  },

  {
    path: '/page-preview',
    name: 'MobilePagePreview',
    component: () => import('@/views/ui/preview/PagePreview.vue'),
    meta: { public: true }
  },

  // ============================
  // 管理后台
  // ============================
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',

    children: [

      // ============================
      // 数据统计
      // ============================
      {
        path: 'no-access',
        name: 'NoAccess',
        component: () => import('@/views/platform/NoModuleAccess.vue'),
        meta: { title: '访问权限' }
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: {
          title: '数据统计',
          permission: 'dashboard'
        }
      },


      // ============================
      // 案件管理
      // ============================
      {
        path: 'case',
        meta: { permission: 'cases' },
        redirect: '/case/list',

        children: [

          {
            path: 'list',
            name: 'CaseList',
            component: CaseList,
            meta: {
              title: '案件列表'
            }
          },

          {
            path: 'query',
            name: 'CaseQuery',
            component: () => import('@/views/case/CaseQuery.vue'),
            meta: {
              title: '综合查询'
            }
          },

          {
            path: 'maintain',
            name: 'CaseMaintain',
            component: CaseList,
            meta: {
              title: '案件维护',
              editProgress: true
            }
          },

          {
            path: 'payee-review',
            name: 'PayeeReview',
            component: CaseList,
            meta: {
              title: '领款人审核',
              reviewCases: true
            }
          },

          {
            path: 'claim-push',
            name: 'ClaimPush',
            component: () => import('@/views/case/ClaimPush.vue'),
            meta: {
              title: '赔案推送'
            }
          },

          {
            path: 'claim-image',
            name: 'ClaimImage',
            component: () => import('@/views/case/ClaimImage.vue'),
            meta: {
              title: '理赔影像'
            }
          },

          {
            path: 'caring-claim',
            name: 'CaringClaim',
            component: () => import('@/views/case/CaringClaim.vue'),
            meta: {
              title: '理赔'
            }
          },

          // 案件详情，不显示左侧菜单
          {
            path: 'detail/:id',
            name: 'CaseDetail',
            component: CaseDetail,
            meta: {
              title: '案件详情',
              hidden: true
            }
          }
        ]
      },


      // ============================
      // 保单管理
      // ============================
      {
        path: 'policy',
        meta: { permission: 'policy' },
        redirect: '/policy/list',

        children: [

          {
            path: 'list',
            name: 'PolicyList',
            component: PolicyList,
            meta: {
              title: '保单管理'
            }
          },

          {
            path: 'query',
            name: 'PolicyQuery',
            redirect: { name: 'PolicyList' }
          },

          {
            path: 'maintain',
            name: 'PolicyMaintain',
            redirect: { name: 'PolicyList' }
          },

          {
            path: 'audit',
            name: 'PolicyAudit',
            redirect: { name: 'PolicyList' }
          },

          {
            path: 'transfer',
            name: 'PolicyTransfer',
            component: () => import('@/views/policy/PolicyTransfer.vue'),
            meta: { title: '保单导入与导出', permission: 'policyTransfer' }
          },

          {
            path: 'operation-log',
            name: 'PolicyOperationLog',
            component: () => import('@/views/policy/PolicyOperationLog.vue'),
            meta: {
              title: '保单操作记录'
            }
          },

          // 保单详情，不显示左侧菜单
          {
            path: 'detail/:id',
            name: 'PolicyDetail',
            component: PolicyDetail,
            meta: {
              title: '保单详情',
              hidden: true
            }
          }
        ]
      },


      // ============================
      // 项目管理
      // ============================
      {
        path: 'project',
        meta: { permission: 'project' },
        redirect: '/project/list',

        children: [
          { path: 'insurers', name: 'InsuranceCompanyList', component: () => import('@/views/project/InsuranceCompanyList.vue'), meta: { title: '保险公司管理' } },
          { path: 'insurer/:id?', name: 'InsuranceCompanyDetail', component: () => import('@/views/project/InsuranceCompanyDetail.vue'), meta: { title: '保险公司详情', hidden: true } },

          {
            path: 'list',
            name: 'ProjectList',
            component: ProjectList,
            meta: {
              title: '项目列表'
            }
          },

          {
            path: 'config/:id?',
            name: 'ProjectConfig',
            component: ProjectConfig,
            meta: {
              title: '项目配置'
            }
          }
        ]
      },


      // ============================
      // 平台管理
      // ============================
      {
        path: 'platform',
        meta: { permission: 'platform' },
        redirect: '/platform/list',

        children: [
          {
            path: 'ui',
            name: 'UserPages',
            component: () => import('@/views/ui/UserPages.vue'),
            meta: { title: '用户端页面', permission: 'uiManagement' }
          },
          {
            path: 'ui-images',
            name: 'UiImages',
            component: () => import('@/views/ui/UiImages.vue'),
            meta: { title: '界面图片', permission: 'uiManagement' }
          },

          {
            path: 'operation-log',
            name: 'OperationLog',
            component: () => import('@/views/platform/OperationLog.vue'),
            meta: { title: '平台操作日志' }
          },
          {
            path: 'list',
            name: 'PlatformList',
            component: PlatformList,
            meta: {
              title: '平台列表'
            }
          },

          {
            path: 'role',
            name: 'RoleList',
            component: RoleList,
            meta: {
              title: '角色管理'
            }
          },

          {
            path: 'permission',
            name: 'PermissionList',
            component: PermissionList,
            meta: {
              title: '权限管理',
              permission: 'superAdmin'
            }
          }
        ]
      },


      // ============================
      // 用户中心
      // ============================
      {
        path: 'user',
        meta: { permission: 'users' },
        redirect: '/user/list',

        children: [

          {
            path: 'list',
            name: 'UserList',
            component: UserList,
            meta: {
              title: '用户列表'
            }
          },

          {
            path: 'detail/:id?',
            name: 'UserDetail',
            component: UserDetail,
            meta: {
              title: '用户详情',
              hidden: true
            }
          },

          {
            path: 'profile',
            name: 'UserProfile',
            component: () => import('@/views/user/UserProfile.vue'),
            meta: {
              title: '个人信息'
            }
          },

          {
            path: 'password',
            name: 'UserPassword',
            component: () => import('@/views/user/UserPassword.vue'),
            meta: {
              title: '修改密码'
            }
          }
        ]
      }
    ]
  },


  // ============================
  // 404
  // ============================
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,

  scrollBehavior () {
    return {
      top: 0
    }
  }
})

installAuthGuard(router, authClient, onUnauthorized, '/dashboard')

router.beforeEach(to => {
  if (canAccess(to.meta.permission)) return true
  if (to.name !== 'Dashboard') return { name: 'NoAccess' }
  const landing = [
    ['cases', 'CaseList'], ['policy', 'PolicyList'], ['project', 'ProjectList'],
    ['platform', 'PlatformList'], ['users', 'UserList']
  ].find(([permission]) => canAccess(permission))
  return { name: landing?.[1] || 'NoAccess' }
})

export default router
