<template>
  <a-card title="款号管理">
    <div class="toolbar">
      <a-space>
        <a-input
          v-model:value="searchKeyword"
          placeholder="搜索款号/名称"
          allow-clear
          style="width: 240px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="resetSearch">重置</a-button>
      </a-space>
      <a-button type="primary" @click="openModal()">新增款号</a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="dataList"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="openModal(record)">编辑</a>
            <a @click="viewDetail(record)">查看详情</a>
            <a-popconfirm
              title="确定删除该款号吗？"
              @confirm="handleDelete(record.id)"
            >
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 款号表单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑款号' : '新增款号'"
      @ok="handleSubmit"
      :confirm-loading="submitting"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="款号" name="styleCode" :rules="[{ required: true, message: '请输入款号' }]">
          <a-input v-model:value="formData.styleCode" placeholder="请输入款号" />
        </a-form-item>
        <a-form-item label="款式名称" name="styleName" :rules="[{ required: true, message: '请输入款式名称' }]">
          <a-input v-model:value="formData.styleName" placeholder="请输入款式名称" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 工序路由弹窗 -->
    <a-modal
      v-model:open="processModalVisible"
      title="管理工序路由"
      width="700px"
      @ok="handleSaveProcesses"
      :confirm-loading="savingProcesses"
    >
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
            >
              <a-select-option v-for="p in allProcesses" :key="p.id" :value="p.id">
                {{ p.name }}
              </a-select-option>
            </a-select>
          </template>
          <template v-else-if="column.key === 'sortOrder'">
            <a-input-number v-model:value="record.sortOrder" :min="0" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'refPrice'">
            <a-input-number v-model:value="record.refPrice" :min="0" :precision="2" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a style="color: #ff4d4f" @click="removeProcessRow(index)">删除</a>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 物料清单弹窗 -->
    <a-modal
      v-model:open="materialModalVisible"
      title="管理物料清单"
      width="700px"
      @ok="handleSaveMaterials"
      :confirm-loading="savingMaterials"
    >
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
            <a-input v-model:value="record.materialName" placeholder="物料名称" />
          </template>
          <template v-else-if="column.key === 'quantity'">
            <a-input-number v-model:value="record.quantity" :min="0" :precision="2" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'unit'">
            <a-input v-model:value="record.unit" placeholder="单位" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a style="color: #ff4d4f" @click="removeMaterialRow(index)">删除</a>
          </template>
        </template>
      </a-table>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { styleApi, processApi } from '../../api/base'

const router = useRouter()

let rowKeyCounter = 0

const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const searchKeyword = ref('')
const dataList = ref<any[]>([])
const formRef = ref()

// 工序路由相关
const processModalVisible = ref(false)
const savingProcesses = ref(false)
const currentStyleId = ref<number | null>(null)
const processList = ref<any[]>([])
const allProcesses = ref<any[]>([])

// 物料清单相关
const materialModalVisible = ref(false)
const savingMaterials = ref(false)
const materialList = ref<any[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const formData = reactive({
  styleCode: '',
  styleName: '',
})

const columns = [
  { title: '款号', dataIndex: 'styleCode', key: 'styleCode' },
  { title: '款式名称', dataIndex: 'styleName', key: 'styleName' },
  { title: '操作', key: 'action', width: 250 },
]

const processColumns = [
  { title: '工序', key: 'processId', width: 200 },
  { title: '排序', key: 'sortOrder', width: 120 },
  { title: '参考工价', key: 'refPrice', width: 120 },
  { title: '操作', key: 'action', width: 80 },
]

const materialColumns = [
  { title: '物料名称', key: 'materialName', width: 200 },
  { title: '数量', key: 'quantity', width: 120 },
  { title: '单位', key: 'unit', width: 120 },
  { title: '操作', key: 'action', width: 80 },
]

function filterProcessOption(input: string, option: any) {
  const proc = allProcesses.value.find((p: any) => p.id === option.value)
  return proc?.name?.toLowerCase().includes(input.toLowerCase()) ?? false
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    const res: any = await styleApi.list(params)
    dataList.value = res.data?.records || res.data?.list || res.data || []
    pagination.total = res.data?.total || 0
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
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

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function resetSearch() {
  searchKeyword.value = ''
  handleSearch()
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

function openModal(record?: any) {
  if (record) {
    editingId.value = record.id
    formData.styleCode = record.styleCode || ''
    formData.styleName = record.styleName || ''
  } else {
    editingId.value = null
    formData.styleCode = ''
    formData.styleName = ''
  }
  modalVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const data = { ...formData }
    if (editingId.value) {
      await styleApi.update(editingId.value, data)
      message.success('编辑成功')
      modalVisible.value = false
      fetchData()
    } else {
      const res: any = await styleApi.create(data)
      message.success('新增成功')
      modalVisible.value = false
      // 跳转到详情页，方便立即配置工序和物料
      router.push(`/base/styles/${res.data}`)
    }
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await styleApi.delete(id)
    message.success('删除成功')
    fetchData()
  } catch {
    // handled by interceptor
  }
}

function viewDetail(record: any) {
  router.push(`/base/styles/${record.id}`)
}

// 工序路由管理
async function openProcessModal(record: any) {
  currentStyleId.value = record.id
  processModalVisible.value = true
  try {
    const res: any = await styleApi.getProcesses(record.id)
    processList.value = (res.data || []).map((item: any) => ({
      ...item,
      _key: ++rowKeyCounter,
    }))
  } catch {
    processList.value = []
  }
}
void openProcessModal

function addProcessRow() {
  processList.value.push({
    _key: ++rowKeyCounter,
    processId: undefined,
    sortOrder: 0,
    refPrice: undefined,
  })
}

function removeProcessRow(index: number) {
  processList.value.splice(index, 1)
}

async function handleSaveProcesses() {
  savingProcesses.value = true
  try {
    const data = processList.value.map(({ processId, sortOrder, refPrice }: any) => ({
      processId,
      sortOrder,
      refPrice,
    }))
    await styleApi.saveProcesses(currentStyleId.value!, data)
    message.success('工序路由保存成功')
    processModalVisible.value = false
  } catch {
    // handled by interceptor
  } finally {
    savingProcesses.value = false
  }
}

// 物料清单管理
async function openMaterialModal(record: any) {
  currentStyleId.value = record.id
  materialModalVisible.value = true
  try {
    const res: any = await styleApi.getMaterials(record.id)
    materialList.value = (res.data || []).map((item: any) => ({
      ...item,
      _key: ++rowKeyCounter,
    }))
  } catch {
    materialList.value = []
  }
}
void openMaterialModal

function addMaterialRow() {
  materialList.value.push({
    _key: ++rowKeyCounter,
    materialName: '',
    quantity: undefined,
    unit: '',
  })
}

function removeMaterialRow(index: number) {
  materialList.value.splice(index, 1)
}

async function handleSaveMaterials() {
  savingMaterials.value = true
  try {
    const data = materialList.value.map(({ materialName, quantity, unit }: any) => ({
      materialName,
      quantity,
      unit,
    }))
    await styleApi.saveMaterials(currentStyleId.value!, data)
    message.success('物料清单保存成功')
    materialModalVisible.value = false
  } catch {
    // handled by interceptor
  } finally {
    savingMaterials.value = false
  }
}

onMounted(() => {
  fetchData()
  fetchAllProcesses()
})

// 页面被 keep-alive 缓存后，再次进入时刷新数据
onActivated(() => {
  fetchData()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>
