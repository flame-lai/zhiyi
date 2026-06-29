<template>
  <a-card title="计时工资录入">
    <div class="toolbar">
      <a-space wrap>
        <a-select
          v-model:value="searchEmployeeId"
          placeholder="选择工人"
          allow-clear
          show-search
          style="width: 220px"
          :filter-option="filterEmployeeOption"
          @change="handleSearch"
        >
          <a-select-option v-for="emp in employeeOptions" :key="emp.id" :value="emp.id">
            {{ formatEmployee(emp) }}
          </a-select-option>
        </a-select>
        <a-range-picker
          v-model:value="searchDateRange"
          format="YYYY-MM-DD"
          valueFormat="YYYY-MM-DD"
          @change="handleSearch"
        />
        <a-month-picker
          v-model:value="searchYearMonth"
          placeholder="选择月份"
          style="width: 160px"
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
        <template v-if="column.key === 'employee'">
          {{ record.employeeNo }} {{ record.employeeName }}
        </template>
        <template v-else-if="column.key === 'amount'">
          {{ money(record.amount) }}
        </template>
        <template v-else-if="column.key === 'createdBy'">
          {{ record.createdByName || record.createdBy || '-' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a @click="openModal(record)">编辑</a>
            <a-popconfirm title="确定删除该记录吗？" @confirm="handleDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑计时工资' : '新增计时工资'"
      @ok="handleSubmit"
      :confirm-loading="submitting"
      destroy-on-close
    >
      <a-form ref="formRef" :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="工人" name="employeeId" :rules="[{ required: true, message: '请选择工人' }]">
          <a-select
            v-model:value="formData.employeeId"
            placeholder="选择工人"
            show-search
            :filter-option="filterEmployeeOption"
          >
            <a-select-option v-for="emp in employeeOptions" :key="emp.id" :value="emp.id">
              {{ formatEmployee(emp) }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="日期" name="workDate" :rules="[{ required: true, message: '请选择日期' }]">
          <a-date-picker v-model:value="formData.workDate" style="width: 100%" format="YYYY-MM-DD" valueFormat="YYYY-MM-DD" />
        </a-form-item>
        <a-form-item label="工时" name="hours" :rules="[{ required: true, message: '请输入工时' }]">
          <a-input-number v-model:value="formData.hours" :min="0.1" :precision="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="时薪" name="hourlyRate" :rules="[{ required: true, message: '请输入时薪' }]">
          <a-input-number v-model:value="formData.hourlyRate" :min="0" :precision="2" style="width: 100%" />
        </a-form-item>
        <a-form-item label="金额">
          <a-input :value="money(calcAmount(formData.hours, formData.hourlyRate))" disabled />
        </a-form-item>
        <a-form-item label="工作内容" name="workContent">
          <a-textarea v-model:value="formData.workContent" :rows="3" placeholder="如：搬运物料、清洁车间" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="batchVisible"
      title="批量新增计时工资"
      width="860px"
      @ok="handleBatchSubmit"
      :confirm-loading="batchSubmitting"
      destroy-on-close
    >
      <a-form layout="inline" class="batch-form">
        <a-form-item label="日期">
          <a-date-picker v-model:value="batchBase.workDate" format="YYYY-MM-DD" valueFormat="YYYY-MM-DD" />
        </a-form-item>
        <a-form-item label="统一时薪">
          <a-input-number v-model:value="batchBase.hourlyRate" :min="0" :precision="2" />
        </a-form-item>
        <a-form-item label="统一工时">
          <a-input-number v-model:value="batchBase.hours" :min="0.1" :precision="1" />
        </a-form-item>
        <a-form-item>
          <a-button @click="applyBatchBase">应用到全部</a-button>
        </a-form-item>
      </a-form>
      <div class="batch-actions">
        <a-select
          v-model:value="batchSelectedEmployeeIds"
          mode="multiple"
          placeholder="选择多个工人"
          show-search
          style="width: 100%"
          :filter-option="filterEmployeeOption"
          @change="syncBatchRows"
        >
          <a-select-option v-for="emp in employeeOptions" :key="emp.id" :value="emp.id">
            {{ formatEmployee(emp) }}
          </a-select-option>
        </a-select>
      </div>
      <a-table :columns="batchColumns" :data-source="batchRows" :pagination="false" row-key="employeeId" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'employee'">
            {{ record.employeeNo }} {{ record.employeeName }}
          </template>
          <template v-else-if="column.key === 'hours'">
            <a-input-number v-model:value="record.hours" :min="0.1" :precision="1" size="small" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'hourlyRate'">
            <a-input-number v-model:value="record.hourlyRate" :min="0" :precision="2" size="small" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'amount'">
            {{ money(calcAmount(record.hours, record.hourlyRate)) }}
          </template>
        </template>
      </a-table>
      <a-textarea v-model:value="batchBase.workContent" :rows="3" placeholder="工作内容" class="batch-content" />
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { timedWageApi } from '../../api/admin'
import { employeeApi } from '../../api/system'

type EmployeeOption = {
  id: number
  employeeNo: string
  name: string
}

const loading = ref(false)
const submitting = ref(false)
const batchSubmitting = ref(false)
const modalVisible = ref(false)
const batchVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()
const dataList = ref<any[]>([])
const employeeOptions = ref<EmployeeOption[]>([])

const searchEmployeeId = ref<number | undefined>(undefined)
const searchYearMonth = ref<string | undefined>(dayjs().format('YYYY-MM'))
const searchDateRange = ref<string[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const formData = reactive({
  employeeId: undefined as number | undefined,
  workDate: dayjs().format('YYYY-MM-DD'),
  hours: undefined as number | undefined,
  hourlyRate: undefined as number | undefined,
  workContent: '',
})

const batchBase = reactive({
  workDate: dayjs().format('YYYY-MM-DD'),
  hours: undefined as number | undefined,
  hourlyRate: undefined as number | undefined,
  workContent: '',
})
const batchSelectedEmployeeIds = ref<number[]>([])
const batchRows = ref<any[]>([])

const columns = [
  { title: '工人', key: 'employee', width: 160 },
  { title: '日期', dataIndex: 'workDate', key: 'workDate', width: 120 },
  { title: '工时', dataIndex: 'hours', key: 'hours', width: 90 },
  { title: '时薪', dataIndex: 'hourlyRate', key: 'hourlyRate', width: 90 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '工作内容', dataIndex: 'workContent', key: 'workContent' },
  { title: '录入人', key: 'createdBy', width: 100 },
  { title: '录入时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 120 },
]

const batchColumns = [
  { title: '工人', key: 'employee', width: 180 },
  { title: '工时', key: 'hours', width: 120 },
  { title: '时薪', key: 'hourlyRate', width: 120 },
  { title: '金额', key: 'amount', width: 120 },
]

function formatEmployee(emp: EmployeeOption) {
  return `${emp.employeeNo || ''} ${emp.name || ''}`.trim()
}

function filterEmployeeOption(input: string, option: any) {
  const text = String(option?.children?.[0]?.children || option?.children || '').toLowerCase()
  return text.includes(input.toLowerCase())
}

function money(value: any) {
  const num = Number(value || 0)
  return `¥${num.toFixed(2)}`
}

function calcAmount(hours?: number, hourlyRate?: number) {
  return Number(hours || 0) * Number(hourlyRate || 0)
}

async function fetchEmployees() {
  const res: any = await employeeApi.list({ page: 1, size: 1000 })
  employeeOptions.value = res.data?.records || []
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      size: pagination.pageSize,
    }
    if (searchEmployeeId.value) params.employeeId = searchEmployeeId.value
    if (searchYearMonth.value) params.yearMonth = searchYearMonth.value
    if (searchDateRange.value?.length === 2) {
      params.startDate = searchDateRange.value[0]
      params.endDate = searchDateRange.value[1]
      delete params.yearMonth
    }
    const res: any = await timedWageApi.list(params)
    dataList.value = res.data?.records || []
    pagination.total = res.data?.total || 0
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
  searchYearMonth.value = dayjs().format('YYYY-MM')
  searchDateRange.value = []
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
    formData.hours = Number(record.hours)
    formData.hourlyRate = Number(record.hourlyRate)
    formData.workContent = record.workContent || ''
  } else {
    editingId.value = null
    formData.employeeId = undefined
    formData.workDate = dayjs().format('YYYY-MM-DD')
    formData.hours = undefined
    formData.hourlyRate = undefined
    formData.workContent = ''
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
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  await timedWageApi.delete(id)
  message.success('删除成功')
  fetchData()
}

function openBatchModal() {
  batchBase.workDate = dayjs().format('YYYY-MM-DD')
  batchBase.hours = undefined
  batchBase.hourlyRate = undefined
  batchBase.workContent = ''
  batchSelectedEmployeeIds.value = []
  batchRows.value = []
  batchVisible.value = true
}

function syncBatchRows() {
  const currentById = new Map(batchRows.value.map((row) => [row.employeeId, row]))
  batchRows.value = batchSelectedEmployeeIds.value.map((id) => {
    const emp = employeeOptions.value.find((item) => item.id === id)
    return currentById.get(id) || {
      employeeId: id,
      employeeNo: emp?.employeeNo,
      employeeName: emp?.name,
      hours: batchBase.hours,
      hourlyRate: batchBase.hourlyRate,
    }
  })
}

function applyBatchBase() {
  batchRows.value.forEach((row) => {
    row.hours = batchBase.hours
    row.hourlyRate = batchBase.hourlyRate
  })
}

async function handleBatchSubmit() {
  if (!batchBase.workDate) {
    message.warning('请选择日期')
    return
  }
  const invalid = batchRows.value.some((row) => !row.employeeId || !row.hours || row.hourlyRate === undefined)
  if (batchRows.value.length === 0 || invalid) {
    message.warning('请完整填写批量记录')
    return
  }
  batchSubmitting.value = true
  try {
    const payload = batchRows.value.map((row) => ({
      employeeId: row.employeeId,
      workDate: batchBase.workDate,
      hours: row.hours,
      hourlyRate: row.hourlyRate,
      workContent: batchBase.workContent,
    }))
    await timedWageApi.createBatch(payload)
    message.success('批量新增成功')
    batchVisible.value = false
    fetchData()
  } finally {
    batchSubmitting.value = false
  }
}

onMounted(async () => {
  await fetchEmployees()
  fetchData()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.batch-form {
  margin-bottom: 12px;
}

.batch-actions {
  margin-bottom: 12px;
}

.batch-content {
  margin-top: 12px;
}
</style>
