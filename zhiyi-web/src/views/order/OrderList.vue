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
      :width="800"
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
        <a-form-item label="款号" name="styleId" v-if="!editingId">
          <a-select
            v-model:value="formData.styleId"
            placeholder="请选择款号"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option
              v-for="item in styleList"
              :key="item.id"
              :value="item.id"
            >
              {{ item.styleCode }} - {{ item.styleName }}
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

      <!-- 颜色尺码矩阵 -->
      <div v-if="!editingId" style="margin-top: 16px;">
        <div style="margin-bottom: 8px; font-weight: bold;">颜色尺码明细</div>
        <div style="margin-bottom: 8px;">
          <a-button size="small" @click="addColorRow">添加颜色</a-button>
          <a-button size="small" style="margin-left: 8px" @click="addSizeColumn">添加尺码</a-button>
        </div>
        <a-table
          :columns="matrixColumns"
          :data-source="matrixData"
          :pagination="false"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.dataIndex === 'color'">
              <a-input v-model:value="record.color" placeholder="颜色" size="small" />
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <a-button type="link" size="small" danger @click="removeColorRow(index)">删除</a-button>
            </template>
            <template v-else>
              <a-input-number
                v-model:value="record[column.dataIndex]"
                :min="0"
                size="small"
                style="width: 80px"
                placeholder="数量"
              />
            </template>
          </template>
        </a-table>
        <div style="margin-top: 8px; text-align: right;">
          总件数：<strong>{{ totalQty }}</strong>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { orderApi } from '../../api/order'
import { customerApi, styleApi } from '../../api/base'

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
  { title: '款号', dataIndex: 'styleCode', width: 120 },
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
const styleList = ref([])
const editingId = ref(null)

const formData = reactive({
  customerId: undefined,
  styleId: undefined,
  totalAmount: undefined,
  deliveryDate: '',
  remark: ''
})

const rules = {
  customerId: [{ required: true, message: '请选择客户' }],
  styleId: [{ required: true, message: '请选择款号' }]
}

// 颜色尺码矩阵
const sizeColumns = ref([])
const matrixData = ref([])

const matrixColumns = computed(() => {
  const cols = [
    { title: '颜色', dataIndex: 'color', width: 120, fixed: 'left' }
  ]
  sizeColumns.value.forEach(size => {
    cols.push({ title: size, dataIndex: size, width: 100, align: 'center' })
  })
  cols.push({ title: '操作', dataIndex: 'action', width: 80, fixed: 'right' })
  return cols
})

const totalQty = computed(() => {
  let total = 0
  matrixData.value.forEach(row => {
    sizeColumns.value.forEach(size => {
      total += row[size] || 0
    })
  })
  return total
})

// 添加颜色行
const addColorRow = () => {
  const newRow = { color: '' }
  sizeColumns.value.forEach(size => {
    newRow[size] = 0
  })
  matrixData.value.push(newRow)
}

// 添加尺码列
const addSizeColumn = () => {
  const size = prompt('请输入尺码名称（如 S、M、L、XL）：')
  if (size && !sizeColumns.value.includes(size)) {
    sizeColumns.value.push(size)
    matrixData.value.forEach(row => {
      row[size] = 0
    })
  }
}

// 删除颜色行
const removeColorRow = (index) => {
  matrixData.value.splice(index, 1)
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

// 加载款号列表
const loadStyles = async () => {
  try {
    const res = await styleApi.list({ page: 1, size: 9999 })
    styleList.value = res.data?.records || []
  } catch (error) {
    console.error('加载款号列表失败:', error)
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
  resetForm()
  modalVisible.value = true
}

// 查看详情
const handleView = (record) => {
  router.push(`/order/detail/${record.id}`)
}

// 编辑
const handleEdit = (record) => {
  editingId.value = record.id
  formData.customerId = record.customerId
  formData.totalAmount = record.totalAmount
  formData.deliveryDate = record.deliveryDate ? dayjs(record.deliveryDate) : null
  formData.remark = record.remark
  modalVisible.value = true
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

    if (editingId.value) {
      // 编辑模式
      const data = {
        customerId: formData.customerId,
        totalAmount: formData.totalAmount,
        deliveryDate: formData.deliveryDate,
        remark: formData.remark
      }
      await orderApi.update(editingId.value, data)
      message.success('编辑成功')
      modalVisible.value = false
      loadData()
    } else {
      // 新增模式
      // 构建颜色尺码数据
      const colorSizes = []
      matrixData.value.forEach(row => {
        if (row.color) {
          sizeColumns.value.forEach(size => {
            if (row[size] > 0) {
              colorSizes.push({
                color: row.color,
                size: size,
                qty: row[size]
              })
            }
          })
        }
      })

      if (colorSizes.length === 0) {
        message.warning('请至少添加一个颜色尺码明细')
        return
      }

      const data = {
        order: {
          customerId: formData.customerId,
          styleId: formData.styleId,
          totalAmount: formData.totalAmount,
          deliveryDate: formData.deliveryDate,
          remark: formData.remark
        },
        colorSizes: colorSizes
      }
      const res = await orderApi.create(data)
      message.success('创建成功')
      modalVisible.value = false
      router.push(`/order/detail/${res.data}`)
    }
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
  formData.styleId = undefined
  formData.totalAmount = undefined
  formData.deliveryDate = ''
  formData.remark = ''
  sizeColumns.value = []
  matrixData.value = []
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
  loadStyles()
})

// 页面被 keep-alive 缓存后，再次进入时刷新数据
onActivated(() => {
  loadData()
})
</script>
