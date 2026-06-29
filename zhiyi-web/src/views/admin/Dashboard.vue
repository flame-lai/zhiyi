<template>
  <div class="dashboard-page">
    <div class="page-actions">
      <a-button @click="refreshAll">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </div>

    <a-row :gutter="12" class="summary-row">
      <a-col :xs="12" :md="8" :xl="4">
        <a-card>
          <a-statistic title="生产订单" :value="overview.orderCount" />
        </a-card>
      </a-col>
      <a-col :xs="12" :md="8" :xl="4">
        <a-card>
          <a-statistic title="总扎数" :value="overview.bundleCount" />
        </a-card>
      </a-col>
      <a-col :xs="12" :md="8" :xl="4">
        <a-card>
          <a-statistic title="订单件数" :value="overview.orderQty" />
        </a-card>
      </a-col>
      <a-col :xs="12" :md="8" :xl="4">
        <a-card>
          <a-statistic title="平均进度" :value="percentValue(overview.averageCompletionRate)" suffix="%" />
        </a-card>
      </a-col>
      <a-col :xs="12" :md="8" :xl="4">
        <a-card>
          <a-statistic title="今日报工件数" :value="overview.todayReportQty" />
        </a-card>
      </a-col>
      <a-col :xs="12" :md="8" :xl="4">
        <a-card>
          <a-statistic title="今日计件工资" :value="Number(overview.todayReportAmount || 0)" :precision="2" prefix="¥" />
        </a-card>
      </a-col>
    </a-row>

    <a-card class="section-card dashboard-tabs-card">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="orders" tab="订单进度">
          <div class="toolbar">
            <a-space wrap>
              <a-input
                v-model:value="orderSearch.keyword"
                placeholder="订单号/款号"
                allow-clear
                style="width: 180px"
                @pressEnter="handleOrderSearch"
              />
              <a-select v-model:value="orderSearch.status" placeholder="订单状态" allow-clear style="width: 150px" @change="handleOrderSearch">
                <a-select-option :value="0">待确认</a-select-option>
                <a-select-option :value="1">已确认</a-select-option>
                <a-select-option :value="2">生产中</a-select-option>
                <a-select-option :value="3">部分出货</a-select-option>
                <a-select-option :value="4">已完工</a-select-option>
                <a-select-option :value="5">已出货</a-select-option>
                <a-select-option :value="6">已结算</a-select-option>
              </a-select>
              <a-button type="primary" @click="handleOrderSearch">查询</a-button>
              <a-button @click="resetOrderSearch">重置</a-button>
            </a-space>
          </div>
          <a-table
            :columns="orderColumns"
            :data-source="orderData"
            :loading="orderLoading"
            :pagination="orderPagination"
            row-key="orderId"
            @change="handleOrderTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'order'">
                <div class="primary-cell">{{ record.orderNo }}</div>
                <div class="secondary-cell">{{ record.customerName || '-' }}</div>
              </template>
              <template v-else-if="column.key === 'style'">
                <div>{{ record.styleCode || '-' }}</div>
                <div class="secondary-cell">{{ record.styleName || '' }}</div>
              </template>
              <template v-else-if="column.key === 'completionRate'">
                <a-progress :percent="percentValue(record.completionRate)" size="small" :status="record.completionRate >= 1 ? 'success' : 'active'" />
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="orderStatusMap[record.status]?.color">{{ orderStatusMap[record.status]?.text || record.status }}</a-tag>
              </template>
            </template>
            <template #expandedRowRender="{ record }">
              <a-table
                :columns="processColumns"
                :data-source="record.processProgress || []"
                :pagination="false"
                row-key="processId"
                size="small"
              >
                <template #bodyCell="{ column, record: process }">
                  <template v-if="column.key === 'completionRate'">
                    <a-progress :percent="percentValue(process.completionRate)" size="small" :status="process.completionRate >= 1 ? 'success' : 'active'" />
                  </template>
                </template>
              </a-table>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="workers" tab="工人产能">
          <div class="toolbar">
            <a-space wrap>
              <a-range-picker
                v-model:value="workerDateRange"
                format="YYYY-MM-DD"
                valueFormat="YYYY-MM-DD"
                style="width: 240px"
                :placeholder="['开始日期', '结束日期']"
                @change="handleWorkerSearch"
              />
              <a-input
                v-model:value="workerSearch.keyword"
                placeholder="工号/姓名"
                allow-clear
                style="width: 180px"
                @pressEnter="handleWorkerSearch"
              />
              <a-button @click="handleWorkerThisMonth">本月</a-button>
              <a-button type="primary" @click="handleWorkerSearch">查询</a-button>
              <a-button @click="resetWorkerSearch">重置</a-button>
            </a-space>
            <div class="worker-total">
              <span>合计件数：{{ workerSummary.totalQty }}</span>
              <span>合计工资：{{ money(workerSummary.totalAmount) }}</span>
            </div>
          </div>
          <a-table
            :columns="workerColumns"
            :data-source="workerData"
            :loading="workerLoading"
            :pagination="workerPagination"
            row-key="employeeId"
            @change="handleWorkerTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'worker'">
                <div class="primary-cell">{{ record.employeeName || '-' }}</div>
                <div class="secondary-cell">{{ record.employeeNo || '' }}</div>
              </template>
              <template v-else-if="column.key === 'totalAmount'">{{ money(record.totalAmount) }}</template>
              <template v-else-if="column.key === 'avgPrice'">{{ money(record.avgPrice) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ReloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { onMounted, reactive, ref } from 'vue'
import { dashboardApi } from '../../api/admin'

const orderLoading = ref(false)
const workerLoading = ref(false)
const activeTab = ref('orders')
const orderData = ref<any[]>([])
const workerData = ref<any[]>([])
const workerDateRange = ref<[string, string] | null>([dayjs().format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])

const overview = reactive({
  orderCount: 0,
  bundleCount: 0,
  orderQty: 0,
  averageCompletionRate: 0,
  todayReportQty: 0,
  todayReportAmount: 0,
})

const orderSearch = reactive({
  keyword: '',
  status: undefined as number | undefined,
})

const workerSearch = reactive({
  keyword: '',
})

const workerSummary = reactive({
  totalQty: 0,
  totalAmount: 0,
})

const orderPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const workerPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const orderStatusMap: Record<number, { text: string; color: string }> = {
  0: { text: '待确认', color: 'default' },
  1: { text: '已确认', color: 'blue' },
  2: { text: '生产中', color: 'processing' },
  3: { text: '部分出货', color: 'warning' },
  4: { text: '已完工', color: 'green' },
  5: { text: '已出货', color: 'cyan' },
  6: { text: '已结算', color: 'purple' },
}

const orderColumns = [
  { title: '订单', key: 'order', width: 180 },
  { title: '款号', key: 'style', width: 160 },
  { title: '颜色', dataIndex: 'colorSummary', key: 'colorSummary', width: 110 },
  { title: '尺码', dataIndex: 'sizeSummary', key: 'sizeSummary', width: 110 },
  { title: '交期', dataIndex: 'deliveryDate', key: 'deliveryDate', width: 110 },
  { title: '扎数', dataIndex: 'totalBundles', key: 'totalBundles', width: 80 },
  { title: '件数', dataIndex: 'totalQty', key: 'totalQty', width: 90 },
  { title: '完成件数', dataIndex: 'completedQty', key: 'completedQty', width: 100 },
  {
    title: '整体进度',
    dataIndex: 'completionRate',
    key: 'completionRate',
    width: 180,
    sorter: (a: any, b: any) => Number(a.completionRate || 0) - Number(b.completionRate || 0),
  },
  { title: '瓶颈工序', dataIndex: 'bottleneckProcess', key: 'bottleneckProcess', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
]

const processColumns = [
  { title: '工序', dataIndex: 'processName', key: 'processName' },
  { title: '完成件数', dataIndex: 'completedQty', key: 'completedQty', width: 120 },
  { title: '计划件数', dataIndex: 'plannedQty', key: 'plannedQty', width: 120 },
  { title: '完成率', dataIndex: 'completionRate', key: 'completionRate', width: 220 },
]

const workerColumns = [
  { title: '工人', key: 'worker', width: 180 },
  { title: '报工次数', dataIndex: 'reportCount', key: 'reportCount', width: 100 },
  { title: '完成件数', dataIndex: 'totalQty', key: 'totalQty', width: 110 },
  { title: '计件工资', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 },
  { title: '平均单价', dataIndex: 'avgPrice', key: 'avgPrice', width: 120 },
]

function percentValue(value: any) {
  return Number((Number(value || 0) * 100).toFixed(1))
}

function money(value: any) {
  return `¥${Number(value || 0).toFixed(2)}`
}

async function fetchOverview() {
  const res: any = await dashboardApi.overview({ date: dayjs().format('YYYY-MM-DD') })
  Object.assign(overview, res.data || {})
}

async function fetchOrders() {
  orderLoading.value = true
  try {
    const params: any = {
      page: orderPagination.current,
      size: orderPagination.pageSize,
    }
    if (orderSearch.keyword) params.keyword = orderSearch.keyword
    if (orderSearch.status !== undefined) params.status = orderSearch.status
    const res: any = await dashboardApi.orders(params)
    orderData.value = res.data?.records || []
    orderPagination.total = res.data?.total || 0
  } finally {
    orderLoading.value = false
  }
}

async function fetchWorkers() {
  workerLoading.value = true
  try {
    const params: any = {
      page: workerPagination.current,
      size: workerPagination.pageSize,
      startDate: workerDateRange.value?.[0] || dayjs().format('YYYY-MM-DD'),
      endDate: workerDateRange.value?.[1] || dayjs().format('YYYY-MM-DD'),
    }
    if (workerSearch.keyword) params.keyword = workerSearch.keyword
    const res: any = await dashboardApi.workers(params)
    workerData.value = res.data?.records || []
    workerPagination.total = res.data?.total || 0
    workerSummary.totalQty = res.data?.totalQty || 0
    workerSummary.totalAmount = res.data?.totalAmount || 0
  } finally {
    workerLoading.value = false
  }
}

function handleOrderSearch() {
  orderPagination.current = 1
  fetchOrders()
}

function resetOrderSearch() {
  orderSearch.keyword = ''
  orderSearch.status = undefined
  handleOrderSearch()
}

function handleWorkerSearch() {
  workerPagination.current = 1
  fetchWorkers()
}

function resetWorkerSearch() {
  workerSearch.keyword = ''
  workerDateRange.value = [dayjs().format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]
  handleWorkerSearch()
}

function handleWorkerThisMonth() {
  workerDateRange.value = [dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().endOf('month').format('YYYY-MM-DD')]
  handleWorkerSearch()
}

function refreshAll() {
  fetchOverview()
  fetchOrders()
  fetchWorkers()
}

function handleOrderTableChange(pag: any) {
  orderPagination.current = pag.current
  orderPagination.pageSize = pag.pageSize
  fetchOrders()
}

function handleWorkerTableChange(pag: any) {
  workerPagination.current = pag.current
  workerPagination.pageSize = pag.pageSize
  fetchWorkers()
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-actions {
  display: flex;
  justify-content: flex-end;
}

.summary-row {
  row-gap: 12px;
}

.section-card {
  border-radius: 6px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.worker-total {
  display: flex;
  gap: 16px;
  min-height: 32px;
  align-items: center;
  color: #4b5563;
  white-space: nowrap;
}

.primary-cell {
  font-weight: 500;
  color: #111827;
}

.secondary-cell {
  margin-top: 2px;
  color: #6b7280;
  font-size: 12px;
}

@media (max-width: 900px) {
  .toolbar {
    flex-direction: column;
  }

  .worker-total {
    flex-wrap: wrap;
  }
}
</style>
