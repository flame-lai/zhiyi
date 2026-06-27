<template>
  <a-card>
    <!-- 搜索区域 -->
    <div style="margin-bottom: 16px; display: flex; justify-content: space-between">
      <a-input-search
        v-model:value="keyword"
        placeholder="搜索工号/姓名"
        style="width: 300px"
        @search="handleSearch"
        allowClear
      />
      <a-button type="primary" @click="handleAdd">新增员工</a-button>
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
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="record.status === 1 ? 'success' : 'default'">
            {{ record.status === 1 ? '启用' : '停用' }}
          </a-tag>
        </template>
        <template v-if="column.dataIndex === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button
              type="link"
              size="small"
              :style="{ color: record.status === 1 ? '#faad14' : '#52c41a' }"
              @click="handleToggleStatus(record)"
            >
              {{ record.status === 1 ? '停用' : '启用' }}
            </a-button>
            <a-popconfirm
              title="确认重置密码为 123456？"
              @confirm="handleResetPassword(record)"
            >
              <a-button type="link" size="small" danger>重置密码</a-button>
            </a-popconfirm>
            <a-popconfirm
              v-if="record.status === 0"
              title="确认删除该员工？删除后不可恢复"
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
      v-model:open="modalVisible"
      :title="isEdit ? '编辑员工' : '新增员工'"
      @ok="handleSubmit"
      :confirmLoading="submitLoading"
      @cancel="handleCancel"
      width="600px"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="姓名" name="name">
          <a-input v-model:value="formData.name" placeholder="请输入姓名" />
        </a-form-item>
        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="formData.phone" placeholder="请输入手机号" />
        </a-form-item>
        <a-form-item label="角色" name="roleId">
          <a-select v-model:value="formData.roleId" placeholder="请选择角色">
            <a-select-option v-for="role in roleOptions" :key="role.id" :value="role.id">
              {{ role.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="登录账号" name="loginAccount">
          <a-input v-model:value="formData.loginAccount" placeholder="默认等于工号" />
        </a-form-item>
        <a-form-item v-if="!isEdit" label="登录密码" name="loginPassword">
          <a-input-password v-model:value="formData.loginPassword" placeholder="初始密码，默认123456" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="formData.status" placeholder="请选择状态">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">停用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { employeeApi, roleApi } from '../../api/system'

// 表格列定义
const columns = [
  { title: '工号', dataIndex: 'employeeNo', width: 100 },
  { title: '姓名', dataIndex: 'name', width: 100 },
  { title: '手机号', dataIndex: 'phone', width: 130 },
  { title: '角色', dataIndex: 'roleName', width: 100 },
  { title: '登录账号', dataIndex: 'loginAccount', width: 120 },
  { title: '状态', dataIndex: 'status', width: 80, align: 'center' as const },
  { title: '操作', dataIndex: 'action', width: 220, fixed: 'right' as const },
]

// 表格数据
const loading = ref(false)
const tableData = ref<any[]>([])
const keyword = ref('')
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

// 弹窗相关
const modalVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({
  id: undefined as number | undefined,
  name: '',
  phone: '',
  roleId: undefined as number | undefined,
  loginAccount: '',
  loginPassword: '',
  status: 1,
  remark: '',
})
const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

// 角色选项
const roleOptions = ref<any[]>([])

// 加载角色列表
async function loadRoles() {
  try {
    const res: any = await roleApi.list()
    roleOptions.value = res.data || []
  } catch {
    // handled by interceptor
  }
}

// 加载表格数据
async function loadData() {
  loading.value = true
  try {
    const res: any = await employeeApi.list({
      keyword: keyword.value,
      page: pagination.current,
      size: pagination.pageSize,
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.current = 1
  loadData()
}

// 表格分页变化
function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

// 新增
function handleAdd() {
  isEdit.value = false
  resetForm()
  modalVisible.value = true
}

// 编辑
function handleEdit(record: any) {
  isEdit.value = true
  Object.assign(formData, {
    id: record.id,
    name: record.name,
    phone: record.phone || '',
    roleId: record.roleId,
    loginAccount: record.loginAccount || '',
    loginPassword: '',
    status: record.status,
    remark: record.remark || '',
  })
  modalVisible.value = true
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
      await employeeApi.update(formData.id, formData)
      message.success('编辑成功')
    } else {
      await employeeApi.create(formData)
      message.success('新增成功')
    }
    modalVisible.value = false
    loadData()
  } catch {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

// 取消弹窗
function handleCancel() {
  modalVisible.value = false
  resetForm()
}

// 重置表单
function resetForm() {
  Object.assign(formData, {
    id: undefined,
    name: '',
    phone: '',
    roleId: undefined,
    loginAccount: '',
    loginPassword: '',
    status: 1,
    remark: '',
  })
  formRef.value?.resetFields()
}

// 切换状态
async function handleToggleStatus(record: any) {
  const newStatus = record.status === 1 ? 0 : 1
  try {
    await employeeApi.updateStatus(record.id, newStatus)
    message.success('状态已更新')
    loadData()
  } catch {
    // handled by interceptor
  }
}

// 重置密码
async function handleResetPassword(record: any) {
  try {
    await employeeApi.resetPassword(record.id)
    message.success('密码已重置为 123456')
  } catch {
    // handled by interceptor
  }
}

// 删除员工
async function handleDelete(record: any) {
  try {
    await employeeApi.delete(record.id)
    message.success('删除成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

onMounted(() => {
  loadData()
  loadRoles()
})
</script>
