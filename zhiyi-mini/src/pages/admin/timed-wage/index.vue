<template>
  <View class="timed-page">
    <!-- 筛选栏 -->
    <View class="filter-bar">
      <View class="quick-btns">
        <View :class="['quick-btn', filterMode === 'thisMonth' && 'active']" @tap="setThisMonth">
          <Text class="quick-btn-text">本月</Text>
        </View>
        <View :class="['quick-btn', filterMode === 'lastMonth' && 'active']" @tap="setLastMonth">
          <Text class="quick-btn-text">上月</Text>
        </View>
        <View :class="['quick-btn', filterMode === 'custom' && 'active']" @tap="filterMode = 'custom'">
          <Text class="quick-btn-text">自定义</Text>
        </View>
      </View>
      <!-- 员工筛选 -->
      <View class="filter-emp">
        <View class="emp-search-wrap">
          <Input
            class="emp-input"
            placeholder="筛选员工"
            :value="filterEmpKeyword"
            @input="onFilterEmpInput"
            @focus="filterEmpFocused = true"
          />
          <View v-if="filterEmployee" class="emp-tag" @tap="clearFilterEmp">
            <Text class="emp-tag-text">{{ filterEmployee.name }}</Text>
            <Text class="emp-tag-x">×</Text>
          </View>
        </View>
        <View v-if="filterEmpFocused && filterEmpResults.length > 0" class="emp-dropdown">
          <View v-for="emp in filterEmpResults" :key="emp.id" class="emp-item" @tap="selectFilterEmp(emp)">
            <Text class="emp-item-no">{{ emp.employeeNo }}</Text>
            <Text class="emp-item-name">{{ emp.name }}</Text>
          </View>
        </View>
      </View>
    </View>

    <!-- 自定义日期范围 -->
    <View v-if="filterMode === 'custom'" class="date-range-bar">
      <Picker mode="date" :value="customStart" @change="(e: any) => { customStart = e.detail.value; onFilterChange() }">
        <View class="date-picker"><Text class="date-text">{{ customStart || '开始日期' }}</Text></View>
      </Picker>
      <Text class="date-sep">~</Text>
      <Picker mode="date" :value="customEnd" @change="(e: any) => { customEnd = e.detail.value; onFilterChange() }">
        <View class="date-picker"><Text class="date-text">{{ customEnd || '结束日期' }}</Text></View>
      </Picker>
    </View>

    <View class="content">
      <View v-if="loading && page === 1" class="loading"><Text class="loading-text">加载中...</Text></View>
      <EmptyState v-else-if="list.length === 0" text="暂无计时工资记录" />
      <View v-else class="card-list">
        <View v-for="item in list" :key="item.id" class="card">
          <View class="card-head">
            <View class="card-head-left">
              <Text class="card-title">{{ item.employeeName }}</Text>
              <Text class="card-sub">工号 {{ item.employeeNo }}</Text>
            </View>
            <View class="card-actions">
              <View class="edit-btn" @tap.stop="openEdit(item)"><Text class="edit-text">编辑</Text></View>
              <View class="delete-btn" @tap.stop="onDelete(item)"><Text class="delete-text">删除</Text></View>
            </View>
          </View>

          <View class="card-stats">
            <View class="stat">
              <Text class="stat-label">日期</Text>
              <Text class="stat-value">{{ formatDate(item.workDate) }}</Text>
            </View>
            <View class="stat">
              <Text class="stat-label">工时</Text>
              <Text class="stat-value">{{ item.hours }}h</Text>
            </View>
            <View class="stat">
              <Text class="stat-label">时薪</Text>
              <Text class="stat-value">{{ formatAmount(item.hourlyRate) }}</Text>
            </View>
            <View class="stat">
              <Text class="stat-label">金额</Text>
              <Text class="stat-value highlight">{{ formatAmount(item.amount) }}</Text>
            </View>
          </View>

          <View v-if="item.workContent" class="card-footer">
            <Text class="footer-label">工作内容</Text>
            <Text class="footer-value">{{ item.workContent }}</Text>
          </View>
        </View>
        <View v-if="loading && page > 1" class="loading-more"><Text class="loading-more-text">加载中...</Text></View>
        <View v-if="!hasMore && list.length > 0" class="no-more"><Text class="no-more-text">没有更多了</Text></View>
      </View>
    </View>

    <!-- Floating add button -->
    <View class="fab" @tap="openAdd">
      <Text class="fab-text">+</Text>
    </View>

    <!-- Add/Edit ConfirmPanel -->
    <ConfirmPanel
      :visible="panelVisible"
      :title="editingId ? '编辑计时工资' : '新增计时工资'"
      confirm-text="保存"
      :loading="submitting"
      @confirm="submitForm"
      @cancel="closePanel"
    >
      <View class="form-item">
        <Text class="form-label">工号</Text>
        <View class="search-wrap">
          <Input
            class="form-input"
            placeholder="输入工号或姓名搜索"
            :value="searchKeyword"
            @input="onSearchInput"
            @focus="searchFocused = true"
          />
          <View v-if="selectedEmployee" class="selected-tag">
            <Text class="selected-tag-text">{{ selectedEmployee.name }}（{{ selectedEmployee.employeeNo }}）</Text>
            <View class="selected-tag-close" @tap="clearSelected"><Text class="close-x">×</Text></View>
          </View>
        </View>
        <View v-if="searchFocused && searchResults.length > 0" class="search-dropdown">
          <View v-for="emp in searchResults" :key="emp.id" class="search-item" @tap="selectEmployee(emp)">
            <Text class="search-item-no">{{ emp.employeeNo }}</Text>
            <Text class="search-item-name">{{ emp.name }}</Text>
          </View>
        </View>
        <View v-if="searchFocused && searchKeyword && searchResults.length === 0 && !searching" class="search-dropdown">
          <View class="search-empty"><Text class="search-empty-text">未找到匹配员工</Text></View>
        </View>
      </View>
      <View class="form-item">
        <Text class="form-label">日期</Text>
        <Picker mode="date" :value="form.workDate" @change="(e: any) => (form.workDate = e.detail.value)">
          <View class="form-picker">
            <Text :class="['form-picker-text', !form.workDate && 'placeholder']">{{ form.workDate || '选择日期' }}</Text>
          </View>
        </Picker>
      </View>
      <View class="form-item">
        <Text class="form-label">工时（小时）</Text>
        <Input class="form-input" type="digit" placeholder="请输入工时" :value="form.hours" @input="(e: any) => (form.hours = e.detail.value)" />
      </View>
      <View class="form-item">
        <Text class="form-label">时薪（元）</Text>
        <Input class="form-input" type="digit" placeholder="请输入时薪" :value="form.hourlyRate" @input="(e: any) => (form.hourlyRate = e.detail.value)" />
      </View>
      <View class="form-item">
        <Text class="form-label">工作内容</Text>
        <Input class="form-input" placeholder="请输入工作内容（可选）" :value="form.workContent" @input="(e: any) => (form.workContent = e.detail.value)" />
      </View>
      <View class="calc-row">
        <Text class="calc-label">金额</Text>
        <Text class="calc-value">{{ formatAmount(calcAmount) }}</Text>
      </View>
    </ConfirmPanel>
  </View>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import Taro, { useDidShow, useReachBottom } from '@tarojs/taro'
import { View, Text, Input, Picker } from '@tarojs/components'
import { getTimedWages, createTimedWage, updateTimedWage, deleteTimedWage, getEmployees } from '../../../api/mobileAdmin'
import { formatAmount, formatDate } from '../../../utils/format'
import { checkAuthAndRedirect } from '../../../utils/auth'
import EmptyState from '../../../components/EmptyState.vue'
import ConfirmPanel from '../../../components/ConfirmPanel.vue'
import type { TimedWageRecord, EmployeeRecord } from '../../../api/types'

definePageConfig({
  navigationBarTitleText: '计时工资',
})

function todayStr() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

function monthStr(offset = 0) {
  const d = new Date()
  d.setMonth(d.getMonth() + offset)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  return `${d.getFullYear()}-${m}`
}

// 筛选状态
const filterMode = ref<'thisMonth' | 'lastMonth' | 'custom'>('thisMonth')
const customStart = ref('')
const customEnd = ref('')
const filterEmployee = ref<EmployeeRecord | null>(null)
const filterEmpKeyword = ref('')
const filterEmpResults = ref<EmployeeRecord[]>([])
const filterEmpFocused = ref(false)
let filterEmpTimer: ReturnType<typeof setTimeout> | null = null

// 列表状态
const loading = ref(false)
const submitting = ref(false)
const list = ref<TimedWageRecord[]>([])
const page = ref(1)
const hasMore = ref(true)
const pageSize = 20

// 弹窗状态
const panelVisible = ref(false)
const editingId = ref(0)

const form = reactive({
  employeeNo: '',
  workDate: todayStr(),
  hours: '',
  hourlyRate: '',
  workContent: '',
})

// 弹窗内员工搜索
const searchKeyword = ref('')
const searchResults = ref<EmployeeRecord[]>([])
const searching = ref(false)
const searchFocused = ref(false)
const selectedEmployee = ref<EmployeeRecord | null>(null)
let searchTimer: ReturnType<typeof setTimeout> | null = null

// ===== 筛选逻辑 =====
function setThisMonth() {
  filterMode.value = 'thisMonth'
  onFilterChange()
}

function setLastMonth() {
  filterMode.value = 'lastMonth'
  onFilterChange()
}

function onFilterChange() {
  page.value = 1
  hasMore.value = true
  list.value = []
  loadList()
}

function onFilterEmpInput(e: any) {
  const val = e.detail.value
  filterEmpKeyword.value = val
  filterEmployee.value = null
  if (filterEmpTimer) clearTimeout(filterEmpTimer)
  if (!val.trim()) {
    filterEmpResults.value = []
    return
  }
  filterEmpTimer = setTimeout(async () => {
    try {
      const res = await getEmployees(val.trim(), 1, 10)
      filterEmpResults.value = res?.records || []
    } catch {
      filterEmpResults.value = []
    }
  }, 300)
}

function selectFilterEmp(emp: EmployeeRecord) {
  filterEmployee.value = emp
  filterEmpKeyword.value = emp.name
  filterEmpResults.value = []
  filterEmpFocused.value = false
  onFilterChange()
}

function clearFilterEmp() {
  filterEmployee.value = null
  filterEmpKeyword.value = ''
  filterEmpResults.value = []
  onFilterChange()
}

// ===== 弹窗内员工搜索 =====
function onSearchInput(e: any) {
  const val = e.detail.value
  searchKeyword.value = val
  selectedEmployee.value = null
  form.employeeNo = ''
  if (searchTimer) clearTimeout(searchTimer)
  if (!val.trim()) {
    searchResults.value = []
    return
  }
  searchTimer = setTimeout(async () => {
    searching.value = true
    try {
      const res = await getEmployees(val.trim(), 1, 10)
      searchResults.value = res?.records || []
    } catch {
      searchResults.value = []
    } finally {
      searching.value = false
    }
  }, 300)
}

function selectEmployee(emp: EmployeeRecord) {
  selectedEmployee.value = emp
  form.employeeNo = emp.employeeNo
  searchKeyword.value = emp.employeeNo
  searchResults.value = []
  searchFocused.value = false
}

function clearSelected() {
  selectedEmployee.value = null
  form.employeeNo = ''
  searchKeyword.value = ''
  searchResults.value = []
}

// ===== 列表加载 =====
const calcAmount = computed(() => {
  const h = parseFloat(form.hours) || 0
  const r = parseFloat(form.hourlyRate) || 0
  return h * r
})

function getFilterParams() {
  const params: any = { page: page.value, size: pageSize }
  if (filterMode.value === 'thisMonth') {
    params.yearMonth = monthStr(0)
  } else if (filterMode.value === 'lastMonth') {
    params.yearMonth = monthStr(-1)
  } else if (filterMode.value === 'custom') {
    if (customStart.value) params.startDate = customStart.value
    if (customEnd.value) params.endDate = customEnd.value
  }
  if (filterEmployee.value) {
    params.employeeId = filterEmployee.value.id
  }
  return params
}

async function loadList() {
  loading.value = true
  try {
    const res = await getTimedWages(getFilterParams())
    const data = res as any
    if (page.value === 1) {
      list.value = data?.records || []
    } else {
      list.value = [...list.value, ...(data?.records || [])]
    }
    hasMore.value = data?.hasMore ?? false
  } catch {
    if (page.value === 1) list.value = []
  } finally {
    loading.value = false
  }
}

useReachBottom(() => {
  if (!hasMore.value || loading.value) return
  page.value++
  loadList()
})

// ===== 弹窗操作 =====
function openAdd() {
  editingId.value = 0
  form.employeeNo = ''
  form.workDate = todayStr()
  form.hours = ''
  form.hourlyRate = ''
  form.workContent = ''
  clearSelected()
  panelVisible.value = true
}

function openEdit(item: TimedWageRecord) {
  editingId.value = item.id
  form.employeeNo = item.employeeNo || ''
  form.workDate = formatDate(item.workDate) || todayStr()
  form.hours = String(item.hours)
  form.hourlyRate = String(item.hourlyRate)
  form.workContent = item.workContent || ''
  if (item.employeeNo && item.employeeName) {
    selectedEmployee.value = { id: item.employeeId, employeeNo: item.employeeNo, name: item.employeeName, phone: '', roleId: 0, roleName: '', loginAccount: '', status: 1, remark: '' }
    searchKeyword.value = item.employeeNo
  } else {
    clearSelected()
  }
  panelVisible.value = true
}

function closePanel() {
  panelVisible.value = false
}

function onDelete(item: TimedWageRecord) {
  Taro.showModal({
    title: '确认删除',
    content: `确定删除 ${item.employeeName} 的计时工资记录?`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteTimedWage(item.id)
          Taro.showToast({ title: '已删除', icon: 'success' })
          page.value = 1
          hasMore.value = true
          await loadList()
        } catch {
          // handled by request util
        }
      }
    },
  })
}

async function submitForm() {
  if (submitting.value) return
  if (!form.employeeNo.trim()) {
    Taro.showToast({ title: '请搜索并选择员工', icon: 'none' })
    return
  }
  if (!form.hours || Number(form.hours) <= 0) {
    Taro.showToast({ title: '请输入有效工时', icon: 'none' })
    return
  }
  if (!form.hourlyRate || Number(form.hourlyRate) <= 0) {
    Taro.showToast({ title: '请输入有效时薪', icon: 'none' })
    return
  }

  submitting.value = true
  const payload = {
    employeeNo: form.employeeNo.trim(),
    workDate: form.workDate,
    hours: Number(form.hours),
    hourlyRate: Number(form.hourlyRate),
    workContent: form.workContent,
  }
  try {
    if (editingId.value) {
      await updateTimedWage(editingId.value, payload)
    } else {
      await createTimedWage(payload)
    }
    Taro.showToast({ title: '保存成功', icon: 'success' })
    panelVisible.value = false
    page.value = 1
    hasMore.value = true
    await loadList()
  } catch {
    // handled by request util
  } finally {
    submitting.value = false
  }
}

useDidShow(() => {
  if (!checkAuthAndRedirect()) return
  loadList()
})
</script>

<style lang="scss">
.timed-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  position: relative;
}

.filter-bar {
  background: #fff;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.quick-btns {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.quick-btn {
  flex: 1;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 8px;
}

.quick-btn.active {
  background: #4A6FA5;
}

.quick-btn-text {
  font-size: 24px;
  color: #6b7280;
}

.quick-btn.active .quick-btn-text {
  color: #fff;
  font-weight: 500;
}

.filter-emp {
  position: relative;
}

.emp-search-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.emp-input {
  flex: 1;
  height: 60px;
  padding: 0 16px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 24px;
}

.emp-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #e8f0fe;
  border-radius: 6px;
}

.emp-tag-text {
  font-size: 22px;
  color: #4A6FA5;
}

.emp-tag-x {
  font-size: 24px;
  color: #9ca3af;
}

.emp-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-top: 4px;
  max-height: 300px;
  overflow-y: auto;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  z-index: 20;
}

.emp-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.emp-item:last-child { border-bottom: none; }
.emp-item:active { background: #f5f7fa; }

.emp-item-no { font-size: 26px; font-weight: 600; color: #4A6FA5; }
.emp-item-name { font-size: 24px; color: #4b5563; }

.date-range-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.date-picker {
  flex: 1;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 8px;
}

.date-text { font-size: 24px; color: #4A6FA5; }
.date-sep { font-size: 24px; color: #9ca3af; }

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

.loading-more {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.loading-more-text {
  font-size: 24px;
  color: #9ca3af;
}

.no-more {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.no-more-text {
  font-size: 24px;
  color: #d1d5db;
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

.stat-value.highlight {
  color: #4A6FA5;
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

.calc-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 10px;
}

.calc-label {
  font-size: 28px;
  color: #4b5563;
  font-weight: 500;
}

.calc-value {
  font-size: 36px;
  color: #4A6FA5;
  font-weight: 700;
}

.search-wrap {
  position: relative;
}

.selected-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 16px;
  background: #e8f0fe;
  border-radius: 8px;
}

.selected-tag-text {
  font-size: 24px;
  color: #4A6FA5;
}

.selected-tag-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-x {
  font-size: 28px;
  color: #9ca3af;
}

.search-dropdown {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  margin-top: 8px;
  max-height: 400px;
  overflow-y: auto;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.search-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-bottom: 1px solid #f3f4f6;
}

.search-item:last-child {
  border-bottom: none;
}

.search-item:active {
  background: #f5f7fa;
}

.search-item-no {
  font-size: 28px;
  font-weight: 600;
  color: #4A6FA5;
}

.search-item-name {
  font-size: 26px;
  color: #4b5563;
}

.search-empty {
  padding: 30px;
  text-align: center;
}

.search-empty-text {
  font-size: 26px;
  color: #9ca3af;
}
</style>
