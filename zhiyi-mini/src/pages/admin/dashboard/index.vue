<template>
  <ScrollView class="page" scroll-y :refresher-enabled="true" :refresher-triggered="refreshing"
    @refresherrefresh="onRefresh">
    <!-- Tab switch -->
    <View class="tab-bar">
      <View :class="['tab-item', activeTab === 'order' && 'tab-active']" @tap="activeTab = 'order'">
        <Text class="tab-text">订单看板</Text>
      </View>
      <View :class="['tab-item', activeTab === 'worker' && 'tab-active']" @tap="activeTab = 'worker'">
        <Text class="tab-text">工人看板</Text>
      </View>
    </View>

    <!-- 订单看板 -->
    <View v-if="activeTab === 'order'" class="section">
      <View v-if="orderList.length === 0 && !loading" class="empty">
        <Text class="empty-text">暂无订单数据</Text>
      </View>
      <View v-for="item in orderList" :key="item.processName" class="card">
        <View class="card-header">
          <Text class="card-title">{{ item.processName }}</Text>
        </View>
        <View class="card-body">
          <View class="stat-row">
            <Text class="stat-label">已报工数</Text>
            <Text class="stat-value">{{ item.reportedCount }}</Text>
          </View>
          <View class="stat-row">
            <Text class="stat-label">计划数</Text>
            <Text class="stat-value">{{ item.planCount }}</Text>
          </View>
          <View class="progress-section">
            <View class="progress-header">
              <Text class="stat-label">完成率</Text>
              <Text class="stat-value progress-text">{{ getPercent(item) }}%</Text>
            </View>
            <View class="progress-bar">
              <View class="progress-fill" :style="{ width: getPercent(item) + '%' }"></View>
            </View>
          </View>
        </View>
      </View>
    </View>

    <!-- 工人看板 -->
    <View v-if="activeTab === 'worker'" class="section">
      <View v-if="workerList.length === 0 && !loading" class="empty">
        <Text class="empty-text">暂无工人数据</Text>
      </View>
      <View v-for="item in workerList" :key="item.workerName" class="card">
        <View class="card-header">
          <Text class="card-title">{{ item.workerName }}</Text>
        </View>
        <View class="card-body">
          <View class="stat-row">
            <Text class="stat-label">今日报工数</Text>
            <Text class="stat-value">{{ item.todayCount }}</Text>
          </View>
          <View class="stat-row">
            <Text class="stat-label">今日工资</Text>
            <Text class="stat-value highlight">¥{{ item.todayWage }}</Text>
          </View>
        </View>
      </View>
    </View>

    <!-- Loading -->
    <View v-if="loading" class="loading">
      <Text class="loading-text">加载中...</Text>
    </View>
  </ScrollView>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro from '@tarojs/taro'
import { ScrollView, View, Text } from '@tarojs/components'
import { getOrderDashboard, getWorkerDashboard } from '../../../api/admin'

definePageConfig({
  navigationBarTitleText: '生产看板',
})

interface OrderProcess {
  processName: string
  reportedCount: number
  planCount: number
}

interface WorkerStat {
  workerName: string
  todayCount: number
  todayWage: number
}

const activeTab = ref<'order' | 'worker'>('order')
const refreshing = ref(false)
const loading = ref(false)
const orderList = ref<OrderProcess[]>([])
const workerList = ref<WorkerStat[]>([])

function getPercent(item: OrderProcess): number {
  if (!item.planCount) return 0
  return Math.round((item.reportedCount / item.planCount) * 100)
}

async function loadOrderData() {
  try {
    const res = await getOrderDashboard()
    orderList.value = res.data || []
  } catch {
    orderList.value = []
  }
}

async function loadWorkerData() {
  try {
    const res = await getWorkerDashboard()
    workerList.value = res.data || []
  } catch {
    workerList.value = []
  }
}

async function loadAll() {
  loading.value = true
  await Promise.all([loadOrderData(), loadWorkerData()])
  loading.value = false
}

async function onRefresh() {
  refreshing.value = true
  await loadAll()
  refreshing.value = false
}

Taro.useDidShow(() => {
  loadAll()
})
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  background: #F5F7FA;
}

.tab-bar {
  display: flex;
  background: #fff;
  padding: 0 24px;
  border-bottom: 1px solid #eee;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 24px 0;
  position: relative;
}

.tab-active {
  .tab-text {
    color: #4A6FA5;
    font-weight: 600;
  }
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

.tab-text {
  font-size: 28px;
  color: #666;
}

.section {
  padding: 24px;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
}

.card-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.card-title {
  font-size: 30px;
  font-weight: 600;
  color: #333;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  font-size: 26px;
  color: #999;
}

.stat-value {
  font-size: 28px;
  color: #333;
  font-weight: 500;
}

.highlight {
  color: #4A6FA5;
  font-weight: 600;
}

.progress-section {
  margin-top: 4px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.progress-text {
  color: #4A6FA5;
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

.empty {
  text-align: center;
  padding: 80px 0;
}

.empty-text {
  font-size: 28px;
  color: #999;
}

.loading {
  text-align: center;
  padding: 40px 0;
}

.loading-text {
  font-size: 26px;
  color: #999;
}
</style>
