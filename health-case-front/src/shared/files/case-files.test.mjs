import test from 'node:test'
import assert from 'node:assert/strict'
import { canUploadCaseFile, isPdfAttachment, MAX_CASE_FILES, MAX_CASE_FILE_SIZE } from './case-files.mjs'
test('case files support PDF and existing images', () => {
  for (const type of ['application/pdf', 'image/jpeg', 'image/png', 'image/gif', 'image/webp']) assert(canUploadCaseFile({ type, name: 'report' }))
  assert(canUploadCaseFile({ type: '', name: 'REPORT.PDF' }))
  assert(canUploadCaseFile({ type: 'application/octet-stream', name: 'report.pdf' }))
  assert(!canUploadCaseFile({ type: 'image/svg+xml', name: 'report.svg' }))
  assert(!canUploadCaseFile({ type: 'text/html', name: 'report.pdf' }))
  assert.equal(MAX_CASE_FILES, 9)
  assert.equal(MAX_CASE_FILE_SIZE, 10 * 1024 * 1024)
})
test('detail distinguishes PDFs independently from signed URLs', () => {
  assert(isPdfAttachment({ fileType: 'pdf', fileUrl: 'https://example.com/signed?token=1' }))
  assert(isPdfAttachment({ fileName: 'report.PDF' }))
  assert(!isPdfAttachment({ fileName: 'report.jpg', fileUrl: 'https://example.com/a.pdf?token=1' }))
})
