<template>
  <View class="order-detail-page">
    <View class="content">
      <View v-if="loading" class="loading"><Text class="loading-text">加载中...</Text></View>
      <EmptyState v-else-if="!result" text="未找到订单详情" />
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
            <View v-for="(item, idx) in result.items" :key="idx" class="item-row" @tap="openColorSize(item)">
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
              <View class="item-arrow"><Text class="item-arrow-text">查看进度 ›</Text></View>
            </View>
          </View>
        </View>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro, { useLoad } from '@tarojs/taro'
import { View, Text } from '@tarojs/components'
import { searchOrder } from '../../../api/mobileAdmin'
import { formatAmount, formatDate, formatPercent } from '../../../utils/format'
import { checkAuthAndRedirect } from '../../../utils/auth'
import EmptyState from '../../../components/EmptyState.vue'
import StatusTag from '../../../components/StatusTag.vue'
import type { OrderSearchResult } from '../../../api/types'

definePageConfig({
  navigationBarTitleText: '订单详情',
})

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

function progressWidth(rate: number | string | null | undefined): string {
  if (rate === null || rate === undefined || rate === '') return '0%'
  const num = typeof rate === 'string' ? parseFloat(rate) : rate
  return Math.min(100, Math.max(0, (isNaN(num) ? 0 : num) * 100)) + '%'
}

function openColorSize(item: { color?: string; size?: string }) {
  if (!result.value) return
  const q = `orderId=${result.value.id}&color=${encodeURIComponent(item.color || '')}&size=${encodeURIComponent(item.size || '')}`
  Taro.navigateTo({ url: `/pages/admin/order-color-size/index?${q}` })
}

useLoad((options: any) => {
  if (!checkAuthAndRedirect()) return
  const orderNo = options?.orderNo ? decodeURIComponent(options.orderNo) : ''
  if (!orderNo) {
    Taro.showToast({ title: '参数缺失', icon: 'none' })
    return
  }
  loadData(orderNo)
})

async function loadData(orderNo: string) {
  loading.value = true
  try {
    const res = await searchOrder(orderNo)
    result.value = res || null
  } catch {
    result.value = null
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss">
.order-detail-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.content {
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
  color: #4A6FA5;
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
  color: #4A6FA5;
}

.info-value.highlight {
  color: #4A6FA5;
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
  background: #4A6FA5;
  border-radius: 7px;
}

.section-title {
  display: block;
  font-size: 28px;
  font-weight: 600;
  color: #4A6FA5;
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

.item-row:active {
  background: #f0f4fa;
}

.item-arrow {
  margin-top: 10px;
  text-align: right;
}

.item-arrow-text {
  font-size: 22px;
  color: #4A6FA5;
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
  color: #4A6FA5;
}

.item-progress {
  font-size: 26px;
  color: #4A6FA5;
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
  background: #4A6FA5;
  border-radius: 4px;
}
</style>