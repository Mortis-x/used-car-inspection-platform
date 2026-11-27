import request from './request'

export function listApprovals(status) {
  return request.get('/approvals', { params: { status } })
}

export function approveTask(id, opinion) {
  return request.post(`/approvals/${id}/approve`, { opinion })
}

export function rejectTask(id, opinion) {
  return request.post(`/approvals/${id}/reject`, { opinion })
}
