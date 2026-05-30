import { get, post } from '../utils/request'

export function getOrderDashboard(orderId?: number) {
  return get('/admin/dashboard/order', { orderId })
}

export function getWorkerDashboard(date?: string) {
  return get('/admin/dashboard/worker', { date })
}

export function getAnomalyList(params: any) {
  return get('/admin/anomalies', params)
}

export function ignoreAnomaly(id: number, remark?: string) {
  return post(`/admin/anomalies/${id}/ignore`, null, { remark })
}

export function clearAnomaly(id: number, remark?: string) {
  return post(`/admin/anomalies/${id}/clear`, null, { remark })
}

export function createTimedWage(data: any) {
  return post('/admin/timed-wages', data)
}

export function getBundleById(id: number) {
  return get(`/cutting/bundles/${id}`)
}
