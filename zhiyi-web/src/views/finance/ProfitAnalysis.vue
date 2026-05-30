<template>
  <div>
    <!-- 利润汇总 -->
    <a-card title="利润汇总" style="margin-bottom: 16px">
      <a-spin :spinning="summaryLoading">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-card>
              <a-statistic
                title="总收入"
                :value="summaryData.totalRevenue"
                :precision="2"
                prefix="¥"
                :value-style="{ color: '#3f8600' }"
              />
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card>
              <a-statistic
                title="总成本"
                :value="summaryData.totalCost"
                :precision="2"
                prefix="¥"
                :value-style="{ color: '#cf1322' }"
              />
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card>
              <a-statistic
                title="总利润"
                :value="summaryData.totalProfit"
                :precision="2"
                prefix="¥"
                :value-style="{ color: summaryData.totalProfit >= 0 ? '#3f8600' : '#cf1322' }"
              />
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card>
              <a-statistic
                title="平均利润率"
                :value="summaryData.avgProfitRate"
                :precision="2"
                suffix="%"
                :value-style="{ color: summaryData.avgProfitRate >= 0 ? '#3f8600' : '#cf1322' }"
              />
            </a-card>
          </a-col>
        </a-row>
      </a-spin>
    </a-card>

    <!-- 按订单利润 -->
    <a-card title="按订单利润">
      <a-table
        :columns="columns"
        :data-source="orderProfitList"
        :loading="tableLoading"
        :pagination="pagination"
        row-key="orderId"
      />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { profitApi } from '../../api/finance'

const tableLoading = ref(false)
const summaryLoading = ref(false)
const orderProfitList = ref<any[]>([])

const summaryData = reactive({
  totalRevenue: 0,
  totalCost: 0,
  totalProfit: 0,
  avgProfitRate: 0,
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: '订单号', dataIndex: 'orderId', key: 'orderId' },
  { title: '收入', dataIndex: 'revenue', key: 'revenue' },
  { title: '计件成本', dataIndex: 'pieceCost', key: 'pieceCost' },
  { title: '计时成本', dataIndex: 'timeCost', key: 'timeCost' },
  { title: '其他成本', dataIndex: 'otherCost', key: 'otherCost' },
  { title: '利润', dataIndex: 'profit', key: 'profit' },
  {
    title: '利润率',
    dataIndex: 'profitRate',
    key: 'profitRate',
    customRender: ({ text }: { text: number }) => text !== undefined ? `${(text * 100).toFixed(2)}%` : '-',
  },
]

async function fetchOrderProfit() {
  tableLoading.value = true
  try {
    const res: any = await profitApi.byOrder()
    const list = res.data?.records || res.data?.list || res.data || []
    orderProfitList.value = Array.isArray(list) ? list : []
    pagination.total = res.data?.total || orderProfitList.value.length
  } catch {
    // handled by interceptor
  } finally {
    tableLoading.value = false
  }
}

async function fetchSummary() {
  summaryLoading.value = true
  try {
    const res: any = await profitApi.summary()
    const data = res.data || {}
    summaryData.totalRevenue = data.totalRevenue || 0
    summaryData.totalCost = data.totalCost || 0
    summaryData.totalProfit = data.totalProfit || 0
    summaryData.avgProfitRate = data.avgProfitRate || 0
  } catch {
    // handled by interceptor
  } finally {
    summaryLoading.value = false
  }
}

onMounted(() => {
  fetchOrderProfit()
  fetchSummary()
})
</script>
