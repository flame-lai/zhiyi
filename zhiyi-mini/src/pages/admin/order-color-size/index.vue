<template>
  <View class="cs-page">
    <View class="content">
      <View v-if="loading" class="loading"><Text class="loading-text">加载中...</Text></View>
      <template v-else-if="data">
        <!-- 头部概览 -->
        <View class="card">
          <View class="head-row">
            <Text class="head-title">{{ data.styleNo || '-' }}</Text>
            <StatusTag :text="formatPercent(data.completionRate)" type="info" />
          </View>
          <View class="head-sub">
            <Text class="head-sub-text">{{ data.color || '-' }} / {{ data.size || '-' }}</Text>
            <Text class="head-sub-text">共 {{ data.totalBundles }} 扎</Text>
          </View>
          <View class="progress-bar">
            <View class="progress-fill" :style="{ width: progressWidth(data.completionRate) }"></View>
          </View>
        </View>

        <!-- 工序进度 -->
        <View class="card">
          <Text class="section-title">工序进度</Text>
          <View v-if="!data.processes || data.processes.length === 0" class="empty-text">
            <Text class="empty-text-inner">暂无工序</Text>
          </View>
          <View v-else class="process-list">
            <View v-for="p in data.processes" :key="p.processId" class="process-row">
              <View class="process-row-head">
                <Text class="process-name">{{ p.processName }}</Text>
                <Text class="process-count">{{ p.completed }}/{{ p.total }}</Text>
              </View>
              <View class="process-bar">
                <View class="process-bar-fill" :style="{ width: progressWidth(p.rate) }"></View>
              </View>
            </View>
          </View>
        </View>

        <!-- 扎号明细 -->
        <View class="card">
          <Text class="section-title">扎号明细</Text>
          <View v-if="!data.bundles || data.bundles.length === 0" class="empty-text">
            <Text class="empty-text-inner">暂无扎号</Text>
          </View>
          <View v-else class="bundle-list">
            <View v-for="b in data.bundles" :key="b.bundleId" class="bundle-card" @tap="openBundle(b)">
              <View class="bundle-head">
                <Text class="bundle-no">{{ b.globalBundleNo }}</Text>
                <Text class="bundle-short">短号 {{ b.bundleNo }}</Text>
                <Text class="bundle-arrow">›</Text>
              </View>
              <View class="bundle-processes">
                <View v-for="(p, i) in b.processes" :key="p.processId" class="bp-item">
                  <View :class="['bp-dot', p.status === 'DONE' && 'bp-dot-done']">
                    <Text v-if="p.status === 'DONE'" class="bp-check">✓</Text>
                  </View>
                  <Text class="bp-name">{{ shortName(p.processName) }}</Text>
                  <Text v-if="p.status === 'DONE' && p.doneBy" class="bp-by">{{ p.doneBy }}</Text>
                </View>
              </View>
            </View>
          </View>
        </View>
      </template>
      <EmptyState v-else text="暂无数据" />
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro, { useLoad } from '@tarojs/taro'
import { View, Text } from '@tarojs/components'
import { getColorSizeProgress } from '../../../api/mobileAdmin'
import { formatPercent } from '../../../utils/format'
import { checkAuthAndRedirect } from '../../../utils/auth'
import EmptyState from '../../../components/EmptyState.vue'
import StatusTag from '../../../components/StatusTag.vue'
import type { ColorSizeProgress } from '../../../api/types'

definePageConfig({
  navigationBarTitleText: '颜色尺码进度',
})

const loading = ref(false)
const data = ref<ColorSizeProgress | null>(null)

function progressWidth(rate: number | string | null | undefined): string {
  if (rate === null || rate === undefined || rate === '') return '0%'
  const num = typeof rate === 'string' ? parseFloat(rate) : rate
  return Math.min(100, Math.max(0, (isNaN(num) ? 0 : num) * 100)) + '%'
}

function shortName(name: string): string {
  if (!name) return ''
  return name.length > 4 ? name.substring(0, 4) : name
}

function openBundle(b: { globalBundleNo: string }) {
  if (!b.globalBundleNo) return
  Taro.navigateTo({ url: `/pages/admin/bundle-query/index?code=${encodeURIComponent(b.globalBundleNo)}` })
}

useLoad((options: any) => {
  if (!checkAuthAndRedirect()) return
  const orderId = Number(options.orderId)
  const rawColor = options.color || ''
  const rawSize = options.size || ''
  const color = rawColor ? decodeURIComponent(rawColor) : undefined
  const size = rawSize ? decodeURIComponent(rawSize) : undefined
  if (!orderId) {
    Taro.showToast({ title: '参数缺失', icon: 'none' })
    return
  }
  loadData(orderId, color, size)
})

async function loadData(orderId: number, color?: string, size?: string) {
  loading.value = true
  try {
    const res = await getColorSizeProgress(orderId, color, size)
    data.value = res || null
  } catch {
    data.value = null
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss">
.cs-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.content {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
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

.card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.head-title {
  font-size: 32px;
  font-weight: 600;
  color: #4A6FA5;
}

.head-sub {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.head-sub-text {
  font-size: 24px;
  color: #6b7280;
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

.empty-text {
  padding: 24px 0;
  text-align: center;
}

.empty-text-inner {
  font-size: 24px;
  color: #9ca3af;
}

.process-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.process-row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.process-name {
  font-size: 26px;
  font-weight: 500;
  color: #4A6FA5;
}

.process-count {
  font-size: 24px;
  color: #6b7280;
}

.process-bar {
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

.bundle-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bundle-card {
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
}

.bundle-card:active {
  background: #f0f4fa;
}

.bundle-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.bundle-no {
  font-size: 26px;
  font-weight: 600;
  color: #4A6FA5;
  flex: 1;
}

.bundle-short {
  font-size: 22px;
  color: #9ca3af;
}

.bundle-arrow {
  font-size: 28px;
  color: #c0c4cc;
}

.bundle-processes {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.bp-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100px;
}

.bp-dot {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #fff;
  border: 2px solid #d1d5db;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bp-dot-done {
  background: #10b981;
  border-color: #10b981;
}

.bp-check {
  color: #fff;
  font-size: 24px;
}

.bp-name {
  font-size: 22px;
  color: #6b7280;
  margin-top: 6px;
  text-align: center;
}

.bp-by {
  font-size: 20px;
  color: #9ca3af;
  margin-top: 2px;
}
</style>
