<template>
  <View class="bundle-page">
    <!-- Search bar -->
    <View class="search-bar">
      <View class="search-wrap">
        <Input
          class="search-input"
          placeholder="请输入或扫描扎号"
          :value="keyword"
          @input="onKeywordInput"
          @focus="showSuggest = true"
          @confirm="onSearch"
        />
        <View v-if="showSuggest && suggestions.length > 0" class="suggest-dropdown">
          <View v-for="item in suggestions" :key="item.id" class="suggest-item" @tap="selectSuggest(item)">
            <View class="suggest-main">
              <Text class="suggest-bundle">{{ item.globalBundleNo }}</Text>
              <Text class="suggest-short">短号 {{ item.bundleNo }}</Text>
            </View>
            <Text class="suggest-sub">{{ item.orderNo }} · {{ item.color }}/{{ item.size }} · {{ item.qty }}件</Text>
          </View>
        </View>
      </View>
      <View class="scan-btn" @tap="onScan">
        <Text class="scan-text">扫码</Text>
      </View>
      <View class="search-btn" @tap="onSearch">
        <Text class="search-btn-text">搜索</Text>
      </View>
    </View>

    <View class="content">
      <View v-if="loading" class="loading"><Text class="loading-text">查询中...</Text></View>
      <EmptyState v-else-if="!result" text="请输入或扫描扎号查询" />
      <View v-else class="result-wrap">
        <!-- Bundle info card -->
        <View class="card">
          <View class="card-head">
            <Text class="card-title">{{ result.bundleNo }}</Text>
            <StatusTag :text="statusText(result.status)" :type="statusType(result.status)" />
          </View>

          <View class="info-row">
            <Text class="info-label">订单号</Text>
            <Text class="info-value">{{ result.orderNo }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">款号</Text>
            <Text class="info-value">{{ result.styleNo }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">颜色</Text>
            <Text class="info-value">{{ result.color }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">尺码</Text>
            <Text class="info-value">{{ result.size }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">数量</Text>
            <Text class="info-value">{{ result.qty }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">状态</Text>
            <Text class="info-value">{{ statusText(result.status) }}</Text>
          </View>
          <View class="card-actions">
            <View class="supplement-btn" @tap="goSupplement">
              <Text class="supplement-btn-text">补录报工</Text>
            </View>
          </View>
        </View>

        <!-- Process list -->
        <View class="card">
          <Text class="section-title">工序进度</Text>
          <View v-if="!result.processes || result.processes.length === 0" class="process-empty">
            <Text class="process-empty-text">暂无工序</Text>
          </View>
          <View v-else class="process-list">
            <View
              v-for="p in result.processes"
              :key="p.processId"
              class="process-item"
              :class="{ done: p.status === 'DONE' }"
            >
              <View class="process-check">
                <View v-if="p.status === 'DONE'" class="check-done"><Text class="check-icon">✓</Text></View>
                <View v-else class="check-pending" />
              </View>
              <View class="process-info">
                <Text class="process-name">{{ p.processName }}</Text>
                <Text v-if="p.status === 'DONE'" class="process-done-info">{{ doneInfo(p) }}</Text>
                <Text v-else class="process-status">{{ statusLabel(p.status) }}</Text>
              </View>
            </View>
          </View>
        </View>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro, { useLoad, useDidShow } from '@tarojs/taro'
import { View, Text, Input } from '@tarojs/components'
import { searchBundle, suggestBundle } from '../../../api/mobileAdmin'
import { formatDateTime } from '../../../utils/format'
import { checkAuthAndRedirect } from '../../../utils/auth'
import EmptyState from '../../../components/EmptyState.vue'
import StatusTag from '../../../components/StatusTag.vue'
import type { BundleDetail, ProcessReportStatus, BundleSuggest } from '../../../api/types'

definePageConfig({
  navigationBarTitleText: '扎号查询',
})

const keyword = ref('')
const loading = ref(false)
const result = ref<BundleDetail | null>(null)
const suggestions = ref<BundleSuggest[]>([])
const showSuggest = ref(false)
let suggestTimer: ReturnType<typeof setTimeout> | null = null

function onKeywordInput(e: any) {
  keyword.value = e.detail.value
  if (suggestTimer) clearTimeout(suggestTimer)
  const val = e.detail.value.trim()
  if (!val) {
    suggestions.value = []
    return
  }
  suggestTimer = setTimeout(async () => {
    try {
      const res = await suggestBundle(val)
      suggestions.value = res || []
    } catch {
      suggestions.value = []
    }
  }, 300)
}

function selectSuggest(item: BundleSuggest) {
  keyword.value = item.globalBundleNo
  suggestions.value = []
  showSuggest.value = false
  doSearch(item.globalBundleNo)
}

function statusText(status: string): string {
  if (status === '已完成') return '已完成'
  return '生产中'
}

function statusType(status: string): 'success' | 'info' {
  if (status === '已完成') return 'success'
  return 'info'
}

function statusLabel(status: string): string {
  if (status === 'AVAILABLE') return '可报工'
  if (status === 'PARTIAL_DONE') return '部分完成'
  if (status === 'UNAVAILABLE') return '不可报工'
  if (status === 'DONE') return '已完成'
  return status
}

function doneInfo(p: ProcessReportStatus): string {
  let info = '已完成'
  if (p.doneBy) {
    info = info + '（' + p.doneBy
    if (p.doneTime) {
      info = info + ' · ' + formatDateTime(p.doneTime)
    }
    info = info + '）'
  }
  return info
}

async function doSearch(code: string) {
  if (!code) {
    Taro.showToast({ title: '请输入或扫描扎号', icon: 'none' })
    return
  }
  showSuggest.value = false
  suggestions.value = []
  loading.value = true
  try {
    const res = await searchBundle(code)
    result.value = res || null
    if (!res) {
      Taro.showToast({ title: '未找到扎号', icon: 'none' })
    }
  } catch {
    result.value = null
  } finally {
    loading.value = false
  }
}

function onSearch() {
  doSearch(keyword.value.trim())
}

async function onScan() {
  try {
    const scanRes = await Taro.scanCode({ scanType: ['qrCode', 'barCode'] })
    const code = scanRes.result
    if (code) {
      keyword.value = code
      showSuggest.value = false
      suggestions.value = []
      doSearch(code)
    }
  } catch {
    // scan cancelled
  }
}

function goSupplement() {
  if (!result.value) return
  Taro.navigateTo({ url: '/pages/admin/report-supplement/index?code=' + encodeURIComponent(result.value.globalBundleNo) })
}

useLoad((options: any) => {
  const code = options?.code
  if (code) {
    keyword.value = decodeURIComponent(code)
    doSearch(keyword.value)
  }
})

useDidShow(() => {
  if (!checkAuthAndRedirect()) return
})
</script>

<style lang="scss">
.bundle-page {
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
  gap: 12px;
}

.suggest-bundle {
  font-size: 26px;
  font-weight: 600;
  color: #4A6FA5;
}

.suggest-short {
  font-size: 22px;
  color: #9ca3af;
}

.suggest-sub {
  display: block;
  font-size: 22px;
  color: #6b7280;
  margin-top: 4px;
}

.scan-btn {
  height: 72px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0ff;
  border-radius: 10px;
}

.scan-text {
  font-size: 26px;
  color: #4A6FA5;
  font-weight: 500;
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
  box-shadow: 0 2px 8px rgba(0, 0,0, 0.04);
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

.card-actions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
}

.supplement-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 0;
  background: #4A6FA5;
  border-radius: 8px;
}

.supplement-btn:active {
  opacity: 0.8;
}

.supplement-btn-text {
  font-size: 26px;
  color: #fff;
  font-weight: 500;
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

.process-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.process-item {
  display: flex;
  align-items: center;
  padding: 20px 16px;
  background: #f9fafb;
  border-radius: 12px;
}

.process-item.done {
  background: #f0fdf4;
}

.process-check {
  margin-right: 16px;
  flex-shrink: 0;
}

.check-done {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #10b981;
  display: flex;
  align-items: center;
  justify-content: center;
}

.check-icon {
  color: #fff;
  font-size: 24px;
}

.check-pending {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #fff;
  border: 2px solid #d1d5db;
  box-sizing: border-box;
}

.process-info {
  flex: 1;
}

.process-name {
  display: block;
  font-size: 28px;
  font-weight: 500;
  color: #4A6FA5;
}

.process-done-info {
  display: block;
  font-size: 24px;
  color: #059669;
  margin-top: 4px;
}

.process-status {
  display: block;
  font-size: 24px;
  color: #9ca3af;
  margin-top: 4px;
}
</style>
