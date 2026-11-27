import request from './request'

export function getChain() {
  return request.get('/evidence/chain')
}

export function verifyChain() {
  return request.get('/evidence/verify')
}

export function verifyReport(reportNo) {
  return request.get(`/evidence/verify/${reportNo}`)
}
