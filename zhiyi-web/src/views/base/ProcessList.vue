<template>
  <a-card title="工序管理">
    <div class="toolbar">
      <a-space>
        <a-select
          v-model:value="searchCategory"
          placeholder="按分类筛选"
          allow-clear
          style="width: 200px"
          @change="handleSearch"
        >
          <a-select-option v-for="c in categoryOptions" :key="c" :value="c">{{ c }}</a-select-option>
        </a-select>
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="resetSearch">重置</a-button>
      </a-space>
      <a-button type="primary" @click="openModal()">新增工序</a-button>
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
              title="确定删除该工序吗？"
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
      :title="editingId ? '编辑工序' : '新增工序'"
      @ok="handleSubmit"
      :confirm-loading="submitting"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="工序名称" name="name" :rules="[{ required: true, message: '请输入工序名称' }]">
          <a-input v-model:value="formData.name" placeholder="请输入工序名称" />
        </a-form-item>
        <a-form-item label="分类" name="category">
          <a-input v-model:value="formData.category" placeholder="请输入分类" />
        </a-form-item>
        <a-form-item label="单价" name="price">
          <a-input-number v-model:value="formData.price" :min="0" :precision="2" style="width: 100%" placeholder="请输入单价" />
        </a-form-item>
        <a-form-item label="排序" name="sortOrder">
          <a-input-number v-model:value="formData.sortOrder" :min="0" style="width: 100%" placeholder="请输入排序号" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { processApi } from '../../api/base'

const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const searchCategory = ref<string | undefined>(undefined)
const dataList = ref<any[]>([])
const categoryOptions = ref<string[]>([])
const formRef = ref()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const formData = reactive({
  name: '',
  category: '',
  price: undefined as number | undefined,
  sortOrder: 0,
})

const columns = [
  { title: '工序名称', dataIndex: 'name', key: 'name' },
  { title: '分类', dataIndex: 'category', key: 'category' },
  { title: '单价', dataIndex: 'price', key: 'price' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder' },
  { title: '操作', key: 'action', width: 150 },
]

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchCategory.value) {
      params.category = searchCategory.value
    }
    const res: any = await processApi.list(params)
    dataList.value = res.data?.records || res.data?.list || res.data || []
    pagination.total = res.data?.total || 0
    // Extract unique categories for filter
    const cats = new Set(dataList.value.map((d: any) => d.category).filter(Boolean))
    categoryOptions.value = Array.from(cats) as string[]
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
  searchCategory.value = undefined
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
    formData.name = record.name || ''
    formData.category = record.category || ''
    formData.price = record.price
    formData.sortOrder = record.sortOrder ?? 0
  } else {
    editingId.value = null
    formData.name = ''
    formData.category = ''
    formData.price = undefined
    formData.sortOrder = 0
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
      await processApi.update(editingId.value, data)
      message.success('编辑成功')
    } else {
      await processApi.create(data)
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
    await processApi.delete(id)
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
