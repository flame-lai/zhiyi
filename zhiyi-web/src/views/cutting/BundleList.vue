<template>
  <div>
    <a-card title="扎号管理">
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
        <a-select
          v-model:value="searchParams.status"
          placeholder="状态"
          style="width: 120px"
          allow-clear
        >
          <a-select-option :value="0">生产中</a-select-option>
          <a-select-option :value="1">已完成</a-select-option>
        </a-select>
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
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
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="statusColorMap[record.status]">
              {{ statusTextMap[record.status] || record.status }}
            </a-tag>
          </template>
          <template v-if="column.dataIndex === 'progress'">
            <span v-if="record.totalProcesses > 0">
              {{ record.completedProcesses || 0 }}/{{ record.totalProcesses }}
            </span>
            <span v-else>-</span>
          </template>
          <template v-if="column.dataIndex === 'action'">
            <a-space>
              <a @click="openDetailModal(record)">查看详情</a>
              <a @click="handleReprint(record)">重新打印</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailModalVisible"
      title="扎号详情"
      :footer="null"
      width="600px"
    >
      <a-descriptions :column="2" bordered v-if="currentBundle">
        <a-descriptions-item label="扎号">{{ currentBundle.bundleNo }}</a-descriptions-item>
        <a-descriptions-item label="订单号">{{ currentBundle.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="款号">{{ currentBundle.styleCode }}</a-descriptions-item>
        <a-descriptions-item label="颜色">{{ currentBundle.color }}</a-descriptions-item>
        <a-descriptions-item label="尺码">{{ currentBundle.size }}</a-descriptions-item>
        <a-descriptions-item label="床次">{{ currentBundle.bedNo }}</a-descriptions-item>
        <a-descriptions-item label="件数">{{ currentBundle.qty }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColorMap[currentBundle.status]">
            {{ statusTextMap[currentBundle.status] || currentBundle.status }}
          </a-tag>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { bundleApi } from '../../api/cutting'

// 表格列定义
const columns = [
  { title: '扎号', dataIndex: 'bundleNo', width: 100 },
  { title: '订单号', dataIndex: 'orderNo', width: 140 },
  { title: '款号', dataIndex: 'styleCode', width: 100 },
  { title: '颜色', dataIndex: 'color', width: 80 },
  { title: '尺码', dataIndex: 'size', width: 70 },
  { title: '件数', dataIndex: 'qty', width: 70 },
  { title: '床次', dataIndex: 'bedNo', width: 70 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '完成进度', dataIndex: 'progress', width: 100 },
  { title: '操作', dataIndex: 'action', width: 150, fixed: 'right' },
]

// 状态映射
const statusColorMap: Record<number, string> = {
  0: 'processing',
  1: 'success',
}

const statusTextMap: Record<number, string> = {
  0: '生产中',
  1: '已完成',
}

// 查询参数
const searchParams = reactive({
  orderNo: '',
  styleCode: '',
  color: '',
  size: '',
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

// 详情弹窗相关
const detailModalVisible = ref(false)
const currentBundle = ref<any>(null)

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
    if (searchParams.status !== undefined) params.status = searchParams.status
    const res: any = await bundleApi.list(params)
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

// 打开详情弹窗
async function openDetailModal(record: any) {
  try {
    const res: any = await bundleApi.getById(record.id)
    currentBundle.value = res.data
    detailModalVisible.value = true
  } catch {
    // error handled by interceptor
  }
}

// 重新打印
function handleReprint(record: any) {
  // TODO: 实现打印功能
  message.info('打印功能待实现')
}

// 初始化加载
loadData()
</script>
