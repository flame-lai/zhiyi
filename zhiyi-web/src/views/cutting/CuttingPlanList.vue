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
              <a @click="openActualQtyModal(record)">录入实际数</a>
              <a
                v-if="record.status < 2"
                @click="handleOpenBundle(record)"
              >开菲</a>
              <a
                v-else
                style="color: #ccc; cursor: not-allowed;"
              >已开菲</a>
              <a @click="openBundleModal(record)">查看扎号</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 录入实际数弹窗 -->
    <a-modal
      v-model:open="actualQtyModalVisible"
      title="录入实际数量"
      @ok="handleActualQtySubmit"
      :confirm-loading="submitLoading"
      width="700px"
    >
      <a-table
        :columns="actualQtyColumns"
        :data-source="actualQtyData"
        :pagination="false"
        size="small"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'actualQty'">
            <a-input-number v-model:value="record.actualQty" :min="0" size="small" style="width: 100%" />
          </template>
        </template>
      </a-table>
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
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { cuttingPlanApi } from '../../api/cutting'

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
  { title: '每扎件数', dataIndex: 'qtyPerBundle', width: 90 },
  { title: '总扎数', dataIndex: 'bundleCount', width: 80 },
  { title: '裁床码', dataIndex: 'barcode', width: 160 },
  { title: '裁床日期', dataIndex: 'cuttingDate', width: 110 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '操作', dataIndex: 'action', width: 280, fixed: 'right' },
]

const actualQtyColumns = [
  { title: '部位', dataIndex: 'part', width: 100 },
  { title: '颜色', dataIndex: 'color', width: 100 },
  { title: '尺码', dataIndex: 'size', width: 80 },
  { title: '计划数', dataIndex: 'planQty', width: 80 },
  { title: '实际数', dataIndex: 'actualQty', width: 120 },
]

const bundleColumns = [
  { title: '扎号', dataIndex: 'bundleNo', width: 100 },
  { title: '颜色', dataIndex: 'color', width: 80 },
  { title: '尺码', dataIndex: 'size', width: 70 },
  { title: '件数', dataIndex: 'qty', width: 70 },
  { title: '状态', dataIndex: 'status', width: 80 },
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
const actualQtyData = ref<any[]>([])
const currentPlanId = ref<number | null>(null)

// 扎号列表相关
const bundleListVisible = ref(false)
const bundleLoading = ref(false)
const bundleList = ref<any[]>([])

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

// 打开录入实际数弹窗
async function openActualQtyModal(record: any) {
  currentPlanId.value = record.id
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
    await cuttingPlanApi.inputActualQty(currentPlanId.value, actualQtyData.value)
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

// 初始化加载
loadData()
</script>
