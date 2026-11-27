import request from './request'

export function listAttachments(orderId) {
  return request.get(`/inspections/${orderId}/attachments`)
}

export function uploadAttachment(orderId, file, itemId) {
  const form = new FormData()
  form.append('file', file)
  if (itemId) {
    form.append('itemId', itemId)
  }
  return request.post(`/inspections/${orderId}/attachments`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getAttachmentUrl(id, type = 'thumb') {
  return request.get(`/attachments/${id}/url`, { params: { type } })
}
