import request from './request'

export function listVehicles(params) {
  return request.get('/vehicles', { params })
}

export function getVehicle(id) {
  return request.get(`/vehicles/${id}`)
}

export function createVehicle(data) {
  return request.post('/vehicles', data)
}

export function updateVehicle(id, data) {
  return request.put(`/vehicles/${id}`, data)
}

export function listMaintenance(vehicleId) {
  return request.get(`/vehicles/${vehicleId}/maintenance`)
}

export function addMaintenance(vehicleId, data) {
  return request.post(`/vehicles/${vehicleId}/maintenance`, data)
}
