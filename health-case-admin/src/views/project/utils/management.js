import { ElMessage } from 'element-plus'
import { errorMessage } from '@/utils/request'
export const projectStates = { DRAFT: '草稿', ACTIVE: '运行中', PAUSED: '已暂停', ENDED: '已结束' }
export const connectionStates = { NOT_CONFIGURED: '未配置', TESTING: '联调中', CONNECTED: '已对接', SUSPENDED: '已暂停' }
export const environments = { TEST: '测试环境', PROD: '生产环境' }
export function time(value) { return value ? String(value).replace('T', ' ').substring(0, 19) : '—' }
export function notifyFailure(error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(errorMessage(error)) }
