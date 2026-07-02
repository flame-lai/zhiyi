<template>
  <a-card title="异常预警">
    <div class="toolbar">
      <a-space wrap>
        <a-select
          v-model:value="searchStatus"
          placeholder="处理状态"
          allow-clear
          style="width: 160px"
          @change="handleSearch"
        >
          <a-select-option :value="0">待处理</a-select-option>
          <a-select-option :value="9">已处理</a-select-option>
          <a-select-option :value="1">已忽略</a-select-option>
          <a-select-option :value="2">已清除</a-select-option>
          <a-select-option :value="3">已补录</a-select-option>
        </a-select>
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="resetSearch">重置</a-button>
      </a-space>
    </div>

    <a-table
      :columns="columns"
      :data-source="dataList"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      :scroll="{ x: 1200 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'bundle'">
          <div class="primary-cell">{{ record.bundleNo || '-' }}</div>
          <div class="secondary-cell">{{ [record.color, record.size].filter(Boolean).join(' / ') }}</div>
        </template>
        <template v-else-if="column.key === 'order'">
          <div class="primary-cell">{{ record.orderNo || '-' }}</div>
          <div class="secondary-cell">{{ record.styleCode || '-' }} {{ record.styleName || '' }}</div>
        </template>
        <template v-else-if="column.key === 'description'">
          <div class="description-cell">{{ record.description || '-' }}</div>
        </template>
        <template v-else-if="column.key === 'reporter'">
          <div>{{ record.reportEmployeeName || '-' }}</div>
          <div class="secondary-cell">{{ record.reportEmployeeNo || '' }}</div>
        </template>
        <template v-else-if="column.key === 'reportTime'">
          {{ formatDateTime(record.reportTime || record.createdAt) }}
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusTagMap[record.status]?.color">
            {{ statusTagMap[record.status]?.text || record.status }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'handle'">
          <div>{{ handleTypeMap[record.handleType] || '-' }}</div>
          <div v-if="record.handleByName" class="secondary-cell">{{ record.handleByName }} {{ formatDateTime(record.handleTime) }}</div>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space v-if="record.status === 0">
            <a-button type="link" size="small" @click="openIgnoreModal(record)">忽略</a-button>
            <a-button type="link" size="small" danger @click="openClearModal(record)">清除后置</a-button>
            <a-button type="link" size="small" @click="openSupplementModal(record)">补录前置</a-button>
          </a-space>
          <span v-else class="secondary-cell">已处理</span>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="ignoreVisible"
      title="忽略异常"
      @ok="handleIgnore"
      :confirm-loading="submitting"
    >
      <div class="context-panel">
        <div>{{ currentRecord?.description }}</div>
        <div class="secondary-cell">{{ currentRecord?.orderNo }} / {{ currentRecord?.bundleNo }}</div>
      </div>
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="原因">
          <a-textarea v-model:value="ignoreRemark" placeholder="请输入忽略原因" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="clearVisible"
      title="清除后置工序"
      @ok="handleClear"
      :confirm-loading="submitting"
    >
      <div class="context-panel">
        <div>{{ currentRecord?.description }}</div>
        <div class="secondary-cell">
          后置：{{ currentRecord?.backProcessName || '-' }}，报工人：{{ currentRecord?.reportEmployeeName || '-' }}，数量：{{ currentRecord?.reportQty || '-' }}
        </div>
      </div>
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="原因">
          <a-textarea v-model:value="clearReason" placeholder="请输入清除原因" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="supplementVisible"
      title="补录前置工序"
      @ok="handleSupplement"
      :confirm-loading="submitting"
    >
      <div class="context-panel">
        <div>{{ currentRecord?.description }}</div>
        <div class="secondary-cell">
          前置：{{ currentRecord?.frontProcessName || '-' }}，扎号：{{ currentRecord?.bundleNo || '-' }}
        </div>
      </div>
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="工人">
          <a-select
            v-model:value="supplementForm.employeeId"
            placeholder="请选择补录工人"
            show-search
            option-filter-prop="label"
            :options="employeeOptions"
          />
        </a-form-item>
        <a-form-item label="数量">
          <a-input-number v-model:value="supplementForm.qty" :min="1" style="width: 100%" placeholder="请输入补录数量" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { anomalyApi } from '../../api/admin'
import { employeeApi } from '../../api/system'

const loading = ref(false)
const submitting = ref(false)
const searchStatus = ref<number | undefined>(0)
const dataList = ref<any[]>([])
const currentRecord = ref<any>(null)
const employees = ref<any[]>([])

const ignoreVisible = ref(false)
const ignoreRemark = ref('')
const clearVisible = ref(false)
const clearReason = ref('')
const supplementVisible = ref(false)
const supplementForm = reactive({
  employeeId: undefined as number | undefined,
  qty: undefined as number | undefined,
})

const statusTagMap: Record<number, { text: string; color: string }> = {
  0: { text: '待处理', color: 'warning' },
  1: { text: '已忽略', color: 'default' },
  2: { text: '已清除', color: 'red' },
  3: { text: '已补录', color: 'green' },
}

const handleTypeMap: Record<number, string> = {
  1: '忽略',
  2: '清除后置',
  3: '补录前置',
}

const employeeOptions = computed(() =>
  employees.value.map((item) => ({
    value: item.id,
    label: `${item.employeeNo || ''} ${item.name || ''}`.trim(),
  }))
)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: '扎号', key: 'bundle', width: 130 },
  { title: '订单/款号', key: 'order', width: 190 },
  { title: '异常描述', key: 'description' },
  { title: '报工人', key: 'reporter', width: 120 },
  { title: '报工时间', key: 'reportTime', width: 160 },
  { title: '状态', key: 'status', width: 100 },
  { title: '处理记录', key: 'handle', width: 190 },
  { title: '操作', key: 'action', width: 230, fixed: 'right' },
]

function formatDateTime(value: any) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      size: pagination.pageSize,
    }
    if (searchStatus.value !== undefined) {
      params.status = searchStatus.value
    }
    const res: any = await anomalyApi.list(params)
    dataList.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function fetchEmployees() {
  const res: any = await employeeApi.list({ page: 1, size: 200 })
  employees.value = res.data?.records || []
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function resetSearch() {
  searchStatus.value = 0
  handleSearch()
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

function openIgnoreModal(record: any) {
  currentRecord.value = record
  ignoreRemark.value = ''
  ignoreVisible.value = true
}

async function handleIgnore() {
  if (!ignoreRemark.value.trim()) {
    message.warning('请输入忽略原因')
    return
  }
  submitting.value = true
  try {
    await anomalyApi.ignore(currentRecord.value.id, ignoreRemark.value.trim())
    message.success('已忽略')
    ignoreVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

function openClearModal(record: any) {
  currentRecord.value = record
  clearReason.value = ''
  clearVisible.value = true
}

async function handleClear() {
  if (!clearReason.value.trim()) {
    message.warning('请输入清除原因')
    return
  }
  submitting.value = true
  try {
    await anomalyApi.clear(currentRecord.value.id, clearReason.value.trim())
    message.success('已清除')
    clearVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

function openSupplementModal(record: any) {
  currentRecord.value = record
  supplementForm.employeeId = undefined
  supplementForm.qty = record.reportQty || 1
  supplementVisible.value = true
  if (!employees.value.length) {
    fetchEmployees()
  }
}

async function handleSupplement() {
  if (!supplementForm.employeeId || !supplementForm.qty) {
    message.warning('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    await anomalyApi.supplement(currentRecord.value.id, supplementForm.employeeId, supplementForm.qty)
    message.success('补录成功')
    supplementVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchData()
  fetchEmployees()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
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

.description-cell {
  color: #111827;
}

.context-panel {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f9fafb;
}

:deep(.ant-table-thead > tr > th) {
  white-space: nowrap;
}
</style>
