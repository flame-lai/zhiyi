<template>
  <View class="scan-page">
    <!-- Step 1: Scan / Manual Input -->
    <View v-if="step === 1" class="step-1">
      <View class="scan-hero">
        <View class="scan-icon-wrap" @tap="handleScan">
          <Text class="scan-icon-text">📷</Text>
        </View>
        <Text class="scan-hero-title">扫码识别</Text>
        <Text class="scan-hero-desc">扫描扎号QR码或裁床条形码</Text>
      </View>

      <View class="manual-section">
        <Text class="section-title">手动输入</Text>
        <View class="manual-form">
          <Input
            class="manual-input"
            type="text"
            placeholder="输入扎号或条形码"
            :value="manualCode"
            @input="manualCode = $event.detail.value"
            @confirm="handleManualSubmit"
          />
          <Button class="submit-btn" :loading="resolving" @tap="handleManualSubmit">
            提交识别
          </Button>
        </View>
      </View>
    </View>

    <!-- Step 2: Detail & Selection -->
    <View v-if="step === 2" class="step-2">
      <!-- Info Card -->
      <View class="info-card">
        <View class="info-row">
          <Text class="info-label">订单号</Text>
          <Text class="info-value">{{ currentOrderNo }}</Text>
        </View>
        <View class="info-row">
          <Text class="info-label">款号</Text>
          <Text class="info-value">{{ currentStyleNo }}</Text>
        </View>
        <View class="info-row">
          <Text class="info-label">颜色</Text>
          <Text class="info-value">{{ currentColor }}</Text>
        </View>
        <View class="info-row">
          <Text class="info-label">尺码</Text>
          <Text class="info-value">{{ currentSize }}</Text>
        </View>
        <View v-if="scanResult?.scanType === 'BUNDLE'" class="info-row">
          <Text class="info-label">扎号</Text>
          <Text class="info-value">{{ (scanResult as BundleScanResult).bundle.bundleNo }}</Text>
        </View>
        <View v-if="scanResult?.scanType === 'BUNDLE'" class="info-row">
          <Text class="info-label">数量</Text>
          <Text class="info-value">{{ (scanResult as BundleScanResult).bundle.qty }}</Text>
        </View>
        <View v-if="scanResult?.scanType === 'CUTTING'" class="info-row">
          <Text class="info-label">床次</Text>
          <Text class="info-value">{{ (scanResult as CuttingScanResult).cutting.bedNo }}</Text>
        </View>
        <View v-if="scanResult?.scanType === 'CUTTING'" class="info-row">
          <Text class="info-label">总扎数</Text>
          <Text class="info-value">{{ (scanResult as CuttingScanResult).cutting.totalBundleCount }}</Text>
        </View>
      </View>

      <!-- Bundle Selection (cutting only) -->
      <View v-if="scanResult?.scanType === 'CUTTING'" class="bundle-section">
        <View class="section-header">
          <Text class="section-title">选择扎号</Text>
          <View class="select-all-btn" @tap="toggleSelectAll">
            <Text class="select-all-text">{{ allBundlesSelected ? '取消全选' : '全选' }}</Text>
          </View>
        </View>
        <View class="bundle-list">
          <View
            v-for="b in (scanResult as CuttingScanResult).bundles"
            :key="b.id"
            class="bundle-item"
            :class="{ disabled: b.disabled, selected: selectedBundleIds.includes(b.id) }"
            @tap="toggleBundle(b)"
          >
            <View class="bundle-check">
              <View v-if="b.disabled" class="check-box disabled-box">
                <Text class="check-disabled-icon">✗</Text>
              </View>
              <View v-else-if="selectedBundleIds.includes(b.id)" class="check-box checked-box">
                <Text class="check-checked-icon">✓</Text>
              </View>
              <View v-else class="check-box unchecked-box" />
            </View>
            <View class="bundle-info">
              <Text class="bundle-no">{{ b.bundleNo }}</Text>
              <Text class="bundle-qty">{{ b.qty }}件</Text>
            </View>
            <Text v-if="b.disabled" class="bundle-reason">{{ b.disabledReason }}</Text>
          </View>
        </View>
      </View>

      <!-- Process Checklist -->
      <View class="process-section">
        <Text class="section-title">工序选择</Text>
        <ProcessCheckList :processes="processes" @change="onProcessChange" />
      </View>

      <!-- Confirm Button -->
      <View class="action-bar">
        <Button class="back-btn" @tap="goBack">返回</Button>
        <Button class="confirm-btn" :loading="submitting" @tap="handleConfirm">
          确认上报
        </Button>
      </View>
    </View>

    <!-- Step 3: Result -->
    <View v-if="step === 3" class="step-3">
      <View class="result-success">
        <Text class="success-icon">✅</Text>
        <Text class="success-text">上报成功</Text>
      </View>

      <View class="summary-card">
        <Text class="summary-title">上报汇总</Text>
        <View class="summary-row">
          <Text class="summary-label">总数量</Text>
          <Text class="summary-value">{{ submitResult?.totalQty }} 件</Text>
        </View>
        <View class="summary-row">
          <Text class="summary-label">总金额</Text>
          <Text class="summary-value highlight">¥{{ submitResult?.totalAmount.toFixed(2) }}</Text>
        </View>
      </View>

      <View class="breakdown-section" v-if="submitResult?.items?.length">
        <Text class="section-title">工序明细</Text>
        <View v-for="item in submitResult.items" :key="item.processId" class="breakdown-item">
          <View class="breakdown-left">
            <Text class="breakdown-name">{{ item.processName }}</Text>
            <Text class="breakdown-detail">{{ item.qty }}件 × ¥{{ item.price.toFixed(2) }}</Text>
          </View>
          <Text class="breakdown-amount">¥{{ item.amount.toFixed(2) }}</Text>
        </View>
      </View>

      <View class="skipped-section" v-if="submitResult?.skippedItems?.length">
        <Text class="section-title">跳过项目</Text>
        <View v-for="(sk, idx) in submitResult.skippedItems" :key="idx" class="skipped-item">
          <Text class="skipped-main">{{ sk.bundleNo }} - {{ sk.processName }}</Text>
          <Text class="skipped-reason">{{ sk.reason }}</Text>
        </View>
      </View>

      <Button class="continue-btn" @tap="resetScan">继续扫码</Button>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Taro from '@tarojs/taro'
import { resolveScan, submitReport } from '../../api/mobileReport'
import ProcessCheckList from '../../components/ProcessCheckList.vue'
import type { BundleScanResult, CuttingScanResult, ProcessReportStatus, ReportSubmitResult } from '../../api/types'

definePageConfig({ navigationBarTitleText: '扫码报工' })

const step = ref(1)
const resolving = ref(false)
const submitting = ref(false)
const manualCode = ref('')
const scanCode = ref('')
const scanResult = ref<BundleScanResult | CuttingScanResult | null>(null)
const selectedBundleIds = ref<number[]>([])
const selectedProcessIds = ref<number[]>([])
const submitResult = ref<ReportSubmitResult | null>(null)

const processes = computed<ProcessReportStatus[]>(() => scanResult.value?.processes ?? [])

const currentOrderNo = computed(() => {
  if (!scanResult.value) return ''
  return scanResult.value.scanType === 'BUNDLE'
    ? (scanResult.value as BundleScanResult).bundle.orderNo
    : (scanResult.value as CuttingScanResult).cutting.orderNo
})
const currentStyleNo = computed(() => {
  if (!scanResult.value) return ''
  return scanResult.value.scanType === 'BUNDLE'
    ? (scanResult.value as BundleScanResult).bundle.styleNo
    : (scanResult.value as CuttingScanResult).cutting.styleNo
})
const currentColor = computed(() => {
  if (!scanResult.value) return ''
  return scanResult.value.scanType === 'BUNDLE'
    ? (scanResult.value as BundleScanResult).bundle.color
    : (scanResult.value as CuttingScanResult).cutting.color
})
const currentSize = computed(() => {
  if (!scanResult.value) return ''
  return scanResult.value.scanType === 'BUNDLE'
    ? (scanResult.value as BundleScanResult).bundle.size
    : (scanResult.value as CuttingScanResult).cutting.size
})

const allBundlesSelected = computed(() => {
  if (scanResult.value?.scanType !== 'CUTTING') return false
  const cutting = scanResult.value as CuttingScanResult
  const selectable = cutting.bundles.filter(b => !b.disabled)
  return selectable.length > 0 && selectable.every(b => selectedBundleIds.value.includes(b.id))
})

async function handleScan() {
  try {
    const res = await Taro.scanCode({ scanType: ['qrCode', 'barCode'] })
    const code = res.result
    if (!code) return
    await resolveCode(code)
  } catch {
    // user cancelled or scan failed
  }
}

async function handleManualSubmit() {
  const code = manualCode.value.trim()
  if (!code) {
    Taro.showToast({ title: '请输入编码', icon: 'none' })
    return
  }
  await resolveCode(code)
}

async function resolveCode(code: string) {
  resolving.value = true
  try {
    const result = await resolveScan(code)
    scanCode.value = code
    scanResult.value = result
    // Default: for cutting scans, select all non-disabled bundles
    if (result.scanType === 'CUTTING') {
      selectedBundleIds.value = (result as CuttingScanResult).bundles
        .filter(b => !b.disabled)
        .map(b => b.id)
    } else {
      selectedBundleIds.value = [(result as BundleScanResult).bundle.id]
    }
    selectedProcessIds.value = []
    step.value = 2
  } catch {
    // error handled by request util
  } finally {
    resolving.value = false
  }
}

function onProcessChange(ids: number[]) {
  selectedProcessIds.value = ids
}

function toggleBundle(b: { id: number; disabled?: boolean }) {
  if (b.disabled) return
  const idx = selectedBundleIds.value.indexOf(b.id)
  if (idx > -1) {
    selectedBundleIds.value.splice(idx, 1)
  } else {
    selectedBundleIds.value.push(b.id)
  }
}

function toggleSelectAll() {
  if (scanResult.value?.scanType !== 'CUTTING') return
  const cutting = scanResult.value as CuttingScanResult
  const selectable = cutting.bundles.filter(b => !b.disabled)
  if (allBundlesSelected.value) {
    selectedBundleIds.value = []
  } else {
    selectedBundleIds.value = selectable.map(b => b.id)
  }
}

async function handleConfirm() {
  if (selectedBundleIds.value.length === 0) {
    Taro.showToast({ title: '请至少选择一个扎号', icon: 'none' })
    return
  }
  if (selectedProcessIds.value.length === 0) {
    Taro.showToast({ title: '请至少选择一个工序', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    const result = await submitReport({
      scanType: scanResult.value!.scanType,
      code: scanCode.value,
      selectedBundleIds: selectedBundleIds.value,
      selectedProcessIds: selectedProcessIds.value,
    })
    submitResult.value = result
    step.value = 3
  } catch {
    // error handled by request util
  } finally {
    submitting.value = false
  }
}

function goBack() {
  step.value = 1
  scanResult.value = null
  scanCode.value = ''
  selectedBundleIds.value = []
  selectedProcessIds.value = []
  manualCode.value = ''
}

function resetScan() {
  step.value = 1
  scanResult.value = null
  scanCode.value = ''
  selectedBundleIds.value = []
  selectedProcessIds.value = []
  submitResult.value = null
  manualCode.value = ''
}
</script>

<style lang="scss">
.scan-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 40px;
  box-sizing: border-box;
}

/* ---- Step 1 ---- */
.step-1 {
  padding: 40px 32px;
}

.scan-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64px 0 48px;
}

.scan-icon-wrap {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.4);
}

.scan-icon-text {
  font-size: 72px;
}

.scan-hero-title {
  font-size: 36px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 8px;
}

.scan-hero-desc {
  font-size: 26px;
  color: #9ca3af;
}

.manual-section {
  background: #ffffff;
  border-radius: 16px;
  padding: 32px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.section-title {
  display: block;
  font-size: 30px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 20px;
}

.manual-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.manual-input {
  width: 100%;
  height: 88px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 0 24px;
  font-size: 30px;
  box-sizing: border-box;
}

.submit-btn {
  width: 100%;
  height: 88px;
  line-height: 88px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  font-size: 32px;
  font-weight: 600;
  border-radius: 12px;
  border: none;
}

/* ---- Step 2 ---- */
.step-2 {
  padding: 24px 32px;
  padding-bottom: 180px;
}

.info-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
  border-left: 6px solid #667eea;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 28px;
  color: #6b7280;
}

.info-value {
  font-size: 28px;
  color: #1a1a2e;
  font-weight: 500;
}

.bundle-section {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header .section-title {
  margin-bottom: 0;
}

.select-all-btn {
  padding: 8px 20px;
  border-radius: 8px;
  background: rgba(102, 126, 234, 0.1);
}

.select-all-text {
  font-size: 24px;
  color: #667eea;
  font-weight: 500;
}

.bundle-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bundle-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #f9fafb;
  border-radius: 10px;
  border: 2px solid transparent;
}

.bundle-item.selected {
  border-color: #667eea;
  background: #f0f0ff;
}

.bundle-item.disabled {
  opacity: 0.5;
  background: #f3f4f6;
}

.bundle-check {
  margin-right: 16px;
  flex-shrink: 0;
}

.check-box {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.checked-box {
  background: #667eea;
}

.check-checked-icon {
  color: #ffffff;
  font-size: 24px;
}

.unchecked-box {
  background: #ffffff;
  border: 2px solid #d1d5db;
}

.disabled-box {
  background: #ef4444;
}

.check-disabled-icon {
  color: #ffffff;
  font-size: 24px;
}

.bundle-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.bundle-no {
  font-size: 28px;
  font-weight: 500;
  color: #1a1a2e;
}

.bundle-qty {
  font-size: 24px;
  color: #9ca3af;
  margin-top: 4px;
}

.bundle-reason {
  font-size: 22px;
  color: #ef4444;
  margin-left: 12px;
  flex-shrink: 0;
}

.process-section {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 16px;
  padding: 20px 32px;
  padding-bottom: calc(20px + env(safe-area-inset-bottom));
  background: #ffffff;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
}

.back-btn {
  flex: 1;
  height: 88px;
  line-height: 88px;
  background: #f3f4f6;
  color: #374151;
  font-size: 30px;
  font-weight: 500;
  border-radius: 12px;
  border: none;
}

.confirm-btn {
  flex: 2;
  height: 88px;
  line-height: 88px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  font-size: 32px;
  font-weight: 600;
  border-radius: 12px;
  border: none;
}

/* ---- Step 3 ---- */
.step-3 {
  padding: 40px 32px;
}

.result-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0 32px;
}

.success-icon {
  font-size: 80px;
  margin-bottom: 16px;
}

.success-text {
  font-size: 36px;
  font-weight: 700;
  color: #10b981;
}

.summary-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
  border-left: 6px solid #667eea;
}

.summary-title {
  display: block;
  font-size: 30px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 16px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.summary-row:last-child {
  border-bottom: none;
}

.summary-label {
  font-size: 28px;
  color: #6b7280;
}

.summary-value {
  font-size: 28px;
  color: #1a1a2e;
  font-weight: 500;
}

.summary-value.highlight {
  color: #667eea;
  font-weight: 700;
  font-size: 32px;
}

.breakdown-section {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}

.breakdown-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f3f4f6;
}

.breakdown-item:last-child {
  border-bottom: none;
}

.breakdown-left {
  display: flex;
  flex-direction: column;
}

.breakdown-name {
  font-size: 28px;
  font-weight: 500;
  color: #1a1a2e;
}

.breakdown-detail {
  font-size: 24px;
  color: #9ca3af;
  margin-top: 4px;
}

.breakdown-amount {
  font-size: 30px;
  font-weight: 600;
  color: #667eea;
}

.skipped-section {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 32px;
  border-left: 6px solid #f59e0b;
}

.skipped-item {
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.skipped-item:last-child {
  border-bottom: none;
}

.skipped-main {
  display: block;
  font-size: 26px;
  color: #374151;
  font-weight: 500;
}

.skipped-reason {
  display: block;
  font-size: 24px;
  color: #f59e0b;
  margin-top: 4px;
}

.continue-btn {
  width: 100%;
  height: 88px;
  line-height: 88px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  font-size: 32px;
  font-weight: 600;
  border-radius: 12px;
  border: none;
  margin-top: 16px;
}
</style>
