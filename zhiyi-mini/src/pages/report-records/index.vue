<template>
  <View class="records-page">
    <!-- Header with gradient -->
    <View class="records-header">
      <Text class="header-title">报工记录</Text>
    </View>

    <!-- Date range filter -->
    <View class="filter-bar">
      <View class="quick-btns">
        <View class="quick-btn" :class="{ active: filterType === '7d' }" @tap="setFilter7d">
          <Text class="quick-btn-text">近7天</Text>
        </View>
        <View class="quick-btn" :class="{ active: filterType === 'month' }" @tap="setFilterMonth">
          <Text class="quick-btn-text">本月</Text>
        </View>
        <View class="quick-btn" :class="{ active: filterType === 'custom' }" @tap="showCustomRange">
          <Text class="quick-btn-text">自定义</Text>
        </View>
      </View>
      <View class="date-range-text" v-if="startDate || endDate">
        <Text class="range-label">{{ startDate || '开始' }} ~ {{ endDate || '结束' }}</Text>
      </View>
    </View>

    <!-- Custom date picker (shown inline) -->
    <View class="custom-picker" v-if="pickerVisible">
      <View class="picker-row">
        <Text class="picker-label">开始日期</Text>
        <Picker mode="date" :value="startDate" @change="onStartDateChange">
          <View class="picker-value">
            <Text class="picker-value-text">{{ startDate || '选择日期' }}</Text>
          </View>
        </Picker>
      </View>
      <View class="picker-row">
        <Text class="picker-label">结束日期</Text>
        <Picker mode="date" :value="endDate" @change="onEndDateChange">
          <View class="picker-value">
            <Text class="picker-value-text">{{ endDate || '选择日期' }}</Text>
          </View>
        </Picker>
      </View>
      <View class="picker-actions">
        <View class="picker-btn cancel" @tap="pickerVisible = false">
          <Text class="picker-btn-text">取消</Text>
        </View>
        <View class="picker-btn confirm" @tap="applyCustomFilter">
          <Text class="picker-btn-text confirm-text">确定</Text>
        </View>
      </View>
    </View>

    <!-- Loading -->
    <View class="loading-wrap" v-if="loading">
      <Text class="loading-text">加载中...</Text>
    </View>

    <!-- Empty state -->
    <EmptyState v-else-if="records.length === 0" text="暂无报工记录" />

    <!-- Record list -->
    <ScrollView v-else scroll-y class="record-list" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
      <View v-for="record in records" :key="record.id" class="record-card">
        <View class="record-top">
          <Text class="record-time">{{ formatDateTime(record.reportTime) }}</Text>
          <Text class="record-amount">{{ formatAmount(record.amount) }}</Text>
        </View>
        <View class="record-info">
          <View class="info-row">
            <Text class="info-label">订单号</Text>
            <Text class="info-value">{{ record.orderNo }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">款号</Text>
            <Text class="info-value">{{ record.styleNo }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">扎号</Text>
            <Text class="info-value">{{ record.bundleNo }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">工序</Text>
            <Text class="info-value">{{ record.processName }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">计件</Text>
            <Text class="info-value">{{ record.qty }} x {{ formatAmount(record.price).replace('¥', '') }}</Text>
          </View>
        </View>
      </View>
    </ScrollView>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'
import { getReportRecords } from '../../api/mobileReport'
import { formatDateTime, formatAmount } from '../../utils/format'
import EmptyState from '../../components/EmptyState.vue'
import type { WorkReportRecord } from '../../api/types'

definePageConfig({
  navigationBarTitleText: '报工记录',
})

const loading = ref(false)
const refreshing = ref(false)
const records = ref<WorkReportRecord[]>([])
const filterType = ref<'7d' | 'month' | 'custom'>('7d')
const startDate = ref('')
const endDate = ref('')
const pickerVisible = ref(false)

function formatDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function setFilter7d() {
  filterType.value = '7d'
  pickerVisible.value = false
  const now = new Date()
  const start = new Date()
  start.setDate(now.getDate() - 6)
  startDate.value = formatDateStr(start)
  endDate.value = formatDateStr(now)
  fetchRecords()
}

function setFilterMonth() {
  filterType.value = 'month'
  pickerVisible.value = false
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()
  startDate.value = formatDateStr(new Date(y, m, 1))
  endDate.value = formatDateStr(now)
  fetchRecords()
}

function showCustomRange() {
  filterType.value = 'custom'
  pickerVisible.value = true
}

function onStartDateChange(e: any) {
  startDate.value = e.detail.value
}

function onEndDateChange(e: any) {
  endDate.value = e.detail.value
}

function applyCustomFilter() {
  pickerVisible.value = false
  fetchRecords()
}

async function fetchRecords() {
  const token = Taro.getStorageSync('satoken')
  if (!token) {
    Taro.redirectTo({ url: '/pages/login/index' })
    return
  }

  loading.value = true
  try {
    const res = await getReportRecords(
      startDate.value || undefined,
      endDate.value || undefined
    )
    records.value = res || []
  } catch {
    // handled by request util
  } finally {
    loading.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  try {
    const res = await getReportRecords(
      startDate.value || undefined,
      endDate.value || undefined
    )
    records.value = res || []
  } catch {
    // handled by request util
  } finally {
    refreshing.value = false
  }
}

useDidShow(() => {
  // Default: last 7 days
  const now = new Date()
  const start = new Date()
  start.setDate(now.getDate() - 6)
  startDate.value = formatDateStr(start)
  endDate.value = formatDateStr(now)
  fetchRecords()
})
</script>

<style lang="scss">
.records-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.records-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 32px 32px;
}

.header-title {
  font-size: 36px;
  font-weight: 700;
  color: #ffffff;
}

.filter-bar {
  background: #ffffff;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.quick-btns {
  display: flex;
  gap: 16px;
}

.quick-btn {
  padding: 12px 28px;
  border-radius: 8px;
  background: #f3f4f6;
}

.quick-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.quick-btn-text {
  font-size: 26px;
  color: #6b7280;
  font-weight: 500;
}

.quick-btn.active .quick-btn-text {
  color: #ffffff;
}

.date-range-text {
  margin-top: 16px;
}

.range-label {
  font-size: 24px;
  color: #9ca3af;
}

.custom-picker {
  background: #ffffff;
  padding: 24px;
  border-top: 1px solid #f3f4f6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.picker-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}

.picker-label {
  font-size: 28px;
  color: #374151;
  font-weight: 500;
}

.picker-value {
  padding: 12px 24px;
  background: #f3f4f6;
  border-radius: 8px;
}

.picker-value-text {
  font-size: 28px;
  color: #667eea;
}

.picker-actions {
  display: flex;
  gap: 16px;
  margin-top: 16px;
}

.picker-btn {
  flex: 1;
  padding: 16px 0;
  text-align: center;
  border-radius: 8px;
}

.picker-btn.cancel {
  background: #f3f4f6;
}

.picker-btn.confirm {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.picker-btn-text {
  font-size: 28px;
  color: #6b7280;
  font-weight: 500;
}

.confirm-text {
  color: #ffffff;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.loading-text {
  font-size: 28px;
  color: #9ca3af;
}

.record-list {
  flex: 1;
  padding: 24px 24px 40px;
}

.record-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 20px;
}

.record-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.record-time {
  font-size: 24px;
  color: #9ca3af;
}

.record-amount {
  font-size: 32px;
  font-weight: 700;
  color: #667eea;
}

.record-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: 26px;
  color: #9ca3af;
  flex-shrink: 0;
  width: 100px;
}

.info-value {
  font-size: 26px;
  color: #1a1a2e;
  font-weight: 500;
  text-align: right;
  flex: 1;
}
</style>
