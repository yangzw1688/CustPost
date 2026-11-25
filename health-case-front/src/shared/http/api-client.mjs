/** 两端共用响应处理规则，每次调用创建独立请求实例与未登录回调。 */
export function createApiClient(axios, timeout) {
  // 由调用方传入 axios，沿用各 Vue 项目的依赖，无需在 shared 下重复安装。
  const request = axios.create({ timeout, withCredentials: true })
  let unauthorizedHandler = () => {}

  request.interceptors.response.use(response => {
    if (response.data && typeof response.data.code === 'number' && response.data.code !== 200) {
      const error = new Error(response.data.message || '操作失败，请稍后重试')
      error.response = response
      return Promise.reject(error)
    }
    return response
  }, error => {
    // 会话探测由路由守卫处理，避免探测失败与普通接口同时触发重复跳转。
    if (error.response?.status === 401 && !error.config?.skipAuthRedirect) unauthorizedHandler()
    if (error.response?.data?.message) error.message = error.response.data.message
    else if (error.code === 'ECONNABORTED') error.message = '请求超时，请检查网络后重试'
    else if (error.code === 'ERR_NETWORK') error.message = '无法连接服务，请稍后重试'
    return Promise.reject(error)
  })

  return { request, onUnauthorized(handler) { unauthorizedHandler = handler } }
}
