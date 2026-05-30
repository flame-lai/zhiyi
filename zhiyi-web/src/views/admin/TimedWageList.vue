<template>
  <a-card title="计时工资">
    <div class="toolbar">
      <a-space>
        <a-input
          v-model:value="searchEmployeeId"
          placeholder="员工ID"
          allow-clear
          style="width: 150px"
          @pressEnter="handleSearch"
        />
        <a-month-picker
          v-model:value="searchYearMonth"
          placeholder="选择年月"
          style="width: 200px"
          format="YYYY-MM"
          valueFormat="YYYY-MM"
          @change="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="resetSearch">重置</a-button>
      </a-space>
      <a-space>
        <a-button type="primary" @click="openModal()">新增</a-button>
        <a-button @click="openBatchModal">批量新增</a-button>
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
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="openModal(record)">编辑</a>
            <a-popconfirm
              title="确定删除该记录吗？"
              @confirm="handleDelete(record.id)"
            >
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑 Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑计时工资' : '新增计时工资'"
      @ok="handleSubmit"
      :confirm-loading="submitting"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="员工ID" name="employeeId" :rules="[{ required: true, message: '请输入员工ID' }]">
          <a-input-number v-model:value="formData.employeeId" :min="1" style="width: 100%" placeholder="请输入员工ID" />
        </a-form-item>
        <a-form-item label="日期" name="workDate" :rules="[{ required: true, message: '请选择日期' }]">
          <a-date-picker
            v-model:value="formData.workDate"
            style="width: 100%"
            format="YYYY-MM-DD"
            valueFormat="YYYY-MM-DD"
            placeholder="请选择日期"
          />
        </a-form-item>
        <a-form-item label="小时数" name="hours" :rules="[{ required: true, message: '请输入小时数' }]">
          <a-input-number v-model:value="formData.hours" :min="0" :precision="1" style="width: 100%" placeholder="请输入小时数" />
        </a-form-item>
        <a-form-item label="时薪" name="hourlyRate" :rules="[{ required: true, message: '请输入时薪' }]">
          <a-input-number v-model:value="formData.hourlyRate" :min="0" :precision="2" style="width: 100%" placeholder="请输入时薪" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 批量新增 Modal -->
    <a-modal
      v-model:open="batchVisible"
      title="批量新增计时工资"
      width="800px"
      @ok="handleBatchSubmit"
      :confirm-loading="batchSubmitting"
    >
      <div style="margin-bottom: 12px">
        <a-button type="dashed" @click="addBatchRow" block>+ 添加一行</a-button>
      </div>
      <a-table
        :columns="batchColumns"
        :data-source="batchRows"
        :pagination="false"
        row-key="_key"
        size="small"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'employeeId'">
            <a-input-number v-model:value="record.employeeId" :min="1" size="small" style="width: 100%" placeholder="员工ID" />
          </template>
          <template v-if="column.key === 'workDate'">
            <a-date-picker
              v-model:value="record.workDate"
              size="small"
              style="width: 100%"
              format="YYYY-MM-DD"
              valueFormat="YYYY-MM-DD"
              placeholder="日期"
            />
          </template>
          <template v-if="column.key === 'hours'">
            <a-input-number v-model:value="record.hours" :min="0" :precision="1" size="small" style="width: 100%" placeholder="小时数" />
          </template>
          <template v-if="column.key === 'hourlyRate'">
            <a-input-number v-model:value="record.hourlyRate" :min="0" :precision="2" size="small" style="width: 100%" placeholder="时薪" />
          </template>
          <template v-if="column.key === 'action'">
            <a style="color: #ff4d4f" @click="removeBatchRow(index)">删除</a>
          </template>
        </template>
      </a-table>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { timedWageApi } from '../../api/admin'

const loading = ref(false)
const submitting = ref(false)
const batchSubmitting = ref(false)
const modalVisible = ref(false)
const batchVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()
const dataList = ref<any[]>([])

const searchEmployeeId = ref<string | undefined>(undefined)
const searchYearMonth = ref<string | undefined>(undefined)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const formData = reactive({
  employeeId: undefined as number | undefined,
  workDate: '',
  hours: undefined as number | undefined,
  hourlyRate: undefined as number | undefined,
})

// 批量新增
let batchKeySeq = 0
const batchRows = ref<any[]>([])

const columns = [
  { title: '员工ID', dataIndex: 'employeeId', key: 'employeeId' },
  { title: '日期', dataIndex: 'workDate', key: 'workDate' },
  { title: '小时数', dataIndex: 'hours', key: 'hours' },
  { title: '时薪', dataIndex: 'hourlyRate', key: 'hourlyRate' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'action', width: 150 },
]

const batchColumns = [
  { title: '员工ID', key: 'employeeId', width: 120 },
  { title: '日期', key: 'workDate', width: 160 },
  { title: '小时数', key: 'hours', width: 120 },
  { title: '时薪', key: 'hourlyRate', width: 120 },
  { title: '操作', key: 'action', width: 80 },
]

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchEmployeeId.value) {
      params.employeeId = searchEmployeeId.value
    }
    if (searchYearMonth.value) {
      params.yearMonth = searchYearMonth.value
    }
    const res: any = await timedWageApi.list(params)
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
  searchEmployeeId.value = undefined
  searchYearMonth.value = undefined
  handleSearch()
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

function openModal(record?: any) {
  if (record) {
    editingId.value = record.id
    formData.employeeId = record.employeeId
    formData.workDate = record.workDate
    formData.hours = record.hours
    formData.hourlyRate = record.hourlyRate
  } else {
    editingId.value = null
    formData.employeeId = undefined
    formData.workDate = ''
    formData.hours = undefined
    formData.hourlyRate = undefined
  }
  modalVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const data = { ...formData }
    if (editingId.value) {
      await timedWageApi.update(editingId.value, data)
      message.success('编辑成功')
    } else {
      await timedWageApi.create(data)
      message.success('新增成功')
    }
    modalVisible.value = false
    fetchData()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await timedWageApi.delete(id)
    message.success('删除成功')
    fetchData()
  } catch {
    // handled by interceptor
  }
}

// 批量新增
function openBatchModal() {
  batchRows.value = []
  addBatchRow()
  batchVisible.value = true
}

function addBatchRow() {
  batchRows.value.push({
    _key: ++batchKeySeq,
    employeeId: undefined,
    workDate: '',
    hours: undefined,
    hourlyRate: undefined,
  })
}

function removeBatchRow(index: number) {
  batchRows.value.splice(index, 1)
}

async function handleBatchSubmit() {
  const validRows = batchRows.value.filter(
    (r) => r.employeeId && r.workDate && r.hours && r.hourlyRate
  )
  if (validRows.length === 0) {
    message.warning('请至少填写一条完整记录')
    return
  }
  batchSubmitting.value = true
  try {
    const payload = validRows.map(({ employeeId, workDate, hours, hourlyRate }) => ({
      employeeId, workDate, hours, hourlyRate,
    }))
    await timedWageApi.createBatch(payload)
    message.success('批量新增成功')
    batchVisible.value = false
    fetchData()
  } catch {
    // handled by interceptor
  } finally {
    batchSubmitting.value = false
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
