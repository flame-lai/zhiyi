import { get, post, put, del } from '../utils/request'
import type { DashboardOrderPage, DashboardWorkerPage, WorkerDetail, AnomalyRecord, TimedWageRecord, EmployeeRecord, RoleOption, BundleDetail, BundleSuggest, OrderSearchResult, OrderSuggest, OrderListPage, ColorSizeProgress, AdminReportRecord } from './types'

export function getDashboardOrders(startDate?: string, endDate?: string, orderNo?: string, page = 1, size = 20) {
  return get<DashboardOrderPage>('/mobile/admin/dashboard/orders', { startDate, endDate, orderNo, page, size })
}

export function getDashboardWorkers(startDate?: string, endDate?: string, keyword?: string, page = 1, size = 20) {
  return get<DashboardWorkerPage>('/mobile/admin/dashboard/workers', { startDate, endDate, keyword, page, size })
}

export function getWorkerDetail(employeeId: number, startDate?: string, endDate?: string) {
  return get<WorkerDetail>('/mobile/admin/dashboard/worker-detail', { employeeId, startDate, endDate })
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

export function supplementAnomaly(id: number, data: { employeeNo: string; qty: number; reportTime?: string }) {
  return post<void>(`/mobile/admin/anomalies/${id}/supplement`, data)
}

export function getTimedWages(params: {
  yearMonth?: string; startDate?: string; endDate?: string
  employeeId?: number; page?: number; size?: number
} = {}) {
  return get<{ records: TimedWageRecord[]; total: number; hasMore: boolean }>('/mobile/admin/timed-wages', params)
}

export function createTimedWage(data: {
  employeeNo: string; workDate?: string; hours: number; hourlyRate: number; workContent?: string
}) {
  return post<void>('/mobile/admin/timed-wages', data)
}

export function updateTimedWage(id: number, data: {
  employeeNo: string; workDate?: string; hours: number; hourlyRate: number; workContent?: string
}) {
  return put<void>(`/mobile/admin/timed-wages/${id}`, data)
}

export function deleteTimedWage(id: number) {
  return del<void>(`/mobile/admin/timed-wages/${id}`)
}

export function searchBundle(code: string) {
  return get<BundleDetail>('/mobile/admin/bundles/search', { code })
}

export function suggestBundle(keyword: string) {
  return get<BundleSuggest[]>('/mobile/admin/bundles/suggest', { keyword })
}

export function searchOrder(orderNo: string) {
  return get<OrderSearchResult>('/mobile/admin/orders/search', { orderNo })
}

export function suggestOrder(keyword: string) {
  return get<OrderSuggest[]>('/mobile/admin/orders/suggest', { keyword })
}

export function getOrders(params: {
  keyword?: string; status?: number; startDate?: string; endDate?: string
  page?: number; size?: number
} = {}) {
  return get<OrderListPage>('/mobile/admin/orders', params)
}

export function getColorSizeProgress(orderId: number, color?: string, size?: string) {
  return get<ColorSizeProgress>('/mobile/admin/orders/color-size-progress', { orderId, color, size })
}

export function getEmployees(keyword?: string, page = 1, size = 20) {
  return get<{ records: EmployeeRecord[]; total: number; hasMore: boolean }>(
    '/mobile/admin/employees', { keyword, page, size }
  )
}

export function getRoles() {
  return get<RoleOption[]>('/mobile/admin/roles')
}

export function createEmployee(data: {
  name: string; phone?: string; roleId: number
  loginAccount?: string; loginPassword?: string; status?: number; remark?: string
}) {
  return post<void>('/mobile/admin/employees', data)
}

export function updateEmployee(id: number, data: {
  name: string; phone?: string; roleId: number
  loginAccount?: string; status?: number; remark?: string
}) {
  return put<void>(`/mobile/admin/employees/${id}`, data)
}

export function deleteEmployee(id: number) {
  return del<void>(`/mobile/admin/employees/${id}`)
}

export function toggleEmployeeStatus(id: number, status: number) {
  return put<void>(`/mobile/admin/employees/${id}/status`, { status })
}

export function getReports(keyword?: string, page = 1, size = 20) {
  return get<{ records: AdminReportRecord[]; total: number; hasMore: boolean }>(
    '/mobile/admin/reports', { keyword, page, size }
  )
}

export function clearReport(id: number, reason: string) {
  return post<void>(`/mobile/admin/reports/${id}/clear`, { reason })
}

export function supplementReport(data: { employeeNo: string; bundleId: number; processId: number; qty: number }) {
  return post<void>('/mobile/admin/reports/supplement', data)
}
