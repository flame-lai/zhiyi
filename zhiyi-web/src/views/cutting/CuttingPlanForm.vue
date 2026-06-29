<template>
  <div class="cutting-plan-form">
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>{{ readonly ? '裁床计划详情' : isEdit ? '编辑裁床计划' : '新增裁床计划' }}</span>
          <a-space>
            <a-button @click="handleBack">{{ readonly ? '返回' : '取消' }}</a-button>
            <a-button v-if="!readonly" type="primary" @click="handleSubmit" :loading="submitLoading">
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
                :disabled="isEdit || readonly"
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
              <a-select
                v-model:value="formData.fabric"
                placeholder="选择面料"
                show-search
                size="small"
                :disabled="readonly"
              >
                <a-select-option v-for="f in fabricList" :key="f" :value="f">
                  {{ f }}
                </a-select-option>
              </a-select>
            </div>
          </a-col>
          <a-col :span="3">
            <div class="field-item">
              <label>床次 <span class="required">*</span></label>
              <a-input-number v-model:value="formData.bedNo" :min="1" :precision="0" size="small" style="width: 100%" :disabled="readonly" />
            </div>
          </a-col>
          <a-col :span="3">
            <div class="field-item">
              <label>层数 <span class="required">*</span></label>
              <a-input-number v-model:value="formData.layers" :min="1" :precision="0" size="small" style="width: 100%" :disabled="readonly" />
            </div>
          </a-col>
          <a-col :span="3">
            <div class="field-item">
              <label>裁床日期</label>
              <a-date-picker v-model:value="formData.cuttingDate" size="small" style="width: 100%" format="YYYY-MM-DD" value-format="YYYY-MM-DD" :disabled="readonly" />
            </div>
          </a-col>
        </a-row>
      </div>

      <!-- 配比明细和参考信息 - 左右布局 -->
      <div class="main-content">
        <!-- 左侧：配比明细 -->
        <div class="left-panel">
          <div class="detail-section">
            <div class="detail-header">
              <div class="detail-header-left">
                <span class="detail-title">配比明细</span>
                <span class="detail-count" v-if="formData.details.length > 0">共 {{ formData.details.length }} 行</span>
              </div>
              <a-button v-if="!readonly" type="primary" size="middle" @click="addDetailRow" class="add-btn">
                <template #icon><plus-outlined /></template>
                添加配比
              </a-button>
            </div>
            <div class="table-container">
              <a-table
                :columns="detailColumns"
                :data-source="formData.details"
                :pagination="false"
                size="middle"
                bordered
                :scroll="{ y: 500 }"
                :row-class-name="(_record: any, index: number) => index % 2 === 0 ? 'table-row-even' : 'table-row-odd'"
              >
                <template #bodyCell="{ column, record, index }">
                  <template v-if="column.dataIndex === 'part'">
                    <a-select
                      v-model:value="record.parts"
                      mode="tags"
                      placeholder="选择或输入部位"
                      size="small"
                      style="width: 100%"
                      :disabled="readonly"
                      :options="partOptions"
                      :token-separators="[',']"
                    />
                  </template>
                  <template v-if="column.dataIndex === 'piecesPerGarment'">
                    <span class="cell-value">{{ calcPiecesPerGarment(record.parts) }}</span>
                  </template>
                  <template v-if="column.dataIndex === 'color'">
                    <a-select v-model:value="record.color" placeholder="颜色" size="small" style="width: 100%" :disabled="readonly">
                      <a-select-option v-for="color in colorList" :key="color" :value="color">{{ color }}</a-select-option>
                    </a-select>
                  </template>
                  <template v-if="column.dataIndex === 'size'">
                    <a-select v-model:value="record.size" placeholder="尺码" size="small" style="width: 100%" :disabled="readonly">
                      <a-select-option v-for="size in sizeList" :key="size" :value="size">{{ size }}</a-select-option>
                    </a-select>
                  </template>
                  <template v-if="column.dataIndex === 'ratio'">
                    <a-input-number v-model:value="record.ratio" :min="1" size="small" style="width: 100%" :disabled="readonly" @change="() => onRatioChange(record)" />
                  </template>
                  <template v-if="column.dataIndex === 'planQty'">
                    <span class="cell-value cell-highlight">{{ record.planQty || 0 }}</span>
                  </template>
                  <template v-if="column.dataIndex === 'planPieces'">
                    <span class="cell-value">{{ calcPlanPieces(record) }}</span>
                  </template>
                  <template v-if="column.dataIndex === 'qtyPerBundle'">
                    <a-input-number v-model:value="record.qtyPerBundle" :min="1" size="small" style="width: 100%" :disabled="readonly" />
                  </template>
                  <template v-if="column.dataIndex === 'action'">
                    <a-button v-if="!readonly" type="link" danger size="small" @click="removeDetailRow(index)">删除</a-button>
                  </template>
                </template>
              </a-table>
            </div>
          </div>
        </div>

        <!-- 右侧：参考信息 -->
        <div class="right-panel" v-if="formData.orderId && colorSizeList.length > 0">
          <div class="ref-block">
            <div class="ref-title">订单颜色尺码明细</div>
            <div class="ref-table-wrapper">
              <a-table
                :columns="colorSizeColumns"
                :data-source="colorSizeList"
                :pagination="false"
                size="small"
                bordered
              />
            </div>
          </div>
          <div class="ref-block" v-if="styleParts.length > 0">
            <div class="ref-title">款号裁剪部位配置</div>
            <div class="ref-table-wrapper">
              <a-table
                :columns="stylePartColumns"
                :data-source="styleParts"
                :pagination="false"
                size="small"
                bordered
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 底部汇总 -->
      <div class="footer-bar">
        <span class="total-label">总计划成衣数：</span>
        <span class="total-value">{{ totalPlanQty }}</span>
        <span class="total-unit">件</span>
        <span class="total-separator">|</span>
        <span class="total-label">总计划裁片数：</span>
        <span class="total-value">{{ totalPlanPieces }}</span>
        <span class="total-unit">片</span>
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
import { PlusOutlined } from '@ant-design/icons-vue'
import { cuttingPlanApi } from '../../api/cutting'
import { orderApi } from '../../api/order'
import { materialApi, styleApi } from '../../api/base'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const planId = computed(() => route.params.id ? Number(route.params.id) : null)
const planStatus = ref(0)
const readonly = computed(() => planStatus.value >= 2)

// 基础数据
const orderList = ref<any[]>([])
const materialList = ref<any[]>([])
const colorSizeList = ref<any[]>([])
const styleParts = ref<any[]>([])

// 表单数据
const submitLoading = ref(false)
const formData = reactive({
  orderId: undefined as number | undefined,
  fabric: undefined as string | undefined,
  bedNo: 1,
  layers: 1,
  cuttingDate: undefined as string | undefined,
  details: [] as any[],
})

// 配比表格列
const detailColumns = [
  { title: '部位', dataIndex: 'part', width: 140 },
  { title: '单件裁片数', dataIndex: 'piecesPerGarment', width: 90, align: 'center' },
  { title: '颜色', dataIndex: 'color', width: 90 },
  { title: '尺码', dataIndex: 'size', width: 70, align: 'center' },
  { title: '配比', dataIndex: 'ratio', width: 70, align: 'center' },
  { title: '计划成衣件数', dataIndex: 'planQty', width: 110, align: 'center' },
  { title: '计划裁片片数', dataIndex: 'planPieces', width: 110, align: 'center' },
  { title: '每扎件数', dataIndex: 'qtyPerBundle', width: 90, align: 'center' },
  { title: '操作', dataIndex: 'action', width: 50, align: 'center' },
]

// 订单颜色尺码表格列
const colorSizeColumns = [
  { title: '颜色', dataIndex: 'color', width: 100 },
  { title: '尺码', dataIndex: 'size', width: 80 },
  { title: '数量', dataIndex: 'qty', width: 80, align: 'center' },
]

// 款号裁剪部位表格列
const stylePartColumns = [
  { title: '部位', dataIndex: 'partName', width: 120 },
  { title: '面料', dataIndex: 'fabric', width: 120 },
  { title: '单件用量', dataIndex: 'qtyPerPiece', width: 100, align: 'center' },
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

// 部位下拉选项
const partOptions = computed(() => {
  const options = filteredStyleParts.value.map(p => ({ label: p.partName, value: p.partName }))
  // 添加已输入的自定义部位
  const existingParts = new Set(options.map(o => o.value))
  formData.details.forEach(d => {
    if (d.parts) {
      d.parts.forEach((p: string) => {
        if (!existingParts.has(p)) {
          options.push({ label: p, value: p })
          existingParts.add(p)
        }
      })
    }
  })
  return options
})

// 总计划成衣数
const totalPlanQty = computed(() => {
  return formData.details.reduce((sum, d) => {
    return sum + (d.planQty || 0)
  }, 0)
})

// 总计划裁片数
const totalPlanPieces = computed(() => {
  return formData.details.reduce((sum, d) => {
    const piecesPerGarment = calcPiecesPerGarment(d.parts)
    return sum + ((d.planQty || 0) * piecesPerGarment)
  }, 0)
})

// 计算单件裁片数（根据所选部位汇总）
function calcPiecesPerGarment(parts: string[]): number {
  if (!parts || parts.length === 0) return 0
  let total = 0
  parts.forEach(partName => {
    const part = styleParts.value.find(p => p.partName === partName)
    if (part && part.qtyPerPiece) {
      total += Math.floor(part.qtyPerPiece)
    }
  })
  return total
}

// 计算计划裁片片数
function calcPlanPieces(record: any): number {
  const piecesPerGarment = calcPiecesPerGarment(record.parts)
  const planPieces = (record.planQty || 0) * piecesPerGarment
  record.planPieces = planPieces
  return planPieces
}

// 配比变化时，更新计划成衣件数和每扎件数
function onRatioChange(record: any) {
  const defaultPlanQty = (formData.layers || 1) * (record.ratio || 1)
  record.planQty = defaultPlanQty
  record.qtyPerBundle = defaultPlanQty
}

// 加载基础数据
async function loadBaseData() {
  try {
    const [orderRes, materialRes] = await Promise.all([
      orderApi.list({ page: 1, size: 9999, status: 1 }),
      materialApi.list({ page: 1, size: 9999 }),
    ])
    orderList.value = orderRes.data?.records || []
    materialList.value = materialRes.data?.records || []
  } catch {}
}

// 加载编辑数据
async function loadEditData() {
  if (!planId.value) return
  console.log('loadEditData called, planId:', planId.value)
  try {
    const [planRes, detailsRes] = await Promise.all([
      cuttingPlanApi.getById(planId.value),
      cuttingPlanApi.getDetails(planId.value),
    ])
    console.log('planRes:', planRes)
    console.log('detailsRes:', detailsRes)
    const plan = planRes.data
    planStatus.value = plan.status || 0
    formData.orderId = plan.orderId
    formData.fabric = plan.fabric
    formData.bedNo = plan.bedNo
    formData.layers = plan.layers || 1
    formData.cuttingDate = plan.cuttingDate
    // 转换part字符串为parts数组
    formData.details = (detailsRes.data || []).map((d: any) => ({
      ...d,
      parts: d.part ? d.part.split(',') : [],
    }))

    if (plan.orderId) {
      // 获取订单信息
      const orderRes = await orderApi.getById(plan.orderId)
      const order = orderRes.data

      // 加载颜色尺码、款号部位
      const [colorSizeRes, partsRes] = await Promise.all([
        orderApi.getColorSizes(plan.orderId),
        order.styleId ? styleApi.getParts(order.styleId) : Promise.resolve({ data: [] }),
      ])
      colorSizeList.value = colorSizeRes.data || []
      styleParts.value = partsRes.data || []
    }
  } catch (e) {
    console.error('loadEditData error:', e)
  }
}

// 订单选择变化
async function onOrderChange(orderId: number) {
  formData.fabric = undefined
  formData.details = []
  colorSizeList.value = []
  styleParts.value = []
  if (orderId) {
    try {
      // 获取订单信息（包含styleId）
      const orderRes = await orderApi.getById(orderId)
      const order = orderRes.data

      // 加载颜色尺码、款号部位
      const [colorSizeRes, partsRes] = await Promise.all([
        orderApi.getColorSizes(orderId),
        order.styleId ? styleApi.getParts(order.styleId) : Promise.resolve({ data: [] }),
      ])
      colorSizeList.value = colorSizeRes.data || []
      styleParts.value = partsRes.data || []

      // 自动选择面料（优先使用款号部位配置的面料）
      if (styleParts.value.length > 0 && styleParts.value[0].fabric) {
        formData.fabric = styleParts.value[0].fabric
      } else if (materialList.value.length > 0) {
        formData.fabric = materialList.value[0].materialName
      }
    } catch {}
  }
}

// 添加配比行
function addDetailRow() {
  const defaultPlanQty = formData.layers || 1
  formData.details.push({ parts: [], color: undefined, size: undefined, ratio: 1, planQty: defaultPlanQty, qtyPerBundle: defaultPlanQty })
}

// 删除配比行
function removeDetailRow(index: number) {
  formData.details.splice(index, 1)
}

// 计划数变化时，更新每扎件数默认值
function onPlanQtyChange(record: any, val: number) {
  if (val && (!record.qtyPerBundle || record.qtyPerBundle === 1)) {
    record.qtyPerBundle = val
  }
}
void onPlanQtyChange

// 订单搜索过滤
function filterOrderOption(input: string, option: any) {
  const order = orderList.value.find(o => o.id === option.value)
  return order?.orderNo?.toLowerCase().includes(input.toLowerCase()) ?? false
}

// 提交表单
async function handleSubmit() {
  if (!formData.orderId || !formData.fabric || !formData.bedNo || !formData.layers) {
    message.warning('请填写完整信息')
    return
  }
  if (formData.details.length === 0) {
    message.warning('请添加配比明细')
    return
  }
  submitLoading.value = true
  try {
    // 转换parts数组为逗号分隔字符串，并移除parts字段
    const details = formData.details.map(d => {
      const { parts, ...rest } = d
      return {
        ...rest,
        part: Array.isArray(parts) ? parts.join(',') : d.part,
      }
    })
    const data = {
      plan: {
        orderId: formData.orderId,
        fabric: formData.fabric,
        bedNo: formData.bedNo,
        layers: formData.layers,
        cuttingDate: formData.cuttingDate,
      },
      details: details,
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
  formData.cuttingDate = undefined
  formData.details = []
  colorSizeList.value = []
  styleParts.value = []
}

// 返回列表
function handleBack() {
  router.push('/cutting/plans')
}

// 初始化
onMounted(async () => {
  console.log('CuttingPlanForm mounted, isEdit:', isEdit.value, 'planId:', planId.value)
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

/* 主内容区域 - 左右布局 */
.main-content {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

/* 左侧配比明细 */
.left-panel {
  flex: 4;
  min-width: 0;
}

/* 右侧参考信息 */
.right-panel {
  flex: 1;
  min-width: 240px;
  max-width: 280px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ref-block {
  background: #f6f8fa;
  border-radius: 4px;
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.ref-table-wrapper {
  max-height: 250px;
  overflow-y: auto;
}

.ref-title {
  font-weight: 500;
  font-size: 13px;
  margin-bottom: 8px;
  color: #333;
  position: sticky;
  top: 0;
  background: #f6f8fa;
  padding-bottom: 4px;
  z-index: 1;
}

/* 配比明细区域 */
.detail-section {
  margin-bottom: 0;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-title {
  font-weight: 600;
  font-size: 15px;
  color: #1a1a1a;
}

.detail-count {
  font-size: 12px;
  color: #8c8c8c;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 10px;
}

.add-btn {
  border-radius: 6px;
  font-weight: 500;
}

.table-container {
  border-radius: 6px;
  overflow: hidden;
}

/* 表格样式优化 */
.table-container :deep(.ant-table) {
  border-radius: 6px;
}

.table-container :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 600;
  font-size: 13px;
  color: #595959;
  padding: 10px 12px;
}

.table-container :deep(.ant-table-tbody > tr > td) {
  padding: 8px 12px;
}

.table-container :deep(.table-row-even) {
  background: #fff;
}

.table-container :deep(.table-row-odd) {
  background: #fafafa;
}

.table-container :deep(.ant-table-tbody > tr:hover > td) {
  background: #e6f7ff !important;
}

.cell-value {
  font-size: 13px;
  color: #333;
}

.cell-highlight {
  font-weight: 600;
  color: #1890ff;
}

.plan-qty {
  font-weight: 500;
  color: #4A6FA5;
}

/* 底部汇总 */
.footer-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  margin-top: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.total-label {
  font-size: 13px;
  color: #8c8c8c;
}

.total-value {
  font-size: 18px;
  font-weight: 700;
  color: #1890ff;
}

.total-unit {
  font-size: 13px;
  color: #8c8c8c;
}

.total-separator {
  color: #e8e8e8;
  margin: 0 4px;
  font-size: 16px;
}
</style>
