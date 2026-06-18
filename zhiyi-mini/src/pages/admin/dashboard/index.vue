<template>
  <View class="dashboard-page">
    <!-- Header -->
    <View class="page-header">
      <Text class="page-title">生产看板</Text>
    </View>

    <!-- Tab switch -->
    <View class="tab-bar">
      <View :class="['tab-item', activeTab === 'order' && 'tab-active']" @tap="switchTab('order')">
        <Text class="tab-text">订单看板</Text>
      </View>
      <View :class="['tab-item', activeTab === 'worker' && 'tab-active']" @tap="switchTab('worker')">
        <Text class="tab-text">工人看板</Text>
      </View>
    </View>

    <!-- Filters -->
    <View class="filter-bar">
      <Picker mode="date" :value="filterDate" @change="onDateChange">
        <View class="filter-item">
          <Text class="filter-label">日期</Text>
          <Text class="filter-value">{{ filterDate || '选择日期' }}</Text>
        </View>
      </Picker>
      <Picker v-if="activeTab === 'order'" mode="selector" :range="orderOptions" range-key="label" :value="orderPickerIndex" @change="onOrderChange">
        <View class="filter-item">
          <Text class="filter-label">订单</Text>
          <Text class="filter-value">{{ orderOptions[orderPickerIndex] ? orderOptions[orderPickerIndex].label : '全部' }}</Text>
        </View>
      </Picker>
    </View>

    <!-- Content -->
    <ScrollView scroll-y class="content" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
      <!-- Order tab -->
      <View v-if="activeTab === 'order'">
        <View v-if="loading" class="loading"><Text class="loading-text">加载中...</Text></View>
        <EmptyState v-else-if="orderList.length === 0" text="暂无订单数据" />
        <View v-else class="card-list">
          <View v-for="item in orderList" :key="item.orderId" class="card">
            <View class="card-head">
              <View class="card-head-left">
                <Text class="card-title">{{ item.orderNo }}</Text>
                <Text class="card-sub">{{ item.styleNo }}</Text>
              </View>
              <StatusTag :text="formatPercent(item.completionRate)" :type="completionType(item.completionRate)" />
            </View>

            <View class="card-stats">
              <View class="stat">
                <Text class="stat-label">总扎数</Text>
                <Text class="stat-value">{{ item.totalBundles }}</Text>
              </View>
              <View class="stat">
                <Text class="stat-label">完成率</Text>
                <Text class="stat-value">{{ formatPercent(item.completionRate) }}</Text>
              </View>
            </View>

            <View v-if="item.bottleneckProcess" class="bottleneck">
              <Text class="bottleneck-label">瓶颈工序</Text>
              <Text class="bottleneck-name">{{ item.bottleneckProcess }}</Text>
            </View>

            <!-- Progress bar -->
            <View class="progress-wrap">
              <View class="progress-bar">
                <View class="progress-fill" :style="{ width: progressWidth(item.completionRate) }"></View>
              </View>
            </View>

            <!-- Expand toggle -->
            <View class="expand-toggle" @tap="toggleExpand(item.orderId)">
              <Text class="expand-text">{{ expandedIds.includes(item.orderId) ? '收起工序' : '展开工序' }}</Text>
              <Text class="expand-icon">{{ expandedIds.includes(item.orderId) ? '▲' : '▼' }}</Text>
            </View>

            <!-- Process progress rows -->
            <View v-if="expandedIds.includes(item.orderId)" class="process-rows">
              <View v-for="p in item.processProgress" :key="p.processId" class="process-row">
                <Text class="process-name">{{ p.processName }}</Text>
                <View class="process-bar">
                  <View class="process-bar-fill" :style="{ width: progressWidth(p.rate) }"></View>
                </View>
                <Text class="process-stat">{{ p.completed }}/{{ p.total }}</Text>
              </View>
              <View v-if="!item.processProgress || item.processProgress.length === 0" class="process-empty">
                <Text class="process-empty-text">暂无工序进度</Text>
              </View>
            </View>
          </View>
        </View>
      </View>

      <!-- Worker tab -->
      <View v-else>
        <View v-if="loading" class="loading"><Text class="loading-text">加载中...</Text></View>
        <EmptyState v-else-if="workerList.length === 0" text="暂无工人数据" />
        <View v-else class="card-list">
          <View v-for="item in workerList" :key="item.employeeId" class="card">
            <View class="card-head">
              <View class="card-head-left">
                <Text class="card-title">{{ item.employeeName }}</Text>
                <Text class="card-sub">工号 {{ item.employeeNo }}</Text>
              </View>
            </View>
            <View class="card-stats">
              <View class="stat">
                <Text class="stat-label">总数量</Text>
                <Text class="stat-value">{{ item.totalQty }}</Text>
              </View>
              <View class="stat">
                <Text class="stat-label">总金额</Text>
                <Text class="stat-value highlight">{{ formatAmount(item.totalAmount) }}</Text>
              </View>
            </View>
          </View>
        </View>
      </View>
    </ScrollView>
  </View>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, ScrollView, Picker } from '@tarojs/components'
import { getDashboardOrders, getDashboardWorkers } from '../../../api/mobileAdmin'
import { formatAmount, formatPercent } from '../../../utils/format'
import { checkAuthAndRedirect } from '../../../utils/auth'
import EmptyState from '../../../components/EmptyState.vue'
import StatusTag from '../../../components/StatusTag.vue'
import type { DashboardOrder, DashboardWorker } from '../../../api/types'

definePageConfig({
  navigationBarTitleText: '生产看板',
})

const activeTab = ref<'order' | 'worker'>('order')
const filterDate = ref('')
const orderId = ref<number | undefined>(undefined)
const orderPickerIndex = ref(0)
const refreshing = ref(false)
const loading = ref(false)
const orderList = ref<DashboardOrder[]>([])
const workerList = ref<DashboardWorker[]>([])
const expandedIds = ref<number[]>([])

// Order options for filter dropdown
const orderOptions = computed(() => {
  const opts: Array<{ label: string; value: number | undefined }> = [{ label: '全部', value: undefined }]
  orderList.value.forEach(o => opts.push({ label: o.orderNo, value: o.orderId }))
  return opts
})

function todayStr() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

filterDate.value = todayStr()

function switchTab(tab: 'order' | 'worker') {
  if (activeTab.value === tab) return
  activeTab.value = tab
  loadData()
}

function onDateChange(e: any) {
  filterDate.value = e.detail.value
  loadData()
}

function onOrderChange(e: any) {
  orderPickerIndex.value = Number(e.detail.value)
  orderId.value = orderOptions.value[orderPickerIndex.value] ? orderOptions.value[orderPickerIndex.value].value : undefined
  loadOrders()
}

function toggleExpand(id: number) {
  const idx = expandedIds.value.indexOf(id)
  if (idx > -1) expandedIds.value.splice(idx, 1)
  else expandedIds.value.push(id)
}

function completionType(rate: number): 'success' | 'warning' | 'danger' | 'info' {
  if (rate >= 1) return 'success'
  if (rate >= 0.7) return 'info'
  if (rate >= 0.4) return 'warning'
  return 'danger'
}

function progressWidth(rate: number): string {
  return Math.min(100, Math.max(0, (rate || 0) * 100)) + '%'
}

async function loadOrders() {
  loading.value = true
  try {
    const res = await getDashboardOrders(filterDate.value, orderId.value)
    orderList.value = res || []
  } catch {
    orderList.value = []
  } finally {
    loading.value = false
  }
}

async function loadWorkers() {
  loading.value = true
  try {
    const res = await getDashboardWorkers(filterDate.value)
    workerList.value = res || []
  } catch {
    workerList.value = []
  } finally {
    loading.value = false
  }
}

async function loadData() {
  if (activeTab.value === 'order') await loadOrders()
  else await loadWorkers()
}

async function onRefresh() {
  refreshing.value = true
  await loadData()
  refreshing.value = false
}

useDidShow(() => {
  if (!checkAuthAndRedirect()) return
  loadData()
})
</script>

<style lang="scss">
.dashboard-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.page-header {
  background: #4A6FA5;
  padding: 32px 32px 28px;
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
}

.tab-bar {
  display: flex;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 24px 0;
  position: relative;
}

.tab-text {
  font-size: 28px;
  color: #6b7280;
}

.tab-active .tab-text {
  color: #4A6FA5;
  font-weight: 600;
}

.tab-active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48px;
  height: 4px;
  background: #4A6FA5;
  border-radius: 2px;
}

.filter-bar {
  display: flex;
  gap: 16px;
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

.card-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-label {
  font-size: 24px;
  color: #9ca3af;
}

.stat-value {
  font-size: 30px;
  font-weight: 600;
  color: #4A6FA5;
}

.stat-value.highlight {
  color: #4A6FA5;
}

.bottleneck {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fef2f2;
  border-radius: 8px;
}

.bottleneck-label {
  font-size: 24px;
  color: #991b1b;
}

.bottleneck-name {
  font-size: 26px;
  font-weight: 600;
  color: #dc2626;
}

.progress-wrap {
  margin-bottom: 12px;
}

.progress-bar {
  height: 12px;
  background: #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #4A6FA5;
  border-radius: 6px;
  transition: width 0.3s ease;
}

.expand-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 0 4px;
}

.expand-text {
  font-size: 24px;
  color: #4A6FA5;
}

.expand-icon {
  font-size: 20px;
  color: #4A6FA5;
}

.process-rows {
  margin-top: 12px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.process-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.process-name {
  width: 140px;
  font-size: 24px;
  color: #4b5563;
  flex-shrink: 0;
}

.process-bar {
  flex: 1;
  height: 10px;
  background: #f0f0f0;
  border-radius: 5px;
  overflow: hidden;
}

.process-bar-fill {
  height: 100%;
  background: #4A6FA5;
  border-radius: 5px;
}

.process-stat {
  width: 90px;
  text-align: right;
  font-size: 24px;
  color: #6b7280;
  flex-shrink: 0;
}

.process-empty {
  padding: 16px 0;
  text-align: center;
}

.process-empty-text {
  font-size: 24px;
  color: #9ca3af;
}
</style>
