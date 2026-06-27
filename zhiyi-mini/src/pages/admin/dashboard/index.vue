<template>
  <View class="dashboard-page">
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
      <View class="filter-row">
        <Picker mode="date" :value="startDate" @change="onStartDateChange">
          <View class="filter-item">
            <Text class="filter-value">{{ startDate || '开始日期' }}</Text>
          </View>
        </Picker>
        <Text class="filter-sep">~</Text>
        <Picker mode="date" :value="endDate" @change="onEndDateChange">
          <View class="filter-item">
            <Text class="filter-value">{{ endDate || '结束日期' }}</Text>
          </View>
        </Picker>
      </View>
      <View class="filter-row">
        <View class="search-wrap">
          <Input
            class="search-input"
            :placeholder="activeTab === 'order' ? '搜索订单号' : '搜索工号/姓名'"
            :value="keyword"
            @input="onKeywordInput"
            @confirm="onSearch"
          />
          <View class="search-btn" @tap="onSearch"><Text class="search-btn-text">搜索</Text></View>
        </View>
      </View>
    </View>

    <!-- Content -->
    <View class="content">
      <!-- Order tab -->
      <View v-if="activeTab === 'order'">
        <View v-if="loading && orderList.length === 0" class="loading"><Text class="loading-text">加载中...</Text></View>
        <EmptyState v-else-if="!loading && orderList.length === 0" text="暂无订单数据" />
        <View v-else class="card-list">
          <View v-for="item in orderList" :key="item.orderId" class="card">
            <View class="card-head">
              <View class="card-head-left">
                <Text class="card-title">{{ item.orderNo }}</Text>
                <Text class="card-sub">{{ item.styleNo }} · {{ item.orderDate || '-' }}</Text>
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

            <View class="progress-wrap">
              <View class="progress-bar">
                <View class="progress-fill" :style="{ width: progressWidth(item.completionRate) }"></View>
              </View>
            </View>

            <View class="expand-toggle" @tap="toggleExpand(item.orderId)">
              <Text class="expand-text">{{ expandedIds.includes(item.orderId) ? '收起工序' : '展开工序' }}</Text>
              <Text class="expand-icon">{{ expandedIds.includes(item.orderId) ? '▲' : '▼' }}</Text>
            </View>

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

          <View class="load-more" v-if="orderList.length > 0">
            <Text class="load-more-text">{{ loading ? '加载中...' : orderHasMore ? '上拉加载更多' : '没有更多了' }}</Text>
          </View>
        </View>
      </View>

      <!-- Worker tab -->
      <View v-else>
        <View v-if="workerSummary" class="summary-cards">
          <View class="summary-card">
            <Text class="card-label">总数量</Text>
            <Text class="card-value">{{ workerSummary.totalQty }}</Text>
            <Text class="card-unit">件</Text>
          </View>
          <View class="summary-card">
            <Text class="card-label">总金额</Text>
            <Text class="card-value">{{ formatAmount(workerSummary.totalAmount) }}</Text>
          </View>
        </View>

        <View v-if="loading && !workerSummary" class="loading"><Text class="loading-text">加载中...</Text></View>
        <EmptyState v-else-if="!loading && (!workerSummary || workerList.length === 0)" text="暂无工人数据" />
        <View v-else class="card-list">
          <View v-for="item in workerList" :key="item.employeeId" class="card" @tap="goWorkerDetail(item.employeeId)">
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

          <View class="load-more" v-if="workerList.length > 0">
            <Text class="load-more-text">{{ loading ? '加载中...' : workerHasMore ? '上拉加载更多' : '没有更多了' }}</Text>
          </View>
        </View>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro, { useDidShow, useReachBottom } from '@tarojs/taro'
import { View, Text, Picker, Input } from '@tarojs/components'
import { getDashboardOrders, getDashboardWorkers } from '../../../api/mobileAdmin'
import { formatAmount, formatPercent } from '../../../utils/format'
import { checkAuthAndRedirect } from '../../../utils/auth'
import EmptyState from '../../../components/EmptyState.vue'
import StatusTag from '../../../components/StatusTag.vue'
import type { DashboardOrder, DashboardWorker, DashboardWorkerPage } from '../../../api/types'

definePageConfig({
  navigationBarTitleText: '生产看板',
})

const activeTab = ref<'order' | 'worker'>('order')
const startDate = ref('')
const endDate = ref('')
const keyword = ref('')
const loading = ref(false)

// 订单看板
const orderList = ref<DashboardOrder[]>([])
const orderPage = ref(1)
const orderHasMore = ref(true)
const expandedIds = ref<number[]>([])

// 工人看板
const workerList = ref<DashboardWorker[]>([])
const workerSummary = ref<DashboardWorkerPage | null>(null)
const workerPage = ref(1)
const workerHasMore = ref(true)

const pageSize = 20

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

startDate.value = monthStartStr()
endDate.value = todayStr()

function switchTab(tab: 'order' | 'worker') {
  if (activeTab.value === tab) return
  activeTab.value = tab
  keyword.value = ''
  loadData()
}

function onStartDateChange(e: any) {
  startDate.value = e.detail.value
  loadData()
}

function onEndDateChange(e: any) {
  endDate.value = e.detail.value
  loadData()
}

function onKeywordInput(e: any) {
  keyword.value = e.detail.value
}

function onSearch() {
  loadData()
}

function goWorkerDetail(employeeId: number) {
  Taro.navigateTo({
    url: `/pages/admin/worker-detail/index?employeeId=${employeeId}&startDate=${startDate.value}&endDate=${endDate.value}`
  })
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

async function loadOrders(reset = false) {
  if (reset) {
    orderPage.value = 1
    orderHasMore.value = true
    orderList.value = []
  }
  if (!orderHasMore.value || loading.value) return

  loading.value = true
  try {
    const res = await getDashboardOrders(
      startDate.value || undefined,
      endDate.value || undefined,
      keyword.value.trim() || undefined,
      orderPage.value,
      pageSize
    )
    const newRecords = res?.records || []
    orderList.value = reset ? newRecords : [...orderList.value, ...newRecords]
    orderHasMore.value = res?.hasMore ?? false
  } catch {
    orderList.value = []
  } finally {
    loading.value = false
  }
}

async function loadWorkers(reset = false) {
  if (reset) {
    workerPage.value = 1
    workerHasMore.value = true
    workerList.value = []
    workerSummary.value = null
  }
  if (!workerHasMore.value || loading.value) return

  loading.value = true
  try {
    const res = await getDashboardWorkers(
      startDate.value || undefined,
      endDate.value || undefined,
      keyword.value.trim() || undefined,
      workerPage.value,
      pageSize
    )
    if (res) {
      const newWorkers = res.workers || []
      workerList.value = reset ? newWorkers : [...workerList.value, ...newWorkers]
      workerHasMore.value = res.hasMore ?? false
      // 汇总始终用首次返回的
      if (reset || !workerSummary.value) {
        workerSummary.value = res
      }
    }
  } catch {
    workerList.value = []
    workerSummary.value = null
  } finally {
    loading.value = false
  }
}

async function loadData() {
  if (activeTab.value === 'order') await loadOrders(true)
  else await loadWorkers(true)
}

useReachBottom(() => {
  if (loading.value) return
  if (activeTab.value === 'order') {
    if (orderHasMore.value) {
      orderPage.value++
      loadOrders(false)
    }
  } else {
    if (workerHasMore.value) {
      workerPage.value++
      loadWorkers(false)
    }
  }
})

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
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-item {
  flex: 1;
  height: 64px;
  padding: 0 20px;
  background: #f5f7fa;
  border-radius: 10px;
  display: flex;
  align-items: center;
}

.filter-value {
  font-size: 26px;
  color: #4A6FA5;
  font-weight: 500;
}

.filter-sep {
  font-size: 24px;
  color: #9ca3af;
}

.search-wrap {
  flex: 1;
  display: flex;
  gap: 12px;
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

.summary-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.summary-card {
  flex: 1;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-label {
  font-size: 24px;
  color: #9ca3af;
}

.card-value {
  font-size: 36px;
  font-weight: 700;
  color: #4A6FA5;
}

.card-unit {
  font-size: 24px;
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

.load-more {
  text-align: center;
  padding: 24px 0 40px;
}

.load-more-text {
  font-size: 24px;
  color: #9ca3af;
}
</style>
