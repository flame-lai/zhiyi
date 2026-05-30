<template>
  <a-card title="收支管理">
    <div class="toolbar">
      <a-space>
        <a-input
          v-model:value="searchOrderId"
          placeholder="按订单ID查询"
          allow-clear
          style="width: 200px"
          @press-enter="handleSearch"
        />
        <a-select
          v-model:value="searchType"
          placeholder="按类型筛选"
          allow-clear
          style="width: 150px"
          @change="handleSearch"
        >
          <a-select-option value="收款">收款</a-select-option>
          <a-select-option value="支出">支出</a-select-option>
        </a-select>
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="resetSearch">重置</a-button>
      </a-space>
      <a-button type="primary" @click="openModal()">新增</a-button>
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
        <template v-if="column.key === 'type'">
          <a-tag :color="record.type === '支出' ? 'red' : 'green'">
            {{ record.type }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="openModal(record)">编辑</a>
            <a-popconfirm
              title="确定删除该收支记录吗？"
              @confirm="handleDelete(record.id)"
            >
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑收支' : '新增收支'"
      @ok="handleSubmit"
      :confirm-loading="submitting"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="订单ID" name="orderId" :rules="[{ required: true, message: '请输入订单ID' }]">
          <a-input-number v-model:value="formData.orderId" :min="1" style="width: 100%" placeholder="请输入订单ID" />
        </a-form-item>
        <a-form-item label="类型" name="type" :rules="[{ required: true, message: '请选择类型' }]">
          <a-select v-model:value="formData.type" placeholder="请选择类型">
            <a-select-option value="收款">收款</a-select-option>
            <a-select-option value="支出">支出</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="金额" name="amount" :rules="[{ required: true, message: '请输入金额' }]">
          <a-input-number v-model:value="formData.amount" :min="0" :precision="2" style="width: 100%" placeholder="请输入金额" />
        </a-form-item>
        <a-form-item label="日期" name="paymentDate" :rules="[{ required: true, message: '请选择日期' }]">
          <a-date-picker v-model:value="formData.paymentDate" style="width: 100%" value-format="YYYY-MM-DD" placeholder="请选择日期" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formData.remark" :rows="3" placeholder="请输入备注" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { paymentApi } from '../../api/finance'

const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const searchOrderId = ref<string>('')
const searchType = ref<string | undefined>(undefined)
const dataList = ref<any[]>([])
const formRef = ref()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const formData = reactive({
  orderId: undefined as number | undefined,
  type: undefined as string | undefined,
  amount: undefined as number | undefined,
  paymentDate: undefined as string | undefined,
  remark: '',
})

const columns = [
  { title: '订单ID', dataIndex: 'orderId', key: 'orderId' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '日期', dataIndex: 'paymentDate', key: 'paymentDate' },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 150 },
]

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchOrderId.value) {
      params.orderId = searchOrderId.value
    }
    if (searchType.value) {
      params.type = searchType.value
    }
    const res: any = await paymentApi.list(params)
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
  searchOrderId.value = ''
  searchType.value = undefined
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
    formData.orderId = record.orderId
    formData.type = record.type
    formData.amount = record.amount
    formData.paymentDate = record.paymentDate
    formData.remark = record.remark || ''
  } else {
    editingId.value = null
    formData.orderId = undefined
    formData.type = undefined
    formData.amount = undefined
    formData.paymentDate = undefined
    formData.remark = ''
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
      await paymentApi.update(editingId.value, data)
      message.success('编辑成功')
    } else {
      await paymentApi.create(data)
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
    await paymentApi.delete(id)
    message.success('删除成功')
    fetchData()
  } catch {
    // handled by interceptor
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
