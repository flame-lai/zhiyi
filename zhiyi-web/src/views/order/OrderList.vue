<template>
  <div>
    <a-card title="订单列表">
      <!-- 搜索区域 -->
      <div style="margin-bottom: 16px; display: flex; gap: 16px; flex-wrap: wrap">
        <a-input
          v-model:value="searchForm.keyword"
          placeholder="搜索订单号/款号"
          style="width: 200px"
          allow-clear
          @pressEnter="handleSearch"
        />
        <a-select
          v-model:value="searchForm.status"
          placeholder="选择状态"
          style="width: 150px"
          allow-clear
        >
          <a-select-option :value="0">待确认</a-select-option>
          <a-select-option :value="1">已确认</a-select-option>
          <a-select-option :value="2">生产中</a-select-option>
          <a-select-option :value="3">部分出货</a-select-option>
          <a-select-option :value="4">已完工</a-select-option>
          <a-select-option :value="5">已出货</a-select-option>
          <a-select-option :value="6">已结算</a-select-option>
        </a-select>
        <a-range-picker
          v-model:value="searchForm.dateRange"
          format="YYYY-MM-DD"
          style="width: 240px"
          :placeholder="['开始日期', '结束日期']"
        />
        <a-button @click="handleThisMonth">本月</a-button>
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button type="primary" @click="handleAdd">新增订单</a-button>
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
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="statusColorMap[record.status]">
              {{ statusTextMap[record.status] }}
            </a-tag>
          </template>
          <template v-if="column.dataIndex === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record)">查看详情</a-button>
              <a-button
                v-if="record.status === 0"
                type="link"
                size="small"
                @click="handleEdit(record)"
              >
                编辑
              </a-button>
              <a-button
                v-if="record.status === 0"
                type="link"
                size="small"
                @click="handleConfirm(record)"
              >
                确认订单
              </a-button>
              <a-popconfirm
                v-if="record.status === 5"
                title="确定要结算该订单吗？"
                @confirm="handleSettle(record)"
              >
                <a-button type="link" size="small">结算订单</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑订单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑订单' : '新增订单'"
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
        <a-form-item label="客户" name="customerId">
          <a-select
            v-model:value="formData.customerId"
            placeholder="请选择客户"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option
              v-for="item in customerList"
              :key="item.id"
              :value="item.id"
            >
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="订单金额" name="totalAmount">
          <a-input-number
            v-model:value="formData.totalAmount"
            :min="0"
            :precision="2"
            placeholder="整单总金额"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="计划交期" name="deliveryDate">
          <a-date-picker
            v-model:value="formData.deliveryDate"
            style="width: 100%"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { orderApi } from '../../api/order'
import { customerApi } from '../../api/base'

const router = useRouter()

// 搜索表单
const searchForm = reactive({
  keyword: '',
  status: undefined,
  dateRange: []
})

// 表格相关
const loading = ref(false)
const dataSource = ref([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

// 状态颜色映射
const statusColorMap = {
  0: 'default',
  1: 'blue',
  2: 'processing',
  3: 'warning',
  4: 'purple',
  5: 'success',
  6: 'default'
}

// 状态文本映射
const statusTextMap = {
  0: '待确认',
  1: '已确认',
  2: '生产中',
  3: '部分出货',
  4: '已完工',
  5: '已出货',
  6: '已结算'
}

// 表格列定义
const columns = [
  { title: '订单号', dataIndex: 'orderNo', width: 150 },
  { title: '客户', dataIndex: 'customerName', width: 120 },
  { title: '款号明细', dataIndex: 'styleNames', width: 180 },
  { title: '总件数', dataIndex: 'totalQty', width: 90, align: 'right' },
  { title: '订单金额', dataIndex: 'totalAmount', width: 110, align: 'right' },
  { title: '状态', dataIndex: 'status', width: 90, align: 'center' },
  { title: '交货期', dataIndex: 'deliveryDate', width: 110 },
  { title: '创建时间', dataIndex: 'createdAt', width: 170 },
  { title: '操作', dataIndex: 'action', width: 200, fixed: 'right' }
]

// 弹窗相关
const modalVisible = ref(false)
const formRef = ref(null)
const customerList = ref([])
const editingId = ref(null)

const formData = reactive({
  customerId: undefined,
  totalAmount: undefined,
  deliveryDate: '',
  remark: ''
})

const rules = {
  customerId: [{ required: true, message: '请选择客户' }]
}

// 加载订单列表
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      size: pagination.pageSize,
      keyword: searchForm.keyword,
      status: searchForm.status
    }
    // 日期范围
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0].format('YYYY-MM-DD')
      params.endDate = searchForm.dateRange[1].format('YYYY-MM-DD')
    }
    const res = await orderApi.list(params)
    dataSource.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error('加载订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载客户列表
const loadCustomers = async () => {
  try {
    const res = await customerApi.list({ page: 1, size: 9999 })
    customerList.value = res.data?.records || []
  } catch (error) {
    console.error('加载客户列表失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = undefined
  searchForm.dateRange = []
  handleSearch()
}

// 本月
const handleThisMonth = () => {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), 1)
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  searchForm.dateRange = [
    dayjs(start),
    dayjs(end)
  ]
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

// 查看详情
const handleView = (record) => {
  router.push(`/order/detail/${record.id}`)
}

// 编辑
const handleEdit = (record) => {
  // 打开编辑弹窗，预填数据
  modalVisible.value = true
  formData.customerId = record.customerId
  formData.totalAmount = record.totalAmount
  formData.deliveryDate = record.deliveryDate
  formData.remark = record.remark
  editingId.value = record.id
}

// 确认订单
const handleConfirm = (record) => {
  Modal.confirm({
    title: '确认订单',
    content: '确定要确认该订单吗？',
    onOk: async () => {
      try {
        await orderApi.confirm(record.id)
        message.success('订单已确认')
        loadData()
      } catch (error) {
        console.error('确认订单失败:', error)
      }
    }
  })
}

// 结算订单
const handleSettle = async (record) => {
  try {
    await orderApi.settle(record.id)
    message.success('订单已结算')
    loadData()
  } catch (error) {
    console.error('结算订单失败:', error)
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validateFields()
    const data = {
      customerId: formData.customerId,
      totalAmount: formData.totalAmount,
      deliveryDate: formData.deliveryDate,
      remark: formData.remark
    }
    if (editingId.value) {
      await orderApi.update(editingId.value, data)
      message.success('编辑成功')
    } else {
      await orderApi.create({ order: data, items: [] })
      message.success('创建成功')
    }
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
  editingId.value = null
  formData.customerId = undefined
  formData.totalAmount = undefined
  formData.deliveryDate = ''
  formData.remark = ''
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 客户搜索过滤
const filterOption = (input, option) => {
  return option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

// 初始化
onMounted(() => {
  loadData()
  loadCustomers()
})
</script>
