import request from '../utils/request'

// 收支管理
export const paymentApi = {
  list: (params: any) => request.get('/finance/payments', { params }),
  create: (data: any) => request.post('/finance/payments', data),
  update: (id: number, data: any) => request.put(`/finance/payments/${id}`, data),
  delete: (id: number) => request.delete(`/finance/payments/${id}`),
}

// 利润分析
export const profitApi = {
  byOrder: () => request.get('/finance/profit/by-order'),
  summary: () => request.get('/finance/profit/summary'),
}
