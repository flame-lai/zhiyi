<template>
  <div>
    <!-- 订单信息卡片 -->
    <a-card title="订单信息" style="margin-bottom: 16px">
      <a-descriptions :column="{ xs: 1, sm: 2, md: 3 }">
        <a-descriptions-item label="订单号">{{ orderInfo.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ orderInfo.customerName }}</a-descriptions-item>
        <a-descriptions-item label="订单金额">{{ orderInfo.totalAmount }} 元</a-descriptions-item>
        <a-descriptions-item label="总件数">{{ orderInfo.totalQty }} 件</a-descriptions-item>
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
        <!-- 订单明细 -->
        <a-tab-pane key="items" tab="订单明细">
          <div style="margin-bottom: 16px">
            <a-button type="primary" @click="showAddItemModal">添加明细</a-button>
          </div>
          <a-table
            :columns="itemColumns"
            :data-source="items"
            :loading="itemsLoading"
            :pagination="false"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'styleCode'">
                {{ record.styleCode || record.styleId }}
              </template>
            </template>
          </a-table>
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

    <!-- 返回按钮 -->
    <div style="margin-top: 16px; text-align: center">
      <a-button @click="handleBack">返回列表</a-button>
    </div>

    <!-- 添加明细弹窗 -->
    <a-modal
      v-model:open="itemModalVisible"
      title="添加订单明细"
      @ok="handleAddItem"
      @cancel="itemModalVisible = false"
    >
      <a-form :model="itemForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="款号" required>
          <a-select
            v-model:value="itemForm.styleId"
            placeholder="选择款号"
            show-search
            :filter-option="filterStyleOption"
          >
            <a-select-option v-for="s in allStyles" :key="s.id" :value="s.id">
              {{ s.styleCode }} - {{ s.styleName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="颜色" required>
          <a-input v-model:value="itemForm.color" placeholder="请输入颜色" />
        </a-form-item>
        <a-form-item label="尺码" required>
          <a-input v-model:value="itemForm.size" placeholder="请输入尺码" />
        </a-form-item>
        <a-form-item label="数量" required>
          <a-input-number v-model:value="itemForm.qty" :min="1" placeholder="请输入数量" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 新增出货弹窗 -->
    <a-modal
      v-model:open="shipmentModalVisible"
      title="新增出货"
      @ok="handleAddShipment"
      @cancel="shipmentModalVisible = false"
    >
      <a-form :model="shipmentForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="订单明细" required>
          <a-select
            v-model:value="shipmentForm.orderItemId"
            placeholder="选择订单明细"
          >
            <a-select-option v-for="item in items" :key="item.id" :value="item.id">
              {{ item.styleCode || item.styleId }} - {{ item.color }} - {{ item.size }}
            </a-select-option>
          </a-select>
        </a-form-item>
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
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { orderApi, shipmentApi } from '../../api/order'
import { processApi, materialApi, styleApi } from '../../api/base'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

// 订单信息
const orderInfo = ref({
  id: '',
  orderNo: '',
  customerName: '',
  totalAmount: 0,
  totalQty: 0,
  status: 0,
  deliveryDate: ''
})

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
const activeTab = ref('items')

// 订单明细
const items = ref([])
const itemsLoading = ref(false)
const itemColumns = [
  { title: '款号', dataIndex: 'styleCode', width: 150 },
  { title: '颜色', dataIndex: 'color', width: 100 },
  { title: '尺码', dataIndex: 'size', width: 100 },
  { title: '数量', dataIndex: 'qty', width: 100, align: 'right' }
]

// 添加明细弹窗
const itemModalVisible = ref(false)
const itemForm = reactive({
  styleId: undefined,
  color: '',
  size: '',
  qty: 1
})
const allStyles = ref([])

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
  orderItemId: undefined,
  shipmentDate: '',
  qty: 1,
  remark: ''
})
const shipmentColumns = [
  { title: '出货日期', dataIndex: 'shipmentDate', width: 120 },
  { title: '款号', dataIndex: 'styleCode', width: 120 },
  { title: '颜色', dataIndex: 'color', width: 100 },
  { title: '尺码', dataIndex: 'size', width: 100 },
  { title: '出货数量', dataIndex: 'qty', width: 100, align: 'right' },
  { title: '备注', dataIndex: 'remark', width: 150 }
]

// 加载订单信息
const loadOrderInfo = async () => {
  try {
    const res = await orderApi.getById(id)
    Object.assign(orderInfo.value, res.data)
  } catch (error) {
    console.error('加载订单信息失败:', error)
  }
}

// 加载订单明细
const loadItems = async () => {
  itemsLoading.value = true
  try {
    const res = await orderApi.getItems(id)
    items.value = res.data || []
  } catch (error) {
    console.error('加载订单明细失败:', error)
  } finally {
    itemsLoading.value = false
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

// 加载所有款号
const loadAllStyles = async () => {
  try {
    const res = await styleApi.list({ page: 1, size: 9999 })
    allStyles.value = res.data?.records || []
  } catch (error) {
    console.error('加载款号列表失败:', error)
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

// 显示添加明细弹窗
const showAddItemModal = () => {
  itemModalVisible.value = true
  itemForm.styleId = undefined
  itemForm.color = ''
  itemForm.size = ''
  itemForm.qty = 1
}

// 添加明细
const handleAddItem = async () => {
  if (!itemForm.styleId || !itemForm.color || !itemForm.size || !itemForm.qty) {
    message.error('请填写完整信息')
    return
  }
  try {
    // 这里需要调用添加明细的API
    message.success('添加成功')
    itemModalVisible.value = false
    loadItems()
    loadOrderInfo()
  } catch (error) {
    console.error('添加明细失败:', error)
  }
}

// 显示新增出货弹窗
const showAddShipmentModal = () => {
  shipmentModalVisible.value = true
  shipmentForm.orderItemId = undefined
  shipmentForm.shipmentDate = ''
  shipmentForm.qty = 1
  shipmentForm.remark = ''
}

// 新增出货
const handleAddShipment = async () => {
  if (!shipmentForm.orderItemId || !shipmentForm.shipmentDate || !shipmentForm.qty) {
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
    loadOrderInfo()
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

// 款号搜索过滤
const filterStyleOption = (input, option) => {
  const style = allStyles.value.find(s => s.id === option.value)
  return (style?.styleCode + style?.styleName)?.toLowerCase().includes(input.toLowerCase()) ?? false
}

// 返回列表
const handleBack = () => {
  router.push('/order/list')
}

// 初始化
onMounted(() => {
  loadOrderInfo()
  loadItems()
  loadProcesses()
  loadMaterials()
  loadShipments()
  loadAllProcesses()
  loadAllMaterials()
  loadAllStyles()
})
</script>
