import { createRouter, createWebHistory } from 'vue-router'
import { authClient } from '@/utils/auth'
import { installAuthGuard } from '@/shared/auth/auth-client.mjs'
import { onUnauthorized } from '@/utils/request'
const routes = [
  { path: '/page-preview', name: 'PagePreview', component: () => import('@/views/ui/PagePreview.vue'), meta: { title: '页面预览', public: true } },
  { path: '/pages/:slug([a-z][a-z0-9-]*)', name: 'ManagedPage', component: () => import('@/views/ui/ManagedPage.vue'), meta: { title: '服务页面' } },
  { path: '/login', name: 'Login', component: () => import('@/views/login/LoginPage.vue'), meta: { title: '手机号登录', public: true } },
  { path: '/', name: 'Home', component: () => import('@/views/home/HomePage.vue'), meta: { title: '健康服务' } },
  { path: '/submit', name: 'Submit', component: () => import('@/views/submit/SubmitPage.vue'), meta: { title: '资料提交' } },
  { path: '/service-record', name: 'ServiceRecord', component: () => import('@/views/record/ServiceRecordPage.vue'), meta: { title: '服务记录' } },
  { path: '/service-detail/:id', name: 'ServiceDetail', component: () => import('@/views/record/ServiceDetailPage.vue'), meta: { title: '服务详情' } },
  { path: '/mine', name: 'Mine', component: () => import('@/views/mine/MinePage.vue'), meta: { title: '个人中心' } },
  { path: '/policy', name: 'Policy', component: () => import('@/views/policy/PolicyPage.vue'), meta: { title: '保单查询' } },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/error/NotFoundPage.vue'), meta: { title: '页面未找到' } }
]
const router = createRouter({ history: createWebHistory(process.env.BASE_URL), routes, scrollBehavior: () => ({ top: 0 }) })
installAuthGuard(router, authClient, onUnauthorized)
router.afterEach(to => { document.title = `${to.meta.title || '健康服务'} · 健康服务平台` })
export default router
