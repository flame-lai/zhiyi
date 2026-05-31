import request from '../utils/request'

// 订单管理
export const orderApi = {
  list: (params: any) => request.get('/orders', { params }),
  getById: (id: number) => request.get(`/orders/${id}`),
  create: (data: any) => request.post('/orders', data),
  update: (id: number, data: any) => request.put(`/orders/${id}`, data),
  getItems: (id: number) => request.get(`/orders/${id}/items`),
  getColorSizes: (id: number) => request.get(`/orders/${id}/color-sizes`),
  saveColorSizes: (id: number, data: any[]) => request.post(`/orders/${id}/color-sizes`, data),
  getProcesses: (id: number) => request.get(`/orders/${id}/processes`),
  saveProcesses: (id: number, data: any[]) => request.post(`/orders/${id}/processes`, data),
  getMaterials: (id: number) => request.get(`/orders/${id}/materials`),
  saveMaterials: (id: number, data: any[]) => request.post(`/orders/${id}/materials`, data),
  confirm: (id: number) => request.post(`/orders/${id}/confirm`),
  settle: (id: number) => request.post(`/orders/${id}/settle`),
}

// 出货管理
export const shipmentApi = {
  list: (orderId: number) => request.get('/shipments', { params: { orderId } }),
  create: (data: any) => request.post('/shipments', data),
  delete: (id: number) => request.delete(`/shipments/${id}`),
}
