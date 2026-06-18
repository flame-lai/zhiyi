<template>
  <View class="anomaly-page">
    <!-- Header with gradient -->
    <View class="page-header">
      <Text class="page-title">异常预警</Text>
    </View>

    <!-- Status filter tabs -->
    <View class="status-tabs">
      <View
        v-for="t in statusTabs"
        :key="t.value"
        :class="['status-tab', activeStatus === t.value && 'status-tab-active']"
        @tap="switchStatus(t.value)"
      >
        <Text class="status-tab-text">{{ t.label }}</Text>
      </View>
    </View>

    <ScrollView scroll-y class="content" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
      <View v-if="loading" class="loading"><Text class="loading-text">加载中...</Text></View>
      <EmptyState v-else-if="list.length === 0" text="暂无异常记录" />
      <View v-else class="card-list">
        <View v-for="item in list" :key="item.id" class="card" @tap="goDetail(item.id)">
          <View class="card-head">
            <Text class="card-bundle">{{ item.bundleNo }}</Text>
            <StatusTag :text="statusText(item.status)" :type="statusType(item.status)" />
          </View>

          <View class="info-row">
            <Text class="info-label">订单号</Text>
            <Text class="info-value">{{ item.orderNo }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">描述</Text>
            <Text class="info-value desc">{{ item.description }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">上报人</Text>
            <Text class="info-value">{{ item.reportEmployeeName }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">上报时间</Text>
            <Text class="info-value">{{ formatDateTime(item.reportTime) }}</Text>
          </View>
        </View>
      </View>
    </ScrollView>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, ScrollView } from '@tarojs/components'
import { getAnomalies } from '../../../api/mobileAdmin'
import { formatDateTime } from '../../../utils/format'
import { checkAuthAndRedirect } from '../../../utils/auth'
import EmptyState from '../../../components/EmptyState.vue'
import StatusTag from '../../../components/StatusTag.vue'
import type { AnomalyRecord } from '../../../api/types'

definePageConfig({
  navigationBarTitleText: '异常预警',
})

const statusTabs = [
  { label: '全部', value: -1 },
  { label: '待处理', value: 0 },
  { label: '已处理', value: 1 },
]

const activeStatus = ref(-1)
const refreshing = ref(false)
const loading = ref(false)
const list = ref<AnomalyRecord[]>([])

function switchStatus(v: number) {
  if (activeStatus.value === v) return
  activeStatus.value = v
  loadList()
}

function statusText(status: number): string {
  return status === 0 ? '待处理' : '已处理'
}

function statusType(status: number): 'warning' | 'success' {
  return status === 0 ? 'warning' : 'success'
}

function goDetail(id: number) {
  Taro.navigateTo({ url: `/pages/admin/anomaly-detail/index?id=${id}` })
}

async function loadList() {
  loading.value = true
  try {
    const params = activeStatus.value === -1 ? undefined : activeStatus.value
    const res = await getAnomalies(params)
    list.value = res || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  await loadList()
  refreshing.value = false
}

useDidShow(() => {
  if (!checkAuthAndRedirect()) return
  loadList()
})
</script>

<style lang="scss">
.anomaly-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 32px 32px 28px;
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
}

.status-tabs {
  display: flex;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.status-tab {
  flex: 1;
  text-align: center;
  padding: 24px 0;
  position: relative;
}

.status-tab-text {
  font-size: 28px;
  color: #6b7280;
}

.status-tab-active .status-tab-text {
  color: #667eea;
  font-weight: 600;
}

.status-tab-active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48px;
  height: 4px;
  background: #667eea;
  border-radius: 2px;
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
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.card-bundle {
  font-size: 30px;
  font-weight: 600;
  color: #1a1a2e;
}

.info-row {
  display: flex;
  margin-bottom: 12px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-label {
  width: 140px;
  font-size: 26px;
  color: #9ca3af;
  flex-shrink: 0;
}

.info-value {
  flex: 1;
  font-size: 26px;
  color: #1a1a2e;
}

.info-value.desc {
  color: #4b5563;
}
</style>
