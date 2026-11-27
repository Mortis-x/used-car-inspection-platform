import request from './request'

export function listInspections(params) {
  return request.get('/inspections', { params })
}

export function getInspection(id) {
  return request.get(`/inspections/${id}`)
}

export function createInspection(data) {
  return request.post('/inspections', data)
}

export function startInspection(id) {
  return request.post(`/inspections/${id}/start`)
}

export function submitInspection(id) {
  return request.post(`/inspections/${id}/submit`)
}

export function withdrawInspection(id, reason) {
  return request.post(`/inspections/${id}/withdraw`, { reason })
}

export function getOrderTimeline(id) {
  return request.get(`/inspections/${id}/timeline`)
}
