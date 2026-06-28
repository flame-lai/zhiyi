<template>
  <a-card title="异常预警">
    <div class="toolbar">
      <a-space>
        <a-select
          v-model:value="searchStatus"
          placeholder="按状态筛选"
          allow-clear
          style="width: 200px"
          @change="handleSearch"
        >
          <a-select-option :value="0">待处理</a-select-option>
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
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusTagMap[record.status]?.color">
            {{ statusTagMap[record.status]?.text || record.status }}
          </a-tag>
        </template>
        <template v-if="column.key === 'handleType'">
          {{ handleTypeMap[record.handleType] || '-' }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space v-if="record.status === 0">
            <a @click="openIgnoreModal(record)">忽略</a>
            <a @click="openClearModal(record)">清除</a>
            <a @click="openSupplementModal(record)">补录</a>
          </a-space>
          <span v-else style="color: #999">已处理</span>
        </template>
      </template>
    </a-table>

    <!-- 忽略 Modal -->
    <a-modal
      v-model:open="ignoreVisible"
      title="忽略异常"
      @ok="handleIgnore"
      :confirm-loading="submitting"
    >
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="备注">
          <a-input v-model:value="ignoreRemark" placeholder="请输入忽略原因" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 清除 Modal -->
    <a-modal
      v-model:open="clearVisible"
      title="清除异常"
      @ok="handleClear"
      :confirm-loading="submitting"
    >
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="原因">
          <a-input v-model:value="clearReason" placeholder="请输入清除原因" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 补录 Modal -->
    <a-modal
      v-model:open="supplementVisible"
      title="补录"
      @ok="handleSupplement"
      :confirm-loading="submitting"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="员工ID">
          <a-input-number v-model:value="supplementForm.employeeId" :min="1" style="width: 100%" placeholder="请输入员工ID" />
        </a-form-item>
        <a-form-item label="数量">
          <a-input-number v-model:value="supplementForm.qty" :min="1" style="width: 100%" placeholder="请输入补录数量" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { anomalyApi } from '../../api/admin'

const loading = ref(false)
const submitting = ref(false)
const searchStatus = ref<number | undefined>(undefined)
const dataList = ref<any[]>([])
const currentRecord = ref<any>(null)

// 忽略
const ignoreVisible = ref(false)
const ignoreRemark = ref('')

// 清除
const clearVisible = ref(false)
const clearReason = ref('')

// 补录
const supplementVisible = ref(false)
const supplementForm = reactive({
  employeeId: undefined as number | undefined,
  qty: undefined as number | undefined,
})

const statusTagMap: Record<number, { text: string; color: string }> = {
  0: { text: '待处理', color: 'warning' },
  1: { text: '已忽略', color: 'default' },
  2: { text: '已清除', color: 'success' },
  3: { text: '已补录', color: 'processing' },
}

const handleTypeMap: Record<number, string> = {
  1: '忽略',
  2: '清除报工',
  3: '补录报工',
}

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: '订单ID', dataIndex: 'orderId', key: 'orderId' },
  { title: '扎号ID', dataIndex: 'bundleId', key: 'bundleId' },
  { title: '前置工序ID', dataIndex: 'frontProcessId', key: 'frontProcessId' },
  { title: '后置工序ID', dataIndex: 'backProcessId', key: 'backProcessId' },
  { title: '报工人ID', dataIndex: 'reportEmployeeId', key: 'reportEmployeeId' },
  { title: '处理方式', key: 'handleType' },
  { title: '状态', key: 'status' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 200 },
]

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
    dataList.value = res.data?.records || res.data?.list || res.data || []
    pagination.total = res.data?.total || 0
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function resetSearch() {
  searchStatus.value = undefined
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
  if (!ignoreRemark.value) {
    message.warning('请输入备注')
    return
  }
  submitting.value = true
  try {
    await anomalyApi.ignore(currentRecord.value.id, ignoreRemark.value)
    message.success('已忽略')
    ignoreVisible.value = false
    fetchData()
  } catch {
    // handled by interceptor
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
  if (!clearReason.value) {
    message.warning('请输入原因')
    return
  }
  submitting.value = true
  try {
    await anomalyApi.clear(currentRecord.value.id, clearReason.value)
    message.success('已清除')
    clearVisible.value = false
    fetchData()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

function openSupplementModal(record: any) {
  currentRecord.value = record
  supplementForm.employeeId = undefined
  supplementForm.qty = undefined
  supplementVisible.value = true
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
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>
