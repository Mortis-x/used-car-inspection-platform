import request from './request'

export function listTemplates() {
  return request.get('/templates')
}

export function listTemplateItems(templateId) {
  return request.get(`/templates/${templateId}/items`)
}

export function listUsersByRole(role) {
  return request.get('/users', { params: { role } })
}
