import request from '../utils/request'

// 生产看板
export const dashboardApi = {
  orderDashboard: (orderId?: number) => request.get('/admin/dashboard/order', { params: { orderId } }),
  workerDashboard: (date?: string) => request.get('/admin/dashboard/worker', { params: { date } }),
}

// 异常预警
export const anomalyApi = {
  list: (params: any) => request.get('/admin/anomalies', { params }),
  ignore: (id: number, remark: string) => request.post(`/admin/anomalies/${id}/ignore`, null, { params: { remark } }),
  clear: (id: number, reason: string) => request.post(`/admin/anomalies/${id}/clear`, null, { params: { reason } }),
  supplement: (id: number, employeeId: number, qty: number) => request.post(`/admin/anomalies/${id}/supplement`, null, { params: { employeeId, qty } }),
}

// 计时工资
export const timedWageApi = {
  list: (params: any) => request.get('/admin/timed-wages', { params }),
  create: (data: any) => request.post('/admin/timed-wages', data),
  createBatch: (data: any[]) => request.post('/admin/timed-wages/batch', data),
  update: (id: number, data: any) => request.put(`/admin/timed-wages/${id}`, data),
  delete: (id: number) => request.delete(`/admin/timed-wages/${id}`),
}

// 工资管理
export const wageManageApi = {
  list: (params: any) => request.get('/admin/wages', { params }),
  export: (params: any) => request.get('/admin/wages/export', { params, responseType: 'blob' }),
  audit: (employeeId: number, yearMonth: string) => request.post('/admin/wages/audit', null, { params: { employeeId, yearMonth } }),
  markPaid: (employeeId: number, yearMonth: string) => request.post('/admin/wages/mark-paid', null, { params: { employeeId, yearMonth } }),
  detail: (employeeId: number, yearMonth: string) => request.get('/admin/wages/detail', { params: { employeeId, yearMonth } }),
  getOtherItems: (employeeId: number, yearMonth: string) => request.get('/admin/wages/other-items', { params: { employeeId, yearMonth } }),
  addOtherItem: (data: any) => request.post('/admin/wages/other-items', data),
  updateOtherItem: (id: number, data: any) => request.put(`/admin/wages/other-items/${id}`, data),
  deleteOtherItem: (id: number) => request.delete(`/admin/wages/other-items/${id}`),
}
