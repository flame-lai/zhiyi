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
        <a-input
          v-model:value="searchParams.color"
          placeholder="颜色"
          style="width: 100px"
          allow-clear
        />
        <a-input
          v-model:value="searchParams.size"
          placeholder="尺码"
          style="width: 100px"
          allow-clear
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button type="primary" @click="openModal()">新增裁床计划</a-button>
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
        :scroll="{ x: 1500 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="statusColorMap[record.status]">
              {{ statusTextMap[record.status] || record.status }}
            </a-tag>
          </template>
          <template v-if="column.dataIndex === 'barcode'">
            <a-tooltip v-if="record.barcode" :title="record.barcode">
              <a-button type="link" size="small" @click="copyBarcode(record.barcode)">
                {{ record.barcode.substring(0, 15) }}...
              </a-button>
            </a-tooltip>
          </template>
          <template v-if="column.dataIndex === 'action'">
            <a-space>
              <a @click="openModal(record)">编辑</a>
              <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
                <a style="color: #ff4d4f">删除</a>
              </a-popconfirm>
              <a @click="openActualQtyModal(record)">录入实际数</a>
              <a @click="handleOpenBundle(record)" :disabled="record.status >= 2">开菲</a>
              <a @click="openBundleModal(record)">查看扎号</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="formModalVisible"
      :title="formTitle"
      @ok="handleFormSubmit"
      :confirm-loading="submitLoading"
      width="600px"
    >
      <a-form :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="订单" required>
          <a-select
            v-model:value="formData.orderId"
            placeholder="选择订单"
            show-search
            :filter-option="filterOrderOption"
            @change="onOrderChange"
          >
            <a-select-option v-for="order in orderList" :key="order.id" :value="order.id">
              {{ order.orderNo }} - {{ order.styleCode }} ({{ order.customerName }})
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="颜色" required>
          <a-select
            v-model:value="formData.color"
            placeholder="选择颜色"
          >
            <a-select-option v-for="color in availableColors" :key="color" :value="color">
              {{ color }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="尺码" required>
          <a-select
            v-model:value="formData.size"
            placeholder="选择尺码"
          >
            <a-select-option v-for="size in availableSizes" :key="size" :value="size">
              {{ size }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="床次" required>
          <a-input-number
            v-model:value="formData.bedNo"
            style="width: 100%"
            :min="1"
            :precision="0"
            placeholder="请输入床次"
          />
        </a-form-item>
        <a-form-item label="计划件数" required>
          <a-input-number
            v-model:value="formData.planQty"
            style="width: 100%"
            :min="1"
            :precision="0"
            placeholder="请输入计划件数"
          />
        </a-form-item>
        <a-form-item label="每扎件数" required>
          <a-input-number
            v-model:value="formData.qtyPerBundle"
            style="width: 100%"
            :min="1"
            :precision="0"
            placeholder="请输入每扎件数"
          />
        </a-form-item>
        <a-form-item label="裁床日期">
          <a-date-picker
            v-model:value="formData.cuttingDate"
            style="width: 100%"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 录入实际数弹窗 -->
    <a-modal
      v-model:open="actualQtyModalVisible"
      title="录入实际数量"
      @ok="handleActualQtySubmit"
      :confirm-loading="submitLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="计划件数">
          <a-input-number :value="actualQtyRecord?.planQty" style="width: 100%" disabled />
        </a-form-item>
        <a-form-item label="实际件数" required>
          <a-input-number
            v-model:value="actualQtyValue"
            style="width: 100%"
            :min="0"
            :precision="0"
            placeholder="请输入实际件数"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 开菲结果弹窗 -->
    <a-modal
      v-model:open="openBundleResultVisible"
      title="开菲结果"
      :footer="null"
    >
      <a-spin :spinning="openBundleLoading">
        <div v-if="openBundleResult.length > 0">
          <p>成功生成 {{ openBundleResult.length }} 个扎号：</p>
          <a-table
            :columns="[
              { title: '扎号', dataIndex: 'bundleNo', width: 100 },
              { title: '件数', dataIndex: 'qty', width: 80 }
            ]"
            :data-source="openBundleResult"
            :pagination="false"
            size="small"
            bordered
          />
        </div>
      </a-spin>
    </a-modal>

    <!-- 查看扎号弹窗 -->
    <a-modal
      v-model:open="bundleListVisible"
      title="扎号列表"
      :footer="null"
      width="700px"
    >
      <a-table
        :columns="bundleColumns"
        :data-source="bundleList"
        :loading="bundleLoading"
        row-key="id"
        :pagination="false"
        bordered
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { cuttingPlanApi } from '../../api/cutting'
import { orderApi } from '../../api/order'

// 订单列表
const orderList = ref<any[]>([])
const orderColorSizes = ref<any[]>([])

// 表格列定义
const columns = [
  { title: '订单号', dataIndex: 'orderNo', width: 140 },
  { title: '款号', dataIndex: 'styleCode', width: 100 },
  { title: '颜色', dataIndex: 'color', width: 80 },
  { title: '尺码', dataIndex: 'size', width: 70 },
  { title: '床次', dataIndex: 'bedNo', width: 70 },
  { title: '计划件数', dataIndex: 'planQty', width: 90 },
  { title: '实际件数', dataIndex: 'actualQty', width: 90 },
  { title: '每扎件数', dataIndex: 'qtyPerBundle', width: 90 },
  { title: '扎数', dataIndex: 'bundleCount', width: 70 },
  { title: '裁床条形码', dataIndex: 'barcode', width: 150 },
  { title: '裁床日期', dataIndex: 'cuttingDate', width: 110 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '操作', dataIndex: 'action', width: 280, fixed: 'right' },
]

const bundleColumns = [
  { title: '扎号', dataIndex: 'bundleNo', width: 100 },
  { title: '件数', dataIndex: 'qty', width: 80 },
  { title: '状态', dataIndex: 'status', width: 80 },
]

// 状态映射
const statusColorMap: Record<number, string> = {
  0: 'default',
  1: 'processing',
  2: 'success',
  3: 'success',
}

const statusTextMap: Record<number, string> = {
  0: '待裁剪',
  1: '已裁剪',
  2: '已开菲',
  3: '已报工',
}

// 查询参数
const searchParams = reactive({
  orderNo: '',
  styleCode: '',
  color: '',
  size: '',
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

// 表单相关
const formModalVisible = ref(false)
const formTitle = ref('新增裁床计划')
const submitLoading = ref(false)
const editingId = ref<number | null>(null)
const formData = reactive({
  orderId: undefined as number | undefined,
  color: undefined as string | undefined,
  size: undefined as string | undefined,
  bedNo: undefined as number | undefined,
  planQty: undefined as number | undefined,
  qtyPerBundle: undefined as number | undefined,
  cuttingDate: undefined as string | undefined,
})

// 可选颜色和尺码
const availableColors = computed(() => {
  if (!formData.orderId || orderColorSizes.value.length === 0) return []
  const colors = new Set<string>()
  orderColorSizes.value.forEach(item => {
    if (item.color) colors.add(item.color)
  })
  return Array.from(colors)
})

const availableSizes = computed(() => {
  if (!formData.orderId || !formData.color || orderColorSizes.value.length === 0) return []
  const sizes = new Set<string>()
  orderColorSizes.value
    .filter(item => item.color === formData.color)
    .forEach(item => {
      if (item.size) sizes.add(item.size)
    })
  return Array.from(sizes)
})

// 录入实际数相关
const actualQtyModalVisible = ref(false)
const actualQtyValue = ref<number | undefined>(undefined)
const actualQtyRecord = ref<any>(null)

// 开菲结果相关
const openBundleResultVisible = ref(false)
const openBundleLoading = ref(false)
const openBundleResult = ref<any[]>([])

// 扎号列表相关
const bundleListVisible = ref(false)
const bundleLoading = ref(false)
const bundleList = ref<any[]>([])

// 加载订单列表（只加载已确认的订单）
async function loadOrders() {
  try {
    const res: any = await orderApi.list({ page: 1, size: 9999, status: 1 })
    orderList.value = res.data?.records || []
  } catch {
    // error handled by interceptor
  }
}

// 订单选择变化
async function onOrderChange(orderId: number) {
  formData.color = undefined
  formData.size = undefined
  orderColorSizes.value = []
  if (orderId) {
    try {
      const res: any = await orderApi.getColorSizes(orderId)
      orderColorSizes.value = res.data || []
    } catch {
      // error handled by interceptor
    }
  }
}

// 订单搜索过滤
function filterOrderOption(input: string, option: any) {
  const order = orderList.value.find(o => o.id === option.value)
  return order?.orderNo?.toLowerCase().includes(input.toLowerCase()) ?? false
}

// 复制裁床条形码
function copyBarcode(barcode: string) {
  navigator.clipboard.writeText(barcode)
  message.success('已复制到剪贴板')
}

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
    if (searchParams.color) params.color = searchParams.color
    if (searchParams.size) params.size = searchParams.size
    const res: any = await cuttingPlanApi.list(params)
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    // error handled by interceptor
  } finally {
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
  searchParams.color = ''
  searchParams.size = ''
  pagination.current = 1
  loadData()
}

// 表格分页变化
function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

// 打开表单弹窗
function openModal(record?: any) {
  if (record) {
    formTitle.value = '编辑裁床计划'
    editingId.value = record.id
    formData.orderId = record.orderId
    formData.color = record.color
    formData.size = record.size
    formData.bedNo = record.bedNo
    formData.planQty = record.planQty
    formData.qtyPerBundle = record.qtyPerBundle
    formData.cuttingDate = record.cuttingDate
  } else {
    formTitle.value = '新增裁床计划'
    editingId.value = null
    formData.orderId = undefined
    formData.color = undefined
    formData.size = undefined
    formData.bedNo = undefined
    formData.planQty = undefined
    formData.qtyPerBundle = undefined
    formData.cuttingDate = undefined
  }
  formModalVisible.value = true
}

// 提交表单
async function handleFormSubmit() {
  if (!formData.orderId || !formData.color || !formData.size || !formData.bedNo || !formData.planQty || !formData.qtyPerBundle) {
    message.warning('请填写完整信息')
    return
  }
  submitLoading.value = true
  try {
    // 生成裁床条形码
    const order = orderList.value.find(o => o.id === formData.orderId)
    const barcode = `CUT-${order?.orderNo || formData.orderId}-${formData.color}-${formData.size}-${formData.bedNo}`

    const data = {
      ...formData,
      barcode,
    }

    if (editingId.value) {
      await cuttingPlanApi.update(editingId.value, data)
      message.success('更新成功')
    } else {
      await cuttingPlanApi.create(data)
      message.success('创建成功')
    }
    formModalVisible.value = false
    loadData()
  } catch {
    // error handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

// 删除
async function handleDelete(id: number) {
  try {
    await cuttingPlanApi.delete(id)
    message.success('删除成功')
    loadData()
  } catch {
    // error handled by interceptor
  }
}

// 打开录入实际数弹窗
function openActualQtyModal(record: any) {
  actualQtyRecord.value = record
  actualQtyValue.value = record.actualQty ?? undefined
  actualQtyModalVisible.value = true
}

// 提交实际数量
async function handleActualQtySubmit() {
  if (actualQtyValue.value === undefined || actualQtyValue.value === null) {
    message.warning('请输入实际数量')
    return
  }
  if (!actualQtyRecord.value) return
  submitLoading.value = true
  try {
    await cuttingPlanApi.inputActualQty(actualQtyRecord.value.id, actualQtyValue.value)
    message.success('录入成功')
    actualQtyModalVisible.value = false
    loadData()
  } catch {
    // error handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

// 开菲
async function handleOpenBundle(record: any) {
  openBundleLoading.value = true
  openBundleResult.value = []
  openBundleResultVisible.value = true
  try {
    const res: any = await cuttingPlanApi.openBundle(record.id)
    openBundleResult.value = res.data || []
    message.success('开菲成功')
    loadData()
  } catch {
    openBundleResult.value = []
  } finally {
    openBundleLoading.value = false
  }
}

// 查看扎号
async function openBundleModal(record: any) {
  bundleList.value = []
  bundleLoading.value = true
  bundleListVisible.value = true
  try {
    const res: any = await cuttingPlanApi.getBundles(record.id)
    bundleList.value = res.data || []
  } catch {
    // error handled by interceptor
  } finally {
    bundleLoading.value = false
  }
}

// 初始化加载
loadData()
loadOrders()
</script>
