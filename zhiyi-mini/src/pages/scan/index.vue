<template>
  <View class="scan-page">
    <!-- Step 1: Scan / Manual Input -->
    <View v-if="step === 1" class="step-1">
      <View class="scan-hero">
        <View class="scan-icon-wrap" @tap="handleScan">
          <View class="scan-frame">
            <View class="scan-corner tl" />
            <View class="scan-corner tr" />
            <View class="scan-corner bl" />
            <View class="scan-corner br" />
            <View class="scan-line" />
          </View>
        </View>
        <Text class="scan-hero-title">扫码报工</Text>
        <Text class="scan-hero-desc">扫描扎号二维码或裁床条形码</Text>
        <View class="scan-btn-wrap" @tap="handleScan">
          <Text class="scan-btn-text">开始扫码</Text>
        </View>
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
        <View v-if="scanResult?.scanType === 'BUNDLE'" class="info-row">
          <Text class="info-label">颜色</Text>
          <Text class="info-value">{{ currentColor }}</Text>
        </View>
        <View v-if="scanResult?.scanType === 'BUNDLE'" class="info-row">
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
          <Text class="info-label">总数量</Text>
          <Text class="info-value">{{ (scanResult as CuttingScanResult).cutting.totalQty }}</Text>
        </View>
        <!-- 颜色尺码汇总 -->
        <View v-if="scanResult?.scanType === 'CUTTING'" class="color-size-summary">
          <Text class="summary-label">颜色尺码</Text>
          <View class="color-size-tags">
            <Text v-for="cs in (scanResult as CuttingScanResult).colorSizeMatrix" :key="cs.color + cs.size" class="color-size-tag">
              {{ cs.color }}{{ cs.size }}: {{ cs.qty }}件
            </Text>
          </View>
        </View>
      </View>

      <!-- Color/Size Selection (cutting only) -->
      <View v-if="scanResult?.scanType === 'CUTTING'" class="bundle-section">
        <View class="section-header">
          <Text class="section-title">选择颜色尺码</Text>
          <View class="select-all-btn" @tap="toggleSelectAll">
            <Text class="select-all-text">{{ allColorSizeSelected ? '取消全选' : '全选' }}</Text>
          </View>
        </View>
        <View class="color-size-list">
          <View
            v-for="cs in (scanResult as CuttingScanResult).colorSizeMatrix"
            :key="cs.color + cs.size"
            class="color-size-item"
            :class="{ selected: isColorSizeSelected(cs) }"
            @tap="toggleColorSize(cs)"
          >
            <View class="cs-check">
              <View v-if="isColorSizeSelected(cs)" class="check-box checked-box">
                <Text class="check-checked-icon">✓</Text>
              </View>
              <View v-else class="check-box unchecked-box" />
            </View>
            <View class="cs-info">
              <Text class="cs-name">{{ cs.color }} {{ cs.size }}</Text>
              <Text class="cs-qty">{{ cs.qty }}件</Text>
            </View>
          </View>
        </View>
      </View>

      <!-- Process Checklist -->
      <View class="process-section">
        <Text class="section-title">工序选择</Text>
        <ProcessCheckList :processes="processes" :showMobilePrice="scanResult?.showMobilePrice" :scanType="scanResult?.scanType" @change="onProcessChange" />
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

      <View class="result-btns">
        <Button class="continue-btn" @tap="resetScan">继续扫码</Button>
        <Button class="home-btn" @tap="goHome">返回首页</Button>
      </View>
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
const selectedColorSizes = ref<Array<{ color: string; size: string }>>([])
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
    : ''
})
const currentSize = computed(() => {
  if (!scanResult.value) return ''
  return scanResult.value.scanType === 'BUNDLE'
    ? (scanResult.value as BundleScanResult).bundle.size
    : ''
})

const allColorSizeSelected = computed(() => {
  if (scanResult.value?.scanType !== 'CUTTING') return false
  const matrix = (scanResult.value as CuttingScanResult).colorSizeMatrix
  return matrix.length > 0 && matrix.every(cs => isColorSizeSelected(cs))
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
    // Default: for cutting scans, select all color/size combinations
    if (result.scanType === 'CUTTING') {
      const matrix = (result as CuttingScanResult).colorSizeMatrix
      selectedColorSizes.value = matrix.map(cs => ({ color: cs.color, size: cs.size }))
      selectedBundleIds.value = matrix.flatMap(cs => cs.bundleIds)
    } else {
      selectedBundleIds.value = [(result as BundleScanResult).bundle.id]
      selectedColorSizes.value = []
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

function isColorSizeSelected(cs: { color: string; size: string }): boolean {
  return selectedColorSizes.value.some(s => s.color === cs.color && s.size === cs.size)
}

function toggleColorSize(cs: { color: string; size: string; bundleIds: number[] }) {
  const idx = selectedColorSizes.value.findIndex(s => s.color === cs.color && s.size === cs.size)
  if (idx > -1) {
    selectedColorSizes.value.splice(idx, 1)
    // Remove bundle IDs for this color/size
    cs.bundleIds.forEach(id => {
      const bIdx = selectedBundleIds.value.indexOf(id)
      if (bIdx > -1) selectedBundleIds.value.splice(bIdx, 1)
    })
  } else {
    selectedColorSizes.value.push({ color: cs.color, size: cs.size })
    // Add bundle IDs for this color/size
    cs.bundleIds.forEach(id => {
      if (!selectedBundleIds.value.includes(id)) {
        selectedBundleIds.value.push(id)
      }
    })
  }
}

function toggleSelectAll() {
  if (scanResult.value?.scanType !== 'CUTTING') return
  const matrix = (scanResult.value as CuttingScanResult).colorSizeMatrix
  if (allColorSizeSelected.value) {
    selectedColorSizes.value = []
    selectedBundleIds.value = []
  } else {
    selectedColorSizes.value = matrix.map(cs => ({ color: cs.color, size: cs.size }))
    selectedBundleIds.value = matrix.flatMap(cs => cs.bundleIds)
  }
}

async function handleConfirm() {
  if (selectedBundleIds.value.length === 0) {
    Taro.showToast({ title: '请至少选择一个颜色尺码', icon: 'none' })
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
    Taro.showToast({ title: '上报成功', icon: 'success' })
    // 重置扫码页面，准备下一次扫码
    resetScan()
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

function goHome() {
  Taro.switchTab({ url: '/pages/index/index' })
}

function resetScan() {
  step.value = 1
  scanResult.value = null
  scanCode.value = ''
  selectedBundleIds.value = []
  selectedColorSizes.value = []
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
  padding: 0 32px 40px;
}

.scan-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120px 0 48px;
}

.scan-icon-wrap {
  width: 200px;
  height: 200px;
  border-radius: 40px;
  background: linear-gradient(135deg, #4A6FA5 0%, #3a5f95 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32px;
  box-shadow: 0 12px 40px rgba(74, 111, 165, 0.35);
}

/* 扫码框图标 */
.scan-frame {
  width: 100px;
  height: 100px;
  position: relative;
}
.scan-corner {
  position: absolute;
  width: 24px;
  height: 24px;
  border: 4px solid #fff;
}
.scan-corner.tl { top: 0; left: 0; border-right: none; border-bottom: none; border-radius: 4px 0 0 0; }
.scan-corner.tr { top: 0; right: 0; border-left: none; border-bottom: none; border-radius: 0 4px 0 0; }
.scan-corner.bl { bottom: 0; left: 0; border-right: none; border-top: none; border-radius: 0 0 0 4px; }
.scan-corner.br { bottom: 0; right: 0; border-left: none; border-top: none; border-radius: 0 0 4px 0; }
.scan-line {
  position: absolute;
  top: 50%;
  left: 10%;
  width: 80%;
  height: 3px;
  background: rgba(255,255,255,0.8);
  border-radius: 2px;
  transform: translateY(-50%);
}

.scan-hero-title {
  font-size: 40px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 12px;
}

.scan-hero-desc {
  font-size: 28px;
  color: #9ca3af;
  margin-bottom: 40px;
}

.scan-btn-wrap {
  padding: 20px 80px;
  background: linear-gradient(135deg, #4A6FA5 0%, #3a5f95 100%);
  border-radius: 48px;
  box-shadow: 0 8px 24px rgba(74, 111, 165, 0.3);
}
.scan-btn-text {
  font-size: 32px;
  font-weight: 600;
  color: #ffffff;
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
  color: #4A6FA5;
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
  background: #4A6FA5;
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
  border-left: 6px solid #4A6FA5;
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
  color: #4A6FA5;
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
  color: #4A6FA5;
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
  border-color: #4A6FA5;
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
  background: #4A6FA5;
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
  color: #4A6FA5;
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

.color-size-summary {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.color-size-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.color-size-tag {
  font-size: 24px;
  color: #4A6FA5;
  background: #f0f0ff;
  padding: 6px 12px;
  border-radius: 6px;
}

.color-size-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.color-size-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #f9fafb;
  border-radius: 10px;
  border: 2px solid transparent;
}

.color-size-item.selected {
  border-color: #4A6FA5;
  background: #f0f0ff;
}

.cs-check {
  margin-right: 16px;
  flex-shrink: 0;
}

.cs-info {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cs-name {
  font-size: 28px;
  font-weight: 500;
  color: #4A6FA5;
}

.cs-qty {
  font-size: 26px;
  color: #6b7280;
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
  background: #4A6FA5;
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
  border-left: 6px solid #4A6FA5;
}

.summary-title {
  display: block;
  font-size: 30px;
  font-weight: 600;
  color: #4A6FA5;
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
  color: #4A6FA5;
  font-weight: 500;
}

.summary-value.highlight {
  color: #4A6FA5;
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
  color: #4A6FA5;
}

.breakdown-detail {
  font-size: 24px;
  color: #9ca3af;
  margin-top: 4px;
}

.breakdown-amount {
  font-size: 30px;
  font-weight: 600;
  color: #4A6FA5;
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

.result-btns {
  display: flex;
  gap: 20px;
  margin-top: 16px;
}

.continue-btn {
  flex: 1;
  height: 88px;
  line-height: 88px;
  background: #4A6FA5;
  color: #ffffff;
  font-size: 30px;
  font-weight: 600;
  border-radius: 12px;
  border: none;
}

.continue-btn::after {
  border: none;
}

.home-btn {
  flex: 1;
  height: 88px;
  line-height: 88px;
  background: #f3f4f6;
  color: #374151;
  font-size: 30px;
  font-weight: 600;
  border-radius: 12px;
  border: none;
}

.home-btn::after {
  border: none;
}
</style>
