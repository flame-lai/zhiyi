import { get, post } from '../utils/request'
import type { BundleScanResult, CuttingScanResult, ReportSubmitResult, WageSummary, WorkReportRecord } from './types'

export function resolveScan(code: string) {
  return post<BundleScanResult | CuttingScanResult>('/mobile/report/scan/resolve', { code })
}

export function submitReport(data: {
  scanType: string; code: string; selectedBundleIds: number[]; selectedProcessIds: number[]
}) {
  return post<ReportSubmitResult>('/mobile/report/submit', data)
}

export function getWageSummary() {
  return get<WageSummary>('/mobile/report/wage-summary')
}

export function getReportRecords(startDate?: string, endDate?: string) {
  return get<WorkReportRecord[]>('/mobile/report/records', { startDate, endDate })
}
