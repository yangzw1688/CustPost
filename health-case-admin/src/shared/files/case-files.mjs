// 前端只做选择提示；最终格式、文件头和大小由后端再次校验。
// 前端只做选择提示；最终格式、文件头和大小由后端再次校验。
export const MAX_CASE_FILES = 9
export const MAX_CASE_FILE_SIZE = 10 * 1024 * 1024
export function isPdfAttachment(file) {
  // OBS 签名 URL 带查询参数，类型判断使用元数据或文件名。
  // OBS 签名 URL 带查询参数，类型判断使用元数据或文件名。
  return file.fileType === 'pdf' || file.type === 'application/pdf' || /\.pdf$/i.test(file.fileName || file.originalName || file.name || '')
}
export function canUploadCaseFile(file) {
  if (['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'application/pdf'].includes(file.type)) return true
  return (!file.type || file.type === 'application/octet-stream') && /\.pdf$/i.test(file.name || '')
}
