<template>
  <View class="wage-page">
    <View class="wage-header">
      <Text class="wage-month">{{ currentMonth }}</Text>
    </View>

    <View class="summary-cards" v-if="summary">
      <View class="summary-card">
        <Text class="summary-label">今日工资</Text>
        <Text class="summary-value">{{ formatMoney(summary.todayWage) }}</Text>
      </View>
      <View class="summary-card">
        <Text class="summary-label">本月计件工资</Text>
        <Text class="summary-value">{{ formatMoney(summary.pieceWage) }}</Text>
      </View>
      <View class="summary-card">
        <Text class="summary-label">本月计时工资</Text>
        <Text class="summary-value">{{ formatMoney(summary.timeWage) }}</Text>
      </View>
      <View class="summary-card total">
        <Text class="summary-label">本月总计</Text>
        <Text class="summary-value total-value">{{ formatMoney(summary.totalWage) }}</Text>
      </View>
    </View>

    <View class="loading-tip" v-if="loading">
      <Text class="loading-text">加载中...</Text>
    </View>

    <View class="detail-section" v-if="details.length > 0">
      <Text class="section-title">工资明细</Text>
      <ScrollView scroll-y class="detail-list">
        <View class="detail-row" v-for="(item, index) in details" :key="index">
          <View class="detail-left">
            <Text class="detail-date">{{ item.date }}</Text>
            <Text class="detail-type">{{ item.type === 'piece' ? '计件' : '计时' }}</Text>
          </View>
          <View class="detail-right">
            <Text class="detail-amount">{{ formatMoney(item.amount) }}</Text>
            <Text class="detail-qty" v-if="item.quantity">x{{ item.quantity }}</Text>
          </View>
        </View>
      </ScrollView>
    </View>

    <View class="empty-tip" v-if="!loading && details.length === 0 && summary">
      <Text class="empty-text">暂无工资明细</Text>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'
import { getWageSummary } from '../../api/report'

definePageConfig({
  navigationBarTitleText: '我的工资',
})

interface WageDetail {
  date: string
  type: 'piece' | 'time'
  amount: number
  quantity?: number
}

interface WageSummary {
  todayWage: number
  pieceWage: number
  timeWage: number
  totalWage: number
  details: WageDetail[]
}

const loading = ref(false)
const summary = ref<WageSummary | null>(null)
const details = ref<WageDetail[]>([])

const currentMonth = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月`
})

function formatMoney(val: number | undefined): string {
  if (val === undefined || val === null) return '0.00'
  return val.toFixed(2)
}

async function fetchWage() {
  const token = Taro.getStorageSync('satoken')
  if (!token) {
    Taro.redirectTo({ url: '/pages/login/index' })
    return
  }

  const now = new Date()
  const yearMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`

  loading.value = true
  try {
    const res = await getWageSummary(yearMonth)
    const data = res.data || {}
    summary.value = {
      todayWage: data.todayWage || 0,
      pieceWage: data.pieceWage || 0,
      timeWage: data.timeWage || 0,
      totalWage: data.totalWage || 0,
    }
    details.value = data.details || []
  } catch (err) {
    // handled by request util
  } finally {
    loading.value = false
  }
}

useDidShow(() => {
  fetchWage()
})
</script>

<style lang="scss">
.wage-page {
  min-height: 100vh;
  background: #F5F7FA;
  padding: 24px;
  box-sizing: border-box;
}

.wage-header {
  margin-bottom: 24px;
}

.wage-month {
  font-size: 32px;
  font-weight: bold;
  color: #333333;
}

.summary-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 32px;
}

.summary-card {
  width: calc(50% - 8px);
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  box-sizing: border-box;
}

.summary-card.total {
  width: 100%;
  background: #4A6FA5;
}

.summary-label {
  display: block;
  font-size: 24px;
  color: #999999;
  margin-bottom: 8px;
}

.summary-card.total .summary-label {
  color: rgba(255, 255, 255, 0.8);
}

.summary-value {
  display: block;
  font-size: 36px;
  font-weight: bold;
  color: #333333;
}

.total-value {
  color: #ffffff;
  font-size: 44px;
}

.loading-tip {
  text-align: center;
  padding: 48px 0;
}

.loading-text {
  font-size: 28px;
  color: #999999;
}

.detail-section {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.section-title {
  display: block;
  font-size: 28px;
  font-weight: bold;
  color: #333333;
  margin-bottom: 16px;
}

.detail-list {
  max-height: 600px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #F0F0F0;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-date {
  font-size: 28px;
  color: #333333;
}

.detail-type {
  font-size: 22px;
  color: #999999;
}

.detail-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-amount {
  font-size: 30px;
  font-weight: bold;
  color: #4A6FA5;
}

.detail-qty {
  font-size: 22px;
  color: #999999;
}

.empty-tip {
  text-align: center;
  padding: 48px 0;
}

.empty-text {
  font-size: 28px;
  color: #999999;
}
</style>
