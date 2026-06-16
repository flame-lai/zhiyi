import { get, post, put, del } from '../utils/request'
import type { DashboardOrder, DashboardWorker, AnomalyRecord, TimedWageRecord, BundleDetail, OrderSearchResult } from './types'

export function getDashboardOrders(date?: string, orderId?: number) {
  return get<DashboardOrder[]>('/mobile/admin/dashboard/orders', { date, orderId })
}

export function getDashboardWorkers(date?: string) {
  return get<DashboardWorker[]>('/mobile/admin/dashboard/workers', { date })
}

export function getAnomalies(status?: number) {
  return get<AnomalyRecord[]>('/mobile/admin/anomalies', { status })
}

export function ignoreAnomaly(id: number, remark: string) {
  return post<void>(`/mobile/admin/anomalies/${id}/ignore`, { remark })
}

export function clearAnomaly(id: number, reason: string) {
  return post<void>(`/mobile/admin/anomalies/${id}/clear`, { reason })
}

export function supplementAnomaly(id: number, data: { employeeId: number; qty: number; reportTime?: string }) {
  return post<void>(`/mobile/admin/anomalies/${id}/supplement`, data)
}

export function getTimedWages(yearMonth?: string) {
  return get<TimedWageRecord[]>('/mobile/admin/timed-wages', { yearMonth })
}

export function createTimedWage(data: {
  employeeId: number; workDate?: string; hours: number; hourlyRate: number; workContent?: string
}) {
  return post<void>('/mobile/admin/timed-wages', data)
}

export function updateTimedWage(id: number, data: {
  employeeId: number; workDate?: string; hours: number; hourlyRate: number; workContent?: string
}) {
  return put<void>(`/mobile/admin/timed-wages/${id}`, data)
}

export function deleteTimedWage(id: number) {
  return del<void>(`/mobile/admin/timed-wages/${id}`)
}

export function searchBundle(code: string) {
  return get<BundleDetail>('/mobile/admin/bundles/search', { code })
}

export function searchOrder(orderNo: string) {
  return get<OrderSearchResult>('/mobile/admin/orders/search', { orderNo })
}
