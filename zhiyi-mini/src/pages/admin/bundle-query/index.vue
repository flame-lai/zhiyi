<template>
  <View class="bundle-page">
    <!-- Header with gradient -->
    <View class="page-header">
      <Text class="page-title">扎号查询</Text>
    </View>

    <!-- Search bar -->
    <View class="search-bar">
      <Input
        class="search-input"
        placeholder="请输入或扫描扎号"
        :value="keyword"
        @input="onKeywordInput"
        @confirm="onSearch"
      />
      <View class="scan-btn" @tap="onScan">
        <Text class="scan-text">扫码</Text>
      </View>
      <View class="search-btn" @tap="onSearch">
        <Text class="search-btn-text">搜索</Text>
      </View>
    </View>

    <ScrollView scroll-y class="content">
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
    </ScrollView>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Input, ScrollView } from '@tarojs/components'
import { searchBundle } from '../../../api/mobileAdmin'
import { formatDateTime } from '../../../utils/format'
import { checkAuthAndRedirect } from '../../../utils/auth'
import EmptyState from '../../../components/EmptyState.vue'
import StatusTag from '../../../components/StatusTag.vue'
import type { BundleDetail, ProcessReportStatus } from '../../../api/types'

definePageConfig({
  navigationBarTitleText: '扎号查询',
})

const keyword = ref('')
const loading = ref(false)
const result = ref<BundleDetail | null>(null)

function onKeywordInput(e: any) {
  keyword.value = e.detail.value
}

function statusText(status: string): string {
  if (status === 'DONE' || status === 'COMPLETED') return '已完成'
  return '生产中'
}

function statusType(status: string): 'success' | 'info' {
  if (status === 'DONE' || status === 'COMPLETED') return 'success'
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
      doSearch(code)
    }
  } catch {
    // scan cancelled
  }
}

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
  color: #667eea;
  font-weight: 500;
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
  color: #1a1a2e;
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
