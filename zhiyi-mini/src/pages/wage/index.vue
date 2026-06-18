<template>
  <View class="wage-page">
    <!-- Header -->
    <View class="wage-header">
      <View class="summary-row">
        <View class="summary-item">
          <Text class="summary-label">今日已赚</Text>
          <Text class="summary-value">{{ formatAmount(summary?.todayAmount ?? 0) }}</Text>
        </View>
        <View class="summary-divider" />
        <View class="summary-item">
          <Text class="summary-label">本月累计</Text>
          <Text class="summary-value">{{ formatAmount(summary?.monthAmount ?? 0) }}</Text>
        </View>
      </View>
    </View>

    <!-- Loading -->
    <View class="loading-wrap" v-if="loading">
      <Text class="loading-text">加载中...</Text>
    </View>

    <!-- Empty state -->
    <EmptyState v-else-if="!summary || !summary.dailyGroups || summary.dailyGroups.length === 0" text="暂无工资记录" />

    <!-- Daily groups -->
    <ScrollView v-else scroll-y class="daily-list" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
      <View v-for="group in summary.dailyGroups" :key="group.date" class="daily-group">
        <View class="group-header">
          <Text class="group-date">{{ group.date }}</Text>
          <Text class="group-amount">{{ formatAmount(group.amount) }}</Text>
        </View>
        <View v-for="record in group.records" :key="record.id" class="record-card">
          <View class="record-main">
            <Text class="record-process">{{ record.processName }}</Text>
            <Text class="record-bundle">扎号: {{ record.bundleNo }}</Text>
          </View>
          <View class="record-detail">
            <Text class="record-calc">{{ record.qty }} x {{ formatAmount(record.price).replace('¥', '') }}</Text>
            <Text class="record-amount">{{ formatAmount(record.amount) }}</Text>
          </View>
        </View>
      </View>
    </ScrollView>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'
import { getWageSummary } from '../../api/mobileReport'
import { formatAmount } from '../../utils/format'
import EmptyState from '../../components/EmptyState.vue'
import type { WageSummary } from '../../api/types'

definePageConfig({
  navigationBarTitleText: '我的工资',
})

const loading = ref(false)
const refreshing = ref(false)
const summary = ref<WageSummary | null>(null)

async function fetchWage() {
  const token = Taro.getStorageSync('satoken')
  if (!token) {
    Taro.redirectTo({ url: '/pages/login/index' })
    return
  }

  loading.value = true
  try {
    const res = await getWageSummary()
    summary.value = res
  } catch {
    // handled by request util
  } finally {
    loading.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  try {
    const res = await getWageSummary()
    summary.value = res
  } catch {
    // handled by request util
  } finally {
    refreshing.value = false
  }
}

useDidShow(() => {
  fetchWage()
})
</script>

<style lang="scss">
.wage-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.wage-header {
  background: #4A6FA5;
  padding: 48px 32px 40px;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.summary-label {
  font-size: 26px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 12px;
}

.summary-value {
  font-size: 44px;
  font-weight: 700;
  color: #ffffff;
}

.summary-divider {
  width: 2px;
  height: 64px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 1px;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.loading-text {
  font-size: 28px;
  color: #9ca3af;
}

.daily-list {
  flex: 1;
  padding: 24px 24px 40px;
}

.daily-group {
  margin-bottom: 24px;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #ffffff;
  border-radius: 16px 16px 0 0;
  border-bottom: 1px solid #f3f4f6;
}

.group-date {
  font-size: 28px;
  font-weight: 600;
  color: #4A6FA5;
}

.group-amount {
  font-size: 30px;
  font-weight: 700;
  color: #4A6FA5;
}

.record-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: #ffffff;

  &:last-child {
    border-radius: 0 0 16px 16px;
  }

  &:not(:last-child) {
    border-bottom: 1px solid #f3f4f6;
  }
}

.record-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.record-process {
  font-size: 28px;
  font-weight: 500;
  color: #4A6FA5;
}

.record-bundle {
  font-size: 24px;
  color: #9ca3af;
}

.record-detail {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.record-calc {
  font-size: 24px;
  color: #9ca3af;
}

.record-amount {
  font-size: 30px;
  font-weight: 600;
  color: #4A6FA5;
}
</style>
