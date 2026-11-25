// 仅查询浏览器的网络出口 IP；请求不携带本系统账号、Cookie 或页面来源。
const endpoint = process.env.VUE_APP_PUBLIC_IP_LOOKUP_URL || 'https://api64.ipify.org?format=json'
let pending

export function collectPublicIp() {
  if (process.env.VUE_APP_PUBLIC_IP_LOOKUP_ENABLED === 'false' || typeof fetch !== 'function'
    || typeof AbortController !== 'function') return Promise.resolve('')
  if (pending) return pending
  const controller = new AbortController()
  let timer
  const lookup = async () => {
    const url = new URL(endpoint)
    if (url.protocol !== 'https:' || url.username || url.password || url.hash) return ''
    const response = await fetch(url.href, { method: 'GET', credentials: 'omit', referrerPolicy: 'no-referrer', cache: 'no-store', signal: controller.signal })
    if (!response.ok) return ''
    const text = await response.text()
    if (text.length > 256) return ''
    const ip = JSON.parse(text).ip
    return typeof ip === 'string' && ip.length <= 64 && /^[0-9a-fA-F:.]+$/.test(ip) ? ip : ''
  }
  // 网络查询失败或超时不影响账号密码登录。
  const timeout = new Promise(resolve => { timer = setTimeout(() => { controller.abort(); resolve('') }, 1500) })
  const task = Promise.race([lookup(), timeout]).catch(() => '').finally(() => {
    clearTimeout(timer)
    if (pending === task) pending = null
  })
  pending = task
  return task
}
