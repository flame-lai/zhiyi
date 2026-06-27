<template>
  <View class="anomaly-page">
    <!-- Status filter tabs -->
    <View class="status-tabs">
      <View
        v-for="t in statusTabs"
        :key="t.value"
        :class="['status-tab', activeStatus === t.value && 'status-tab-active']"
        @tap="switchStatus(t.value)"
      >
        <Text class="status-tab-text">{{ t.label }}</Text>
      </View>
    </View>

    <View class="content">
      <View v-if="loading" class="loading"><Text class="loading-text">加载中...</Text></View>
      <EmptyState v-else-if="list.length === 0" text="暂无异常记录" />
      <View v-else class="card-list">
        <View v-for="item in list" :key="item.id" class="card">
          <View class="card-head">
            <Text class="card-bundle">{{ item.bundleNo }}</Text>
            <StatusTag :text="statusText(item.status)" :type="statusType(item.status)" />
          </View>

          <View class="info-row">
            <Text class="info-label">订单号</Text>
            <Text class="info-value">{{ item.orderNo }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">前工序</Text>
            <Text class="info-value">{{ item.frontProcessName || '-' }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">后工序</Text>
            <Text class="info-value">{{ item.backProcessName || '-' }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">上报人</Text>
            <Text class="info-value">{{ item.reportEmployeeName }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">上报时间</Text>
            <Text class="info-value">{{ formatDateTime(item.reportTime) }}</Text>
          </View>

          <!-- 已处理的备注 -->
          <View v-if="item.status !== 0 && item.handleRemark" class="info-row">
            <Text class="info-label">处理备注</Text>
            <Text class="info-value">{{ item.handleRemark }}</Text>
          </View>

          <!-- 操作按钮（仅待处理） -->
          <View v-if="item.status === 0" class="action-bar">
            <View class="action-btn ignore-btn" @tap="onIgnore(item)">
              <Text class="action-text">忽略</Text>
            </View>
            <View class="action-btn clear-btn" @tap="onClear(item)">
              <Text class="action-text">清除报工</Text>
            </View>
            <View class="action-btn supplement-btn" @tap="onSupplement(item)">
              <Text class="action-text">补录报工</Text>
            </View>
          </View>
        </View>
      </View>
    </View>

    <!-- 忽略弹窗 -->
    <ConfirmPanel
      :visible="ignorePanel.visible"
      title="忽略预警"
      confirm-text="确认忽略"
      :loading="ignorePanel.loading"
      @confirm="submitIgnore"
      @cancel="ignorePanel.visible = false"
    >
      <View class="form-item">
        <Text class="form-label">忽略原因 <Text class="required">*</Text></Text>
        <Input class="form-input" placeholder="请输入忽略原因" :value="ignorePanel.remark" @input="(e: any) => (ignorePanel.remark = e.detail.value)" />
      </View>
    </ConfirmPanel>

    <!-- 清除弹窗 -->
    <ConfirmPanel
      :visible="clearPanel.visible"
      title="清除报工"
      confirm-text="确认清除"
      :loading="clearPanel.loading"
      @confirm="submitClear"
      @cancel="clearPanel.visible = false"
    >
      <View class="form-item">
        <Text class="form-label">清除原因 <Text class="required">*</Text></Text>
        <Input class="form-input" placeholder="请输入清除原因" :value="clearPanel.reason" @input="(e: any) => (clearPanel.reason = e.detail.value)" />
      </View>
    </ConfirmPanel>

    <!-- 补录弹窗 -->
    <ConfirmPanel
      :visible="supplementPanel.visible"
      title="补录报工"
      confirm-text="确认补录"
      :loading="supplementPanel.loading"
      @confirm="submitSupplement"
      @cancel="supplementPanel.visible = false"
    >
      <View class="form-item">
        <Text class="form-label">工号 <Text class="required">*</Text></Text>
        <Input class="form-input" placeholder="请输入工号" :value="supplementPanel.employeeNo" @input="(e: any) => (supplementPanel.employeeNo = e.detail.value)" />
      </View>
      <View class="form-item">
        <Text class="form-label">数量 <Text class="required">*</Text></Text>
        <Input class="form-input" type="number" placeholder="请输入补录数量" :value="supplementPanel.qty" @input="(e: any) => (supplementPanel.qty = e.detail.value)" />
      </View>
    </ConfirmPanel>
  </View>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Input } from '@tarojs/components'
import { getAnomalies, ignoreAnomaly, clearAnomaly, supplementAnomaly } from '../../../api/mobileAdmin'
import { formatDateTime } from '../../../utils/format'
import { checkAuthAndRedirect } from '../../../utils/auth'
import EmptyState from '../../../components/EmptyState.vue'
import StatusTag from '../../../components/StatusTag.vue'
import ConfirmPanel from '../../../components/ConfirmPanel.vue'
import type { AnomalyRecord } from '../../../api/types'

definePageConfig({
  navigationBarTitleText: '异常预警',
})

const statusTabs = [
  { label: '全部', value: -1 },
  { label: '待处理', value: 0 },
  { label: '已处理', value: 1 },
]

const activeStatus = ref(-1)
const loading = ref(false)
const list = ref<AnomalyRecord[]>([])

// 当前操作的记录ID
const currentId = ref(0)

// 忽略弹窗
const ignorePanel = reactive({ visible: false, loading: false, remark: '' })

// 清除弹窗
const clearPanel = reactive({ visible: false, loading: false, reason: '' })

// 补录弹窗
const supplementPanel = reactive({ visible: false, loading: false, employeeNo: '', qty: '' })

function switchStatus(v: number) {
  if (activeStatus.value === v) return
  activeStatus.value = v
  loadList()
}

function statusText(status: number): string {
  const map: Record<number, string> = { 0: '待处理', 1: '已忽略', 2: '已清除', 3: '已补录' }
  return map[status] || '未知'
}

function statusType(status: number): 'warning' | 'success' | 'default' {
  if (status === 0) return 'warning'
  return 'success'
}

// 忽略
function onIgnore(item: AnomalyRecord) {
  currentId.value = item.id
  ignorePanel.remark = ''
  ignorePanel.visible = true
}

async function submitIgnore() {
  if (!ignorePanel.remark.trim()) {
    Taro.showToast({ title: '请输入忽略原因', icon: 'none' })
    return
  }
  ignorePanel.loading = true
  try {
    await ignoreAnomaly(currentId.value, ignorePanel.remark.trim())
    Taro.showToast({ title: '已忽略', icon: 'success' })
    ignorePanel.visible = false
    await loadList()
  } catch {} finally {
    ignorePanel.loading = false
  }
}

// 清除
function onClear(item: AnomalyRecord) {
  currentId.value = item.id
  clearPanel.reason = ''
  clearPanel.visible = true
}

async function submitClear() {
  if (!clearPanel.reason.trim()) {
    Taro.showToast({ title: '请输入清除原因', icon: 'none' })
    return
  }
  clearPanel.loading = true
  try {
    await clearAnomaly(currentId.value, clearPanel.reason.trim())
    Taro.showToast({ title: '已清除', icon: 'success' })
    clearPanel.visible = false
    await loadList()
  } catch {} finally {
    clearPanel.loading = false
  }
}

// 补录
function onSupplement(item: AnomalyRecord) {
  currentId.value = item.id
  supplementPanel.employeeNo = ''
  supplementPanel.qty = ''
  supplementPanel.visible = true
}

async function submitSupplement() {
  if (!supplementPanel.employeeNo.trim()) {
    Taro.showToast({ title: '请输入工号', icon: 'none' })
    return
  }
  if (!supplementPanel.qty || Number(supplementPanel.qty) <= 0) {
    Taro.showToast({ title: '请输入有效数量', icon: 'none' })
    return
  }
  supplementPanel.loading = true
  try {
    await supplementAnomaly(currentId.value, {
      employeeNo: supplementPanel.employeeNo.trim(),
      qty: Number(supplementPanel.qty),
    })
    Taro.showToast({ title: '已补录', icon: 'success' })
    supplementPanel.visible = false
    await loadList()
  } catch {} finally {
    supplementPanel.loading = false
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await getAnomalies()
    const all = res || []
    if (activeStatus.value === -1) {
      list.value = all
    } else if (activeStatus.value === 0) {
      list.value = all.filter(a => a.status === 0)
    } else {
      list.value = all.filter(a => a.status !== 0)
    }
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
.anomaly-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}



.status-tabs {
  display: flex;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.status-tab {
  flex: 1;
  text-align: center;
  padding: 24px 0;
  position: relative;
}

.status-tab-text {
  font-size: 28px;
  color: #6b7280;
}

.status-tab-active .status-tab-text {
  color: #4A6FA5;
  font-weight: 600;
}

.status-tab-active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48px;
  height: 4px;
  background: #4A6FA5;
  border-radius: 2px;
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

.card-bundle {
  font-size: 30px;
  font-weight: 600;
  color: #4A6FA5;
}

.info-row {
  display: flex;
  margin-bottom: 12px;
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

.info-value.desc {
  color: #4b5563;
}

.action-bar {
  display: flex;
  gap: 16px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f3f4f6;
}

.action-btn {
  flex: 1;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
}

.ignore-btn {
  background: #f3f4f6;
}

.ignore-btn .action-text {
  color: #6b7280;
}

.clear-btn {
  background: #fee2e2;
}

.clear-btn .action-text {
  color: #dc2626;
}

.supplement-btn {
  background: #dcfce7;
}

.supplement-btn .action-text {
  color: #16a34a;
}

.action-text {
  font-size: 26px;
  font-weight: 500;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
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
