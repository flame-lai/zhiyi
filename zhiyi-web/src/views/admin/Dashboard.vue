<template>
  <div>
    <a-card title="订单看板" style="margin-bottom: 16px">
      <a-table
        :columns="orderColumns"
        :data-source="orderData"
        :loading="orderLoading"
        :pagination="false"
        row-key="processId"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'completionRate'">
            <a-progress
              :percent="Number((record.completionRate * 100).toFixed(1))"
              :status="record.completionRate >= 1 ? 'success' : 'active'"
              size="small"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card title="工人看板">
      <a-table
        :columns="workerColumns"
        :data-source="workerData"
        :loading="workerLoading"
        :pagination="false"
        row-key="employeeId"
      />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { dashboardApi } from '../../api/admin'

const orderLoading = ref(false)
const workerLoading = ref(false)
const orderData = ref<any[]>([])
const workerData = ref<any[]>([])

const orderColumns = [
  { title: '工序名', dataIndex: 'processName', key: 'processName' },
  { title: '已报工数', dataIndex: 'reportedQty', key: 'reportedQty' },
  { title: '计划数', dataIndex: 'plannedQty', key: 'plannedQty' },
  { title: '完成率', key: 'completionRate', dataIndex: 'completionRate' },
]

const workerColumns = [
  { title: '工人名', dataIndex: 'employeeName', key: 'employeeName' },
  { title: '今日报工数', dataIndex: 'todayQty', key: 'todayQty' },
  { title: '今日工资', dataIndex: 'todayWage', key: 'todayWage' },
]

async function fetchOrderDashboard() {
  orderLoading.value = true
  try {
    const res: any = await dashboardApi.orderDashboard()
    orderData.value = res.data || []
  } catch {
    // handled by interceptor
  } finally {
    orderLoading.value = false
  }
}

async function fetchWorkerDashboard() {
  workerLoading.value = true
  try {
    const res: any = await dashboardApi.workerDashboard()
    workerData.value = res.data || []
  } catch {
    // handled by interceptor
  } finally {
    workerLoading.value = false
  }
}

onMounted(() => {
  fetchOrderDashboard()
  fetchWorkerDashboard()
})
</script>
