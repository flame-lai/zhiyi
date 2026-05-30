<template>
  <a-card title="供应商管理">
    <div class="toolbar">
      <a-space>
        <a-input
          v-model:value="searchKeyword"
          placeholder="搜索供应商名称/联系人/电话"
          allow-clear
          style="width: 260px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="resetSearch">重置</a-button>
      </a-space>
      <a-button type="primary" @click="openModal()">新增供应商</a-button>
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
              title="确定删除该供应商吗？"
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
      :title="editingId ? '编辑供应商' : '新增供应商'"
      @ok="handleSubmit"
      :confirm-loading="submitting"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="名称" name="name" :rules="[{ required: true, message: '请输入供应商名称' }]">
          <a-input v-model:value="formData.name" placeholder="请输入供应商名称" />
        </a-form-item>
        <a-form-item label="联系人" name="contact">
          <a-input v-model:value="formData.contact" placeholder="请输入联系人" />
        </a-form-item>
        <a-form-item label="电话" name="phone">
          <a-input v-model:value="formData.phone" placeholder="请输入电话" />
        </a-form-item>
        <a-form-item label="地址" name="address">
          <a-textarea v-model:value="formData.address" placeholder="请输入地址" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { supplierApi } from '../../api/base'

const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const searchKeyword = ref('')
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
  name: '',
  contact: '',
  phone: '',
  address: '',
})

const columns = [
  { title: '供应商编号', dataIndex: 'supplierNo', key: 'supplierNo' },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '联系人', dataIndex: 'contact', key: 'contact' },
  { title: '电话', dataIndex: 'phone', key: 'phone' },
  { title: '地址', dataIndex: 'address', key: 'address', ellipsis: true },
  { title: '操作', key: 'action', width: 150 },
]

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    const res: any = await supplierApi.list(params)
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
  searchKeyword.value = ''
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
    formData.contact = record.contact || ''
    formData.phone = record.phone || ''
    formData.address = record.address || ''
  } else {
    editingId.value = null
    formData.name = ''
    formData.contact = ''
    formData.phone = ''
    formData.address = ''
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
      await supplierApi.update(editingId.value, data)
      message.success('编辑成功')
    } else {
      await supplierApi.create(data)
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
    await supplierApi.delete(id)
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
