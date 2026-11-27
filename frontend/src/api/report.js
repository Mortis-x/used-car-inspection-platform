import request from './request'

export function generateReport(orderId, reason) {
  return request.post(`/reports/orders/${orderId}/generate`, null, { params: { reason } })
}

export function getReport(reportNo) {
  return request.get(`/reports/${reportNo}`)
}

export function getMarketReports(params) {
  return request.get('/reports/market', { params })
}

export function listReports(orderId) {
  return request.get('/reports', { params: { orderId } })
}

export function submitCorrection(reportNo, data) {
  return request.post(`/reports/${reportNo}/corrections`, data)
}
