<template>
  <div>
    <a-card :title="isEdit ? '编辑裁床计划' : '新增裁床计划'">
      <a-form :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <!-- 基本信息 -->
        <a-divider orientation="left">基本信息</a-divider>
        <a-form-item label="订单" required>
          <a-select
            v-model:value="formData.orderId"
            placeholder="选择订单"
            show-search
            :filter-option="filterOrderOption"
            @change="onOrderChange"
            :disabled="isEdit"
          >
            <a-select-option v-for="order in orderList" :key="order.id" :value="order.id">
              {{ order.orderNo }} - {{ order.styleCode }} ({{ order.customerName }})
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="裁剪面料" required>
          <a-select v-model:value="formData.fabric" placeholder="选择面料">
            <a-select-option v-for="m in materialList" :key="m.materialName" :value="m.materialName">
              {{ m.materialName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="裁剪部位">
          <a-select v-model:value="formData.part" placeholder="选择或输入部位" show-search allow-custom-input>
            <a-select-option v-for="p in partList" :key="p.name" :value="p.name">
              {{ p.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="床次" required>
          <a-input-number v-model:value="formData.bedNo" :min="1" :precision="0" style="width: 100%" placeholder="床次" />
        </a-form-item>
        <a-form-item label="铺料层数" required>
          <a-input-number v-model:value="formData.layers" :min="1" :precision="0" style="width: 100%" placeholder="层数" />
        </a-form-item>
        <a-form-item label="每扎件数" required>
          <a-input-number v-model:value="formData.qtyPerBundle" :min="1" :precision="0" style="width: 100%" placeholder="每扎件数" />
        </a-form-item>
        <a-form-item label="裁床日期">
          <a-date-picker v-model:value="formData.cuttingDate" style="width: 100%" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
        </a-form-item>

        <!-- 配比矩阵 -->
        <a-divider orientation="left">颜色尺码配比</a-divider>
        <div style="margin-bottom: 16px;">
          <a-button type="dashed" @click="addDetailRow" style="width: 100%">
            + 添加配比行
          </a-button>
        </div>
        <a-table
          :columns="detailColumns"
          :data-source="formData.details"
          :pagination="false"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.dataIndex === 'color'">
              <a-select v-model:value="record.color" placeholder="颜色" size="small" style="width: 100%">
                <a-select-option v-for="color in colorList" :key="color" :value="color">{{ color }}</a-select-option>
              </a-select>
            </template>
            <template v-if="column.dataIndex === 'size'">
              <a-select v-model:value="record.size" placeholder="尺码" size="small" style="width: 100%">
                <a-select-option v-for="size in sizeList" :key="size" :value="size">{{ size }}</a-select-option>
              </a-select>
            </template>
            <template v-if="column.dataIndex === 'ratio'">
              <a-input-number v-model:value="record.ratio" :min="1" size="small" style="width: 100%" />
            </template>
            <template v-if="column.dataIndex === 'planQty'">
              <span>{{ (formData.layers || 0) * (record.ratio || 0) }}</span>
            </template>
            <template v-if="column.dataIndex === 'action'">
              <a-popconfirm title="确定删除？" @confirm="removeDetailRow(index)">
                <a style="color: #ff4d4f">删除</a>
              </a-popconfirm>
            </template>
          </template>
        </a-table>

        <!-- 汇总信息 -->
        <div style="margin-top: 16px; padding: 16px; background: #f5f5f5; border-radius: 4px;">
          <a-row :gutter="16">
            <a-col :span="8">
              <div style="color: #666;">总计划数</div>
              <div style="font-size: 20px; font-weight: bold;">{{ totalPlanQty }}</div>
            </a-col>
            <a-col :span="8">
              <div style="color: #666;">配比行数</div>
              <div style="font-size: 20px; font-weight: bold;">{{ formData.details.length }}</div>
            </a-col>
          </a-row>
        </div>

        <!-- 操作按钮 -->
        <div style="margin-top: 24px; text-align: center;">
          <a-space>
            <a-button @click="handleBack">取消</a-button>
            <a-button type="primary" @click="handleSubmit" :loading="submitLoading">
              {{ isEdit ? '保存修改' : '创建计划' }}
            </a-button>
          </a-space>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { cuttingPlanApi } from '../../api/cutting'
import { orderApi } from '../../api/order'
import { materialApi, partApi } from '../../api/base'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const planId = computed(() => route.params.id ? Number(route.params.id) : null)

// 基础数据
const orderList = ref<any[]>([])
const materialList = ref<any[]>([])
const partList = ref<any[]>([])
const colorSizeList = ref<any[]>([])

// 表单数据
const submitLoading = ref(false)
const formData = reactive({
  orderId: undefined as number | undefined,
  fabric: undefined as string | undefined,
  part: '全套',
  bedNo: 1,
  layers: 1,
  qtyPerBundle: 20,
  cuttingDate: undefined as string | undefined,
  details: [] as any[],
})

// 配比表格列
const detailColumns = [
  { title: '颜色', dataIndex: 'color', width: 150 },
  { title: '尺码', dataIndex: 'size', width: 120 },
  { title: '配比', dataIndex: 'ratio', width: 100 },
  { title: '计划数', dataIndex: 'planQty', width: 100 },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' },
]

// 可选颜色和尺码
const colorList = computed(() => {
  const colors = new Set<string>()
  colorSizeList.value.forEach(item => colors.add(item.color))
  return Array.from(colors)
})
const sizeList = computed(() => {
  const sizes = new Set<string>()
  colorSizeList.value.forEach(item => sizes.add(item.size))
  return Array.from(sizes)
})

// 总计划数
const totalPlanQty = computed(() => {
  return formData.details.reduce((sum, d) => {
    return sum + ((formData.layers || 0) * (d.ratio || 0))
  }, 0)
})

// 加载基础数据
async function loadBaseData() {
  try {
    const [orderRes, materialRes, partRes] = await Promise.all([
      orderApi.list({ page: 1, size: 9999, status: 1 }),
      materialApi.list({ page: 1, size: 9999, type: 1 }),
      partApi.list({ page: 1, size: 9999 }),
    ])
    orderList.value = orderRes.data?.records || []
    materialList.value = materialRes.data?.records || []
    partList.value = partRes.data?.records || []
  } catch {}
}

// 加载编辑数据
async function loadEditData() {
  if (!planId.value) return
  try {
    const [planRes, detailsRes] = await Promise.all([
      cuttingPlanApi.getById(planId.value),
      cuttingPlanApi.getDetails(planId.value),
    ])
    const plan = planRes.data
    formData.orderId = plan.orderId
    formData.fabric = plan.fabric
    formData.part = plan.part || '全套'
    formData.bedNo = plan.bedNo
    formData.layers = plan.layers || 1
    formData.qtyPerBundle = plan.qtyPerBundle
    formData.cuttingDate = plan.cuttingDate
    formData.details = detailsRes.data || []

    // 加载颜色尺码
    if (plan.orderId) {
      const res = await orderApi.getColorSizes(plan.orderId)
      colorSizeList.value = res.data || []
    }
  } catch {}
}

// 订单选择变化
async function onOrderChange(orderId: number) {
  formData.fabric = undefined
  formData.details = []
  colorSizeList.value = []
  if (orderId) {
    try {
      const res = await orderApi.getColorSizes(orderId)
      colorSizeList.value = res.data || []
      if (materialList.value.length > 0) {
        formData.fabric = materialList.value[0].materialName
      }
    } catch {}
  }
}

// 添加配比行
function addDetailRow() {
  formData.details.push({ color: undefined, size: undefined, ratio: 1 })
}

// 删除配比行
function removeDetailRow(index: number) {
  formData.details.splice(index, 1)
}

// 订单搜索过滤
function filterOrderOption(input: string, option: any) {
  const order = orderList.value.find(o => o.id === option.value)
  return order?.orderNo?.toLowerCase().includes(input.toLowerCase()) ?? false
}

// 提交表单
async function handleSubmit() {
  if (!formData.orderId || !formData.fabric || !formData.bedNo || !formData.layers || !formData.qtyPerBundle) {
    message.warning('请填写完整信息')
    return
  }
  if (formData.details.length === 0) {
    message.warning('请添加颜色尺码配比明细')
    return
  }
  submitLoading.value = true
  try {
    const data = {
      plan: {
        orderId: formData.orderId,
        fabric: formData.fabric,
        part: formData.part,
        bedNo: formData.bedNo,
        layers: formData.layers,
        qtyPerBundle: formData.qtyPerBundle,
        cuttingDate: formData.cuttingDate,
      },
      details: formData.details,
    }
    if (isEdit.value) {
      await cuttingPlanApi.update(planId.value!, data)
      message.success('修改成功')
    } else {
      await cuttingPlanApi.create(data)
      message.success('创建成功')
    }
    router.push('/cutting/plans')
  } catch {} finally {
    submitLoading.value = false
  }
}

// 返回列表
function handleBack() {
  router.push('/cutting/plans')
}

// 初始化
onMounted(async () => {
  await loadBaseData()
  if (isEdit.value) {
    await loadEditData()
  }
})
</script>
