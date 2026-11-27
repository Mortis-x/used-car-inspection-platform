import request from './request'

export function verifyPublicReport(reportNo) {
  return request.get(`/public/verify/${reportNo}`)
}
