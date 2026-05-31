<template>
  <a-card :title="`款号详情：${styleInfo.styleCode}`" :loading="loading">
    <template #extra>
      <a-button @click="router.push('/base/styles')">返回列表</a-button>
    </template>

    <!-- 基本信息 -->
    <a-descriptions bordered :column="2" style="margin-bottom: 24px">
      <a-descriptions-item label="款号">{{ styleInfo.styleCode }}</a-descriptions-item>
      <a-descriptions-item label="款式名称">{{ styleInfo.styleName }}</a-descriptions-item>
    </a-descriptions>

    <!-- Tab：工序路由 + 物料清单 -->
    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="process" tab="标准工序路由">
        <div style="margin-bottom: 12px">
          <a-button type="dashed" block @click="addProcessRow">+ 添加工序</a-button>
        </div>
        <a-table
          :columns="processColumns"
          :data-source="processList"
          :pagination="false"
          row-key="_key"
          size="small"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'processId'">
              <a-select
                v-model:value="record.processId"
                placeholder="选择工序"
                style="width: 100%"
                show-search
                :filter-option="filterProcessOption"
                @change="(val: number) => onProcessChange(record, val)"
              >
                <a-select-option v-for="p in allProcesses" :key="p.id" :value="p.id">
                  {{ p.name }}
                </a-select-option>
              </a-select>
            </template>
            <template v-else-if="column.key === 'sort'">
              <a-input-number v-model:value="record.sort" :min="0" style="width: 100%" />
            </template>
            <template v-else-if="column.key === 'refPrice'">
              <a-input-number v-model:value="record.refPrice" :min="0" :precision="2" style="width: 100%" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a style="color: #ff4d4f" @click="removeProcessRow(index)">删除</a>
            </template>
          </template>
        </a-table>
        <div style="margin-top: 16px; text-align: right">
          <a-button type="primary" :loading="savingProcesses" @click="handleSaveProcesses">保存工序路由</a-button>
        </div>
      </a-tab-pane>

      <a-tab-pane key="material" tab="标准物料清单">
        <div style="margin-bottom: 12px">
          <a-button type="dashed" block @click="addMaterialRow">+ 添加物料</a-button>
        </div>
        <a-table
          :columns="materialColumns"
          :data-source="materialList"
          :pagination="false"
          row-key="_key"
          size="small"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'materialName'">
              <a-select
                v-model:value="record.materialName"
                placeholder="选择或输入物料"
                style="width: 100%"
                show-search
                allow-custom-input
                :filter-option="filterMaterialOption"
                @change="(val: string) => onMaterialChange(record, val)"
              >
                <a-select-option v-for="m in allMaterials" :key="m.id" :value="m.materialName">
                  {{ m.materialName }} ({{ m.type === 1 ? '原料' : '辅料' }})
                </a-select-option>
              </a-select>
            </template>
            <template v-else-if="column.key === 'type'">
              <a-radio-group v-model:value="record.type">
                <a-radio :value="1">原料</a-radio>
                <a-radio :value="2">辅料</a-radio>
              </a-radio-group>
            </template>
            <template v-else-if="column.key === 'unitQty'">
              <a-input-number v-model:value="record.unitQty" :min="0" :precision="4" style="width: 100%" />
            </template>
            <template v-else-if="column.key === 'unit'">
              <a-input v-model:value="record.unit" placeholder="单位" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a style="color: #ff4d4f" @click="removeMaterialRow(index)">删除</a>
            </template>
          </template>
        </a-table>
        <div style="margin-top: 16px; text-align: right">
          <a-button type="primary" :loading="savingMaterials" @click="handleSaveMaterials">保存物料清单</a-button>
        </div>
      </a-tab-pane>
    </a-tabs>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onActivated } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { styleApi, processApi, materialApi } from '../../api/base'

const router = useRouter()
const route = useRoute()
const styleId = Number(route.params.id)

let rowKeyCounter = 0

const loading = ref(false)
const activeTab = ref('process')

const styleInfo = reactive({
  styleCode: '',
  styleName: '',
})

// 工序路由
const savingProcesses = ref(false)
const processList = ref<any[]>([])
const allProcesses = ref<any[]>([])

// 物料清单
const savingMaterials = ref(false)
const materialList = ref<any[]>([])
const allMaterials = ref<any[]>([])

const processColumns = [
  { title: '工序', key: 'processId', width: 200 },
  { title: '排序', key: 'sort', width: 120 },
  { title: '工价（元/件）', key: 'refPrice', width: 150 },
  { title: '操作', key: 'action', width: 80 },
]

const materialColumns = [
  { title: '物料名称', key: 'materialName', width: 200 },
  { title: '类型', key: 'type', width: 150 },
  { title: '单件用量', key: 'unitQty', width: 120 },
  { title: '单位', key: 'unit', width: 120 },
  { title: '操作', key: 'action', width: 80 },
]

function filterProcessOption(input: string, option: any) {
  const proc = allProcesses.value.find((p: any) => p.id === option.value)
  return proc?.name?.toLowerCase().includes(input.toLowerCase()) ?? false
}

function filterMaterialOption(input: string, option: any) {
  return option.value?.toLowerCase().includes(input.toLowerCase()) ?? false
}

function onMaterialChange(record: any, materialName: string) {
  const material = allMaterials.value.find((m: any) => m.materialName === materialName)
  if (material) {
    record.type = material.type
    record.unit = material.unit
  }
}

async function fetchStyleInfo() {
  loading.value = true
  try {
    const res: any = await styleApi.getById(styleId)
    const data = res.data || {}
    styleInfo.styleCode = data.styleCode || ''
    styleInfo.styleName = data.styleName || ''
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function fetchProcesses() {
  try {
    const res: any = await styleApi.getProcesses(styleId)
    processList.value = (res.data || []).map((item: any) => ({
      ...item,
      _key: ++rowKeyCounter,
    }))
  } catch {
    processList.value = []
  }
}

async function fetchMaterials() {
  try {
    const res: any = await styleApi.getMaterials(styleId)
    materialList.value = (res.data || []).map((item: any) => ({
      ...item,
      _key: ++rowKeyCounter,
    }))
  } catch {
    materialList.value = []
  }
}

async function fetchAllProcesses() {
  try {
    const res: any = await processApi.list({ page: 1, pageSize: 9999 })
    allProcesses.value = res.data?.records || res.data?.list || res.data || []
  } catch {
    // handled by interceptor
  }
}

async function fetchAllMaterials() {
  try {
    const res: any = await materialApi.list({ page: 1, size: 9999 })
    allMaterials.value = res.data?.records || []
  } catch {
    // handled by interceptor
  }
}

// 工序路由操作
function addProcessRow() {
  processList.value.push({
    _key: ++rowKeyCounter,
    processId: undefined,
    sort: 0,
    refPrice: undefined,
  })
}

function onProcessChange(record: any, processId: number) {
  const process = allProcesses.value.find((p: any) => p.id === processId)
  if (process) {
    record.refPrice = process.refPrice
  }
}

function removeProcessRow(index: number) {
  processList.value.splice(index, 1)
}

async function handleSaveProcesses() {
  savingProcesses.value = true
  try {
    const data = processList.value.map(({ processId, sort, refPrice }: any) => ({
      processId,
      sort,
      refPrice,
    }))
    await styleApi.saveProcesses(styleId, data)
    message.success('工序路由保存成功')
  } catch {
    // handled by interceptor
  } finally {
    savingProcesses.value = false
  }
}

// 物料清单操作
function addMaterialRow() {
  materialList.value.push({
    _key: ++rowKeyCounter,
    materialName: '',
    type: 1,
    unitQty: undefined,
    unit: '',
  })
}

function removeMaterialRow(index: number) {
  materialList.value.splice(index, 1)
}

async function handleSaveMaterials() {
  savingMaterials.value = true
  try {
    const data = materialList.value.map(({ materialName, type, unitQty, unit }: any) => ({
      materialName,
      type,
      unitQty,
      unit,
    }))
    await styleApi.saveMaterials(styleId, data)
    message.success('物料清单保存成功')
  } catch {
    // handled by interceptor
  } finally {
    savingMaterials.value = false
  }
}

onMounted(() => {
  fetchStyleInfo()
  fetchProcesses()
  fetchMaterials()
  fetchAllProcesses()
  fetchAllMaterials()
})

// 页面被 keep-alive 缓存后，再次进入时刷新下拉数据
onActivated(() => {
  fetchAllProcesses()
  fetchAllMaterials()
})
</script>
