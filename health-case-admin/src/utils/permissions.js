import { auth } from './auth'

export const moduleOptions = [
  { key: 'dashboard', label: '数据统计' },
  { key: 'cases', label: '案件管理' },
  { key: 'policy', label: '保单管理' },
  { key: 'project', label: '项目管理' },
  { key: 'platform', label: '平台管理' },
  { key: 'users', label: '用户中心' }
]

export function canAccess(permission) {
  if (!permission) return true
  const user = auth.user
  if (user?.superAdmin) return true
  if (permission === 'superAdmin') return false
  if (permission === 'policyTransfer') return user?.modules?.policy === true && user.policyTransfer === true
  if (permission === 'uiManagement') return user?.modules?.platform === true && user.uiManagement === true
  return user?.modules?.[permission] === true
}
