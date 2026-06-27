<template>
  <View class="emp-page">
    <!-- Search bar -->
    <View class="filter-bar">
      <View class="search-wrap">
        <Input
          class="search-input"
          placeholder="搜索工号/姓名/手机号"
          :value="keyword"
          @input="onKeywordInput"
          @confirm="onSearch"
        />
        <View class="search-btn" @tap="onSearch"><Text class="search-btn-text">搜索</Text></View>
      </View>
    </View>

    <View class="content">
      <View v-if="loading && list.length === 0" class="loading"><Text class="loading-text">加载中...</Text></View>
      <EmptyState v-else-if="!loading && list.length === 0" text="暂无员工数据" />
      <View v-else class="card-list">
        <View v-for="item in list" :key="item.id" class="card">
          <View class="card-head">
            <View class="card-head-left">
              <Text class="card-title">{{ item.name }}</Text>
              <Text class="card-sub">工号 {{ item.employeeNo }}</Text>
            </View>
            <View class="card-actions">
              <View class="edit-btn" @tap.stop="openEdit(item)"><Text class="edit-text">编辑</Text></View>
              <View :class="[item.status === 1 ? 'disable-btn' : 'enable-btn']" @tap.stop="onToggleStatus(item)">
                <Text :class="[item.status === 1 ? 'disable-text' : 'enable-text']">{{ item.status === 1 ? '停用' : '启用' }}</Text>
              </View>
              <View class="delete-btn" @tap.stop="onDelete(item)"><Text class="delete-text">删除</Text></View>
            </View>
          </View>

          <View class="card-stats">
            <View class="stat">
              <Text class="stat-label">角色</Text>
              <Text class="stat-value">{{ item.roleName || '-' }}</Text>
            </View>
            <View class="stat">
              <Text class="stat-label">手机号</Text>
              <Text class="stat-value">{{ item.phone || '-' }}</Text>
            </View>
            <View class="stat">
              <Text class="stat-label">登录账号</Text>
              <Text class="stat-value">{{ item.loginAccount || '-' }}</Text>
            </View>
            <View class="stat">
              <Text class="stat-label">状态</Text>
              <Text :class="['stat-value', item.status === 1 ? 'active' : 'disabled']">
                {{ item.status === 1 ? '启用' : '停用' }}
              </Text>
            </View>
          </View>

          <View v-if="item.remark" class="card-footer">
            <Text class="footer-label">备注</Text>
            <Text class="footer-value">{{ item.remark }}</Text>
          </View>
        </View>

        <!-- 加载更多提示 -->
        <View class="load-more" v-if="list.length > 0">
          <Text class="load-more-text">{{ loading ? '加载中...' : hasMore ? '上拉加载更多' : '没有更多了' }}</Text>
        </View>
      </View>
    </View>

    <!-- Floating add button -->
    <View class="fab" @tap="openAdd">
      <Text class="fab-text">+</Text>
    </View>

    <!-- Add/Edit ConfirmPanel -->
    <ConfirmPanel
      :visible="panelVisible"
      :title="editingId ? '编辑员工' : '新增员工'"
      confirm-text="保存"
      :loading="submitting"
      @confirm="submitForm"
      @cancel="closePanel"
    >
      <View class="form-item">
        <Text class="form-label">姓名 <Text class="required">*</Text></Text>
        <Input class="form-input" placeholder="请输入姓名" :value="form.name" @input="(e: any) => (form.name = e.detail.value)" />
      </View>
      <View class="form-item">
        <Text class="form-label">手机号</Text>
        <Input class="form-input" type="number" placeholder="请输入手机号" :value="form.phone" @input="(e: any) => (form.phone = e.detail.value)" />
      </View>
      <View class="form-item">
        <Text class="form-label">角色 <Text class="required">*</Text></Text>
        <Picker :range="roleNames" :value="roleIndex" @change="onRoleChange">
          <View class="form-picker">
            <Text :class="['form-picker-text', roleIndex < 0 && 'placeholder']">
              {{ roleIndex >= 0 ? roleNames[roleIndex] : '请选择角色' }}
            </Text>
          </View>
        </Picker>
      </View>
      <View class="form-item">
        <Text class="form-label">登录账号</Text>
        <Input class="form-input" placeholder="默认等于工号" :value="form.loginAccount" @input="(e: any) => (form.loginAccount = e.detail.value)" />
      </View>
      <View v-if="!editingId" class="form-item">
        <Text class="form-label">初始密码</Text>
        <Input class="form-input" password placeholder="默认 123456" :value="form.loginPassword" @input="(e: any) => (form.loginPassword = e.detail.value)" />
      </View>
      <View class="form-item">
        <Text class="form-label">状态</Text>
        <Picker :range="statusNames" :value="form.status === 1 ? 0 : 1" @change="onStatusChange">
          <View class="form-picker">
            <Text class="form-picker-text">{{ form.status === 1 ? '启用' : '停用' }}</Text>
          </View>
        </Picker>
      </View>
      <View class="form-item">
        <Text class="form-label">备注</Text>
        <Input class="form-input" placeholder="请输入备注（可选）" :value="form.remark" @input="(e: any) => (form.remark = e.detail.value)" />
      </View>
    </ConfirmPanel>
  </View>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import Taro, { useDidShow, useReachBottom } from '@tarojs/taro'
import { View, Text, Input, Picker } from '@tarojs/components'
import { getEmployees, getRoles, createEmployee, updateEmployee, deleteEmployee, toggleEmployeeStatus } from '../../../api/mobileAdmin'
import { checkAuthAndRedirect } from '../../../utils/auth'
import EmptyState from '../../../components/EmptyState.vue'
import ConfirmPanel from '../../../components/ConfirmPanel.vue'
import type { EmployeeRecord, RoleOption } from '../../../api/types'

definePageConfig({
  navigationBarTitleText: '员工管理',
})

const keyword = ref('')
const loading = ref(false)
const submitting = ref(false)
const list = ref<EmployeeRecord[]>([])
const page = ref(1)
const hasMore = ref(true)
const pageSize = 20
const roles = ref<RoleOption[]>([])
const panelVisible = ref(false)
const editingId = ref(0)

const form = reactive({
  name: '',
  phone: '',
  roleId: 0,
  loginAccount: '',
  loginPassword: '',
  status: 1,
  remark: '',
})

const roleNames = computed(() => roles.value.map(r => r.name))
const roleIndex = computed(() => roles.value.findIndex(r => r.id === form.roleId))
const statusNames = ['启用', '停用']

function onRoleChange(e: any) {
  const idx = Number(e.detail.value)
  if (idx >= 0 && idx < roles.value.length) {
    form.roleId = roles.value[idx].id
  }
}

function onStatusChange(e: any) {
  form.status = Number(e.detail.value) === 0 ? 1 : 0
}

function onKeywordInput(e: any) {
  keyword.value = e.detail.value
}

function onSearch() {
  loadList(true)
}

function openAdd() {
  editingId.value = 0
  form.name = ''
  form.phone = ''
  form.roleId = 0
  form.loginAccount = ''
  form.loginPassword = ''
  form.status = 1
  form.remark = ''
  panelVisible.value = true
}

function openEdit(item: EmployeeRecord) {
  editingId.value = item.id
  form.name = item.name
  form.phone = item.phone || ''
  form.roleId = item.roleId
  form.loginAccount = item.loginAccount || ''
  form.loginPassword = ''
  form.status = item.status
  form.remark = item.remark || ''
  panelVisible.value = true
}

function closePanel() {
  panelVisible.value = false
}

function onDelete(item: EmployeeRecord) {
  Taro.showModal({
    title: '确认删除',
    content: `确定删除员工 ${item.name}（${item.employeeNo}）？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteEmployee(item.id)
          Taro.showToast({ title: '已删除', icon: 'success' })
          await loadList(true)
        } catch {}
      }
    },
  })
}

function onToggleStatus(item: EmployeeRecord) {
  const newStatus = item.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '停用'
  Taro.showModal({
    title: `确认${action}`,
    content: `确定${action}员工 ${item.name}（${item.employeeNo}）？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await toggleEmployeeStatus(item.id, newStatus)
          Taro.showToast({ title: `已${action}`, icon: 'success' })
          await loadList(true)
        } catch {}
      }
    },
  })
}

async function submitForm() {
  if (submitting.value) return
  if (!form.name.trim()) {
    Taro.showToast({ title: '请输入姓名', icon: 'none' })
    return
  }
  if (!form.roleId) {
    Taro.showToast({ title: '请选择角色', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    if (editingId.value) {
      await updateEmployee(editingId.value, {
        name: form.name.trim(),
        phone: form.phone.trim() || undefined,
        roleId: form.roleId,
        loginAccount: form.loginAccount.trim() || undefined,
        status: form.status,
        remark: form.remark.trim() || undefined,
      })
    } else {
      await createEmployee({
        name: form.name.trim(),
        phone: form.phone.trim() || undefined,
        roleId: form.roleId,
        loginAccount: form.loginAccount.trim() || undefined,
        loginPassword: form.loginPassword.trim() || undefined,
        status: form.status,
        remark: form.remark.trim() || undefined,
      })
    }
    Taro.showToast({ title: '保存成功', icon: 'success' })
    panelVisible.value = false
    await loadList(true)
  } catch {} finally {
    submitting.value = false
  }
}

async function loadRoles() {
  try {
    const res = await getRoles()
    roles.value = res || []
  } catch {}
}

async function loadList(reset = false) {
  if (reset) {
    page.value = 1
    hasMore.value = true
    list.value = []
  }

  if (!hasMore.value || loading.value) return

  loading.value = true
  try {
    const kw = keyword.value.trim() || undefined
    const res = await getEmployees(kw, page.value, pageSize)
    const newRecords = res?.records || []
    list.value = reset ? newRecords : [...list.value, ...newRecords]
    hasMore.value = res?.hasMore ?? false
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

// 触底加载更多
useReachBottom(() => {
  if (hasMore.value && !loading.value) {
    page.value++
    loadList(false)
  }
})

useDidShow(() => {
  if (!checkAuthAndRedirect()) return
  loadRoles()
  loadList(true)
})
</script>

<style lang="scss">
.emp-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  position: relative;
}



.filter-bar {
  display: flex;
  padding: 20px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.search-wrap {
  flex: 1;
  display: flex;
  gap: 16px;
}

.search-input {
  flex: 1;
  height: 64px;
  padding: 0 20px;
  background: #f5f7fa;
  border-radius: 10px;
  font-size: 26px;
  color: #4A6FA5;
}

.search-btn {
  height: 64px;
  padding: 0 28px;
  background: #4A6FA5;
  border-radius: 10px;
  display: flex;
  align-items: center;
}

.search-btn-text {
  font-size: 26px;
  color: #fff;
  font-weight: 500;
}

.content {
  flex: 1;
  padding: 24px;
  padding-bottom: 140px;
  box-sizing: border-box;
}

.loading {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.loading-text {
  font-size: 28px;
  color: #9ca3af;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.card-head-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.card-title {
  font-size: 30px;
  font-weight: 600;
  color: #4A6FA5;
}

.card-sub {
  font-size: 24px;
  color: #9ca3af;
}

.card-actions {
  display: flex;
  gap: 12px;
}

.edit-btn {
  padding: 8px 20px;
  background: #f0f0ff;
  border-radius: 8px;
}

.edit-text {
  font-size: 24px;
  color: #4A6FA5;
}

.delete-btn {
  padding: 8px 20px;
  background: #fee2e2;
  border-radius: 8px;
}

.delete-text {
  font-size: 24px;
  color: #dc2626;
}

.enable-btn {
  padding: 8px 20px;
  background: #dcfce7;
  border-radius: 8px;
}

.enable-text {
  font-size: 24px;
  color: #16a34a;
}

.disable-btn {
  padding: 8px 20px;
  background: #fef3c7;
  border-radius: 8px;
}

.disable-text {
  font-size: 24px;
  color: #d97706;
}

.card-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.stat {
  flex: 1;
  min-width: 40%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-label {
  font-size: 24px;
  color: #9ca3af;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #4A6FA5;
}

.stat-value.active {
  color: #22c55e;
}

.stat-value.disabled {
  color: #ef4444;
}

.card-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
  display: flex;
  gap: 16px;
}

.footer-label {
  font-size: 24px;
  color: #9ca3af;
  flex-shrink: 0;
}

.footer-value {
  font-size: 24px;
  color: #4b5563;
}

.fab {
  position: fixed;
  right: 40px;
  bottom: 60px;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: #4A6FA5;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
  z-index: 10;
}

.fab-text {
  font-size: 56px;
  color: #fff;
  line-height: 1;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.form-label {
  font-size: 26px;
  color: #4b5563;
  font-weight: 500;
}

.required {
  color: #ef4444;
}

.form-input {
  height: 80px;
  padding: 0 20px;
  background: #f5f7fa;
  border-radius: 10px;
  font-size: 28px;
  color: #4A6FA5;
}

.form-picker {
  height: 80px;
  padding: 0 20px;
  background: #f5f7fa;
  border-radius: 10px;
  display: flex;
  align-items: center;
}

.form-picker-text {
  font-size: 28px;
  color: #4A6FA5;
}

.form-picker-text.placeholder {
  color: #9ca3af;
}

.load-more {
  text-align: center;
  padding: 24px 0 40px;
}

.load-more-text {
  font-size: 24px;
  color: #9ca3af;
}
</style>
