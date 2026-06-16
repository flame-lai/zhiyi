export interface MobileLoginResult {
  token: string
  employee: { id: number; code: string; name: string }
  role: { id: number; name: string }
  menus: MobileMenu[]
  permissions: string[]
  defaultHomeType: 'employee' | 'admin'
}

export interface MobileMenu {
  code: string
  title: string
  path: string
  group: 'employee' | 'admin'
  icon?: string
  sort: number
}

export interface MobileProfile {
  employee: { id: number; code: string; name: string }
  role: { id: number; name: string }
  menus: MobileMenu[]
  permissions: string[]
  defaultHomeType: 'employee' | 'admin'
}

export interface BundleScanResult {
  scanType: 'BUNDLE'
  code: string
  bundle: {
    id: number; globalBundleNo: string; bundleNo: string; qty: number
    orderNo: string; styleNo: string; color: string; size: string
  }
  processes: ProcessReportStatus[]
}

export interface CuttingScanResult {
  scanType: 'CUTTING'
  code: string
  cutting: {
    id: number; barcode: string; bedNo: number; totalBundleCount: number; totalQty: number
    orderNo: string; styleNo: string; color: string; size: string
  }
  bundles: Array<{
    id: number; globalBundleNo: string; bundleNo: string; qty: number
    disabled?: boolean; disabledReason?: string
  }>
  processes: ProcessReportStatus[]
}

export interface ProcessReportStatus {
  processId: number
  processName: string
  price: number
  status: 'AVAILABLE' | 'DONE' | 'PARTIAL_DONE' | 'UNAVAILABLE'
  doneBy?: string
  doneTime?: string
  hint?: string
}

export interface ReportSubmitResult {
  totalQty: number
  totalAmount: number
  items: Array<{
    processId: number; processName: string; qty: number; price: number; amount: number
  }>
  skippedItems?: Array<{
    bundleNo: string; processName: string; reason: string
  }>
}

export interface WageSummary {
  todayAmount: number
  monthAmount: number
  dailyGroups: Array<{
    date: string; amount: number; records: WorkReportRecord[]
  }>
}

export interface WorkReportRecord {
  id: number
  reportTime: string
  orderNo: string
  styleNo: string
  bundleNo: string
  processName: string
  qty: number
  price: number
  amount: number
}

export interface DashboardOrder {
  orderId: number; orderNo: string; styleNo: string; totalBundles: number
  completionRate: number; bottleneckProcess: string
  processProgress: Array<{
    processId: number; processName: string; completed: number; total: number; rate: number
  }>
}

export interface DashboardWorker {
  employeeId: number; employeeNo: string; employeeName: string
  totalQty: number; totalAmount: number
}

export interface AnomalyRecord {
  id: number; bundleNo: string; orderNo: string; description: string
  frontProcessName: string; backProcessName: string; reportEmployeeName: string
  reportTime: string; status: number; handleType: number; handleRemark: string
}

export interface TimedWageRecord {
  id: number; employeeId: number; employeeNo: string; employeeName: string
  workDate: string; hours: number; hourlyRate: number; amount: number; workContent: string
}

export interface BundleDetail {
  id: number; globalBundleNo: string; bundleNo: string; qty: number
  orderNo: string; styleNo: string; color: string; size: string
  status: string; processes: ProcessReportStatus[]
}

export interface OrderSearchResult {
  id: number; orderNo: string; customerName: string; totalAmount: number
  deliveryDate: string; status: number; completionRate: number
  items: Array<{
    styleNo: string; color: string; size: string; qty: number
    shippedQty: number; progress: number
  }>
}
