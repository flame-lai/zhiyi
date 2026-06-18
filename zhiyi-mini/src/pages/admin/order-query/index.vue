<template>
  <View class="order-page">
    <!-- Header with gradient -->
    <View class="page-header">
      <Text class="page-title">订单查询</Text>
    </View>

    <!-- Search bar -->
    <View class="search-bar">
      <Input
        class="search-input"
        placeholder="请输入订单号"
        :value="keyword"
        @input="(e: any) => (keyword = e.detail.value)"
        @confirm="onSearch"
      />
      <View class="search-btn" @tap="onSearch">
        <Text class="search-btn-text">搜索</Text>
      </View>
    </View>

    <ScrollView scroll-y class="content">
      <View v-if="loading" class="loading"><Text class="loading-text">查询中...</Text></View>
      <EmptyState v-else-if="!result" text="请输入订单号查询" />
      <View v-else class="result-wrap">
        <!-- Order info card -->
        <View class="card">
          <View class="card-head">
            <Text class="card-title">{{ result.orderNo }}</Text>
            <StatusTag :text="orderStatusText(result.status)" :type="orderStatusType(result.status)" />
          </View>

          <View class="info-row">
            <Text class="info-label">客户</Text>
            <Text class="info-value">{{ result.customerName || '-' }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">总金额</Text>
            <Text class="info-value highlight">{{ formatAmount(result.totalAmount) }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">交货日期</Text>
            <Text class="info-value">{{ formatDate(result.deliveryDate) }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">状态</Text>
            <Text class="info-value">{{ orderStatusText(result.status) }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">完成率</Text>
            <Text class="info-value">{{ formatPercent(result.completionRate) }}</Text>
          </View>

          <!-- Overall completion progress bar -->
          <View class="progress-section">
            <View class="progress-bar">
              <View class="progress-fill" :style="{ width: progressWidth(result.completionRate) }"></View>
            </View>
          </View>
        </View>

        <!-- Item list -->
        <View class="card">
          <Text class="section-title">订单明细</Text>
          <View v-if="!result.items || result.items.length === 0" class="process-empty">
            <Text class="process-empty-text">暂无明细</Text>
          </View>
          <View v-else class="item-list">
            <View v-for="(item, idx) in result.items" :key="idx" class="item-row">
              <View class="item-head">
                <Text class="item-style">{{ item.styleNo }}</Text>
                <Text class="item-progress">{{ formatPercent(item.progress) }}</Text>
              </View>
              <View class="item-detail">
                <Text class="item-attr">{{ item.color }} / {{ item.size }}</Text>
                <Text class="item-qty">计划 {{ item.qty }} · 已发 {{ item.shippedQty }}</Text>
              </View>
              <View class="item-bar">
                <View class="item-bar-fill" :style="{ width: progressWidth(item.progress) }"></View>
              </View>
            </View>
          </View>
        </View>
      </View>
    </ScrollView>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Input, ScrollView } from '@tarojs/components'
import { searchOrder } from '../../../api/mobileAdmin'
import { formatAmount, formatDate, formatPercent } from '../../../utils/format'
import { checkAuthAndRedirect } from '../../../utils/auth'
import EmptyState from '../../../components/EmptyState.vue'
import StatusTag from '../../../components/StatusTag.vue'
import type { OrderSearchResult } from '../../../api/types'

definePageConfig({
  navigationBarTitleText: '订单查询',
})

const keyword = ref('')
const loading = ref(false)
const result = ref<OrderSearchResult | null>(null)

function orderStatusText(status: number): string {
  switch (status) {
    case 0: return '待确认'
    case 1: return '生产中'
    case 2: return '已完成'
    case 3: return '已取消'
    default: return String(status)
  }
}

function orderStatusType(status: number): 'default' | 'info' | 'success' | 'warning' | 'danger' {
  switch (status) {
    case 0: return 'warning'
    case 1: return 'info'
    case 2: return 'success'
    case 3: return 'danger'
    default: return 'default'
  }
}

function progressWidth(rate: number): string {
  return Math.min(100, Math.max(0, (rate || 0) * 100)) + '%'
}

async function onSearch() {
  const code = keyword.value.trim()
  if (!code) {
    Taro.showToast({ title: '请输入订单号', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const res = await searchOrder(code)
    result.value = res || null
    if (!res) {
      Taro.showToast({ title: '未找到订单', icon: 'none' })
    }
  } catch {
    result.value = null
  } finally {
    loading.value = false
  }
}

useDidShow(() => {
  if (!checkAuthAndRedirect()) return
})
</script>

<style lang="scss">
.order-page {
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

.search-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.search-input {
  flex: 1;
  height: 72px;
  padding: 0 20px;
  background: #f5f7fa;
  border-radius: 10px;
  font-size: 28px;
  color: #1a1a2e;
}

.search-btn {
  height: 72px;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
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

.result-wrap {
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

.card-title {
  font-size: 32px;
  font-weight: 600;
  color: #1a1a2e;
}

.info-row {
  display: flex;
  margin-bottom: 14px;
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

.info-value.highlight {
  color: #667eea;
  font-weight: 600;
}

.progress-section {
  margin-top: 20px;
}

.progress-bar {
  height: 14px;
  background: #f0f0f0;
  border-radius: 7px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 7px;
  transition: width 0.3s ease;
}

.section-title {
  display: block;
  font-size: 28px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 20px;
}

.process-empty {
  padding: 24px 0;
  text-align: center;
}

.process-empty-text {
  font-size: 24px;
  color: #9ca3af;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.item-row {
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
}

.item-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.item-style {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a2e;
}

.item-progress {
  font-size: 26px;
  color: #667eea;
  font-weight: 600;
}

.item-detail {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.item-attr {
  font-size: 24px;
  color: #6b7280;
}

.item-qty {
  font-size: 24px;
  color: #9ca3af;
}

.item-bar {
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.item-bar-fill {
  height: 100%;
  background: #667eea;
  border-radius: 4px;
}
</style>
