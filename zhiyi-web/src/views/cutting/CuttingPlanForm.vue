<template>
  <div class="cutting-plan-form">
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>{{ isEdit ? '编辑裁床计划' : '新增裁床计划' }}</span>
          <a-space>
            <a-button @click="handleBack">取消</a-button>
            <a-button type="primary" @click="handleSubmit" :loading="submitLoading">
              {{ isEdit ? '保存修改' : '创建计划' }}
            </a-button>
          </a-space>
        </div>
      </template>
      <!-- 基本信息 - 紧凑布局 -->
      <div class="basic-info">
        <a-row :gutter="16">
          <a-col :span="8">
            <div class="field-item">
              <label>订单 <span class="required">*</span></label>
              <a-select
                v-model:value="formData.orderId"
                placeholder="选择订单"
                show-search
                :filter-option="filterOrderOption"
                @change="onOrderChange"
                :disabled="isEdit"
                size="small"
              >
                <a-select-option v-for="order in orderList" :key="order.id" :value="order.id">
                  {{ order.orderNo }} - {{ order.styleCode }}
                </a-select-option>
              </a-select>
            </div>
          </a-col>
          <a-col :span="4">
            <div class="field-item">
              <label>面料 <span class="required">*</span></label>
              <a-select v-model:value="formData.fabric" placeholder="选择面料" size="small">
                <a-select-option v-for="f in fabricList" :key="f" :value="f">
                  {{ f }}
                </a-select-option>
              </a-select>
            </div>
          </a-col>
          <a-col :span="3">
            <div class="field-item">
              <label>床次 <span class="required">*</span></label>
              <a-input-number v-model:value="formData.bedNo" :min="1" :precision="0" size="small" style="width: 100%" />
            </div>
          </a-col>
          <a-col :span="3">
            <div class="field-item">
              <label>层数 <span class="required">*</span></label>
              <a-input-number v-model:value="formData.layers" :min="1" :precision="0" size="small" style="width: 100%" />
            </div>
          </a-col>
          <a-col :span="3">
            <div class="field-item">
              <label>每扎件数 <span class="required">*</span></label>
              <a-input-number v-model:value="formData.qtyPerBundle" :min="1" :precision="0" size="small" style="width: 100%" />
            </div>
          </a-col>
          <a-col :span="3">
            <div class="field-item">
              <label>裁床日期</label>
              <a-date-picker v-model:value="formData.cuttingDate" size="small" style="width: 100%" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
            </div>
          </a-col>
        </a-row>
      </div>

      <!-- 配比明细表格 -->
      <div class="detail-section">
        <div class="detail-header">
          <span class="detail-title">配比明细</span>
          <a-button type="dashed" size="small" @click="addDetailRow">+ 添加配比</a-button>
        </div>
        <a-table
          :columns="detailColumns"
          :data-source="formData.details"
          :pagination="false"
          size="small"
          bordered
          :scroll="{ y: 400 }"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.dataIndex === 'part'">
              <a-select v-model:value="record.part" placeholder="部位" size="small" style="width: 100%" show-search allow-custom-input>
                <a-select-option v-for="p in filteredStyleParts" :key="p.partName" :value="p.partName">{{ p.partName }}</a-select-option>
              </a-select>
            </template>
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
            <template v-if="column.dataIndex === 'totalNeeded'">
              <span>{{ getProgress(record.part, record.color, record.size).totalNeeded }}</span>
            </template>
            <template v-if="column.dataIndex === 'alreadyCut'">
              <span style="color: #52c41a;">{{ getProgress(record.part, record.color, record.size).alreadyCut }}</span>
            </template>
            <template v-if="column.dataIndex === 'remaining'">
              <span style="color: #ff4d4f;">{{ getProgress(record.part, record.color, record.size).remaining }}</span>
            </template>
            <template v-if="column.dataIndex === 'ratio'">
              <a-input-number v-model:value="record.ratio" :min="1" size="small" style="width: 100%" />
            </template>
            <template v-if="column.dataIndex === 'planQty'">
              <span class="plan-qty">{{ (formData.layers || 0) * (record.ratio || 0) }}</span>
            </template>
            <template v-if="column.dataIndex === 'action'">
              <a style="color: #ff4d4f; font-size: 12px; cursor: pointer;" @click="removeDetailRow(index)">删除</a>
            </template>
          </template>
        </a-table>
      </div>

      <!-- 底部汇总 -->
      <div class="footer-bar">
        <span class="total-label">总计划数：</span>
        <span class="total-value">{{ totalPlanQty }}</span>
        <span class="total-unit">件</span>
        <span class="total-separator">|</span>
        <span class="total-label">配比行数：</span>
        <span class="total-value">{{ formData.details.length }}</span>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { cuttingPlanApi } from '../../api/cutting'
import { orderApi } from '../../api/order'
import { materialApi, partApi, styleApi } from '../../api/base'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const planId = computed(() => route.params.id ? Number(route.params.id) : null)

// 基础数据
const orderList = ref<any[]>([])
const materialList = ref<any[]>([])
const partList = ref<any[]>([])
const colorSizeList = ref<any[]>([])
const styleParts = ref<any[]>([])
const cuttingProgress = ref<any[]>([])

// 表单数据
const submitLoading = ref(false)
const formData = reactive({
  orderId: undefined as number | undefined,
  fabric: undefined as string | undefined,
  bedNo: 1,
  layers: 1,
  qtyPerBundle: 20,
  cuttingDate: undefined as string | undefined,
  details: [] as any[],
})

// 配比表格列
const detailColumns = [
  { title: '部位', dataIndex: 'part', width: 120 },
  { title: '颜色', dataIndex: 'color', width: 100 },
  { title: '尺码', dataIndex: 'size', width: 80 },
  { title: '需裁', dataIndex: 'totalNeeded', width: 70, align: 'center' },
  { title: '已裁', dataIndex: 'alreadyCut', width: 70, align: 'center' },
  { title: '待裁', dataIndex: 'remaining', width: 70, align: 'center' },
  { title: '配比', dataIndex: 'ratio', width: 70, align: 'center' },
  { title: '计划数', dataIndex: 'planQty', width: 70, align: 'center' },
  { title: '操作', dataIndex: 'action', width: 50, align: 'center' },
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

// 可选面料（从款号部位BOM中提取）
const fabricList = computed(() => {
  const fabrics = new Set<string>()
  styleParts.value.forEach(item => {
    if (item.fabric) fabrics.add(item.fabric)
  })
  return Array.from(fabrics)
})

// 可选部位（根据选择的面料过滤）
const filteredStyleParts = computed(() => {
  if (!formData.fabric) return styleParts.value
  return styleParts.value.filter(item => item.fabric === formData.fabric)
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
    formData.bedNo = plan.bedNo
    formData.layers = plan.layers || 1
    formData.qtyPerBundle = plan.qtyPerBundle
    formData.cuttingDate = plan.cuttingDate
    formData.details = detailsRes.data || []

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
  styleParts.value = []
  cuttingProgress.value = []
  if (orderId) {
    try {
      // 获取订单信息（包含styleId）
      const orderRes = await orderApi.getById(orderId)
      const order = orderRes.data

      // 加载颜色尺码、款号部位、裁剪进度
      const [colorSizeRes, partsRes, progressRes] = await Promise.all([
        orderApi.getColorSizes(orderId),
        order.styleId ? styleApi.getParts(order.styleId) : Promise.resolve({ data: [] }),
        order.styleId ? cuttingPlanApi.getProgress(orderId, order.styleId) : Promise.resolve({ data: [] }),
      ])
      colorSizeList.value = colorSizeRes.data || []
      styleParts.value = partsRes.data || []
      cuttingProgress.value = progressRes.data || []

      // 自动选择面料（优先使用款号部位配置的面料）
      if (styleParts.value.length > 0 && styleParts.value[0].fabric) {
        formData.fabric = styleParts.value[0].fabric
      } else if (materialList.value.length > 0) {
        formData.fabric = materialList.value[0].materialName
      }
    } catch {}
  }
}

// 获取裁剪进度
function getProgress(part: string, color: string, size: string) {
  const key = `${part}|${color}|${size}`
  return cuttingProgress.value.find(p =>
    `${p.part}|${p.color}|${p.size}` === key
  ) || { totalNeeded: 0, alreadyCut: 0, remaining: 0 }
}

// 添加配比行
function addDetailRow() {
  formData.details.push({ part: undefined, color: undefined, size: undefined, ratio: 1 })
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
    message.warning('请添加配比明细')
    return
  }
  submitLoading.value = true
  try {
    const data = {
      plan: {
        orderId: formData.orderId,
        fabric: formData.fabric,
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

// 重置表单
function resetForm() {
  formData.orderId = undefined
  formData.fabric = undefined
  formData.bedNo = 1
  formData.layers = 1
  formData.qtyPerBundle = 20
  formData.cuttingDate = undefined
  formData.details = []
  colorSizeList.value = []
  styleParts.value = []
  cuttingProgress.value = []
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
  } else {
    resetForm()
  }
})
</script>

<style scoped>
.cutting-plan-form {
  height: 100%;
}

/* 基本信息区域 */
.basic-info {
  background: #fafafa;
  padding: 12px 16px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-item label {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
}

.required {
  color: #ff4d4f;
}

/* 配比明细区域 */
.detail-section {
  margin-bottom: 16px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.detail-title {
  font-weight: 500;
  font-size: 14px;
}

.plan-qty {
  font-weight: 500;
  color: #4A6FA5;
}

/* 底部汇总 */
.footer-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
}

.total-label {
  font-size: 13px;
  color: #666;
}

.total-value {
  font-size: 18px;
  font-weight: 600;
  color: #4A6FA5;
}

.total-unit {
  font-size: 13px;
  color: #666;
}

.total-separator {
  color: #e8e8e8;
  margin: 0 4px;
}
</style>
