import { reactive } from 'vue'
import request from './request'
import { createAuthClient } from '@/shared/auth/auth-client.mjs'
import { collectPublicIp } from './public-ip'
export { safeRedirect, PHONE_PATTERN } from '@/shared/auth/auth-client.mjs'
export const auth = reactive({ user: null })
export const authClient = createAuthClient(request, auth, { basePath: '/api/admin/auth', credentialField: 'password' })
export const { logout } = authClient
export async function login(phone, password) {
  const reportedPublicIp = await collectPublicIp()
  return authClient.login(phone, password, reportedPublicIp ? { reportedPublicIp } : {})
}
