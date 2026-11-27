import request from './request'

export function listIntentions() {
  return request.get('/intentions')
}

export function createIntention(data) {
  return request.post('/intentions', data)
}

export function counterIntention(id, price) {
  return request.post(`/intentions/${id}/counter`, { price })
}

export function acceptIntention(id) {
  return request.post(`/intentions/${id}/accept`)
}

export function rejectIntention(id) {
  return request.post(`/intentions/${id}/reject`)
}

export function cancelIntention(id) {
  return request.post(`/intentions/${id}/cancel`)
}

export function confirmIntention(id) {
  return request.post(`/intentions/${id}/confirm`)
}

export function declineIntention(id) {
  return request.post(`/intentions/${id}/decline`)
}
