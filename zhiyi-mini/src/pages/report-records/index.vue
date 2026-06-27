<template>
  <View class="records-page">
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

    <!-- Loading (首次加载) -->
    <View class="loading-wrap" v-if="loading && records.length === 0">
      <Text class="loading-text">加载中...</Text>
    </View>

    <!-- Empty state -->
    <EmptyState v-else-if="!loading && records.length === 0" text="暂无报工记录" />

    <!-- Record list -->
    <View v-else class="record-list">
      <View v-for="record in records" :key="record.id" class="record-card">
        <View class="record-top">
          <Text class="record-time">{{ formatDateTime(record.reportTime) }}</Text>
          <Text class="record-amount">{{ record.showMobilePrice !== 0 ? formatAmount(record.amount) : '-' }}</Text>
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
            <Text class="info-value">{{ record.qty }} x {{ record.showMobilePrice !== 0 ? formatAmount(record.price).replace('¥', '') : '-' }}</Text>
          </View>
        </View>
      </View>

      <!-- 加载更多提示 -->
      <View class="load-more" v-if="records.length > 0">
        <Text class="load-more-text">{{ loading ? '加载中...' : hasMore ? '上拉加载更多' : '没有更多了' }}</Text>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro, { useDidShow, useReachBottom } from '@tarojs/taro'
import { getReportRecords } from '../../api/mobileReport'
import { formatDateTime, formatAmount } from '../../utils/format'
import EmptyState from '../../components/EmptyState.vue'
import type { WorkReportRecord } from '../../api/types'

definePageConfig({
  navigationBarTitleText: '报工记录',
})

const loading = ref(false)
const records = ref<WorkReportRecord[]>([])
const page = ref(1)
const hasMore = ref(true)
const pageSize = 20
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
  fetchRecords(true)
}

function setFilterMonth() {
  filterType.value = 'month'
  pickerVisible.value = false
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()
  startDate.value = formatDateStr(new Date(y, m, 1))
  endDate.value = formatDateStr(now)
  fetchRecords(true)
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
  fetchRecords(true)
}

async function fetchRecords(reset = false) {
  const token = Taro.getStorageSync('satoken')
  if (!token) {
    Taro.redirectTo({ url: '/pages/login/index' })
    return
  }

  if (reset) {
    page.value = 1
    hasMore.value = true
    records.value = []
  }

  if (!hasMore.value || loading.value) return

  loading.value = true
  try {
    const res = await getReportRecords(
      startDate.value || undefined,
      endDate.value || undefined,
      page.value,
      pageSize
    )
    const newRecords = res?.records || []
    records.value = reset ? newRecords : [...records.value, ...newRecords]
    hasMore.value = res?.hasMore ?? false
  } catch {
    // handled by request util
  } finally {
    loading.value = false
  }
}

// 触底加载更多
useReachBottom(() => {
  if (hasMore.value && !loading.value) {
    page.value++
    fetchRecords(false)
  }
})

useDidShow(() => {
  // Default: last 7 days
  const now = new Date()
  const start = new Date()
  start.setDate(now.getDate() - 6)
  startDate.value = formatDateStr(start)
  endDate.value = formatDateStr(now)
  fetchRecords(true)
})
</script>

<style lang="scss">
.records-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
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
  background: #4A6FA5;
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
  color: #4A6FA5;
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
  background: #4A6FA5;
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
  color: #4A6FA5;
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
  color: #4A6FA5;
  font-weight: 500;
  text-align: right;
  flex: 1;
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
