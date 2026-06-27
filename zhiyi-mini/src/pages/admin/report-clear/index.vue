<template>
  <View class="report-page">
    <!-- 搜索栏 -->
    <View class="filter-bar">
      <View class="search-wrap">
        <Input
          class="search-input"
          placeholder="搜索工号/姓名/扎号"
          :value="keyword"
          @input="keyword = $event.detail.value"
          @confirm="loadList"
        />
        <View class="scan-btn" @tap="handleScan">
          <View class="scan-icon-mini">
            <View class="scan-line-h" />
            <View class="scan-line-v" />
          </View>
        </View>
        <View class="search-btn" @tap="loadList"><Text class="search-btn-text">搜索</Text></View>
      </View>
    </View>

    <View class="content">
      <View v-if="loading" class="loading"><Text class="loading-text">加载中...</Text></View>
      <EmptyState v-else-if="list.length === 0" text="暂无报工记录" />
      <View v-else class="card-list">
        <View v-for="item in list" :key="item.id" class="card">
          <View class="card-head">
            <View class="card-head-left">
              <Text class="card-title">{{ item.employeeName }}</Text>
              <Text class="card-sub">工号 {{ item.employeeNo }}</Text>
            </View>
            <View class="clear-btn" @tap="onClear(item)">
              <Text class="clear-text">清除</Text>
            </View>
          </View>

          <View class="card-stats">
            <View class="stat">
              <Text class="stat-label">扎号</Text>
              <Text class="stat-value">{{ item.bundleNo || '-' }}</Text>
            </View>
            <View class="stat">
              <Text class="stat-label">工序</Text>
              <Text class="stat-value">{{ item.processName || '-' }}</Text>
            </View>
            <View class="stat">
              <Text class="stat-label">数量</Text>
              <Text class="stat-value">{{ item.qty }}</Text>
            </View>
            <View class="stat">
              <Text class="stat-label">金额</Text>
              <Text class="stat-value highlight">¥{{ item.amount }}</Text>
            </View>
          </View>

          <View class="card-footer">
            <Text class="footer-label">订单</Text>
            <Text class="footer-value">{{ item.orderNo || '-' }}</Text>
            <Text class="footer-time">{{ item.reportTime }}</Text>
          </View>
        </View>
      </View>
    </View>

    <!-- 清除确认弹窗 -->
    <ConfirmPanel
      :visible="panelVisible"
      title="确认清除报工"
      confirm-text="确认清除"
      :loading="submitting"
      @confirm="submitClear"
      @cancel="panelVisible = false"
    >
      <View class="confirm-info">
        <Text class="confirm-text">确定清除 {{ currentItem?.employeeName }} 的报工记录？</Text>
        <Text class="confirm-detail">{{ currentItem?.processName }} × {{ currentItem?.qty }}件</Text>
      </View>
      <View class="form-item">
        <Text class="form-label">清除原因 <Text class="required">*</Text></Text>
        <Input class="form-input" placeholder="请输入清除原因" :value="clearReason" @input="(e: any) => (clearReason = e.detail.value)" />
      </View>
    </ConfirmPanel>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Input } from '@tarojs/components'
import { getReports, clearReport } from '../../../api/mobileAdmin'
import { checkAuthAndRedirect } from '../../../utils/auth'
import EmptyState from '../../../components/EmptyState.vue'
import ConfirmPanel from '../../../components/ConfirmPanel.vue'
import type { AdminReportRecord } from '../../../api/types'

definePageConfig({ navigationBarTitleText: '清除报工' })

const keyword = ref('')
const loading = ref(false)
const list = ref<AdminReportRecord[]>([])
const panelVisible = ref(false)
const submitting = ref(false)
const currentItem = ref<AdminReportRecord | null>(null)
const clearReason = ref('')

async function handleScan() {
  try {
    const res = await Taro.scanCode({ scanType: ['qrCode', 'barCode'] })
    const code = res.result
    if (!code) return
    keyword.value = code
    await loadList()
  } catch {}
}

function onClear(item: AdminReportRecord) {
  currentItem.value = item
  clearReason.value = ''
  panelVisible.value = true
}

async function submitClear() {
  if (!clearReason.value.trim()) {
    Taro.showToast({ title: '请输入清除原因', icon: 'none' })
    return
  }
  if (!currentItem.value) return
  submitting.value = true
  try {
    await clearReport(currentItem.value.id, clearReason.value.trim())
    Taro.showToast({ title: '已清除', icon: 'success' })
    panelVisible.value = false
    await loadList()
  } catch {} finally {
    submitting.value = false
  }
}

async function loadList() {
  loading.value = true
  try {
    const kw = keyword.value.trim() || undefined
    const res = await getReports(kw)
    list.value = res || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

useDidShow(() => {
  if (!checkAuthAndRedirect()) return
  loadList()
})
</script>

<style lang="scss">
.report-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}



.filter-bar {
  display: flex;
  padding: 20px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.search-wrap {
  flex: 1;
  display: flex;
  gap: 12px;
}

.search-input {
  flex: 1;
  height: 64px;
  padding: 0 20px;
  background: #f5f7fa;
  border-radius: 10px;
  font-size: 26px;
  color: #4A6FA5;
}

.scan-btn {
  width: 64px;
  height: 64px;
  background: #4A6FA5;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.scan-icon-mini {
  width: 36px;
  height: 36px;
  position: relative;
}

.scan-line-h {
  position: absolute;
  top: 50%;
  left: 2px;
  right: 2px;
  height: 2px;
  background: #fff;
  transform: translateY(-50%);
}

.scan-line-v {
  position: absolute;
  left: 50%;
  top: 2px;
  bottom: 2px;
  width: 2px;
  background: #fff;
  transform: translateX(-50%);
}

.search-btn {
  height: 64px;
  padding: 0 28px;
  background: #4A6FA5;
  border-radius: 10px;
  display: flex;
  align-items: center;
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

.card-head-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 30px;
  font-weight: 600;
  color: #4A6FA5;
}

.card-sub {
  font-size: 24px;
  color: #9ca3af;
}

.clear-btn {
  padding: 10px 24px;
  background: #fee2e2;
  border-radius: 8px;
}

.clear-text {
  font-size: 26px;
  color: #dc2626;
  font-weight: 500;
}

.card-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.stat {
  flex: 1;
  min-width: 40%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 24px;
  color: #9ca3af;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #4A6FA5;
}

.stat-value.highlight {
  color: #dc2626;
}

.card-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
  display: flex;
  align-items: center;
  gap: 12px;
}

.footer-label {
  font-size: 24px;
  color: #9ca3af;
}

.footer-value {
  font-size: 24px;
  color: #4b5563;
}

.footer-time {
  font-size: 22px;
  color: #9ca3af;
  margin-left: auto;
}

.confirm-info {
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 10px;
}

.confirm-text {
  font-size: 28px;
  color: #374151;
  display: block;
  margin-bottom: 8px;
}

.confirm-detail {
  font-size: 24px;
  color: #6b7280;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-label {
  font-size: 26px;
  color: #4b5563;
  font-weight: 500;
}

.required {
  color: #ef4444;
}

.form-input {
  height: 80px;
  padding: 0 20px;
  background: #f5f7fa;
  border-radius: 10px;
  font-size: 28px;
  color: #4A6FA5;
}
</style>
