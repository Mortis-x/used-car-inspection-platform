import request from './request'

export function getStatistics(params) {
  return request.get('/statistics/overview', { params })
}

export function exportStatistics(params) {
  return request.get('/statistics/export', { params, responseType: 'blob' })
}
