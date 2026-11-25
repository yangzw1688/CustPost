import axios from 'axios'
import { createApiClient } from '@/shared/http/api-client.mjs'

const { request, onUnauthorized } = createApiClient(axios, 30000)
export { onUnauthorized }
export default request
