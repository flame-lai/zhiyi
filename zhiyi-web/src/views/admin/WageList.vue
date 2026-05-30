<template>
  <a-card title="工资管理">
    <div class="toolbar">
      <a-space>
        <a-month-picker
          v-model:value="searchYearMonth"
          placeholder="选择年月"
          style="width: 200px"
          format="YYYY-MM"
          valueFormat="YYYY-MM"
          @change="handleSearch"
        />
        <a-select
          v-model:value="searchStatus"
          placeholder="按状态筛选"
          allow-clear
          style="width: 200px"
          @change="handleSearch"
        >
          <a-select-option value="PENDING">待审核</a-select-option>
          <a-select-option value="AUDITED">已审核</a-select-option>
          <a-select-option value="PAID">已发放</a-select-option>
        </a-select>
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="resetSearch">重置</a-button>
      </a-space>
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
        <template v-if="column.key === 'status'">
          <a-tag :color="statusTagMap[record.status]?.color">
            {{ statusTagMap[record.status]?.text || record.status }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-popconfirm
              v-if="record.status === 'PENDING'"
              title="确定审核通过吗？"
              @confirm="handleAudit(record.id)"
            >
              <a>审核</a>
            </a-popconfirm>
            <a-popconfirm
              v-if="record.status === 'AUDITED'"
              title="确定标记为已发放吗？"
              @confirm="handleMarkPaid(record.id)"
            >
              <a>标记已发放</a>
            </a-popconfirm>
            <a @click="openOtherDrawer(record)">查看其他项目</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 其他项目 Drawer -->
    <a-drawer
      v-model:open="drawerVisible"
      title="其他工资项目"
      :width="600"
    >
      <div style="margin-bottom: 16px">
        <a-form layout="inline" :model="otherForm">
          <a-form-item label="名称">
            <a-input v-model:value="otherForm.name" placeholder="项目名称" style="width: 140px" />
          </a-form-item>
          <a-form-item label="金额">
            <a-input-number v-model:value="otherForm.amount" :precision="2" placeholder="金额" style="width: 120px" />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="otherForm.remark" placeholder="备注" style="width: 140px" />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" :loading="addOtherLoading" @click="handleAddOther">添加</a-button>
          </a-form-item>
        </a-form>
      </div>

      <a-table
        :columns="otherColumns"
        :data-source="otherItems"
        :loading="otherLoading"
        :pagination="false"
        row-key="id"
        size="small"
      />
    </a-drawer>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { wageManageApi } from '../../api/admin'

const loading = ref(false)
const otherLoading = ref(false)
const addOtherLoading = ref(false)
const drawerVisible = ref(false)
const dataList = ref<any[]>([])
const otherItems = ref<any[]>([])
const currentRecord = ref<any>(null)

const searchYearMonth = ref<string | undefined>(undefined)
const searchStatus = ref<string | undefined>(undefined)

const statusTagMap: Record<string, { text: string; color: string }> = {
  PENDING: { text: '待审核', color: 'default' },
  AUDITED: { text: '已审核', color: 'blue' },
  PAID: { text: '已发放', color: 'green' },
}

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const otherForm = reactive({
  name: '',
  amount: undefined as number | undefined,
  remark: '',
})

const columns = [
  { title: '员工ID', dataIndex: 'employeeId', key: 'employeeId' },
  { title: '年月', dataIndex: 'yearMonth', key: 'yearMonth' },
  { title: '计件工资', dataIndex: 'pieceWage', key: 'pieceWage' },
  { title: '计时工资', dataIndex: 'timedWage', key: 'timedWage' },
  { title: '其他', dataIndex: 'otherAmount', key: 'otherAmount' },
  { title: '总计', dataIndex: 'totalAmount', key: 'totalAmount' },
  { title: '状态', key: 'status', dataIndex: 'status' },
  { title: '操作', key: 'action', width: 250 },
]

const otherColumns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
]

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchYearMonth.value) {
      params.yearMonth = searchYearMonth.value
    }
    if (searchStatus.value) {
      params.status = searchStatus.value
    }
    const res: any = await wageManageApi.list(params)
    dataList.value = res.data?.records || res.data?.list || res.data || []
    pagination.total = res.data?.total || 0
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function resetSearch() {
  searchYearMonth.value = undefined
  searchStatus.value = undefined
  handleSearch()
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

async function handleAudit(id: number) {
  try {
    await wageManageApi.audit(id)
    message.success('审核成功')
    fetchData()
  } catch {
    // handled by interceptor
  }
}

async function handleMarkPaid(id: number) {
  try {
    await wageManageApi.markPaid(id)
    message.success('已标记为已发放')
    fetchData()
  } catch {
    // handled by interceptor
  }
}

// 其他项目
async function openOtherDrawer(record: any) {
  currentRecord.value = record
  otherForm.name = ''
  otherForm.amount = undefined
  otherForm.remark = ''
  drawerVisible.value = true
  await fetchOtherItems()
}

async function fetchOtherItems() {
  otherLoading.value = true
  try {
    const res: any = await wageManageApi.getOtherItems(
      currentRecord.value.employeeId,
      currentRecord.value.yearMonth
    )
    otherItems.value = res.data || []
  } catch {
    // handled by interceptor
  } finally {
    otherLoading.value = false
  }
}

async function handleAddOther() {
  if (!otherForm.name || otherForm.amount === undefined) {
    message.warning('请填写名称和金额')
    return
  }
  addOtherLoading.value = true
  try {
    await wageManageApi.addOtherItem({
      employeeId: currentRecord.value.employeeId,
      yearMonth: currentRecord.value.yearMonth,
      name: otherForm.name,
      amount: otherForm.amount,
      remark: otherForm.remark,
    })
    message.success('添加成功')
    otherForm.name = ''
    otherForm.amount = undefined
    otherForm.remark = ''
    fetchOtherItems()
    fetchData()
  } catch {
    // handled by interceptor
  } finally {
    addOtherLoading.value = false
  }
}

onMounted(() => {
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
