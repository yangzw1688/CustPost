import { reactive } from 'vue'
import request from './request'
import { createAuthClient } from '@/shared/auth/auth-client.mjs'
export { safeRedirect, PHONE_PATTERN, CODE_PATTERN } from '@/shared/auth/auth-client.mjs'
export const auth = reactive({ user: null })
export const authClient = createAuthClient(request, auth)
export const { login, logout, sendCode } = authClient
