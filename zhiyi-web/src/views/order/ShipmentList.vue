<template>
  <div>
    <a-card title="出货记录">
      <!-- 操作按钮 -->
      <div style="margin-bottom: 16px; display: flex; gap: 16px; flex-wrap: wrap; align-items: center">
        <a-range-picker
          v-model:value="searchForm.dateRange"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :placeholder="['开始日期', '结束日期']"
          style="width: 280px"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button type="primary" @click="handleAdd">新增出货</a-button>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'action'">
            <a-popconfirm
              title="确定删除该出货记录吗？"
              @confirm="handleDelete(record)"
            >
              <a-button type="link" danger size="small">删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增出货弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      title="新增出货"
      :width="600"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="订单ID" name="orderId">
          <a-input-number
            v-model:value="formData.orderId"
            placeholder="请输入订单ID"
            style="width: 100%"
            :min="1"
          />
        </a-form-item>
        <a-form-item label="数量" name="qty">
          <a-input-number
            v-model:value="formData.qty"
            placeholder="请输入数量"
            style="width: 100%"
            :min="1"
          />
        </a-form-item>
        <a-form-item label="出货日期" name="shipmentDate">
          <a-date-picker
            v-model:value="formData.shipmentDate"
            style="width: 100%"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="formData.remark"
            placeholder="请输入备注"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { shipmentApi } from '../../api/order'

// 表格相关
const loading = ref(false)
const dataSource = ref([])
const searchForm = reactive({
  dateRange: []
})
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

// 表格列定义
const columns = [
  { title: '订单ID', dataIndex: 'orderId', width: 120 },
  { title: '数量', dataIndex: 'qty', width: 100, align: 'right' },
  { title: '出货日期', dataIndex: 'shipmentDate', width: 150 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', dataIndex: 'action', width: 100, align: 'center', fixed: 'right' }
]

// 弹窗相关
const modalVisible = ref(false)
const formRef = ref(null)

const formData = reactive({
  orderId: undefined,
  qty: undefined,
  shipmentDate: '',
  remark: ''
})

const rules = {
  orderId: [{ required: true, message: '请输入订单ID' }],
  qty: [{ required: true, message: '请输入数量' }],
  shipmentDate: [{ required: true, message: '请选择出货日期' }]
}

// 加载出货列表
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      size: pagination.pageSize
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await shipmentApi.getList(params)
    dataSource.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error('加载出货列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 查询
const handleSearch = () => {
  pagination.current = 1
  loadData()
}

// 重置筛选
const handleReset = () => {
  searchForm.dateRange = []
  handleSearch()
}

// 表格分页变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

// 新增
const handleAdd = () => {
  modalVisible.value = true
  resetForm()
}

// 删除
const handleDelete = async (record) => {
  try {
    await shipmentApi.delete(record.id)
    message.success('删除成功')
    loadData()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validateFields()
    await shipmentApi.create(formData)
    message.success('创建成功')
    modalVisible.value = false
    loadData()
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

// 取消
const handleCancel = () => {
  modalVisible.value = false
  resetForm()
}

// 重置表单
const resetForm = () => {
  formData.orderId = undefined
  formData.qty = undefined
  formData.shipmentDate = ''
  formData.remark = ''
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 初始化
onMounted(() => {
  loadData()
})
</script>
