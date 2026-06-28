<template>
  <View class="order-page">
    <!-- Search bar -->
    <View class="search-bar">
      <View class="search-wrap">
        <Input
          class="search-input"
          placeholder="搜索订单号"
          :value="keyword"
          @input="onKeywordInput"
          @focus="showSuggest = true"
          @confirm="onSearchKeyword"
        />
        <View v-if="showSuggest && suggestions.length > 0" class="suggest-dropdown">
          <View v-for="item in suggestions" :key="item.id" class="suggest-item" @tap="selectSuggest(item)">
            <View class="suggest-main">
              <Text class="suggest-order">{{ item.orderNo }}</Text>
              <Text class="suggest-status">{{ orderStatusText(item.status) }}</Text>
            </View>
            <Text class="suggest-sub">{{ item.customerName || '-' }} · 交货 {{ formatDate(item.deliveryDate) }}</Text>
          </View>
        </View>
      </View>
      <View class="search-btn" @tap="onSearchKeyword">
        <Text class="search-btn-text">搜索</Text>
      </View>
    </View>

    <!-- Filter bar -->
    <View class="filter-bar">
      <View class="quick-btns">
        <View :class="['quick-btn', filterMode === 'thisMonth' && 'active']" @tap="setThisMonth">
          <Text class="quick-btn-text">本月</Text>
        </View>
        <View :class="['quick-btn', filterMode === 'lastMonth' && 'active']" @tap="setLastMonth">
          <Text class="quick-btn-text">上月</Text>
        </View>
        <View :class="['quick-btn', filterMode === 'custom' && 'active']" @tap="filterMode = 'custom'">
          <Text class="quick-btn-text">自定义</Text>
        </View>
      </View>
      <View class="filter-extra">
        <Picker mode="selector" :range="statusOptions" range-key="label" :value="statusIndex" @change="onStatusChange">
          <View class="picker">
            <Text class="picker-text">{{ statusLabel }}</Text>
          </View>
        </Picker>
      </View>
    </View>

    <!-- Custom date range -->
    <View v-if="filterMode === 'custom'" class="date-range-bar">
      <Picker mode="date" :value="customStart" @change="(e: any) => { customStart = e.detail.value; reload() }">
        <View class="date-picker"><Text class="date-text">{{ customStart || '开始日期' }}</Text></View>
      </Picker>
      <Text class="date-sep">~</Text>
      <Picker mode="date" :value="customEnd" @change="(e: any) => { customEnd = e.detail.value; reload() }">
        <View class="date-picker"><Text class="date-text">{{ customEnd || '结束日期' }}</Text></View>
      </Picker>
    </View>

    <View class="content">
      <View v-if="loading && page === 1" class="loading"><Text class="loading-text">加载中...</Text></View>
      <EmptyState v-else-if="list.length === 0" text="暂无订单" />
      <View v-else class="card-list">
        <View v-for="item in list" :key="item.id" class="card" @tap="openDetail(item)">
          <View class="card-head">
            <Text class="card-title">{{ item.orderNo }}</Text>
            <StatusTag :text="orderStatusText(item.status)" :type="orderStatusType(item.status)" />
          </View>
          <View class="card-row">
            <Text class="card-label">客户</Text>
            <Text class="card-value">{{ item.customerName || '-' }}</Text>
          </View>
          <View class="card-row">
            <Text class="card-label">款号</Text>
            <Text class="card-value">{{ item.styleNo || '-' }}</Text>
          </View>
          <View class="card-row">
            <Text class="card-label">总数量</Text>
            <Text class="card-value">{{ item.totalQty != null ? item.totalQty : '-' }}</Text>
          </View>
          <View class="card-row">
            <Text class="card-label">总金额</Text>
            <Text class="card-value highlight">{{ formatAmount(item.totalAmount) }}</Text>
          </View>
          <View class="card-row">
            <Text class="card-label">交货日期</Text>
            <Text class="card-value">{{ formatDate(item.deliveryDate) }}</Text>
          </View>
          <View class="card-row">
            <Text class="card-label">创建时间</Text>
            <Text class="card-value">{{ item.createdAt }}</Text>
          </View>
        </View>
        <View v-if="loading && page > 1" class="loading-more"><Text class="loading-more-text">加载中...</Text></View>
        <View v-if="!hasMore && list.length > 0" class="no-more"><Text class="no-more-text">没有更多了</Text></View>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Taro, { useDidShow, useReachBottom } from '@tarojs/taro'
import { View, Text, Input, Picker } from '@tarojs/components'
import { getOrders, suggestOrder } from '../../../api/mobileAdmin'
import { formatAmount, formatDate } from '../../../utils/format'
import { checkAuthAndRedirect } from '../../../utils/auth'
import EmptyState from '../../../components/EmptyState.vue'
import StatusTag from '../../../components/StatusTag.vue'
import type { OrderListItem, OrderSuggest } from '../../../api/types'

definePageConfig({
  navigationBarTitleText: '订单查询',
})

const pageSize = 20
const list = ref<OrderListItem[]>([])
const page = ref(1)
const hasMore = ref(true)
const loading = ref(false)

const keyword = ref('')
const suggestions = ref<OrderSuggest[]>([])
const showSuggest = ref(false)
let suggestTimer: ReturnType<typeof setTimeout> | null = null

const filterMode = ref<'thisMonth' | 'lastMonth' | 'custom'>('thisMonth')
const customStart = ref('')
const customEnd = ref('')

const statusOptions = [
  { value: -1, label: '全部状态' },
  { value: 0, label: '待确认' },
  { value: 1, label: '生产中' },
  { value: 2, label: '已完成' },
  { value: 3, label: '已取消' },
]
const statusIndex = ref(0)
const statusLabel = computed(() => statusOptions[statusIndex.value]?.label || '全部状态')

function monthRange(offset = 0): [string, string] {
  const d = new Date()
  d.setMonth(d.getMonth() + offset)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const lastDay = new Date(y, d.getMonth() + 1, 0).getDate()
  return [`${y}-${m}-01`, `${y}-${m}-${String(lastDay).padStart(2, '0')}`]
}

function getFilterParams() {
  const params: any = { page: page.value, size: pageSize }
  if (filterMode.value === 'thisMonth') {
    const [s, e] = monthRange(0); params.startDate = s; params.endDate = e
  } else if (filterMode.value === 'lastMonth') {
    const [s, e] = monthRange(-1); params.startDate = s; params.endDate = e
  } else {
    if (customStart.value) params.startDate = customStart.value
    if (customEnd.value) params.endDate = customEnd.value
  }
  const st = statusOptions[statusIndex.value].value
  if (st >= 0) params.status = st
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  return params
}

async function loadList() {
  loading.value = true
  try {
    const res = await getOrders(getFilterParams())
    const data = res as any
    if (page.value === 1) {
      list.value = data?.records || []
    } else {
      list.value = [...list.value, ...(data?.records || [])]
    }
    hasMore.value = data?.hasMore ?? false
  } catch {
    if (page.value === 1) list.value = []
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  hasMore.value = true
  loadList()
}

function setThisMonth() { filterMode.value = 'thisMonth'; reload() }
function setLastMonth() { filterMode.value = 'lastMonth'; reload() }

function onStatusChange(e: any) {
  statusIndex.value = Number(e.detail.value)
  reload()
}

useReachBottom(() => {
  if (!loading.value && hasMore.value) {
    page.value++
    loadList()
  }
})

function onKeywordInput(e: any) {
  keyword.value = e.detail.value
  if (suggestTimer) clearTimeout(suggestTimer)
  const val = e.detail.value.trim()
  if (!val) { suggestions.value = []; return }
  suggestTimer = setTimeout(async () => {
    try {
      const res = await suggestOrder(val)
      suggestions.value = res || []
    } catch {
      suggestions.value = []
    }
  }, 300)
}

function onSearchKeyword() {
  showSuggest.value = false
  suggestions.value = []
  reload()
}

function selectSuggest(item: OrderSuggest) {
  openDetail({ ...item, totalQty: undefined, createdAt: '' } as any)
}

function openDetail(item: { orderNo: string }) {
  if (!item.orderNo) return
  showSuggest.value = false
  suggestions.value = []
  Taro.navigateTo({ url: `/pages/admin/order-detail/index?orderNo=${encodeURIComponent(item.orderNo)}` })
}

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

useDidShow(() => {
  if (!checkAuthAndRedirect()) return
  if (list.value.length === 0) loadList()
})
</script>

<style lang="scss">
.order-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.search-wrap {
  flex: 1;
  position: relative;
}

.search-input {
  width: 100%;
  height: 72px;
  padding: 0 20px;
  background: #f5f7fa;
  border-radius: 10px;
  font-size: 28px;
  color: #4A6FA5;
  box-sizing: border-box;
}

.suggest-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-top: 4px;
  max-height: 360px;
  overflow-y: auto;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  z-index: 20;
}

.suggest-item {
  padding: 16px 20px;
  border-bottom: 1px solid #f3f4f6;
}

.suggest-item:last-child { border-bottom: none; }
.suggest-item:active { background: #f5f7fa; }

.suggest-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.suggest-order {
  font-size: 26px;
  font-weight: 600;
  color: #4A6FA5;
}

.suggest-status {
  font-size: 22px;
  color: #9ca3af;
}

.suggest-sub {
  display: block;
  font-size: 22px;
  color: #6b7280;
  margin-top: 4px;
}

.search-btn {
  height: 72px;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #4A6FA5;
  border-radius: 10px;
}

.search-btn-text {
  font-size: 26px;
  color: #fff;
  font-weight: 500;
}

.filter-bar {
  background: #fff;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.quick-btns {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.quick-btn {
  flex: 1;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 8px;
}

.quick-btn.active {
  background: #4A6FA5;
}

.quick-btn-text {
  font-size: 24px;
  color: #6b7280;
}

.quick-btn.active .quick-btn-text {
  color: #fff;
  font-weight: 500;
}

.filter-extra {
  display: flex;
}

.picker {
  flex: 1;
  height: 60px;
  padding: 0 16px;
  background: #f5f7fa;
  border-radius: 8px;
  display: flex;
  align-items: center;
}

.picker-text {
  font-size: 24px;
  color: #6b7280;
}

.date-range-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.date-picker {
  flex: 1;
  height: 60px;
  padding: 0 16px;
  background: #f5f7fa;
  border-radius: 8px;
  display: flex;
  align-items: center;
}

.date-text {
  font-size: 24px;
  color: #4A6FA5;
}

.date-sep {
  font-size: 24px;
  color: #9ca3af;
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

.card:active {
  background: #f5f7fa;
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
  font-size: 30px;
  font-weight: 600;
  color: #4A6FA5;
}

.card-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.card-row:last-child {
  margin-bottom: 0;
}

.card-label {
  width: 130px;
  font-size: 26px;
  color: #9ca3af;
  flex-shrink: 0;
}

.card-value {
  flex: 1;
  font-size: 26px;
  color: #6b7280;
}

.card-value.highlight {
  color: #4A6FA5;
  font-weight: 600;
}

.loading-more, .no-more {
  padding: 24px 0;
  text-align: center;
}

.loading-more-text, .no-more-text {
  font-size: 24px;
  color: #9ca3af;
}
</style>