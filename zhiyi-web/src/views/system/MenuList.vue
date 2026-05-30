<template>
  <a-card>
    <!-- 操作栏 -->
    <div style="margin-bottom: 16px; display: flex; justify-content: flex-end">
      <a-button type="primary" @click="handleAdd()">新增菜单</a-button>
    </div>

    <!-- 数据表格（树形） -->
    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="middle"
      default-expand-all-rows
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'platform'">
          <a-tag :color="record.platform === 'web' ? 'blue' : 'green'">
            {{ record.platform === 'web' ? 'Web端' : '移动端' }}
          </a-tag>
        </template>
        <template v-if="column.dataIndex === 'type'">
          <a-tag :color="record.type === 1 ? 'blue' : 'green'">
            {{ record.type === 1 ? '目录' : '菜单' }}
          </a-tag>
        </template>
        <template v-if="column.dataIndex === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleAdd(record.id)">新增子菜单</a-button>
            <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-popconfirm
              title="确认删除该菜单？"
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
      :title="isEdit ? '编辑菜单' : '新增菜单'"
      @ok="handleSubmit"
      :confirmLoading="submitLoading"
      @cancel="handleCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="所属平台" name="platform">
          <a-select v-model:value="formData.platform" placeholder="请选择平台">
            <a-select-option value="web">Web端</a-select-option>
            <a-select-option value="mobile">移动端</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="菜单类型" name="type">
          <a-select v-model:value="formData.type" placeholder="请选择类型">
            <a-select-option :value="1">目录</a-select-option>
            <a-select-option :value="2">菜单</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="菜单名称" name="name">
          <a-input v-model:value="formData.name" placeholder="请输入菜单名称" />
        </a-form-item>
        <a-form-item label="上级菜单" name="parentId">
          <a-tree-select
            v-model:value="formData.parentId"
            :tree-data="parentTreeData"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择上级菜单（留空为顶级）"
            allow-clear
            tree-default-expand-all
          />
        </a-form-item>
        <a-form-item label="路由路径" name="path">
          <a-input v-model:value="formData.path" placeholder="请输入路由路径" />
        </a-form-item>
        <a-form-item label="图标" name="icon">
          <a-input v-model:value="formData.icon" placeholder="请输入图标名称" />
        </a-form-item>
        <a-form-item label="排序" name="sort">
          <a-input-number v-model:value="formData.sort" :min="0" style="width: 100%" placeholder="请输入排序值" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { menuApi } from '../../api/system'

// 表格列定义
const columns = [
  { title: '菜单名称', dataIndex: 'name', width: 180 },
  { title: '所属平台', dataIndex: 'platform', width: 100, align: 'center' as const },
  { title: '路由路径', dataIndex: 'path', width: 180 },
  { title: '图标', dataIndex: 'icon', width: 120 },
  { title: '排序', dataIndex: 'sort', width: 80, align: 'center' as const },
  { title: '类型', dataIndex: 'type', width: 80, align: 'center' as const },
  { title: '操作', dataIndex: 'action', width: 240, fixed: 'right' as const },
]

// 表格数据
const loading = ref(false)
const tableData = ref<any[]>([])

// 弹窗相关
const modalVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({
  id: undefined as number | undefined,
  parentId: 0 as number,
  name: '',
  path: '',
  icon: '',
  sort: 0,
  type: 1 as number,
  platform: 'web' as string,
})
const rules = {
  platform: [{ required: true, message: '请选择平台', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
}

// 上级菜单树数据（过滤当前平台）
const parentTreeData = computed(() => {
  const filtered = filterByPlatform(tableData.value, formData.platform)
  if (!isEdit.value || !formData.id) return filtered
  return filterTreeNode(filtered, formData.id)
})

// 按平台过滤
function filterByPlatform(tree: any[], platform: string): any[] {
  return tree
    .filter((item) => item.platform === platform)
    .map((item) => ({
      ...item,
      children: item.children ? filterByPlatform(item.children, platform) : [],
    }))
}

// 过滤掉指定 id 及其子节点
function filterTreeNode(tree: any[], excludeId: number): any[] {
  return tree
    .filter((item) => item.id !== excludeId)
    .map((item) => ({
      ...item,
      children: item.children ? filterTreeNode(item.children, excludeId) : [],
    }))
}

// 加载菜单列表
async function loadData() {
  loading.value = true
  try {
    const res: any = await menuApi.tree()
    tableData.value = res.data || []
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

// 新增
function handleAdd(parentId?: number) {
  isEdit.value = false
  resetForm()
  if (parentId) {
    formData.parentId = parentId
    // 从父菜单继承平台
    const parent = findMenuById(tableData.value, parentId)
    if (parent) formData.platform = parent.platform
  }
  modalVisible.value = true
}

// 查找菜单
function findMenuById(tree: any[], id: number): any {
  for (const item of tree) {
    if (item.id === id) return item
    if (item.children) {
      const found = findMenuById(item.children, id)
      if (found) return found
    }
  }
  return null
}

// 编辑
function handleEdit(record: any) {
  isEdit.value = true
  Object.assign(formData, {
    id: record.id,
    parentId: record.parentId || 0,
    name: record.name,
    path: record.path,
    icon: record.icon,
    sort: record.sort,
    type: record.type,
    platform: record.platform || 'web',
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
      await menuApi.update(formData.id, formData)
      message.success('编辑成功')
    } else {
      await menuApi.create(formData)
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
    parentId: 0,
    name: '',
    path: '',
    icon: '',
    sort: 0,
    type: 1,
    platform: 'web',
  })
  formRef.value?.resetFields()
}

// 删除
async function handleDelete(record: any) {
  try {
    await menuApi.delete(record.id)
    message.success('删除成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

onMounted(() => {
  loadData()
})
</script>
