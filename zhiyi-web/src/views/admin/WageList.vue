<template>
  <a-card title="工资管理">
    <div class="toolbar">
      <a-space wrap>
        <a-month-picker
          v-model:value="searchYearMonth"
          placeholder="选择月份"
          style="width: 160px"
          format="YYYY-MM"
          valueFormat="YYYY-MM"
          @change="handleSearch"
        />
        <a-select
          v-model:value="searchEmployeeId"
          placeholder="选择工人"
          allow-clear
          show-search
          style="width: 220px"
          :filter-option="filterEmployeeOption"
          @change="handleSearch"
        >
          <a-select-option v-for="emp in employeeOptions" :key="emp.id" :value="emp.id">
            {{ formatEmployee(emp) }}
          </a-select-option>
        </a-select>
        <a-select v-model:value="searchWageType" placeholder="工资类型" allow-clear style="width: 140px" @change="handleSearch">
          <a-select-option value="PIECE">计件</a-select-option>
          <a-select-option value="TIMED">计时</a-select-option>
          <a-select-option value="OTHER">其他</a-select-option>
        </a-select>
        <a-select v-model:value="searchStatus" placeholder="状态" allow-clear style="width: 140px" @change="handleSearch">
          <a-select-option :value="0">待审核</a-select-option>
          <a-select-option :value="1">已审核</a-select-option>
          <a-select-option :value="2">已发放</a-select-option>
        </a-select>
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="resetSearch">重置</a-button>
      </a-space>
      <a-button :loading="exportLoading" @click="exportExcel">导出Excel</a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="dataList"
      :loading="loading"
      :pagination="pagination"
      row-key="employeeId"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'employee'">
          {{ record.employeeNo }} {{ record.employeeName }}
        </template>
        <template v-else-if="column.key === 'pieceWage'">
          {{ money(record.pieceWage) }}
        </template>
        <template v-else-if="column.key === 'timedWage'">
          {{ money(record.timedWage) }}
        </template>
        <template v-else-if="column.key === 'otherWage'">
          <a @click="openOtherDrawer(record)">{{ money(record.otherWage) }}</a>
        </template>
        <template v-else-if="column.key === 'totalWage'">
          <strong>{{ money(record.totalWage) }}</strong>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusTagMap[record.status]?.color">
            {{ statusTagMap[record.status]?.text || record.status }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a @click="openDetail(record)">查看明细</a>
            <a-popconfirm v-if="record.status !== 2" title="确定审核通过吗？" @confirm="handleAudit(record)">
              <a>审核</a>
            </a-popconfirm>
            <a-popconfirm v-if="record.status === 1" title="确定标记为已发放吗？" @confirm="handleMarkPaid(record)">
              <a>标记发放</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-drawer v-model:open="detailVisible" title="工资明细" :width="900">
      <a-tabs>
        <a-tab-pane key="piece" tab="计件明细">
          <a-table :columns="pieceColumns" :data-source="wageDetail.pieceDetails" :pagination="false" row-key="id" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'reportDate'">{{ formatDate(record.reportTime) }}</template>
              <template v-else-if="column.key === 'amount'">{{ money(record.amount) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="timed" tab="计时明细">
          <a-table :columns="timedColumns" :data-source="wageDetail.timedDetails" :pagination="false" row-key="id" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">{{ money(record.amount) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="other" tab="其他项目">
          <a-table :columns="otherColumnsReadonly" :data-source="wageDetail.otherItems" :pagination="false" row-key="id" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">{{ money(record.amount) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-drawer>

    <a-drawer v-model:open="otherDrawerVisible" title="其他工资项目" :width="760">
      <a-alert v-if="currentRecord?.status === 2" type="warning" show-icon message="已发放工资不可修改其他项目" class="drawer-alert" />
      <div class="other-context">
        <span>{{ currentRecord?.employeeNo }} {{ currentRecord?.employeeName }}</span>
        <a-divider type="vertical" />
        <span>{{ currentRecord?.yearMonth }}</span>
      </div>

      <a-form layout="vertical" :model="otherForm" class="other-form">
        <a-form-item label="项目名称">
          <a-input v-model:value="otherForm.itemName" placeholder="如：全勤奖、迟到扣款" :disabled="currentRecord?.status === 2" />
        </a-form-item>
        <a-form-item label="金额">
          <a-input-number v-model:value="otherForm.amount" :precision="2" placeholder="可为负数" class="full-input" :disabled="currentRecord?.status === 2" />
        </a-form-item>
        <a-form-item label="备注" class="remark-item">
          <a-input v-model:value="otherForm.remark" placeholder="备注" :disabled="currentRecord?.status === 2" />
        </a-form-item>
        <a-form-item label=" " class="other-actions">
          <a-button type="primary" :loading="otherSubmitting" :disabled="currentRecord?.status === 2" @click="handleSaveOther">
            {{ editingOtherId ? '保存' : '添加' }}
          </a-button>
          <a-button v-if="editingOtherId" :disabled="otherSubmitting" @click="resetOtherForm">取消编辑</a-button>
        </a-form-item>
      </a-form>

      <a-table class="other-table" :columns="otherColumns" :data-source="otherItems" :loading="otherLoading" :pagination="false" row-key="id" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">{{ money(record.amount) }}</template>
          <template v-else-if="column.key === 'action'">
            <a-space v-if="currentRecord?.status !== 2">
              <a @click="editOther(record)">编辑</a>
              <a-popconfirm title="确定删除该项目吗？" @confirm="deleteOther(record.id)">
                <a style="color: #ff4d4f">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-drawer>
  </a-card>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { wageManageApi } from '../../api/admin'
import { employeeApi } from '../../api/system'

type EmployeeOption = {
  id: number
  employeeNo: string
  name: string
}

const loading = ref(false)
const detailVisible = ref(false)
const otherDrawerVisible = ref(false)
const otherLoading = ref(false)
const otherSubmitting = ref(false)
const exportLoading = ref(false)
const dataList = ref<any[]>([])
const employeeOptions = ref<EmployeeOption[]>([])
const otherItems = ref<any[]>([])
const currentRecord = ref<any>(null)
const editingOtherId = ref<number | null>(null)

const searchYearMonth = ref<string>(dayjs().format('YYYY-MM'))
const searchEmployeeId = ref<number | undefined>(undefined)
const searchWageType = ref<string | undefined>(undefined)
const searchStatus = ref<number | undefined>(undefined)

const statusTagMap: Record<number, { text: string; color: string }> = {
  0: { text: '待审核', color: 'default' },
  1: { text: '已审核', color: 'blue' },
  2: { text: '已发放', color: 'green' },
}

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const wageDetail = reactive({
  pieceDetails: [] as any[],
  timedDetails: [] as any[],
  otherItems: [] as any[],
})

const otherForm = reactive({
  itemName: '',
  amount: undefined as number | undefined,
  remark: '',
})

const columns = [
  { title: '工人', key: 'employee', width: 160 },
  { title: '月份', dataIndex: 'yearMonth', key: 'yearMonth', width: 100 },
  { title: '计件件数', dataIndex: 'pieceQty', key: 'pieceQty', width: 100 },
  { title: '计件工资', dataIndex: 'pieceWage', key: 'pieceWage', width: 110 },
  { title: '计时工时', dataIndex: 'timedHours', key: 'timedHours', width: 100 },
  { title: '计时工资', dataIndex: 'timedWage', key: 'timedWage', width: 110 },
  { title: '其他项目', dataIndex: 'otherWage', key: 'otherWage', width: 110 },
  { title: '应发合计', dataIndex: 'totalWage', key: 'totalWage', width: 120 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 90 },
  { title: '操作', key: 'action', width: 230 },
]

const pieceColumns = [
  { title: '日期', key: 'reportDate', width: 110 },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '扎号', dataIndex: 'bundleNo', key: 'bundleNo' },
  { title: '工序', dataIndex: 'processName', key: 'processName' },
  { title: '件数', dataIndex: 'qty', key: 'qty' },
  { title: '工价', dataIndex: 'price', key: 'price' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
]

const timedColumns = [
  { title: '日期', dataIndex: 'workDate', key: 'workDate' },
  { title: '工时', dataIndex: 'hours', key: 'hours' },
  { title: '时薪', dataIndex: 'hourlyRate', key: 'hourlyRate' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '工作内容', dataIndex: 'workContent', key: 'workContent' },
]

const otherColumnsReadonly = [
  { title: '项目名称', dataIndex: 'itemName', key: 'itemName' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
]

const otherColumns = [
  ...otherColumnsReadonly,
  { title: '录入时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 120 },
]

function formatEmployee(emp: EmployeeOption) {
  return `${emp.employeeNo || ''} ${emp.name || ''}`.trim()
}

function filterEmployeeOption(input: string, option: any) {
  const text = String(option?.children?.[0]?.children || option?.children || '').toLowerCase()
  return text.includes(input.toLowerCase())
}

function money(value: any) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function formatDate(value: string) {
  return value ? dayjs(value).format('YYYY-MM-DD') : ''
}

function compactYearMonth(value: string) {
  return value ? value.replace('-', '') : ''
}

async function fetchEmployees() {
  const res: any = await employeeApi.list({ page: 1, size: 1000 })
  employeeOptions.value = res.data?.records || []
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      size: pagination.pageSize,
      yearMonth: searchYearMonth.value,
    }
    if (searchEmployeeId.value) params.employeeId = searchEmployeeId.value
    if (searchWageType.value) params.wageType = searchWageType.value
    if (searchStatus.value !== undefined) params.status = searchStatus.value
    const res: any = await wageManageApi.list(params)
    dataList.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function resetSearch() {
  searchYearMonth.value = dayjs().format('YYYY-MM')
  searchEmployeeId.value = undefined
  searchWageType.value = undefined
  searchStatus.value = undefined
  handleSearch()
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

async function handleAudit(record: any) {
  await wageManageApi.audit(record.employeeId, record.yearMonth)
  message.success('审核成功')
  fetchData()
}

async function handleMarkPaid(record: any) {
  await wageManageApi.markPaid(record.employeeId, record.yearMonth)
  message.success('已标记为已发放')
  fetchData()
}

async function openDetail(record: any) {
  currentRecord.value = record
  const res: any = await wageManageApi.detail(record.employeeId, record.yearMonth)
  wageDetail.pieceDetails = res.data?.pieceDetails || []
  wageDetail.timedDetails = res.data?.timedDetails || []
  wageDetail.otherItems = res.data?.otherItems || []
  detailVisible.value = true
}

async function openOtherDrawer(record: any) {
  currentRecord.value = record
  resetOtherForm()
  otherDrawerVisible.value = true
  await fetchOtherItems()
}

async function fetchOtherItems() {
  otherLoading.value = true
  try {
    const res: any = await wageManageApi.getOtherItems(currentRecord.value.employeeId, currentRecord.value.yearMonth)
    otherItems.value = res.data || []
  } finally {
    otherLoading.value = false
  }
}

function resetOtherForm() {
  editingOtherId.value = null
  otherForm.itemName = ''
  otherForm.amount = undefined
  otherForm.remark = ''
}

function editOther(record: any) {
  editingOtherId.value = record.id
  otherForm.itemName = record.itemName
  otherForm.amount = Number(record.amount)
  otherForm.remark = record.remark || ''
}

async function handleSaveOther() {
  if (!otherForm.itemName || otherForm.amount === undefined) {
    message.warning('请填写项目名称和金额')
    return
  }
  otherSubmitting.value = true
  try {
    const payload = {
      employeeId: currentRecord.value.employeeId,
      yearMonth: currentRecord.value.yearMonth,
      itemName: otherForm.itemName,
      amount: otherForm.amount,
      remark: otherForm.remark,
    }
    if (editingOtherId.value) {
      await wageManageApi.updateOtherItem(editingOtherId.value, payload)
      message.success('保存成功')
    } else {
      await wageManageApi.addOtherItem(payload)
      message.success('添加成功')
    }
    resetOtherForm()
    await fetchOtherItems()
    fetchData()
  } finally {
    otherSubmitting.value = false
  }
}

async function deleteOther(id: number) {
  await wageManageApi.deleteOtherItem(id)
  message.success('删除成功')
  await fetchOtherItems()
  fetchData()
}

function buildExportParams() {
  const params: any = {
    yearMonth: searchYearMonth.value,
  }
  if (searchEmployeeId.value) params.employeeId = searchEmployeeId.value
  if (searchWageType.value) params.wageType = searchWageType.value
  if (searchStatus.value !== undefined) params.status = searchStatus.value
  return params
}

function getDownloadFilename(disposition: string | undefined) {
  if (!disposition) {
    return `工资汇总-${compactYearMonth(searchYearMonth.value)}.xlsx`
  }
  const encodedMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (encodedMatch?.[1]) {
    return decodeURIComponent(encodedMatch[1])
  }
  const filenameMatch = disposition.match(/filename="?([^"]+)"?/i)
  return filenameMatch?.[1] || `工资汇总-${compactYearMonth(searchYearMonth.value)}.xlsx`
}

function downloadBlob(blob: Blob, filename: string) {
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}

async function exportExcel() {
  if (!pagination.total && !dataList.value.length) {
    message.warning('暂无可导出的工资数据')
    return
  }

  exportLoading.value = true
  try {
    const response: any = await wageManageApi.export(buildExportParams())
    const filename = getDownloadFilename(response.headers?.['content-disposition'])
    downloadBlob(response.data, filename)
  } catch (error) {
    console.error('导出工资 Excel 失败', error)
    message.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

onMounted(async () => {
  await fetchEmployees()
  fetchData()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.drawer-alert {
  margin-bottom: 12px;
}

.other-context {
  display: flex;
  align-items: center;
  min-height: 32px;
  margin-bottom: 14px;
  color: #4b5563;
  font-size: 14px;
}

.other-form {
  display: grid;
  grid-template-columns: minmax(220px, 1.3fr) minmax(140px, 0.7fr);
  gap: 10px 16px;
  padding: 14px 16px 4px;
  margin-bottom: 18px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
}

.other-form :deep(.ant-form-item) {
  margin-bottom: 12px;
}

.full-input {
  width: 100%;
}

.remark-item {
  grid-column: 1 / 2;
}

.other-actions {
  grid-column: 2 / 3;
}

.other-actions :deep(.ant-form-item-control-input-content) {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.other-table {
  margin-top: 4px;
}

@media (max-width: 760px) {
  .other-form {
    grid-template-columns: 1fr;
  }

  .remark-item,
  .other-actions {
    grid-column: auto;
  }

  .other-actions :deep(.ant-form-item-control-input-content) {
    justify-content: flex-start;
  }
}
</style>
