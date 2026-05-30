import request from '../utils/request'

// 扫码报工
export const scanReportApi = {
  reportByBundle: (data: { bundleId: number; processIds: number[] }) => request.post('/report/bundle', data),
  reportByCutting: (data: { barcode: string }) => request.post('/report/cutting', data),
}

// 工资查询
export const wageApi = {
  getSummary: (yearMonth?: string) => request.get('/report/wage/summary', { params: { yearMonth } }),
}
