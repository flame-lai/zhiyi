import request from '../utils/request'

// 工序管理
export const processApi = {
  list: (params: any) => request.get('/base/processes', { params }),
  create: (data: any) => request.post('/base/processes', data),
  update: (id: number, data: any) => request.put(`/base/processes/${id}`, data),
  delete: (id: number) => request.delete(`/base/processes/${id}`),
  getCategories: () => request.get('/base/processes/categories'),
}

// 款号管理
export const styleApi = {
  list: (params: any) => request.get('/base/styles', { params }),
  getById: (id: number) => request.get(`/base/styles/${id}`),
  create: (data: any) => request.post('/base/styles', data),
  update: (id: number, data: any) => request.put(`/base/styles/${id}`, data),
  delete: (id: number) => request.delete(`/base/styles/${id}`),
  getProcesses: (id: number) => request.get(`/base/styles/${id}/processes`),
  saveProcesses: (id: number, data: any[]) => request.post(`/base/styles/${id}/processes`, data),
  getMaterials: (id: number) => request.get(`/base/styles/${id}/materials`),
  saveMaterials: (id: number, data: any[]) => request.post(`/base/styles/${id}/materials`, data),
}

// 客户管理
export const customerApi = {
  list: (params: any) => request.get('/base/customers', { params }),
  create: (data: any) => request.post('/base/customers', data),
  update: (id: number, data: any) => request.put(`/base/customers/${id}`, data),
  delete: (id: number) => request.delete(`/base/customers/${id}`),
}

// 供应商管理
export const supplierApi = {
  list: (params: any) => request.get('/base/suppliers', { params }),
  create: (data: any) => request.post('/base/suppliers', data),
  update: (id: number, data: any) => request.put(`/base/suppliers/${id}`, data),
  delete: (id: number) => request.delete(`/base/suppliers/${id}`),
}
