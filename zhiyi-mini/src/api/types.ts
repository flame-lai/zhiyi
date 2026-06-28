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
  showMobilePrice?: number
}

export interface CuttingScanResult {
  scanType: 'CUTTING'
  code: string
  cutting: {
    id: number; barcode: string; bedNo: number; totalBundleCount: number; totalQty: number
    orderNo: string; styleNo: string
  }
  bundles: Array<{
    id: number; globalBundleNo: string; bundleNo: string; qty: number
    disabled?: boolean; disabledReason?: string
  }>
  colorSizeMatrix: Array<{
    color: string; size: string; qty: number; bundleIds: number[]
  }>
  processes: ProcessReportStatus[]
  showMobilePrice?: number
}

export interface ProcessReportStatus {
  processId: number
  processName: string
  price: number
  status: 'AVAILABLE' | 'DONE' | 'PARTIAL_DONE' | 'UNAVAILABLE'
  doneBy?: string
  doneTime?: string
  hint?: string
  batchReport?: number
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
  totalQty: number
  totalAmount: number
  visibleAmount: number
  styleSummaries: Array<{
    styleNo: string; qty: number; amount: number; showMobilePrice: number
    processes: Array<{
      processName: string; qty: number; price: number; amount: number
    }>
  }>
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
  showMobilePrice: number // 1=显示工价, 0=隐藏工价
}

export interface DashboardOrder {
  orderId: number; orderNo: string; styleNo: string; orderDate: string
  totalBundles: number; completionRate: number; bottleneckProcess: string
  processProgress: Array<{
    processId: number; processName: string; completed: number; total: number; rate: number
  }>
}

export interface DashboardOrderPage {
  records: DashboardOrder[]
  total: number; page: number; size: number; hasMore: boolean
}

export interface DashboardWorker {
  employeeId: number; employeeNo: string; employeeName: string
  totalQty: number; totalAmount: number
}

export interface DashboardWorkerPage {
  workers: DashboardWorker[]
  totalQty: number; totalAmount: number
  total: number; page: number; size: number; hasMore: boolean
}

export interface WorkerDetail {
  employee: { id: number; employeeNo: string; employeeName: string }
  totalQty: number; totalAmount: number
  byOrder: Array<{ orderId: number; orderNo: string; styleNo: string; qty: number; amount: number }>
  byProcess: Array<{ processId: number; processName: string; qty: number; amount: number }>
  dailyReports: Array<{
    date: string; qty: number; amount: number
    records: Array<{ id: number; orderNo: string; styleNo: string; processName: string; bundleNo: string; qty: number; price: number; amount: number }>
  }>
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

export interface EmployeeRecord {
  id: number; employeeNo: string; name: string; phone: string
  roleId: number; roleName: string; loginAccount: string
  status: number; remark: string
}

export interface RoleOption {
  id: number; name: string
}

export interface AdminReportRecord {
  id: number; employeeId: number; employeeName: string; employeeNo: string
  orderNo: string; bundleNo: string; processName: string
  qty: number; price: number; amount: number
  reportType: number; status: number; reportTime: string
  clearReason: string; supplementFlag: number
}

export interface BundleDetail {
  id: number; globalBundleNo: string; bundleNo: string; qty: number
  orderNo: string; styleNo: string; color: string; size: string
  status: string; processes: ProcessReportStatus[]
}

export interface BundleSuggest {
  id: number; globalBundleNo: string; bundleNo: string
  orderNo: string; color: string; size: string; qty: number
}

export interface OrderSearchResult {
  id: number; orderNo: string; customerName: string; totalAmount: number
  deliveryDate: string; status: number; completionRate: number
  items: Array<{
    styleNo: string; color: string; size: string; qty: number
    shippedQty: number; progress: number
  }>
}

export interface OrderSuggest {
  id: number; orderNo: string; customerName: string
  totalAmount: number; deliveryDate: string; status: number
}

export interface OrderListItem {
  id: number; orderNo: string; customerName: string
  styleNo: string; totalAmount: number; totalQty: number
  deliveryDate: string; status: number; createdAt: string
}

export interface OrderListPage {
  records: OrderListItem[]; total: number; hasMore: boolean
}

export interface ColorSizeProgress {
  styleNo: string; color: string; size: string
  totalBundles: number; completionRate: number
  processes: ColorSizeProcessRow[]
  bundles: ColorSizeBundleRow[]
}

export interface ColorSizeProcessRow {
  processId: number; processName: string
  completed: number; total: number; rate: number
}

export interface ColorSizeBundleRow {
  bundleId: number; globalBundleNo: string; bundleNo: string
  processes: ColorSizeBundleProcess[]
}

export interface ColorSizeBundleProcess {
  processId: number; processName: string
  status: string; doneBy: string; doneTime: string
}
