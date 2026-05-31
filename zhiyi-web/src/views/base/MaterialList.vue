<template>
  <a-card title="物料管理">
    <div class="toolbar">
      <a-space>
        <a-input
          v-model:value="searchKeyword"
          placeholder="搜索物料名称"
          allow-clear
          style="width: 200px"
          @press-enter="handleSearch"
        />
        <a-select
          v-model:value="searchType"
          placeholder="按类型筛选"
          allow-clear
          style="width: 120px"
          @change="handleSearch"
        >
          <a-select-option :value="1">原料</a-select-option>
          <a-select-option :value="2">辅料</a-select-option>
        </a-select>
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="resetSearch">重置</a-button>
      </a-space>
      <a-button type="primary" @click="openModal()">新增物料</a-button>
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
          <a-tag :color="record.type === 1 ? 'blue' : 'green'">
            {{ record.type === 1 ? '原料' : '辅料' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'refPrice'">
          <span v-if="record.refPrice">{{ record.refPrice }} 元/{{ record.unit }}</span>
          <span v-else>-</span>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="openModal(record)">编辑</a>
            <a-popconfirm
              title="确定删除该物料吗？"
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
      :title="editingId ? '编辑物料' : '新增物料'"
      @ok="handleSubmit"
      :confirm-loading="submitting"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="物料名称" name="materialName" :rules="[{ required: true, message: '请输入物料名称' }]">
          <a-input v-model:value="formData.materialName" placeholder="请输入物料名称" />
        </a-form-item>
        <a-form-item label="类型" name="type" :rules="[{ required: true, message: '请选择类型' }]">
          <a-radio-group v-model:value="formData.type">
            <a-radio :value="1">原料</a-radio>
            <a-radio :value="2">辅料</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="规格" name="specification">
          <a-input v-model:value="formData.specification" placeholder="如：150cm门幅、5号" />
        </a-form-item>
        <a-form-item label="单位" name="unit" :rules="[{ required: true, message: '请输入单位' }]">
          <a-input v-model:value="formData.unit" placeholder="如：米、条、个、卷" />
        </a-form-item>
        <a-form-item label="参考单价" name="refPrice">
          <a-input-number v-model:value="formData.refPrice" :min="0" :precision="2" style="width: 100%" placeholder="元/单位" />
        </a-form-item>
        <a-form-item label="默认供应商" name="supplierId">
          <a-select
            v-model:value="formData.supplierId"
            placeholder="请选择供应商"
            allow-clear
            show-search
            :filter-option="filterSupplierOption"
          >
            <a-select-option v-for="s in supplierList" :key="s.id" :value="s.id">
              {{ s.supplierName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { materialApi, supplierApi } from '../../api/base'

const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const searchKeyword = ref('')
const searchType = ref<number | undefined>(undefined)
const dataList = ref<any[]>([])
const supplierList = ref<any[]>([])
const formRef = ref()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const formData = reactive({
  materialName: '',
  type: 1,
  specification: '',
  unit: '',
  refPrice: undefined as number | undefined,
  supplierId: undefined as number | undefined,
})

const columns = [
  { title: '物料编号', dataIndex: 'materialCode', key: 'materialCode', width: 100 },
  { title: '物料名称', dataIndex: 'materialName', key: 'materialName' },
  { title: '类型', dataIndex: 'type', key: 'type', width: 80 },
  { title: '规格', dataIndex: 'specification', key: 'specification' },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '参考单价', dataIndex: 'refPrice', key: 'refPrice', width: 120 },
  { title: '操作', key: 'action', width: 150 },
]

function filterSupplierOption(input: string, option: any) {
  const supplier = supplierList.value.find((s: any) => s.id === option.value)
  return supplier?.supplierName?.toLowerCase().includes(input.toLowerCase()) ?? false
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      size: pagination.pageSize,
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    if (searchType.value) {
      params.type = searchType.value
    }
    const res: any = await materialApi.list(params)
    dataList.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function fetchSuppliers() {
  try {
    const res: any = await supplierApi.list({ page: 1, size: 9999 })
    supplierList.value = res.data?.records || []
  } catch {
    // handled by interceptor
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function resetSearch() {
  searchKeyword.value = ''
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
    formData.materialName = record.materialName || ''
    formData.type = record.type || 1
    formData.specification = record.specification || ''
    formData.unit = record.unit || ''
    formData.refPrice = record.refPrice
    formData.supplierId = record.supplierId
  } else {
    editingId.value = null
    formData.materialName = ''
    formData.type = 1
    formData.specification = ''
    formData.unit = ''
    formData.refPrice = undefined
    formData.supplierId = undefined
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
      await materialApi.update(editingId.value, data)
      message.success('编辑成功')
    } else {
      await materialApi.create(data)
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
    await materialApi.delete(id)
    message.success('删除成功')
    fetchData()
  } catch {
    // handled by interceptor
  }
}

onMounted(() => {
  fetchData()
  fetchSuppliers()
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
