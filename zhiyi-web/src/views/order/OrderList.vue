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
          <a-select-option value="待确认">待确认</a-select-option>
          <a-select-option value="已确认">已确认</a-select-option>
          <a-select-option value="生产中">生产中</a-select-option>
          <a-select-option value="已完工">已完工</a-select-option>
          <a-select-option value="已结算">已结算</a-select-option>
        </a-select>
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
              {{ record.status }}
            </a-tag>
          </template>
          <template v-if="column.dataIndex === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record)">查看详情</a-button>
              <a-button
                v-if="record.status === '待确认'"
                type="link"
                size="small"
                @click="handleConfirm(record)"
              >
                确认订单
              </a-button>
              <a-popconfirm
                v-if="record.status === '已完工'"
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

    <!-- 新增订单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      title="新增订单"
      :width="900"
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
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="订单号" name="orderNo">
              <a-input v-model:value="formData.orderNo" disabled placeholder="自动生成" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
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
          </a-col>
          <a-col :span="12">
            <a-form-item label="款号" name="styleId">
              <a-input v-model:value="formData.styleId" placeholder="请输入款号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="交货期" name="deliveryDate">
              <a-date-picker
                v-model:value="formData.deliveryDate"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 订单明细表格 -->
        <div style="margin-top: 16px">
          <div style="margin-bottom: 8px; font-weight: bold">
            订单明细
            <a-button type="link" size="small" @click="addItem">+ 添加</a-button>
          </div>
          <a-table
            :columns="itemColumns"
            :data-source="formData.items"
            :pagination="false"
            row-key="key"
            size="small"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.dataIndex === 'color'">
                <a-input v-model:value="record.color" placeholder="颜色" />
              </template>
              <template v-if="column.dataIndex === 'size'">
                <a-input v-model:value="record.size" placeholder="尺码" />
              </template>
              <template v-if="column.dataIndex === 'qty'">
                <a-input-number v-model:value="record.qty" :min="1" placeholder="数量" />
              </template>
              <template v-if="column.dataIndex === 'action'">
                <a-popconfirm title="确定删除该行？" @confirm="removeItem(index)">
                  <a-button type="link" danger size="small">删除</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { orderApi } from '../../api/order'
import { customerApi } from '../../api/base'

const router = useRouter()

// 搜索表单
const searchForm = reactive({
  keyword: '',
  status: undefined
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
  '待确认': 'default',
  '已确认': 'blue',
  '生产中': 'processing',
  '已完工': 'success',
  '已结算': 'green'
}

// 表格列定义
const columns = [
  { title: '订单号', dataIndex: 'orderNo', width: 150 },
  { title: '客户', dataIndex: 'customerName', width: 120 },
  { title: '款号', dataIndex: 'styleId', width: 120 },
  { title: '总数量', dataIndex: 'totalQty', width: 100, align: 'right' },
  { title: '状态', dataIndex: 'status', width: 100, align: 'center' },
  { title: '交货期', dataIndex: 'deliveryDate', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', dataIndex: 'action', width: 200, fixed: 'right' }
]

// 弹窗相关
const modalVisible = ref(false)
const formRef = ref(null)
const customerList = ref([])

const formData = reactive({
  orderNo: '',
  customerId: undefined,
  styleId: '',
  deliveryDate: '',
  items: []
})

const rules = {
  customerId: [{ required: true, message: '请选择客户' }],
  styleId: [{ required: true, message: '请输入款号' }],
  deliveryDate: [{ required: true, message: '请选择交货期' }]
}

// 明细表格列定义
const itemColumns = [
  { title: '颜色', dataIndex: 'color', width: 150 },
  { title: '尺码', dataIndex: 'size', width: 150 },
  { title: '数量', dataIndex: 'qty', width: 120 },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' }
]

// 加载订单列表
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      pageSize: pagination.pageSize,
      ...searchForm
    }
    const res = await orderApi.getList(params)
    dataSource.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('加载订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载客户列表
const loadCustomers = async () => {
  try {
    const res = await customerApi.getList()
    customerList.value = res.data
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
    const data = { ...formData }
    data.totalQty = data.items.reduce((sum, item) => sum + (item.qty || 0), 0)
    await orderApi.create(data)
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
  formData.orderNo = ''
  formData.customerId = undefined
  formData.styleId = ''
  formData.deliveryDate = ''
  formData.items = [{ key: Date.now(), color: '', size: '', qty: 1 }]
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 添加明细行
const addItem = () => {
  formData.items.push({
    key: Date.now(),
    color: '',
    size: '',
    qty: 1
  })
}

// 删除明细行
const removeItem = (index) => {
  formData.items.splice(index, 1)
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
