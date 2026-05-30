import { get, post } from '../utils/request'

export function reportByBundle(data: { bundleId: number; processIds: number[] }) {
  return post('/report/bundle', data)
}

export function reportByCutting(data: { barcode: string }) {
  return post('/report/cutting', data)
}

export function getWageSummary(yearMonth?: string) {
  return get('/report/wage/summary', { yearMonth })
}
