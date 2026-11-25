// 两端统一通过服务端 Session 鉴权，不使用本地存储模拟登录状态。
export const PHONE_PATTERN = /^1[3-9]\d{9}$/
export const CODE_PATTERN = /^\d{6}$/

export function createAuthClient(request, state, { basePath = '/api/auth', credentialField = 'code' } = {}) {
  // 合并并发的会话查询；revision 让退出或重新登录前的旧请求结果失效。
  // 合并并发的会话查询；revision 让退出或重新登录前的旧请求结果失效。
  let pending
  let revision = 0
  function clearAuth() { revision++; state.user = null }
  function currentUser() {
    if (!pending) {
      const startedAt = revision
      const task = request.get(`${basePath}/me`, { skipAuthRedirect: true }).then(response => {
        if (startedAt !== revision) return !!state.user
        state.user = response.data.data
        return !!state.user
      }).catch(error => {
        if (error.response?.status !== 401) throw error
        if (startedAt !== revision) return !!state.user
        clearAuth()
        return false
      }).finally(() => { if (pending === task) pending = null })
      pending = task
    }
    return pending
  }
  async function login(phone, credential, metadata = {}) {
    // 附加审计信息不能覆盖账号与登录凭据。
    const response = await request.post(`${basePath}/login`, { ...metadata, phone, [credentialField]: credential })
    revision++
    pending = null
    state.user = response.data.data
  }
  async function logout() {
    await request.post(`${basePath}/logout`)
    clearAuth()
    pending = null
  }
  async function sendCode(phone) {
    const response = await request.post('/api/auth/send-code', { phone })
    return response.data.data
  }
  return { currentUser, clearAuth, login, logout, sendCode }
}

export function safeRedirect(value, router, fallback = '/') {
  // 仅接受路由中存在的站内页面，阻止外部跳转与回到登录页的循环。
  // 仅接受路由中存在的站内页面，阻止外部跳转与回到登录页的循环。
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//') || value.includes('\\')) return fallback
  const route = router.resolve(value)
  return route.name && !['Login', 'NotFound'].includes(route.name) ? value : fallback
}

export function installAuthGuard(router, client, onUnauthorized, fallback = '/') {
  router.beforeEach(async to => {
    if (to.meta.public === true && to.name !== 'Login') return true
    try {
      const loggedIn = await client.currentUser()
      if (to.name === 'Login') return loggedIn ? safeRedirect(to.query.redirect, router, fallback) : true
      return loggedIn ? true : { name: 'Login', query: { redirect: to.fullPath } }
    } catch (error) {
      return to.name === 'Login' ? true : { name: 'Login', query: { redirect: to.fullPath, reason: 'unavailable' } }
    }
  })
  onUnauthorized(() => {
    client.clearAuth()
    const current = router.currentRoute.value
    if (current.name !== 'Login') router.replace({ name: 'Login', query: { redirect: current.fullPath } })
  })
}
