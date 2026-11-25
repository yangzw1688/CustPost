import test from 'node:test'
import assert from 'node:assert/strict'
import { createAuthClient, safeRedirect, installAuthGuard } from './auth-client.mjs'

function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
const userResponse = { data: { data: { phone: '13800138000' } } }

test('login and logout use the session API', async () => {
  const calls = []
  const state = { user: null }
  const client = createAuthClient({ post: async (...args) => { calls.push(args); return userResponse } }, state)
  await client.login('13800138000', '123004')
  assert.equal(state.user.phone, '13800138000')
  assert.deepEqual(calls[0], ['/api/auth/login', { phone: '13800138000', code: '123004' }])
  await client.logout()
  assert.equal(state.user, null)
  assert.equal(calls[1][0], '/api/auth/logout')
})
test('concurrent guards share a single current-user request', async () => {
  const wait = deferred()
  let calls = 0
  const client = createAuthClient({ get: () => { calls++; return wait.promise } }, { user: null })
  const one = client.currentUser(), two = client.currentUser()
  assert.equal(one, two)
  wait.resolve(userResponse)
  assert.equal(await one, true)
  assert.equal(calls, 1)
})
test('late current-user result cannot restore a logged-out user', async () => {
  const wait = deferred(), state = { user: userResponse.data.data }
  const client = createAuthClient({ get: () => wait.promise, post: async () => ({}) }, state)
  const loading = client.currentUser()
  await client.logout()
  wait.resolve(userResponse)
  assert.equal(await loading, false)
  assert.equal(state.user, null)
})
test('late 401 cannot overwrite a newer successful login', async () => {
  const wait = deferred(), state = { user: null }
  const client = createAuthClient({ get: () => wait.promise, post: async () => userResponse }, state)
  const loading = client.currentUser()
  await client.login('13800138000', '123004')
  wait.reject({ response: { status: 401 } })
  assert.equal(await loading, true)
  assert.equal(state.user.phone, '13800138000')
})
test('network failure does not masquerade as a valid session', async () => {
  const client = createAuthClient({ get: async () => { throw new Error('offline') } }, { user: null })
  await assert.rejects(client.currentUser(), /offline/)
})
test('failed logout keeps current state for retry', async () => {
  const state = { user: userResponse.data.data }
  const client = createAuthClient({ post: async () => { throw new Error('offline') } }, state)
  await assert.rejects(client.logout(), /offline/)
  assert.equal(state.user.phone, '13800138000')
})
test('redirect allows internal routes and rejects external and login destinations', () => {
  const router = { resolve: path => ({ name: path.startsWith('/login') ? 'Login' : path === '/missing' ? 'NotFound' : 'Mine' }) }
  assert.equal(safeRedirect('/mine?tab=1', router), '/mine?tab=1')
  for (const path of ['https://example.com', '//example.com', '/\\example.com', '/login', '/missing', ['//example.com']]) {
    assert.equal(safeRedirect(path, router, '/dashboard'), '/dashboard')
  }
})
test('the auth guard preserves the original destination', async () => {
  let guard, unauthorized, replaced
  const router = { beforeEach: handler => { guard = handler }, currentRoute: { value: { name: 'Mine', fullPath: '/mine' } }, replace: value => { replaced = value } }
  let cleared = false
  installAuthGuard(router, { currentUser: async () => false, clearAuth: () => { cleared = true } }, handler => { unauthorized = handler })
  assert.deepEqual(await guard({ name: 'Mine', fullPath: '/mine', meta: {} }), { name: 'Login', query: { redirect: '/mine' } })
  unauthorized()
  assert.equal(cleared, true)
  assert.deepEqual(replaced, { name: 'Login', query: { redirect: '/mine' } })
})
