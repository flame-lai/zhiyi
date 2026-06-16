<template>
  <div>
    <a-card title="扎号管理">
      <!-- 搜索区域 -->
      <div style="margin-bottom: 16px; display: flex; gap: 16px; flex-wrap: wrap">
        <a-input
          v-model:value="searchParams.bundleNo"
          placeholder="扎号"
          style="width: 120px"
          allow-clear
        />
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
        :scroll="{ x: 1400 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'progress'">
            <span v-if="record.totalProcesses > 0">
              <a-tag :color="record.completedProcesses >= record.totalProcesses ? 'green' : 'blue'">
                {{ record.completedProcesses || 0 }}/{{ record.totalProcesses }}
              </a-tag>
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
      width="720px"
    >
      <template v-if="currentBundle">
        <!-- 基本信息 -->
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="扎号">{{ currentBundle.bundleNo }}</a-descriptions-item>
          <a-descriptions-item label="扎号码">{{ currentBundle.globalBundleNo }}</a-descriptions-item>
          <a-descriptions-item label="订单号">{{ currentBundle.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="款号">{{ currentBundle.styleCode }}</a-descriptions-item>
          <a-descriptions-item label="部位">{{ currentBundle.part }}</a-descriptions-item>
          <a-descriptions-item label="面料">{{ currentBundle.fabric }}</a-descriptions-item>
          <a-descriptions-item label="颜色">{{ currentBundle.color }}</a-descriptions-item>
          <a-descriptions-item label="尺码">{{ currentBundle.size }}</a-descriptions-item>
          <a-descriptions-item label="床次">{{ currentBundle.bedNo }}</a-descriptions-item>
          <a-descriptions-item label="成衣件数">{{ currentBundle.qty }}</a-descriptions-item>
          <a-descriptions-item label="裁片片数">{{ currentBundle.cuttingPieces || '-' }}</a-descriptions-item>
          <a-descriptions-item label="完成进度">
            <span v-if="currentBundle.totalProcesses > 0">
              <a-tag :color="currentBundle.completedProcesses >= currentBundle.totalProcesses ? 'green' : 'blue'">
                {{ currentBundle.completedProcesses || 0 }}/{{ currentBundle.totalProcesses }}
              </a-tag>
            </span>
            <span v-else>-</span>
          </a-descriptions-item>
        </a-descriptions>

        <!-- 工序完成状态 -->
        <a-divider orientation="left" style="font-size: 14px">工序完成状态</a-divider>
        <a-table
          :columns="workDetailColumns"
          :data-source="workDetails"
          :loading="workDetailLoading"
          :pagination="false"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'completed'">
              <a-tag :color="record.completed ? 'green' : 'default'">
                {{ record.completed ? '✅ 已完成' : '❌ 未完成' }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'reportTime'">
              {{ record.reportTime ? formatTime(record.reportTime) : '-' }}
            </template>
          </template>
        </a-table>
      </template>
    </a-modal>

    <!-- 重新打印弹窗 -->
    <BundlePrint
      v-model:visible="printModalVisible"
      :bundles="printBundles"
      :cutting-barcode="printCuttingBarcode"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { bundleApi, cuttingPlanApi } from '../../api/cutting'
import BundlePrint from '../../components/BundlePrint.vue'

// 表格列定义
const columns = [
  { title: '扎号', dataIndex: 'bundleNo', width: 100 },
  { title: '订单号', dataIndex: 'orderNo', width: 140 },
  { title: '款号', dataIndex: 'styleCode', width: 100 },
  { title: '部位', dataIndex: 'part', width: 100 },
  { title: '面料', dataIndex: 'fabric', width: 100 },
  { title: '颜色', dataIndex: 'color', width: 80 },
  { title: '尺码', dataIndex: 'size', width: 70 },
  { title: '成衣件数', dataIndex: 'qty', width: 90 },
  { title: '裁片片数', dataIndex: 'cuttingPieces', width: 90 },
  { title: '床次', dataIndex: 'bedNo', width: 70 },
  { title: '完成进度', dataIndex: 'progress', width: 100 },
  { title: '操作', dataIndex: 'action', width: 150, fixed: 'right' },
]

// 工序明细列
const workDetailColumns = [
  { title: '工序', dataIndex: 'processName', width: 150 },
  { title: '状态', dataIndex: 'completed', width: 120, align: 'center' as const },
  { title: '报工人', dataIndex: 'employeeName', width: 120 },
  { title: '报工时间', dataIndex: 'reportTime', width: 180 },
]

// 查询参数
const searchParams = reactive({
  bundleNo: '',
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

// 详情弹窗相关
const detailModalVisible = ref(false)
const currentBundle = ref<any>(null)
const workDetails = ref<any[]>([])
const workDetailLoading = ref(false)

// 打印相关
const printModalVisible = ref(false)
const printBundles = ref<any[]>([])
const printCuttingBarcode = ref('')

// 格式化时间
function formatTime(time: string) {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

// 加载表格数据
async function loadData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      size: pagination.pageSize,
    }
    if (searchParams.bundleNo) params.bundleNo = searchParams.bundleNo
    if (searchParams.orderNo) params.orderNo = searchParams.orderNo
    if (searchParams.styleCode) params.styleCode = searchParams.styleCode
    if (searchParams.color) params.color = searchParams.color
    if (searchParams.size) params.size = searchParams.size
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
  searchParams.bundleNo = ''
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

// 打开详情弹窗
async function openDetailModal(record: any) {
  try {
    const res: any = await bundleApi.getById(record.id)
    currentBundle.value = res.data
    detailModalVisible.value = true
    // 加载工序完成明细
    loadWorkDetails(record.id)
  } catch {
    // error handled by interceptor
  }
}

// 加载工序完成明细
async function loadWorkDetails(bundleId: number) {
  workDetailLoading.value = true
  workDetails.value = []
  try {
    const res: any = await bundleApi.getWorkDetails(bundleId)
    workDetails.value = res.data || []
  } catch {
    // error handled by interceptor
  } finally {
    workDetailLoading.value = false
  }
}

// 重新打印
async function handleReprint(record: any) {
  try {
    // 获取该扎号所属裁床计划的所有菲仔数据
    const bundleDetail: any = await bundleApi.getById(record.id)
    const bundleData = bundleDetail.data
    if (!bundleData || !bundleData.cuttingPlanId) {
      message.error('无法获取裁床计划信息')
      return
    }

    // 获取裁床计划的裁床码
    const planRes: any = await cuttingPlanApi.getById(bundleData.cuttingPlanId)
    printCuttingBarcode.value = planRes.data?.barcode || ''

    // 获取该裁床计划所有扎号详情
    const detailsRes: any = await cuttingPlanApi.getBundleDetails(bundleData.cuttingPlanId)
    const allBundles = detailsRes.data || []

    // 默认选中当前扎号（同一扎号可能有多条记录-多部位）
    const sameBundles = allBundles.filter((b: any) => b.bundleNo === record.bundleNo)

    printBundles.value = sameBundles.map((b: any) => ({
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
  } catch {
    message.error('获取打印数据失败')
  }
}

// 初始化加载
loadData()
</script>

<style scoped>
:deep(.ant-descriptions-item-label) {
  font-weight: 500;
  background-color: #fafafa;
}
</style>
