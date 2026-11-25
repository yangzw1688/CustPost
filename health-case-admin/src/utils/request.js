import axios from 'axios'
import { createApiClient } from '@/shared/http/api-client.mjs'

const { request, onUnauthorized } = createApiClient(axios, 20000)
export { onUnauthorized }

export async function getList(url, config) {
  const { data } = await request.get(url, config)
  if (data?.code !== 200 || !Array.isArray(data.data)) {
    throw new Error(data?.message || '数据暂时无法加载，请稍后重试')
  }
  return data.data
}

export function errorMessage(error) {
  return error.response?.data?.message || (error.code === 'ECONNABORTED'
    ? '请求超时，请检查网络后重试' : error.message === 'Network Error'
      ? '无法连接服务，请稍后重试' : error.message || '加载失败，请稍后重试')
}

export default request
