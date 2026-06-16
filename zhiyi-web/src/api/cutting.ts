import request from '../utils/request'

// 裁床计划
export const cuttingPlanApi = {
  list: (params: any) => request.get('/cutting/plans', { params }),
  getById: (id: number) => request.get(`/cutting/plans/${id}`),
  getDetails: (id: number) => request.get(`/cutting/plans/${id}/details`),
  create: (data: any) => request.post('/cutting/plans', data),
  update: (id: number, data: any) => request.put(`/cutting/plans/${id}`, data),
  delete: (id: number) => request.delete(`/cutting/plans/${id}`),
  inputActualQty: (id: number, data: { details: any[], cuttingDate?: string }) => request.post(`/cutting/plans/${id}/input-actual`, data),
  openBundle: (id: number) => request.post(`/cutting/plans/${id}/open-bundle`),
  getBundles: (id: number) => request.get(`/cutting/plans/${id}/bundles`),
  getBundleDetails: (id: number) => request.get(`/cutting/plans/${id}/bundle-details`),
  getProgress: (orderId: number, styleId: number) => request.get('/cutting/plans/progress', { params: { orderId, styleId } }),
}

// 扎号管理
export const bundleApi = {
  list: (params: any) => request.get('/cutting/bundles', { params }),
  getById: (id: number) => request.get(`/cutting/bundles/${id}`),
  getWorkDetails: (id: number) => request.get(`/cutting/bundles/${id}/work-details`),
  getByOrderId: (orderId: number) => request.get(`/cutting/bundles/by-order/${orderId}`),
}
