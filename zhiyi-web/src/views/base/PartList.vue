<template>
  <div>
    <a-card title="部位管理">
      <!-- 搜索区域 -->
      <div style="margin-bottom: 16px; display: flex; gap: 16px; flex-wrap: wrap">
        <a-input
          v-model:value="searchKeyword"
          placeholder="搜索部位名称"
          style="width: 200px"
          allow-clear
          @pressEnter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button type="primary" @click="openModal()">新增部位</a-button>
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
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'action'">
            <a-space>
              <a @click="openModal(record)">编辑</a>
              <a-popconfirm title="确定删除该部位？" @confirm="handleDelete(record.id)">
                <a style="color: #ff4d4f">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑部位' : '新增部位'"
      @ok="handleSubmit"
      :confirm-loading="submitLoading"
    >
      <a-form :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="部位名称" required>
          <a-input v-model:value="formData.name" placeholder="请输入部位名称" />
        </a-form-item>
        <a-form-item label="排序号">
          <a-input-number v-model:value="formData.sort" :min="0" :precision="0" placeholder="数字越小越靠前" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { partApi } from '../../api/base'

// 表格列定义
const columns = [
  { title: '排序号', dataIndex: 'sort', width: 100, align: 'center' },
  { title: '部位名称', dataIndex: 'name', width: 200 },
  { title: '操作', dataIndex: 'action', width: 150, align: 'center' },
]

// 搜索
const searchKeyword = ref('')

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

// 弹窗相关
const modalVisible = ref(false)
const submitLoading = ref(false)
const editingId = ref<number | null>(null)
const formData = reactive({
  name: '',
  sort: undefined as number | undefined,
})

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res: any = await partApi.list({
      page: pagination.current,
      size: pagination.pageSize,
      keyword: searchKeyword.value || undefined,
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.current = 1
  loadData()
}

// 重置
function handleReset() {
  searchKeyword.value = ''
  pagination.current = 1
  loadData()
}

// 表格分页变化
function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

// 打开弹窗
function openModal(record?: any) {
  if (record) {
    editingId.value = record.id
    formData.name = record.name
    formData.sort = record.sort
  } else {
    editingId.value = null
    formData.name = ''
    formData.sort = undefined
  }
  modalVisible.value = true
}

// 提交表单
async function handleSubmit() {
  if (!formData.name) {
    message.warning('请输入部位名称')
    return
  }
  submitLoading.value = true
  try {
    if (editingId.value) {
      await partApi.update(editingId.value, { ...formData })
      message.success('编辑成功')
    } else {
      await partApi.create({ ...formData })
      message.success('新增成功')
    }
    modalVisible.value = false
    loadData()
  } catch {
    // error handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

// 删除
async function handleDelete(id: number) {
  try {
    await partApi.delete(id)
    message.success('删除成功')
    loadData()
  } catch {
    // error handled by interceptor
  }
}

// 初始化加载
loadData()
</script>
