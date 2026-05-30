<template>
  <a-card>
    <!-- 操作栏 -->
    <div style="margin-bottom: 16px; display: flex; justify-content: flex-end">
      <a-button type="primary" @click="handleAdd">新增角色</a-button>
    </div>

    <!-- 数据表格 -->
    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      row-key="id"
      size="middle"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button type="link" size="small" @click="handleAssignMenus(record)">分配菜单</a-button>
            <a-popconfirm
              title="确认删除该角色？"
              @confirm="handleDelete(record)"
            >
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="formModalVisible"
      :title="isEdit ? '编辑角色' : '新增角色'"
      @ok="handleSubmit"
      :confirmLoading="submitLoading"
      @cancel="handleCancelForm"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="角色名称" name="name">
          <a-input v-model:value="formData.name" placeholder="请输入角色名称" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formData.description" placeholder="请输入描述" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分配菜单弹窗 -->
    <a-modal
      v-model:open="menuModalVisible"
      title="分配菜单"
      @ok="handleMenuSubmit"
      :confirmLoading="menuSubmitLoading"
      @cancel="menuModalVisible = false"
    >
      <a-spin :spinning="menuTreeLoading">
        <a-tree
          v-if="menuTreeData.length > 0"
          v-model:checkedKeys="checkedMenuKeys"
          :tree-data="menuTreeData"
          :field-names="{ title: 'name', key: 'id', children: 'children' }"
          checkable
          default-expand-all
          :style="{ maxHeight: '400px', overflow: 'auto' }"
        />
        <a-empty v-else description="暂无菜单数据" />
      </a-spin>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { roleApi, menuApi } from '../../api/system'

// 表格列定义
const columns = [
  { title: '角色名称', dataIndex: 'name', width: 180 },
  { title: '描述', dataIndex: 'description', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', dataIndex: 'action', width: 220, fixed: 'right' as const },
]

// 表格数据
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

// 表单弹窗
const formModalVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({
  id: undefined as number | undefined,
  name: '',
  description: '',
})
const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

// 菜单分配弹窗
const menuModalVisible = ref(false)
const menuSubmitLoading = ref(false)
const menuTreeLoading = ref(false)
const menuTreeData = ref<any[]>([])
const checkedMenuKeys = ref<number[]>([])
const currentRoleId = ref<number>(0)

// 加载角色列表
async function loadData() {
  loading.value = true
  try {
    const res: any = await roleApi.list()
    const list = res.data || []
    tableData.value = list
    pagination.total = list.length
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

// 表格分页变化
function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
}

// 新增
function handleAdd() {
  isEdit.value = false
  resetForm()
  formModalVisible.value = true
}

// 编辑
function handleEdit(record: any) {
  isEdit.value = true
  Object.assign(formData, {
    id: record.id,
    name: record.name,
    description: record.description,
  })
  formModalVisible.value = true
}

// 提交表单
async function handleSubmit() {
  try {
    await formRef.value?.validateFields()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await roleApi.update(formData.id, formData)
      message.success('编辑成功')
    } else {
      await roleApi.create(formData)
      message.success('新增成功')
    }
    formModalVisible.value = false
    loadData()
  } catch {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

// 取消表单弹窗
function handleCancelForm() {
  formModalVisible.value = false
  resetForm()
}

// 重置表单
function resetForm() {
  Object.assign(formData, {
    id: undefined,
    name: '',
    description: '',
  })
  formRef.value?.resetFields()
}

// 删除角色
async function handleDelete(record: any) {
  try {
    await roleApi.delete(record.id)
    message.success('删除成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

// 打开分配菜单弹窗
async function handleAssignMenus(record: any) {
  currentRoleId.value = record.id
  checkedMenuKeys.value = []
  menuTreeLoading.value = true
  menuModalVisible.value = true
  try {
    const [menuRes, roleMenuRes]: any[] = await Promise.all([
      menuApi.tree(),
      roleApi.getRoleMenus(record.id),
    ])
    menuTreeData.value = menuRes.data || []
    // roleMenuRes.data 为当前角色已有的菜单 id 列表
    checkedMenuKeys.value = roleMenuRes.data || []
  } catch {
    // handled by interceptor
  } finally {
    menuTreeLoading.value = false
  }
}

// 提交菜单分配
async function handleMenuSubmit() {
  menuSubmitLoading.value = true
  try {
    await roleApi.assignMenus(currentRoleId.value, checkedMenuKeys.value as number[])
    message.success('菜单分配成功')
    menuModalVisible.value = false
  } catch {
    // handled by interceptor
  } finally {
    menuSubmitLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>
