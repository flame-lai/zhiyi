<template>
  <div>
    <a-card title="裁床计划管理">
      <div style="margin-bottom: 16px; display: flex; gap: 16px; flex-wrap: wrap">
        <a-input-number
          v-model:value="searchParams.orderItemId"
          placeholder="订单明细ID"
          style="width: 200px"
          :precision="0"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button type="primary" @click="openModal()">新增</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="statusColorMap[record.status]">
              {{ statusTextMap[record.status] || record.status }}
            </a-tag>
          </template>
          <template v-if="column.dataIndex === 'action'">
            <a-space>
              <a @click="openModal(record)">编辑</a>
              <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
                <a style="color: #ff4d4f">删除</a>
              </a-popconfirm>
              <a @click="openActualQtyModal(record)">录入实际数</a>
              <a @click="handleOpenBundle(record)">开菲</a>
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
    >
      <a-form :model="formData" layout="vertical">
        <a-form-item label="订单明细ID" required>
          <a-input-number
            v-model:value="formData.orderItemId"
            style="width: 100%"
            :precision="0"
            placeholder="请输入订单明细ID"
          />
        </a-form-item>
        <a-form-item label="床次" required>
          <a-input-number
            v-model:value="formData.bedNo"
            style="width: 100%"
            :precision="0"
            placeholder="请输入床次"
          />
        </a-form-item>
        <a-form-item label="计划数量" required>
          <a-input-number
            v-model:value="formData.planQty"
            style="width: 100%"
            :min="1"
            :precision="0"
            placeholder="请输入计划数量"
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
        <a-form-item label="实际数量" required>
          <a-input-number
            v-model:value="actualQtyValue"
            style="width: 100%"
            :min="0"
            :precision="0"
            placeholder="请输入实际数量"
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
        <pre style="white-space: pre-wrap">{{ openBundleResult }}</pre>
      </a-spin>
    </a-modal>

    <!-- 查看扎号弹窗 -->
    <a-modal
      v-model:open="bundleListVisible"
      title="扎号列表"
      :footer="null"
      width="600px"
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
import { message } from 'ant-design-vue'
import { cuttingPlanApi } from '../../api/cutting'

interface CuttingPlan {
  id: number
  orderItemId: number
  bedNo: number
  planQty: number
  actualQty: number | null
  status: string
  createTime: string
}

interface Bundle {
  id: number
  bundleNo: string
  qty: number
}

// 表格列定义
const columns = [
  { title: '订单明细ID', dataIndex: 'orderItemId', width: 120 },
  { title: '床次', dataIndex: 'bedNo', width: 80 },
  { title: '计划数量', dataIndex: 'planQty', width: 100 },
  { title: '实际数量', dataIndex: 'actualQty', width: 100 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', dataIndex: 'action', width: 320, fixed: 'right' },
]

const bundleColumns = [
  { title: '扎号', dataIndex: 'bundleNo', width: 150 },
  { title: '数量', dataIndex: 'qty', width: 100 },
]

// 状态映射
const statusColorMap: Record<string, string> = {
  pending: 'default',
  processing: 'processing',
  completed: 'success',
}

const statusTextMap: Record<string, string> = {
  pending: '待处理',
  processing: '进行中',
  completed: '已完成',
}

// 查询参数
const searchParams = reactive({
  orderItemId: undefined as number | undefined,
})

// 表格数据
const tableData = ref<CuttingPlan[]>([])
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
  orderItemId: undefined as number | undefined,
  bedNo: undefined as number | undefined,
  planQty: undefined as number | undefined,
})

// 录入实际数相关
const actualQtyModalVisible = ref(false)
const actualQtyValue = ref<number | undefined>(undefined)
const actualQtyRecordId = ref<number | null>(null)

// 开菲结果相关
const openBundleResultVisible = ref(false)
const openBundleLoading = ref(false)
const openBundleResult = ref('')

// 扎号列表相关
const bundleListVisible = ref(false)
const bundleLoading = ref(false)
const bundleList = ref<Bundle[]>([])

// 加载表格数据
async function loadData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      size: pagination.pageSize,
    }
    if (searchParams.orderItemId !== undefined) {
      params.orderItemId = searchParams.orderItemId
    }
    const res: any = await cuttingPlanApi.list(params)
    tableData.value = res.data.records || res.data.list || res.data || []
    pagination.total = res.data.total || 0
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
  searchParams.orderItemId = undefined
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
function openModal(record?: CuttingPlan) {
  if (record) {
    formTitle.value = '编辑裁床计划'
    editingId.value = record.id
    formData.orderItemId = record.orderItemId
    formData.bedNo = record.bedNo
    formData.planQty = record.planQty
  } else {
    formTitle.value = '新增裁床计划'
    editingId.value = null
    formData.orderItemId = undefined
    formData.bedNo = undefined
    formData.planQty = undefined
  }
  formModalVisible.value = true
}

// 提交表单
async function handleFormSubmit() {
  if (!formData.orderItemId || !formData.bedNo || !formData.planQty) {
    message.warning('请填写完整信息')
    return
  }
  submitLoading.value = true
  try {
    if (editingId.value) {
      await cuttingPlanApi.update(editingId.value, { ...formData })
      message.success('更新成功')
    } else {
      await cuttingPlanApi.create({ ...formData })
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
function openActualQtyModal(record: CuttingPlan) {
  actualQtyRecordId.value = record.id
  actualQtyValue.value = record.actualQty ?? undefined
  actualQtyModalVisible.value = true
}

// 提交实际数量
async function handleActualQtySubmit() {
  if (actualQtyValue.value === undefined || actualQtyValue.value === null) {
    message.warning('请输入实际数量')
    return
  }
  if (!actualQtyRecordId.value) return
  submitLoading.value = true
  try {
    await cuttingPlanApi.inputActualQty(actualQtyRecordId.value, actualQtyValue.value)
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
async function handleOpenBundle(record: CuttingPlan) {
  openBundleLoading.value = true
  openBundleResult.value = ''
  openBundleResultVisible.value = true
  try {
    const res: any = await cuttingPlanApi.openBundle(record.id)
    openBundleResult.value = typeof res.data === 'string' ? res.data : JSON.stringify(res.data, null, 2)
  } catch {
    openBundleResult.value = '操作失败'
  } finally {
    openBundleLoading.value = false
  }
}

// 查看扎号
async function openBundleModal(record: CuttingPlan) {
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
</script>
