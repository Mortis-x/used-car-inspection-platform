import request from './request'

export function updateInspectionItem(orderId, itemId, data) {
  return request.put(`/inspections/${orderId}/items/${itemId}`, data)
}
