<template>
  <div>
    <a-card title="扎号管理">
      <div style="margin-bottom: 16px; display: flex; gap: 16px; flex-wrap: wrap">
        <a-input-number
          v-model:value="searchOrderId"
          placeholder="订单ID"
          style="width: 200px"
          :precision="0"
        />
        <a-button type="primary" @click="handleSearch" :loading="loading">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        row-key="id"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="statusColorMap[record.status]">
              {{ statusTextMap[record.status] || record.status }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { bundleApi } from '../../api/cutting'

interface Bundle {
  id: number
  bundleNo: string
  cuttingPlanId: number
  qty: number
  status: string
  createTime: string
}

// 表格列定义
const columns = [
  { title: '扎号', dataIndex: 'bundleNo', width: 150 },
  { title: '裁床计划ID', dataIndex: 'cuttingPlanId', width: 120 },
  { title: '数量', dataIndex: 'qty', width: 100 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
]

// 状态映射
const statusColorMap: Record<string, string> = {
  pending: 'default',
  completed: 'success',
  used: 'processing',
}

const statusTextMap: Record<string, string> = {
  pending: '待使用',
  completed: '已完成',
  used: '已使用',
}

// 搜索参数
const searchOrderId = ref<number | undefined>(undefined)

// 表格数据
const tableData = ref<Bundle[]>([])
const loading = ref(false)

// 查询
async function handleSearch() {
  if (!searchOrderId.value) {
    message.warning('请输入订单ID')
    return
  }
  loading.value = true
  try {
    const res: any = await bundleApi.getByOrderId(searchOrderId.value)
    tableData.value = res.data || []
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

// 重置
function handleReset() {
  searchOrderId.value = undefined
  tableData.value = []
}
</script>
