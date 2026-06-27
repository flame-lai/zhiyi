<template>
  <View class="detail-page">
    <!-- Loading -->
    <View v-if="loading" class="loading"><Text class="loading-text">加载中...</Text></View>

    <View v-else-if="detail" class="content">
      <!-- 工人信息 -->
      <View class="info-card">
        <Text class="worker-name">{{ detail.employee.employeeName }}</Text>
        <Text class="worker-no">工号 {{ detail.employee.employeeNo }}</Text>
      </View>

      <!-- 汇总 -->
      <View class="summary-cards">
        <View class="summary-card">
          <Text class="card-label">总数量</Text>
          <Text class="card-value">{{ detail.totalQty }}</Text>
          <Text class="card-unit">件</Text>
        </View>
        <View class="summary-card">
          <Text class="card-label">总金额</Text>
          <Text class="card-value">{{ formatAmount(detail.totalAmount) }}</Text>
        </View>
      </View>

      <!-- 按订单汇总 -->
      <View v-if="detail.byOrder.length > 0" class="section">
        <Text class="section-title">按订单汇总</Text>
        <View class="css-table">
          <View class="css-tr css-thead">
            <View class="css-td" style="width:30%"><Text class="th-text">订单号</Text></View>
            <View class="css-td" style="width:20%"><Text class="th-text">款号</Text></View>
            <View class="css-td css-right" style="width:20%"><Text class="th-text">数量</Text></View>
            <View class="css-td css-right" style="width:30%"><Text class="th-text">金额</Text></View>
          </View>
          <View v-for="item in detail.byOrder" :key="item.orderId" class="css-tr">
            <View class="css-td" style="width:30%"><Text class="td-text">{{ item.orderNo }}</Text></View>
            <View class="css-td" style="width:20%"><Text class="td-text">{{ item.styleNo }}</Text></View>
            <View class="css-td css-right" style="width:20%"><Text class="td-text">{{ item.qty }}</Text></View>
            <View class="css-td css-right" style="width:30%"><Text class="td-text">{{ formatAmount(item.amount) }}</Text></View>
          </View>
        </View>
      </View>

      <!-- 按工序汇总 -->
      <View v-if="detail.byProcess.length > 0" class="section">
        <Text class="section-title">按工序汇总</Text>
        <View class="css-table">
          <View class="css-tr css-thead">
            <View class="css-td" style="width:40%"><Text class="th-text">工序</Text></View>
            <View class="css-td css-right" style="width:30%"><Text class="th-text">数量</Text></View>
            <View class="css-td css-right" style="width:30%"><Text class="th-text">金额</Text></View>
          </View>
          <View v-for="item in detail.byProcess" :key="item.processId" class="css-tr">
            <View class="css-td" style="width:40%"><Text class="td-text">{{ item.processName }}</Text></View>
            <View class="css-td css-right" style="width:30%"><Text class="td-text">{{ item.qty }}</Text></View>
            <View class="css-td css-right" style="width:30%"><Text class="td-text">{{ formatAmount(item.amount) }}</Text></View>
          </View>
        </View>
      </View>

      <!-- 日报工明细 -->
      <View v-if="detail.dailyReports.length > 0" class="section">
        <Text class="section-title">报工明细</Text>
        <View v-for="day in detail.dailyReports" :key="day.date" class="day-group">
          <View class="day-header">
            <Text class="day-date">{{ day.date }}</Text>
            <Text class="day-summary">{{ day.qty }}件 · {{ formatAmount(day.amount) }}</Text>
          </View>
          <View class="day-records">
            <View v-for="rec in day.records" :key="rec.id" class="record-item">
              <View class="record-main">
                <Text class="record-order">{{ rec.orderNo }} · {{ rec.processName }}</Text>
                <Text class="record-amount">{{ formatAmount(rec.amount) }}</Text>
              </View>
              <View class="record-sub">
                <Text class="record-detail">{{ rec.styleNo }} · 扎号{{ rec.bundleNo }} · {{ rec.qty }}件 × ¥{{ rec.price }}</Text>
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
import Taro, { useRouter, useDidShow } from '@tarojs/taro'
import { View, Text } from '@tarojs/components'
import { getWorkerDetail } from '../../../api/mobileAdmin'
import { formatAmount } from '../../../utils/format'
import type { WorkerDetail } from '../../../api/types'

definePageConfig({ navigationBarTitleText: '工人详情' })

const router = useRouter()
const loading = ref(false)
const detail = ref<WorkerDetail | null>(null)

async function loadData() {
  const employeeId = Number(router.params.employeeId)
  const startDate = router.params.startDate
  const endDate = router.params.endDate
  if (!employeeId) return

  loading.value = true
  try {
    detail.value = await getWorkerDetail(employeeId, startDate, endDate)
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

useDidShow(() => {
  loadData()
})
</script>

<style lang="scss">
.detail-page {
  min-height: 100vh;
  background: #f5f7fa;
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

.content {
  padding: 24px;
  padding-bottom: 40px;
}

.info-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 20px;
  border-left: 6px solid #4A6FA5;
}

.worker-name {
  font-size: 36px;
  font-weight: 700;
  color: #4A6FA5;
  display: block;
}

.worker-no {
  font-size: 26px;
  color: #9ca3af;
  display: block;
  margin-top: 8px;
}

.summary-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.summary-card {
  flex: 1;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-label {
  font-size: 24px;
  color: #9ca3af;
}

.card-value {
  font-size: 36px;
  font-weight: 700;
  color: #4A6FA5;
}

.card-unit {
  font-size: 24px;
  color: #9ca3af;
}

.section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 20px;
}

.section-title {
  font-size: 30px;
  font-weight: 600;
  color: #4A6FA5;
  margin-bottom: 16px;
  display: block;
}

.css-table {
  display: table;
  width: 100%;
  border-collapse: collapse;
}

.css-tr {
  display: table-row;
}

.css-thead .css-td {
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.css-td {
  display: table-cell;
  padding: 12px 16px;
  border-right: 1px solid #e5e7eb;
  vertical-align: middle;
}

.css-td:last-child {
  border-right: none;
}

.css-tr:not(.css-thead) {
  border-bottom: 1px solid #f3f4f6;
}

.css-right {
  text-align: right;
}

.th-text {
  font-size: 24px;
  color: #6b7280;
  font-weight: 600;
}

.td-text {
  font-size: 26px;
  color: #374151;
}

.css-right .td-text {
  color: #4A6FA5;
  font-weight: 500;
}

.day-group {
  margin-bottom: 16px;
}

.day-group:last-child {
  margin-bottom: 0;
}

.day-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
}

.day-date {
  font-size: 28px;
  font-weight: 600;
  color: #374151;
}

.day-summary {
  font-size: 24px;
  color: #9ca3af;
}

.day-records {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.record-item {
  padding: 12px 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.record-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.record-order {
  font-size: 26px;
  color: #4A6FA5;
  font-weight: 500;
}

.record-amount {
  font-size: 28px;
  font-weight: 600;
  color: #4A6FA5;
}

.record-sub {
  margin-top: 6px;
}

.record-detail {
  font-size: 22px;
  color: #9ca3af;
}
</style>
