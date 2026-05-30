<template>
  <a-card title="工序管理">
    <div class="toolbar">
      <a-space>
        <a-select
          v-model:value="searchCategory"
          placeholder="按分类筛选"
          allow-clear
          show-search
          style="width: 200px"
          :options="categoryOptions.map(c => ({ label: c, value: c }))"
          :filter-option="(input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())"
          @change="handleSearch"
        />
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
        <template v-if="column.key === 'category'">
          <a-tag v-if="record.category" color="blue">{{ record.category }}</a-tag>
        </template>
        <template v-if="column.key === 'refPrice'">
          <span v-if="record.refPrice">{{ record.refPrice }} 元/件</span>
        </template>
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
        <a-form-item label="分类" name="category" :rules="[{ required: true, message: '请选择或输入分类' }]">
          <a-select
            v-model:value="formData.category"
            placeholder="请选择或输入分类"
            show-search
            allow-clear
            mode="tags"
            :max-tag-count="1"
            :options="categoryOptions.map(c => ({ label: c, value: c }))"
            :filter-option="(input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())"
            @search="handleCategorySearch"
          />
        </a-form-item>
        <a-form-item label="参考工价" name="refPrice" :rules="[{ required: true, message: '请输入参考工价' }]">
          <a-input-number v-model:value="formData.refPrice" :min="0" :precision="2" style="width: 100%" placeholder="请输入参考工价（元/件）" />
        </a-form-item>
        <a-form-item label="排序" name="sort">
          <a-input-number v-model:value="formData.sort" :min="0" style="width: 100%" placeholder="请输入排序号" />
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

async function fetchCategories() {
  try {
    const res: any = await processApi.getCategories()
    categoryOptions.value = res.data || []
  } catch {
    // handled by interceptor
  }
}

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const formData = reactive({
  name: '',
  category: [] as string[],
  refPrice: undefined as number | undefined,
  sort: 0,
})

const columns = [
  { title: '工序名称', dataIndex: 'name', key: 'name' },
  { title: '分类', dataIndex: 'category', key: 'category' },
  { title: '参考工价', dataIndex: 'refPrice', key: 'refPrice' },
  { title: '排序', dataIndex: 'sort', key: 'sort' },
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

function handleCategorySearch(value: string) {
  if (value && !categoryOptions.value.includes(value)) {
    categoryOptions.value = [...categoryOptions.value, value]
  }
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
    formData.category = record.category ? [record.category] : []
    formData.refPrice = record.refPrice
    formData.sort = record.sort ?? 0
  } else {
    editingId.value = null
    formData.name = ''
    formData.category = []
    formData.refPrice = undefined
    formData.sort = 0
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
    // 处理分类字段：mode="tags" 返回数组，取最后一个值
    const category = Array.isArray(formData.category)
      ? (formData.category as string[])[formData.category.length - 1] || ''
      : formData.category
    const data = { ...formData, category }
    if (editingId.value) {
      await processApi.update(editingId.value, data)
      message.success('编辑成功')
    } else {
      await processApi.create(data)
      message.success('新增成功')
    }
    modalVisible.value = false
    fetchData()
    fetchCategories()
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
  fetchCategories()
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
