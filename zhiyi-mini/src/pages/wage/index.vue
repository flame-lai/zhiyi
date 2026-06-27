<template>
  <View class="wage-page">
    <!-- User Info Header -->
    <View class="wage-header" @tap="goProfile">
      <View class="user-avatar">
        <Text class="avatar-text">{{ (userInfo?.employee?.name || '用')[0] }}</Text>
      </View>
      <View class="user-info">
        <Text class="user-name">{{ userInfo?.employee?.name || '未登录' }}</Text>
        <Text class="user-code">工号：{{ userInfo?.employee?.code || '-' }}</Text>
      </View>
      <Text class="header-arrow">›</Text>
    </View>

    <!-- Date Filter -->
    <View class="filter-bar">
      <View class="quick-btns">
        <View class="quick-btn" :class="{ active: filterType === 'today' }" @tap="setFilterToday">
          <Text class="quick-btn-text">今日</Text>
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

    <!-- Custom date picker -->
    <View class="custom-picker" v-if="pickerVisible">
      <View class="picker-row">
        <Text class="picker-label">开始日期</Text>
        <Picker mode="date" :value="startDate" @change="(e: any) => (startDate = e.detail.value)">
          <View class="picker-value">
            <Text class="picker-value-text">{{ startDate || '选择日期' }}</Text>
          </View>
        </Picker>
      </View>
      <View class="picker-row">
        <Text class="picker-label">结束日期</Text>
        <Picker mode="date" :value="endDate" @change="(e: any) => (endDate = e.detail.value)">
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
    <View v-else-if="!summary || !summary.dailyGroups || summary.dailyGroups.length === 0" class="empty-wrap">
      <Text class="empty-icon">📊</Text>
      <Text class="empty-title">暂无工资记录</Text>
      <Text class="empty-desc">完成报工后即可查看收入明细</Text>
    </View>

    <View v-else class="content">
      <!-- Summary Cards -->
      <View class="summary-cards">
        <View class="summary-card">
          <Text class="card-label">总数量</Text>
          <Text class="card-value">{{ summary.totalQty }}</Text>
          <Text class="card-unit">件</Text>
        </View>
        <View class="summary-card">
          <Text class="card-label">总收入</Text>
          <Text class="card-value">{{ formatAmount(summary.visibleAmount) }}</Text>
        </View>
      </View>

      <!-- Disclaimer -->
      <View v-if="hasHiddenPrice" class="disclaimer">
        <Text class="disclaimer-text">* 部分款号工价未公开，合计仅含已公开部分</Text>
      </View>

      <!-- Style Summaries -->
      <View class="style-section">
        <Text class="section-title">按款号汇总</Text>
        <View class="style-list">
          <View v-for="s in summary.styleSummaries" :key="s.styleNo" class="style-item">
            <View class="style-head">
              <Text class="style-no">{{ s.styleNo }}</Text>
              <View class="style-right">
                <Text class="style-qty">{{ s.qty }}件</Text>
                <Text class="style-amount">{{ s.showMobilePrice === 1 ? formatAmount(s.amount) : '-' }}</Text>
              </View>
            </View>
            <!-- 工序明细 -->
            <View class="process-list" v-if="s.processes && s.processes.length > 0">
              <View v-for="p in s.processes" :key="p.processName" class="process-row">
                <Text class="process-name">{{ p.processName }}</Text>
                <Text class="process-qty">{{ p.qty }}件</Text>
                <Text class="process-price">{{ s.showMobilePrice === 1 ? ('×' + formatAmount(p.price).replace('¥', '')) : '' }}</Text>
                <Text class="process-amount">{{ s.showMobilePrice === 1 ? formatAmount(p.amount) : '-' }}</Text>
              </View>
            </View>
          </View>
        </View>
      </View>

    </View>
  </View>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Picker } from '@tarojs/components'
import { getWageSummary } from '../../api/mobileReport'
import { formatAmount } from '../../utils/format'
import { getUserInfo } from '../../utils/auth'
import type { WageSummary } from '../../api/types'

definePageConfig({ navigationBarTitleText: '我的' })

const loading = ref(false)
const summary = ref<WageSummary | null>(null)
const userInfo = ref<any>(null)
const filterType = ref<'today' | 'month' | 'custom'>('today')
const startDate = ref('')
const endDate = ref('')
const pickerVisible = ref(false)

function todayStr() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

function monthStartStr() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  return `${d.getFullYear()}-${m}-01`
}

const hasHiddenPrice = computed(() => {
  if (!summary.value?.styleSummaries) return false
  return summary.value.styleSummaries.some(s => s.showMobilePrice === 0)
})

function goProfile() {
  Taro.navigateTo({ url: '/pages/profile/index' })
}

function setFilterToday() {
  filterType.value = 'today'
  pickerVisible.value = false
  startDate.value = todayStr()
  endDate.value = todayStr()
  fetchWage()
}

function setFilterMonth() {
  filterType.value = 'month'
  pickerVisible.value = false
  startDate.value = monthStartStr()
  endDate.value = todayStr()
  fetchWage()
}

function showCustomRange() {
  filterType.value = 'custom'
  pickerVisible.value = true
}

function applyCustomFilter() {
  if (!startDate.value || !endDate.value) {
    Taro.showToast({ title: '请选择日期范围', icon: 'none' })
    return
  }
  pickerVisible.value = false
  fetchWage()
}

async function fetchWage() {
  const token = Taro.getStorageSync('satoken')
  if (!token) {
    Taro.redirectTo({ url: '/pages/login/index' })
    return
  }

  userInfo.value = getUserInfo()
  loading.value = true
  try {
    const res = await getWageSummary(
      startDate.value || undefined,
      endDate.value || undefined
    )
    summary.value = res
  } catch {
    // handled by request util
  } finally {
    loading.value = false
  }
}

useDidShow(() => {
  setFilterToday()
})
</script>

<style lang="scss">
.wage-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.wage-header {
  background: #4A6FA5;
  padding: 48px 32px 40px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.user-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-text {
  font-size: 44px;
  font-weight: 700;
  color: #ffffff;
}

.user-info {
  flex: 1;
}

.user-name {
  display: block;
  font-size: 36px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 8px;
}

.user-code {
  display: block;
  font-size: 26px;
  color: rgba(255, 255, 255, 0.8);
}

.header-arrow {
  font-size: 40px;
  color: rgba(255, 255, 255, 0.7);
  margin-left: auto;
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
  margin-top: 12px;
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

.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100px 48px 80px;
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 24px;
}

.empty-title {
  font-size: 32px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.empty-desc {
  font-size: 26px;
  color: #9ca3af;
  text-align: center;
}

.content {
  padding: 24px;
  padding-bottom: 40px;
}

.summary-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.summary-card {
  flex: 1;
  background: #ffffff;
  border-radius: 16px;
  padding: 28px 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.card-label {
  font-size: 26px;
  color: #6b7280;
  margin-bottom: 12px;
}

.card-value {
  font-size: 40px;
  font-weight: 700;
  color: #4A6FA5;
}

.card-unit {
  font-size: 24px;
  color: #9ca3af;
  margin-top: 4px;
}

.disclaimer {
  background: #fffbeb;
  border: 2px solid #fef3c7;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 24px;
}

.disclaimer-text {
  font-size: 24px;
  color: #92400e;
}

.style-section {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.section-title {
  display: block;
  font-size: 28px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
}

.style-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.style-item {
  padding: 16px;
  background: #f9fafb;
  border-radius: 10px;
}

.style-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.style-no {
  font-size: 28px;
  font-weight: 600;
  color: #4A6FA5;
}

.style-qty {
  font-size: 26px;
  color: #6b7280;
}

.style-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.style-qty {
  font-size: 26px;
  color: #6b7280;
}

.style-amount {
  font-size: 30px;
  font-weight: 700;
  color: #4A6FA5;
}

.process-list {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.process-row {
  display: flex;
  align-items: center;
  padding: 8px 0;
}

.process-name {
  flex: 1;
  font-size: 26px;
  color: #6b7280;
}

.process-qty {
  width: 100px;
  text-align: right;
  font-size: 26px;
  color: #6b7280;
}

.process-price {
  width: 120px;
  text-align: right;
  font-size: 24px;
  color: #9ca3af;
}

.process-amount {
  width: 120px;
  text-align: right;
  font-size: 26px;
  font-weight: 500;
  color: #4A6FA5;
}

</style>
