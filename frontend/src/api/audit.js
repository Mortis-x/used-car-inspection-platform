import request from './request'

export function getAuditLogs(params) {
  return request.get('/audit-logs', { params })
}

export function getVerifyRecords(params) {
  return request.get('/verify-records', { params })
}
