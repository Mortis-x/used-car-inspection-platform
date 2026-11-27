import request from './request'

export function listDisputes(params) {
  return request.get('/disputes', { params })
}

export function getDispute(id) {
  return request.get(`/disputes/${id}`)
}

export function createDispute(data) {
  return request.post('/disputes', data)
}

export function claimDispute(id) {
  return request.post(`/disputes/${id}/claim`)
}

export function addDisputeEvidence(id, data) {
  return request.post(`/disputes/${id}/evidence`, data)
}

export function resolveDispute(id, data) {
  return request.post(`/disputes/${id}/resolve`, data)
}

export function closeDispute(id) {
  return request.post(`/disputes/${id}/close`)
}
