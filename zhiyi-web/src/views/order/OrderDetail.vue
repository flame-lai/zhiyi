<template>
  <div>
    <!-- 订单信息卡片 -->
    <a-card title="订单信息" style="margin-bottom: 16px">
      <a-descriptions :column="{ xs: 1, sm: 2, md: 3 }">
        <a-descriptions-item label="订单号">{{ orderInfo.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ orderInfo.customerName }}</a-descriptions-item>
        <a-descriptions-item label="款号">{{ orderInfo.styleId }}</a-descriptions-item>
        <a-descriptions-item label="总数量">{{ orderInfo.totalQty }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColorMap[orderInfo.status]">
            {{ orderInfo.status }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="交货期">{{ orderInfo.deliveryDate }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ orderInfo.createTime }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ orderInfo.updateTime }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <!-- 选项卡 -->
    <a-card>
      <a-tabs v-model:activeKey="activeTab">
        <!-- 订单明细 -->
        <a-tab-pane key="items" tab="订单明细">
          <a-table
            :columns="itemColumns"
            :data-source="items"
            :loading="itemsLoading"
            :pagination="false"
            row-key="id"
          />
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
              <template v-if="column.dataIndex === 'processName'">
                <a-input v-model:value="record.processName" placeholder="工序名称" />
              </template>
              <template v-if="column.dataIndex === 'price'">
                <a-input-number
                  v-model:value="record.price"
                  :min="0"
                  :precision="2"
                  placeholder="单价"
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
                <a-input v-model:value="record.materialName" placeholder="物料名称" />
              </template>
              <template v-if="column.dataIndex === 'quantity'">
                <a-input-number
                  v-model:value="record.quantity"
                  :min="0"
                  :precision="2"
                  placeholder="数量"
                  style="width: 120px"
                />
              </template>
              <template v-if="column.dataIndex === 'unit'">
                <a-input v-model:value="record.unit" placeholder="单位" style="width: 80px" />
              </template>
              <template v-if="column.dataIndex === 'action'">
                <a-popconfirm title="确定删除该物料？" @confirm="removeMaterial(index)">
                  <a-button type="link" danger size="small">删除</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 返回按钮 -->
    <div style="margin-top: 16px; text-align: center">
      <a-button @click="handleBack">返回列表</a-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { orderApi } from '../../api/order'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

// 订单信息
const orderInfo = ref({
  id: '',
  orderNo: '',
  customerName: '',
  styleId: '',
  totalQty: 0,
  status: '',
  deliveryDate: '',
  createTime: '',
  updateTime: ''
})

// 状态颜色映射
const statusColorMap = {
  '待确认': 'default',
  '已确认': 'blue',
  '生产中': 'processing',
  '已完工': 'success',
  '已结算': 'green'
}

// 当前选项卡
const activeTab = ref('items')

// 订单明细
const items = ref([])
const itemsLoading = ref(false)
const itemColumns = [
  { title: '颜色', dataIndex: 'color', width: 150 },
  { title: '尺码', dataIndex: 'size', width: 150 },
  { title: '数量', dataIndex: 'qty', width: 120, align: 'right' }
]

// 工序定价
const processes = ref([])
const processesLoading = ref(false)
const processesSaving = ref(false)
const processColumns = [
  { title: '工序名称', dataIndex: 'processName', width: 200 },
  { title: '单价', dataIndex: 'price', width: 150, align: 'right' },
  { title: '操作', dataIndex: 'action', width: 100, align: 'center' }
]

// 物料清单
const materials = ref([])
const materialsLoading = ref(false)
const materialsSaving = ref(false)
const materialColumns = [
  { title: '物料名称', dataIndex: 'materialName', width: 200 },
  { title: '数量', dataIndex: 'quantity', width: 120, align: 'right' },
  { title: '单位', dataIndex: 'unit', width: 100 },
  { title: '操作', dataIndex: 'action', width: 100, align: 'center' }
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
    items.value = res.data
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
    processes.value = res.data
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
    materials.value = res.data
  } catch (error) {
    console.error('加载物料清单失败:', error)
  } finally {
    materialsLoading.value = false
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
    processName: '',
    price: 0
  })
}

// 删除工序
const removeProcess = (index) => {
  processes.value.splice(index, 1)
}

// 添加物料
const addMaterial = () => {
  materials.value.push({
    id: Date.now(),
    materialName: '',
    quantity: 0,
    unit: ''
  })
}

// 删除物料
const removeMaterial = (index) => {
  materials.value.splice(index, 1)
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
})
</script>
