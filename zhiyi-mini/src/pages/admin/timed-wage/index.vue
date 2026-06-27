<template>
  <View class="timed-page">
    <!-- Month filter -->
    <View class="filter-bar">
      <Picker mode="date" fields="month" :value="filterMonth" @change="onMonthChange">
        <View class="filter-item">
          <Text class="filter-label">月份</Text>
          <Text class="filter-value">{{ filterMonth || '选择月份' }}</Text>
        </View>
      </Picker>
    </View>

    <View class="content">
      <View v-if="loading" class="loading"><Text class="loading-text">加载中...</Text></View>
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
              <Text class="stat-value">{{ formatAmount(item.hourlyRate).replace('¥', '¥') }}</Text>
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
        <Input class="form-input" placeholder="请输入工号" :value="form.employeeNo" @input="(e: any) => (form.employeeNo = e.detail.value)" />
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
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Input, Picker } from '@tarojs/components'
import { getTimedWages, createTimedWage, updateTimedWage, deleteTimedWage } from '../../../api/mobileAdmin'
import { formatAmount, formatDate } from '../../../utils/format'
import { checkAuthAndRedirect } from '../../../utils/auth'
import EmptyState from '../../../components/EmptyState.vue'
import ConfirmPanel from '../../../components/ConfirmPanel.vue'
import type { TimedWageRecord } from '../../../api/types'

definePageConfig({
  navigationBarTitleText: '计时工资',
})

function todayStr() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

function currentMonthStr() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  return `${d.getFullYear()}-${m}`
}

const filterMonth = ref(currentMonthStr())
const loading = ref(false)
const submitting = ref(false)
const list = ref<TimedWageRecord[]>([])
const panelVisible = ref(false)
const editingId = ref(0)

const form = reactive({
  employeeNo: '',
  workDate: todayStr(),
  hours: '',
  hourlyRate: '',
  workContent: '',
})

const calcAmount = computed(() => {
  const h = parseFloat(form.hours) || 0
  const r = parseFloat(form.hourlyRate) || 0
  return h * r
})

function onMonthChange(e: any) {
  filterMonth.value = e.detail.value
  loadList()
}

function openAdd() {
  editingId.value = 0
  form.employeeNo = ''
  form.workDate = todayStr()
  form.hours = ''
  form.hourlyRate = ''
  form.workContent = ''
  panelVisible.value = true
}

function openEdit(item: TimedWageRecord) {
  editingId.value = item.id
  form.employeeNo = item.employeeNo || ''
  form.workDate = formatDate(item.workDate) || todayStr()
  form.hours = String(item.hours)
  form.hourlyRate = String(item.hourlyRate)
  form.workContent = item.workContent || ''
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
    Taro.showToast({ title: '请输入工号', icon: 'none' })
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
    await loadList()
  } catch {
    // handled by request util
  } finally {
    submitting.value = false
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await getTimedWages(filterMonth.value)
    list.value = res || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
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
  display: flex;
  padding: 20px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.filter-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 20px;
  background: #f5f7fa;
  border-radius: 10px;
}

.filter-label {
  font-size: 24px;
  color: #9ca3af;
}

.filter-value {
  font-size: 26px;
  color: #4A6FA5;
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
</style>
