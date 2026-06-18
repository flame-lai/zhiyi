<template>
  <div>
    <a-card title="裁床计划管理">
      <!-- 搜索区域 -->
      <div style="margin-bottom: 16px; display: flex; gap: 16px; flex-wrap: wrap">
        <a-input
          v-model:value="searchParams.orderNo"
          placeholder="订单号"
          style="width: 150px"
          allow-clear
        />
        <a-input
          v-model:value="searchParams.styleCode"
          placeholder="款号"
          style="width: 120px"
          allow-clear
        />
        <a-select
          v-model:value="searchParams.status"
          placeholder="状态"
          style="width: 120px"
          allow-clear
        >
          <a-select-option :value="0">待裁剪</a-select-option>
          <a-select-option :value="1">已裁剪</a-select-option>
          <a-select-option :value="2">已开菲</a-select-option>
          <a-select-option :value="3">已报工</a-select-option>
        </a-select>
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button type="primary" @click="handleCreate">新增裁床计划</a-button>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
        bordered
        :scroll="{ x: 1800 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="statusColorMap[record.status]">
              {{ statusTextMap[record.status] }}
            </a-tag>
          </template>
          <template v-if="column.dataIndex === 'barcode'">
            <a-tooltip v-if="record.barcode" :title="record.barcode">
              <span>{{ record.barcode }}</span>
            </a-tooltip>
          </template>
          <template v-if="column.dataIndex === 'action'">
            <a-space>
              <a v-if="record.status < 2" @click="handleEdit(record)">编辑</a>
              <a v-else @click="handleEdit(record)">查看</a>
              <a-popconfirm v-if="record.status < 2" title="确定删除？" @confirm="handleDelete(record.id)">
                <a style="color: #ff4d4f">删除</a>
              </a-popconfirm>
              <a v-if="record.status < 2" @click="openActualQtyModal(record, false)">录入实际数</a>
              <a v-else @click="openActualQtyModal(record, true)">查看实际数</a>
              <a v-if="record.status < 2" @click="handleOpenBundle(record)">开菲</a>
              <a @click="openBundleModal(record)">查看扎号</a>
              <a v-if="record.status >= 2" @click="openPrintModal(record)">打印菲仔</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 录入/查看实际数弹窗 -->
    <a-modal
      v-model:open="actualQtyModalVisible"
      :title="actualQtyReadonly ? '实际数量明细' : '录入实际数量'"
      :footer="actualQtyReadonly ? null : undefined"
      @ok="handleActualQtySubmit"
      :confirm-loading="submitLoading"
      width="700px"
      :body-style="{ padding: '16px 24px' }"
    >
      <div class="actual-qty-header">
        <div class="header-field">
          <span class="header-label">裁床日期</span>
          <a-date-picker
            v-model:value="actualCuttingDate"
            size="middle"
            style="width: 200px"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled="actualQtyReadonly"
            placeholder="选择裁床日期"
          />
        </div>
        <div class="header-summary" v-if="actualQtyData.length > 0">
          <span class="summary-item">
            <span class="summary-label">总计划数</span>
            <span class="summary-value">{{ actualQtyData.reduce((sum: number, d: any) => sum + (d.planQty || 0), 0) }}</span>
          </span>
          <span class="summary-divider">|</span>
          <span class="summary-item">
            <span class="summary-label">总实际数</span>
            <span class="summary-value highlight">{{ actualQtyData.reduce((sum: number, d: any) => sum + (d.actualQty || 0), 0) }}</span>
          </span>
        </div>
      </div>
      <div class="actual-qty-table">
        <a-table
          :columns="actualQtyColumns"
          :data-source="actualQtyData"
          :pagination="false"
          size="middle"
          bordered
          :scroll="{ y: 350 }"
          :row-class-name="(_record: any, index: number) => index % 2 === 0 ? 'table-row-even' : 'table-row-odd'"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'actualQty'">
              <a-input-number
                v-model:value="record.actualQty"
                :min="0"
                size="middle"
                style="width: 100%"
                :disabled="actualQtyReadonly"
                :class="{ 'input-highlight': !actualQtyReadonly }"
              />
            </template>
            <template v-if="column.dataIndex === 'planQty'">
              <span class="plan-qty-cell">{{ record.planQty || 0 }}</span>
            </template>
          </template>
        </a-table>
      </div>
    </a-modal>

    <!-- 查看扎号弹窗 -->
    <a-modal
      v-model:open="bundleListVisible"
      title="扎号列表"
      :footer="null"
      width="800px"
    >
      <a-table
        :columns="bundleColumns"
        :data-source="bundleList"
        :loading="bundleLoading"
        row-key="id"
        :pagination="false"
        bordered
      >
      </a-table>
    </a-modal>

    <!-- 打印菲仔弹窗 -->
    <BundlePrint
      v-model:visible="printModalVisible"
      :bundles="printBundles"
      :cutting-barcode="printCuttingBarcode"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { cuttingPlanApi } from '../../api/cutting'
import BundlePrint from '../../components/BundlePrint.vue'

const router = useRouter()

// 表格列定义
const columns = [
  { title: '订单号', dataIndex: 'orderNo', width: 140 },
  { title: '款号', dataIndex: 'styleCode', width: 100 },
  { title: '面料', dataIndex: 'fabric', width: 100 },
  { title: '床次', dataIndex: 'bedNo', width: 70 },
  { title: '层数', dataIndex: 'layers', width: 70 },
  { title: '总计划数', dataIndex: 'planQty', width: 90 },
  { title: '总实际数', dataIndex: 'actualQty', width: 90 },
  { title: '总扎数', dataIndex: 'bundleCount', width: 80 },
  { title: '裁床码', dataIndex: 'barcode', width: 160 },
  { title: '裁床日期', dataIndex: 'cuttingDate', width: 110 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '操作', dataIndex: 'action', width: 280, fixed: 'right' },
]

const actualQtyColumns = [
  { title: '部位', dataIndex: 'part', width: 120, align: 'center' },
  { title: '颜色', dataIndex: 'color', width: 100, align: 'center' },
  { title: '尺码', dataIndex: 'size', width: 80, align: 'center' },
  { title: '计划成衣件数', dataIndex: 'planQty', width: 120, align: 'center' },
  { title: '实际成衣件数', dataIndex: 'actualQty', width: 140, align: 'center' },
]

const bundleColumns = [
  { title: '扎号', dataIndex: 'bundleNo', width: 100 },
  { title: '部位', dataIndex: 'part', width: 120 },
  { title: '颜色', dataIndex: 'color', width: 80 },
  { title: '尺码', dataIndex: 'size', width: 70 },
  { title: '件数', dataIndex: 'qty', width: 70 },
]

// 状态映射
const statusColorMap: Record<number, string> = {
  0: 'default', 1: 'processing', 2: 'success', 3: 'success',
}
const statusTextMap: Record<number, string> = {
  0: '待裁剪', 1: '已裁剪', 2: '已开菲', 3: '已报工',
}

// 查询参数
const searchParams = reactive({
  orderNo: '',
  styleCode: '',
  status: undefined as number | undefined,
})

// 表格数据
const tableData = ref<any[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

// 录入实际数相关
const submitLoading = ref(false)
const actualQtyModalVisible = ref(false)
const actualQtyReadonly = ref(false)
const actualQtyData = ref<any[]>([])
const currentPlanId = ref<number | null>(null)
const actualCuttingDate = ref<string | undefined>(undefined)

// 扎号列表相关
const bundleListVisible = ref(false)
const bundleLoading = ref(false)
const bundleList = ref<any[]>([])

// 打印菲仔相关
const printModalVisible = ref(false)
const printBundles = ref<any[]>([])
const printCuttingBarcode = ref('')

// 加载表格数据
async function loadData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      size: pagination.pageSize,
    }
    if (searchParams.orderNo) params.orderNo = searchParams.orderNo
    if (searchParams.styleCode) params.styleCode = searchParams.styleCode
    if (searchParams.status !== undefined) params.status = searchParams.status
    const res: any = await cuttingPlanApi.list(params)
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {} finally {
    loading.value = false
  }
}

// 查询
function handleSearch() {
  pagination.current = 1
  loadData()
}

// 重置
function handleReset() {
  searchParams.orderNo = ''
  searchParams.styleCode = ''
  searchParams.status = undefined
  pagination.current = 1
  loadData()
}

// 表格分页变化
function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

// 新增
function handleCreate() {
  router.push('/cutting/plans/create')
}

// 编辑
function handleEdit(record: any) {
  router.push(`/cutting/plans/edit/${record.id}`)
}

// 删除
async function handleDelete(id: number) {
  try {
    await cuttingPlanApi.delete(id)
    message.success('删除成功')
    loadData()
  } catch {}
}

// 打开录入/查看实际数弹窗
async function openActualQtyModal(record: any, readonly: boolean = false) {
  currentPlanId.value = record.id
  actualQtyReadonly.value = readonly
  actualCuttingDate.value = record.cuttingDate || undefined
  try {
    const res = await cuttingPlanApi.getDetails(record.id)
    actualQtyData.value = (res.data || []).map((d: any) => ({
      ...d,
      actualQty: d.actualQty || d.planQty,
    }))
    actualQtyModalVisible.value = true
  } catch {}
}

// 提交实际数量
async function handleActualQtySubmit() {
  if (!currentPlanId.value) return
  submitLoading.value = true
  try {
    await cuttingPlanApi.inputActualQty(currentPlanId.value, {
      details: actualQtyData.value,
      cuttingDate: actualCuttingDate.value,
    })
    message.success('录入成功')
    actualQtyModalVisible.value = false
    loadData()
  } catch {} finally {
    submitLoading.value = false
  }
}

// 开菲
async function handleOpenBundle(record: any) {
  try {
    const res = await cuttingPlanApi.openBundle(record.id)
    message.success(`开菲成功，生成 ${(res.data || []).length} 个扎号`)
    loadData()
  } catch {}
}

// 查看扎号
async function openBundleModal(record: any) {
  bundleList.value = []
  bundleLoading.value = true
  bundleListVisible.value = true
  try {
    const res = await cuttingPlanApi.getBundles(record.id)
    bundleList.value = res.data || []
  } catch {} finally {
    bundleLoading.value = false
  }
}

// 打印菲仔
async function openPrintModal(record: any) {
  try {
    const res = await cuttingPlanApi.getBundleDetails(record.id)
    const bundles = res.data || []
    if (bundles.length === 0) {
      message.warning('暂无扎号数据')
      return
    }
    // 使用裁床码（从裁床计划获取）
    printCuttingBarcode.value = record.barcode || ''
    printBundles.value = bundles.map((b: any) => ({
      id: b.id,
      orderNo: b.orderNo,
      styleCode: b.styleCode,
      color: b.color,
      size: b.size,
      part: b.part,
      fabric: b.fabric,
      bundleNo: b.bundleNo,
      globalBundleNo: b.globalBundleNo,
      qty: b.qty,
      cuttingPieces: b.cuttingPieces,
      bedNo: b.bedNo,
    }))
    printModalVisible.value = true
  } catch (e) {
    message.error('获取扎号数据失败')
  }
}

// 初始化加载
loadData()
</script>

<style scoped>
/* 录入实际数弹窗样式 */
.actual-qty-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f6f8fa;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.header-field {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-label {
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.header-summary {
  display: flex;
  align-items: center;
  gap: 16px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.summary-label {
  font-size: 12px;
  color: #8c8c8c;
}

.summary-value {
  font-size: 18px;
  font-weight: 700;
  color: #333;
}

.summary-value.highlight {
  color: #1890ff;
}

.summary-divider {
  color: #e8e8e8;
  font-size: 18px;
}

.actual-qty-table {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
  max-height: 400px;
  overflow-y: auto;
}

.actual-qty-table :deep(.ant-table) {
  border-radius: 8px;
}

.actual-qty-table :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 600;
  font-size: 13px;
  color: #595959;
  padding: 12px 16px;
  text-align: center;
  position: sticky;
  top: 0;
  z-index: 10;
}

.actual-qty-table :deep(.ant-table-tbody > tr > td) {
  padding: 10px 16px;
  text-align: center;
}

.actual-qty-table :deep(.table-row-even) {
  background: #fff;
}

.actual-qty-table :deep(.table-row-odd) {
  background: #fafafa;
}

.actual-qty-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #e6f7ff !important;
}

.plan-qty-cell {
  font-weight: 500;
  color: #595959;
}

.input-highlight :deep(.ant-input-number) {
  border-color: #1890ff;
}

.input-highlight :deep(.ant-input-number:focus) {
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}
</style>
