import request from '@/utils/request'

/**
 * 提交病例资料
 */
export function saveServiceRecord(formData) {
    return request.post(
        '/api/service-record/save',
        formData
    )
}

/**
 * 服务记录列表
 */
export function getServiceRecordList() {
    return request.get(
        '/api/service-record/list'
    )
}

/**
 * 服务详情
 */
export function getServiceRecordDetail(id) {
    return request.get(
        `/api/service-record/${id}`
    )
}
