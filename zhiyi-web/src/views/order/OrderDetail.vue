<template>
  <div>
    <!-- 订单信息卡片 -->
    <a-card style="margin-bottom: 16px">
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>订单信息</span>
          <a-button @click="handleBack">返回列表</a-button>
        </div>
      </template>
      <a-descriptions :column="{ xs: 1, sm: 2, md: 3 }">
        <a-descriptions-item label="订单号">{{ orderInfo.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ customerName }}</a-descriptions-item>
        <a-descriptions-item label="款号">{{ styleCode }} {{ styleName }}</a-descriptions-item>
        <a-descriptions-item label="订单金额">{{ orderInfo.totalAmount }} 元</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColorMap[orderInfo.status]">
            {{ statusTextMap[orderInfo.status] }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="交货期">{{ orderInfo.deliveryDate }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <!-- 选项卡 -->
    <a-card>
      <a-tabs v-model:activeKey="activeTab">
        <!-- 颜色尺码明细 -->
        <a-tab-pane key="colorSizes" tab="颜色尺码明细">
          <div style="margin-bottom: 16px">
            <a-button type="primary" @click="handleSaveColorSizes" :loading="colorSizesSaving">
              保存
            </a-button>
          </div>

          <!-- 颜色输入区 -->
          <div style="margin-bottom: 16px;">
            <div style="margin-bottom: 8px; font-weight: bold;">颜色：</div>
            <div style="display: flex; flex-wrap: wrap; gap: 8px; align-items: center;">
              <a-tag
                v-for="(color, index) in colorList"
                :key="index"
                closable
                @close="removeColor(index)"
                color="blue"
              >
                {{ color }}
              </a-tag>
              <a-input
                v-if="colorInputVisible"
                ref="colorInputRef"
                v-model:value="colorInputValue"
                size="small"
                style="width: 100px"
                @blur="handleColorInputConfirm"
                @pressEnter="handleColorInputConfirm"
              />
              <a-button v-else size="small" @click="showColorInput">+ 添加颜色</a-button>
            </div>
          </div>

          <!-- 尺码输入区 -->
          <div style="margin-bottom: 16px;">
            <div style="margin-bottom: 8px; font-weight: bold;">尺码：</div>
            <div style="display: flex; flex-wrap: wrap; gap: 8px; align-items: center;">
              <a-tag
                v-for="(size, index) in sizeList"
                :key="index"
                closable
                @close="removeSize(index)"
                color="green"
              >
                {{ size }}
              </a-tag>
              <a-input
                v-if="sizeInputVisible"
                ref="sizeInputRef"
                v-model:value="sizeInputValue"
                size="small"
                style="width: 100px"
                @blur="handleSizeInputConfirm"
                @pressEnter="handleSizeInputConfirm"
              />
              <a-button v-else size="small" @click="showSizeInput">+ 添加尺码</a-button>
            </div>
          </div>

          <!-- 数量矩阵 -->
          <div v-if="colorList.length > 0 && sizeList.length > 0">
            <div style="margin-bottom: 8px; font-weight: bold;">数量：</div>
            <a-table
              :columns="matrixColumns"
              :data-source="matrixData"
              :pagination="false"
              size="small"
              bordered
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'color'">
                  <span style="font-weight: bold;">{{ record.color }}</span>
                </template>
                <template v-else-if="column.dataIndex === 'total'">
                  <span style="font-weight: bold;">{{ getRowTotal(record) }}</span>
                </template>
                <template v-else>
                  <a-input-number
                    v-model:value="record[column.dataIndex]"
                    :min="0"
                    size="small"
                    style="width: 80px"
                    placeholder="0"
                  />
                </template>
              </template>
            </a-table>
            <div style="margin-top: 8px; text-align: right; font-size: 16px;">
              总件数：<strong style="color: #1890ff;">{{ totalQty }}</strong>
            </div>
          </div>
          <a-empty v-else description="请先添加颜色和尺码" />
        </a-tab-pane>

        <!-- 工序定价 -->
        <a-tab-pane key="processes" tab="工序定价">
          <div style="margin-bottom: 16px">
            <a-button type="primary" @click="handleSaveProcesses" :loading="processesSaving">
              保存工序定价
            </a-button>
            <a-button style="margin-left: 8px" @click="addProcess">添加工序</a-button>
          </div>
          <a-table
            :columns="processColumns"
            :data-source="processes"
            :loading="processesLoading"
            :pagination="false"
            row-key="id"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.dataIndex === 'processId'">
                <a-select
                  v-model:value="record.processId"
                  placeholder="选择工序"
                  style="width: 100%"
                  show-search
                  :filter-option="filterProcessOption"
                  @change="(val) => onProcessChange(record, val)"
                >
                  <a-select-option v-for="p in allProcesses" :key="p.id" :value="p.id">
                    {{ p.name }}
                  </a-select-option>
                </a-select>
              </template>
              <template v-if="column.dataIndex === 'processPrice'">
                <a-input-number
                  v-model:value="record.processPrice"
                  :min="0"
                  :precision="2"
                  placeholder="工价"
                  style="width: 120px"
                />
              </template>
              <template v-if="column.dataIndex === 'action'">
                <a-popconfirm title="确定删除该工序？" @confirm="removeProcess(index)">
                  <a-button type="link" danger size="small">删除</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 物料清单 -->
        <a-tab-pane key="materials" tab="物料清单">
          <div style="margin-bottom: 16px">
            <a-button type="primary" @click="handleSaveMaterials" :loading="materialsSaving">
              保存物料清单
            </a-button>
            <a-button style="margin-left: 8px" @click="addMaterial">添加物料</a-button>
          </div>
          <a-table
            :columns="materialColumns"
            :data-source="materials"
            :loading="materialsLoading"
            :pagination="false"
            row-key="id"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.dataIndex === 'materialName'">
                <a-select
                  v-model:value="record.materialName"
                  placeholder="选择或输入物料"
                  style="width: 100%"
                  show-search
                  allow-custom-input
                  :filter-option="filterMaterialOption"
                  @change="(val) => onMaterialChange(record, val)"
                >
                  <a-select-option v-for="m in allMaterials" :key="m.id" :value="m.materialName">
                    {{ m.materialName }} ({{ m.type === 1 ? '原料' : '辅料' }})
                  </a-select-option>
                </a-select>
              </template>
              <template v-if="column.dataIndex === 'type'">
                <a-tag :color="record.type === 1 ? 'blue' : 'green'">
                  {{ record.type === 1 ? '原料' : '辅料' }}
                </a-tag>
              </template>
              <template v-if="column.dataIndex === 'totalQty'">
                <a-input-number
                  v-model:value="record.totalQty"
                  :min="0"
                  :precision="2"
                  placeholder="总用量"
                  style="width: 120px"
                />
              </template>
              <template v-if="column.dataIndex === 'action'">
                <a-popconfirm title="确定删除该物料？" @confirm="removeMaterial(index)">
                  <a-button type="link" danger size="small">删除</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 出货管理 -->
        <a-tab-pane key="shipments" tab="出货管理">
          <div style="margin-bottom: 16px">
            <a-button type="primary" @click="showAddShipmentModal">新增出货</a-button>
          </div>
          <!-- 出货进度 -->
          <div style="margin-bottom: 16px; padding: 16px; background: #f5f5f5; border-radius: 4px;">
            <a-row :gutter="16">
              <a-col :span="6">
                <div style="color: #666;">订单总件数</div>
                <div style="font-size: 24px; font-weight: bold;">{{ totalQty }}</div>
              </a-col>
              <a-col :span="6">
                <div style="color: #666;">已出货数量</div>
                <div style="font-size: 24px; font-weight: bold;">{{ shippedQty }}</div>
              </a-col>
              <a-col :span="6">
                <div style="color: #666;">出货进度</div>
                <a-progress :percent="shipmentProgress" style="margin-top: 8px;" />
              </a-col>
              <a-col :span="6">
                <div style="color: #666;">状态</div>
                <a-tag :color="shipmentStatus === '已出齐' ? 'success' : shipmentStatus === '部分出货' ? 'warning' : 'default'">
                  {{ shipmentStatus }}
                </a-tag>
              </a-col>
            </a-row>
          </div>
          <a-table
            :columns="shipmentColumns"
            :data-source="shipments"
            :loading="shipmentsLoading"
            :pagination="false"
            row-key="id"
          />
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 新增出货弹窗 -->
    <a-modal
      v-model:open="shipmentModalVisible"
      title="新增出货"
      @ok="handleAddShipment"
      @cancel="shipmentModalVisible = false"
    >
      <a-form :model="shipmentForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="出货日期" required>
          <a-date-picker
            v-model:value="shipmentForm.shipmentDate"
            style="width: 100%"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
        <a-form-item label="出货数量" required>
          <a-input-number v-model:value="shipmentForm.qty" :min="1" placeholder="出货数量" style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="shipmentForm.remark" placeholder="备注" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { orderApi, shipmentApi } from '../../api/order'
import { processApi, materialApi } from '../../api/base'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

// 订单信息
const orderInfo = ref({
  id: '',
  orderNo: '',
  customerId: null,
  styleId: null,
  totalAmount: 0,
  status: 0,
  deliveryDate: ''
})

const customerName = ref('')
const styleCode = ref('')
const styleName = ref('')

// 状态映射
const statusColorMap = {
  0: 'default',
  1: 'blue',
  2: 'processing',
  3: 'warning',
  4: 'purple',
  5: 'success',
  6: 'default'
}

const statusTextMap = {
  0: '待确认',
  1: '已确认',
  2: '生产中',
  3: '部分出货',
  4: '已完工',
  5: '已出货',
  6: '已结算'
}

// 当前选项卡
const activeTab = ref('colorSizes')

// 颜色尺码管理
const colorList = ref([])
const sizeList = ref([])
const matrixData = ref([])

const colorInputVisible = ref(false)
const colorInputValue = ref('')
const colorInputRef = ref(null)

const sizeInputVisible = ref(false)
const sizeInputValue = ref('')
const sizeInputRef = ref(null)

const matrixColumns = computed(() => {
  const cols = [
    { title: '颜色', dataIndex: 'color', width: 100, fixed: 'left' }
  ]
  sizeList.value.forEach(size => {
    cols.push({ title: size, dataIndex: size, width: 100, align: 'center' })
  })
  cols.push({ title: '小计', dataIndex: 'total', width: 80, align: 'center', fixed: 'right' })
  return cols
})

const totalQty = computed(() => {
  let total = 0
  matrixData.value.forEach(row => {
    sizeList.value.forEach(size => {
      total += row[size] || 0
    })
  })
  return total
})

const getRowTotal = (record) => {
  let total = 0
  sizeList.value.forEach(size => {
    total += record[size] || 0
  })
  return total
}

// 颜色输入
const showColorInput = () => {
  colorInputVisible.value = true
  nextTick(() => {
    colorInputRef.value?.focus()
  })
}

const handleColorInputConfirm = () => {
  if (colorInputValue.value && !colorList.value.includes(colorInputValue.value)) {
    colorList.value.push(colorInputValue.value)
    // 为新颜色添加到矩阵
    matrixData.value.push({ color: colorInputValue.value })
  }
  colorInputVisible.value = false
  colorInputValue.value = ''
}

const removeColor = (index) => {
  colorList.value.splice(index, 1)
  matrixData.value.splice(index, 1)
}

// 尺码输入
const showSizeInput = () => {
  sizeInputVisible.value = true
  nextTick(() => {
    sizeInputRef.value?.focus()
  })
}

const handleSizeInputConfirm = () => {
  if (sizeInputValue.value && !sizeList.value.includes(sizeInputValue.value)) {
    sizeList.value.push(sizeInputValue.value)
    // 为新尺码添加到所有颜色行
    matrixData.value.forEach(row => {
      row[sizeInputValue.value] = 0
    })
  }
  sizeInputVisible.value = false
  sizeInputValue.value = ''
}

const removeSize = (index) => {
  const size = sizeList.value[index]
  sizeList.value.splice(index, 1)
  // 从矩阵中移除该尺码列
  matrixData.value.forEach(row => {
    delete row[size]
  })
}

// 保存颜色尺码
const colorSizesSaving = ref(false)
const handleSaveColorSizes = async () => {
  colorSizesSaving.value = true
  try {
    const data = []
    matrixData.value.forEach(row => {
      if (row.color) {
        sizeList.value.forEach(size => {
          if (row[size] > 0) {
            data.push({
              color: row.color,
              size: size,
              qty: row[size]
            })
          }
        })
      }
    })
    await orderApi.saveColorSizes(id, data)
    message.success('颜色尺码保存成功')
    loadColorSizes()
  } catch (error) {
    console.error('保存颜色尺码失败:', error)
  } finally {
    colorSizesSaving.value = false
  }
}

// 加载颜色尺码
const loadColorSizes = async () => {
  try {
    const res = await orderApi.getColorSizes(id)
    const data = res.data || []
    // 转换为矩阵格式
    const colors = new Set()
    const sizes = new Set()
    data.forEach(item => {
      colors.add(item.color)
      sizes.add(item.size)
    })
    colorList.value = Array.from(colors)
    sizeList.value = Array.from(sizes)
    const matrix = []
    colors.forEach(color => {
      const row = { color }
      data.filter(item => item.color === color).forEach(item => {
        row[item.size] = item.qty
      })
      matrix.push(row)
    })
    matrixData.value = matrix
  } catch (error) {
    console.error('加载颜色尺码失败:', error)
  }
}

// 工序定价
const processes = ref([])
const processesLoading = ref(false)
const processesSaving = ref(false)
const allProcesses = ref([])
const processColumns = [
  { title: '工序名称', dataIndex: 'processId', width: 200 },
  { title: '工价（元/件）', dataIndex: 'processPrice', width: 150, align: 'right' },
  { title: '操作', dataIndex: 'action', width: 100, align: 'center' }
]

// 物料清单
const materials = ref([])
const materialsLoading = ref(false)
const materialsSaving = ref(false)
const allMaterials = ref([])
const materialColumns = [
  { title: '物料名称', dataIndex: 'materialName', width: 200 },
  { title: '类型', dataIndex: 'type', width: 80 },
  { title: '总用量', dataIndex: 'totalQty', width: 120, align: 'right' },
  { title: '单位', dataIndex: 'unit', width: 80 },
  { title: '操作', dataIndex: 'action', width: 100, align: 'center' }
]

// 出货管理
const shipments = ref([])
const shipmentsLoading = ref(false)
const shipmentModalVisible = ref(false)
const shipmentForm = reactive({
  shipmentDate: '',
  qty: 1,
  remark: ''
})
const shipmentColumns = [
  { title: '出货日期', dataIndex: 'shipmentDate', width: 120 },
  { title: '出货数量', dataIndex: 'qty', width: 100, align: 'right' },
  { title: '备注', dataIndex: 'remark', width: 200 }
]

// 出货统计
const shippedQty = computed(() => {
  return shipments.value.reduce((sum, s) => sum + (s.qty || 0), 0)
})

const shipmentProgress = computed(() => {
  if (!totalQty.value) return 0
  return Math.round((shippedQty.value / totalQty.value) * 100)
})

const shipmentStatus = computed(() => {
  if (shippedQty.value >= totalQty.value) return '已出齐'
  if (shippedQty.value > 0) return '部分出货'
  return '未出货'
})

// 加载订单信息
const loadOrderInfo = async () => {
  try {
    const res = await orderApi.getById(id)
    Object.assign(orderInfo.value, res.data)
    customerName.value = res.data.customerName || ''
    styleCode.value = res.data.styleCode || ''
    styleName.value = res.data.styleName || ''
  } catch (error) {
    console.error('加载订单信息失败:', error)
  }
}

// 加载工序定价
const loadProcesses = async () => {
  processesLoading.value = true
  try {
    const res = await orderApi.getProcesses(id)
    processes.value = res.data || []
  } catch (error) {
    console.error('加载工序定价失败:', error)
  } finally {
    processesLoading.value = false
  }
}

// 加载物料清单
const loadMaterials = async () => {
  materialsLoading.value = true
  try {
    const res = await orderApi.getMaterials(id)
    materials.value = res.data || []
  } catch (error) {
    console.error('加载物料清单失败:', error)
  } finally {
    materialsLoading.value = false
  }
}

// 加载出货记录
const loadShipments = async () => {
  shipmentsLoading.value = true
  try {
    const res = await shipmentApi.list(id)
    shipments.value = res.data || []
  } catch (error) {
    console.error('加载出货记录失败:', error)
  } finally {
    shipmentsLoading.value = false
  }
}

// 加载所有工序
const loadAllProcesses = async () => {
  try {
    const res = await processApi.list({ page: 1, size: 9999 })
    allProcesses.value = res.data?.records || []
  } catch (error) {
    console.error('加载工序列表失败:', error)
  }
}

// 加载所有物料
const loadAllMaterials = async () => {
  try {
    const res = await materialApi.list({ page: 1, size: 9999 })
    allMaterials.value = res.data?.records || []
  } catch (error) {
    console.error('加载物料列表失败:', error)
  }
}

// 保存工序定价
const handleSaveProcesses = async () => {
  processesSaving.value = true
  try {
    await orderApi.saveProcesses(id, processes.value)
    message.success('工序定价保存成功')
  } catch (error) {
    console.error('保存工序定价失败:', error)
  } finally {
    processesSaving.value = false
  }
}

// 保存物料清单
const handleSaveMaterials = async () => {
  materialsSaving.value = true
  try {
    await orderApi.saveMaterials(id, materials.value)
    message.success('物料清单保存成功')
  } catch (error) {
    console.error('保存物料清单失败:', error)
  } finally {
    materialsSaving.value = false
  }
}

// 添加工序
const addProcess = () => {
  processes.value.push({
    id: Date.now(),
    processId: undefined,
    processPrice: 0
  })
}

// 删除工序
const removeProcess = (index) => {
  processes.value.splice(index, 1)
}

// 工序选择变化
const onProcessChange = (record, processId) => {
  const process = allProcesses.value.find(p => p.id === processId)
  if (process) {
    record.processPrice = process.refPrice
  }
}

// 添加物料
const addMaterial = () => {
  materials.value.push({
    id: Date.now(),
    materialName: '',
    type: 1,
    totalQty: 0,
    unit: ''
  })
}

// 删除物料
const removeMaterial = (index) => {
  materials.value.splice(index, 1)
}

// 物料选择变化
const onMaterialChange = (record, materialName) => {
  const material = allMaterials.value.find(m => m.materialName === materialName)
  if (material) {
    record.type = material.type
    record.unit = material.unit
  }
}

// 显示新增出货弹窗
const showAddShipmentModal = () => {
  shipmentModalVisible.value = true
  shipmentForm.shipmentDate = ''
  shipmentForm.qty = 1
  shipmentForm.remark = ''
}

// 新增出货
const handleAddShipment = async () => {
  if (!shipmentForm.shipmentDate || !shipmentForm.qty) {
    message.error('请填写完整信息')
    return
  }
  try {
    await shipmentApi.create({
      orderId: id,
      ...shipmentForm
    })
    message.success('出货成功')
    shipmentModalVisible.value = false
    loadShipments()
  } catch (error) {
    console.error('出货失败:', error)
  }
}

// 工序搜索过滤
const filterProcessOption = (input, option) => {
  const process = allProcesses.value.find(p => p.id === option.value)
  return process?.name?.toLowerCase().includes(input.toLowerCase()) ?? false
}

// 物料搜索过滤
const filterMaterialOption = (input, option) => {
  return option.value?.toLowerCase().includes(input.toLowerCase()) ?? false
}

// 返回列表
const handleBack = () => {
  router.push('/order/list')
}

// 初始化
onMounted(() => {
  loadOrderInfo()
  loadColorSizes()
  loadProcesses()
  loadMaterials()
  loadShipments()
  loadAllProcesses()
  loadAllMaterials()
})
</script>
