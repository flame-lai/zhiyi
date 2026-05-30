import request from '../utils/request'

// 裁床计划
export const cuttingPlanApi = {
  list: (params: any) => request.get('/cutting/plans', { params }),
  create: (data: any) => request.post('/cutting/plans', data),
  update: (id: number, data: any) => request.put(`/cutting/plans/${id}`, data),
  delete: (id: number) => request.delete(`/cutting/plans/${id}`),
  inputActualQty: (id: number, actualQty: number) => request.post(`/cutting/plans/${id}/input-actual`, null, { params: { actualQty } }),
  openBundle: (id: number) => request.post(`/cutting/plans/${id}/open-bundle`),
  getBundles: (id: number) => request.get(`/cutting/plans/${id}/bundles`),
}

// 扎号管理
export const bundleApi = {
  getById: (id: number) => request.get(`/cutting/bundles/${id}`),
  getByOrderId: (orderId: number) => request.get(`/cutting/bundles/by-order/${orderId}`),
}
