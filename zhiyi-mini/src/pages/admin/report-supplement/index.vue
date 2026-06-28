<template>
  <View class="page">
    <!-- Step 1: 扫码 -->
    <View v-if="step === 1" class="step-1">
      <View class="scan-section">
        <Text class="section-title">扎号/条码 <Text class="required">*</Text></Text>
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
          <Text class="scan-desc">扫描扎号二维码或裁床条形码</Text>
          <View class="scan-btn" @tap="handleScan">
            <Text class="scan-btn-text">开始扫码</Text>
          </View>
        </View>
        <View class="manual-wrap">
          <Input
            class="manual-input"
            placeholder="手动输入扎号或条形码"
            :value="scanCode"
            @input="scanCode = $event.detail.value"
            @confirm="onManualResolve"
          />
          <Button class="query-btn" :loading="resolving" @tap="onManualResolve">识别</Button>
        </View>
      </View>
    </View>

    <!-- Step 2: 扎号/工序确认 -->
    <View v-if="step === 2 && scanResult" class="step-2">
      <!-- 员工选择 -->
      <View class="emp-section">
        <Text class="section-label">选择员工 <Text class="required">*</Text></Text>
        <View class="emp-search-wrap">
          <Input
            class="emp-input"
            placeholder="输入工号或姓名搜索"
            :value="searchKeyword"
            @input="onSearchInput"
            @focus="searchFocused = true"
          />
          <View v-if="selectedEmployee" class="emp-tag">
            <Text class="emp-tag-text">{{ selectedEmployee.name }}（{{ selectedEmployee.employeeNo }}）</Text>
            <View class="emp-tag-close" @tap="clearSelected"><Text class="close-x">×</Text></View>
          </View>
          <View v-if="searchFocused && searchResults.length > 0" class="emp-dropdown">
            <View v-for="emp in searchResults" :key="emp.id" class="emp-item" @tap="selectEmployee(emp)">
              <Text class="emp-item-no">{{ emp.employeeNo }}</Text>
              <Text class="emp-item-name">{{ emp.name }}</Text>
            </View>
          </View>
          <View v-if="searchFocused && searchKeyword && searchResults.length === 0 && !searching" class="emp-dropdown">
            <View class="search-empty"><Text class="search-empty-text">未找到匹配员工</Text></View>
          </View>
        </View>
      </View>

      <!-- Info Card -->
      <View class="info-card">
        <View class="info-row">
          <Text class="info-label">订单号</Text>
          <Text class="info-value">{{ orderNo }}</Text>
        </View>
        <View class="info-row">
          <Text class="info-label">款号</Text>
          <Text class="info-value">{{ styleNo }}</Text>
        </View>
        <View v-if="scanResult.scanType === 'BUNDLE'" class="info-row">
          <Text class="info-label">颜色</Text>
          <Text class="info-value">{{ (scanResult as BundleScanResult).bundle.color }}</Text>
        </View>
        <View v-if="scanResult.scanType === 'BUNDLE'" class="info-row">
          <Text class="info-label">尺码</Text>
          <Text class="info-value">{{ (scanResult as BundleScanResult).bundle.size }}</Text>
        </View>
        <View v-if="scanResult.scanType === 'BUNDLE'" class="info-row">
          <Text class="info-label">扎号</Text>
          <Text class="info-value">{{ (scanResult as BundleScanResult).bundle.bundleNo }}</Text>
        </View>
        <View v-if="scanResult.scanType === 'BUNDLE'" class="info-row">
          <Text class="info-label">数量</Text>
          <Text class="info-value">{{ (scanResult as BundleScanResult).bundle.qty }}</Text>
        </View>
        <View v-if="scanResult.scanType === 'CUTTING'" class="info-row">
          <Text class="info-label">床次</Text>
          <Text class="info-value">{{ (scanResult as CuttingScanResult).cutting.bedNo }}</Text>
        </View>
        <View v-if="scanResult.scanType === 'CUTTING'" class="info-row">
          <Text class="info-label">总数量</Text>
          <Text class="info-value">{{ (scanResult as CuttingScanResult).cutting.totalQty }}</Text>
        </View>
        <View v-if="scanResult.scanType === 'CUTTING'" class="color-size-summary">
          <Text class="summary-label">颜色尺码</Text>
          <View class="color-size-tags">
            <Text v-for="cs in (scanResult as CuttingScanResult).colorSizeMatrix" :key="cs.color + cs.size" class="color-size-tag">
              {{ cs.color }}{{ cs.size }}: {{ cs.qty }}件
            </Text>
          </View>
        </View>
      </View>

      <!-- Color/Size Selection (cutting only) -->
      <View v-if="scanResult.scanType === 'CUTTING'" class="cs-section">
        <View class="section-header">
          <Text class="section-title">选择颜色尺码</Text>
          <View class="select-all-btn" @tap="toggleSelectAll">
            <Text class="select-all-text">{{ allSelected ? '取消全选' : '全选' }}</Text>
          </View>
        </View>
        <View class="cs-list">
          <View
            v-for="cs in (scanResult as CuttingScanResult).colorSizeMatrix"
            :key="cs.color + cs.size"
            :class="['cs-item', isColorSizeSelected(cs) && 'selected']"
            @tap="toggleColorSize(cs)"
          >
            <View class="cs-check">
              <View v-if="isColorSizeSelected(cs)" class="check-box checked-box"><Text class="check-icon">✓</Text></View>
              <View v-else class="check-box unchecked-box" />
            </View>
            <View class="cs-info">
              <Text class="cs-name">{{ cs.color }} {{ cs.size }}</Text>
              <Text class="cs-qty">{{ cs.qty }}件</Text>
            </View>
          </View>
        </View>
      </View>

      <!-- Process Selection -->
      <View class="process-section">
        <Text class="section-title">工序选择</Text>
        <ProcessCheckList
          :processes="scanResult.processes"
          :showMobilePrice="scanResult.showMobilePrice"
          :scanType="scanResult.scanType"
          @change="onProcessChange"
        />
      </View>

      <!-- Action Bar -->
      <View class="action-bar">
        <Button class="back-btn" @tap="step = 1">返回</Button>
        <Button class="confirm-btn" :loading="submitting" @tap="handleSubmit">确认补录</Button>
      </View>
    </View>

    <!-- Step 3: 结果 -->
    <View v-if="step === 3" class="step-3">
      <View class="result-success">
        <Text class="success-icon">✅</Text>
        <Text class="success-text">补录成功</Text>
      </View>
      <View class="result-summary">
        <Text class="result-employee">{{ selectedEmployee!.name }}（{{ selectedEmployee!.employeeNo }}）</Text>
        <View class="result-items">
          <View v-for="(item, idx) in resultItems" :key="idx" class="result-item">
            <Text class="result-name">{{ item.processName }}</Text>
            <Text class="result-detail">{{ item.qty }}件</Text>
          </View>
          <View v-if="failedItems.length > 0" class="result-failed">
            <Text class="failed-title">以下工序补录失败：</Text>
            <View v-for="(item, idx) in failedItems" :key="idx" class="failed-item">
              <Text class="failed-text">{{ item.processName }} - {{ item.reason }}</Text>
            </View>
          </View>
        </View>
      </View>
      <View class="result-btns">
        <Button class="continue-btn" @tap="resetAll">继续补录</Button>
        <Button class="home-btn" @tap="goBack">返回</Button>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Input, Button } from '@tarojs/components'
import { resolveScan } from '../../../api/mobileReport'
import { supplementReport, getEmployees } from '../../../api/mobileAdmin'
import { checkAuthAndRedirect } from '../../../utils/auth'
import ProcessCheckList from '../../../components/ProcessCheckList.vue'
import type { BundleScanResult, CuttingScanResult, ProcessReportStatus, EmployeeRecord } from '../../../api/types'

definePageConfig({ navigationBarTitleText: '补录报工' })

// Step
const step = ref(1)

// Employee
const searchKeyword = ref('')
const searchResults = ref<EmployeeRecord[]>([])
const searchFocused = ref(false)
const searching = ref(false)
const selectedEmployee = ref<EmployeeRecord | null>(null)
let searchTimer: ReturnType<typeof setTimeout> | null = null

// Scan
const resolving = ref(false)
const submitting = ref(false)
const scanCode = ref('')
const scanResult = ref<BundleScanResult | CuttingScanResult | null>(null)
const selectedProcessIds = ref<number[]>([])
const selectedColorSizes = ref<Array<{ color: string; size: string }>>([])
const selectedBundleIds = ref<number[]>([])

// Result
const resultItems = ref<Array<{ processName: string; qty: number }>>([])
const failedItems = ref<Array<{ processName: string; reason: string }>>([])

const orderNo = computed(() => {
  if (!scanResult.value) return ''
  return scanResult.value.scanType === 'BUNDLE'
    ? (scanResult.value as BundleScanResult).bundle.orderNo
    : (scanResult.value as CuttingScanResult).cutting.orderNo
})
const styleNo = computed(() => {
  if (!scanResult.value) return ''
  return scanResult.value.scanType === 'BUNDLE'
    ? (scanResult.value as BundleScanResult).bundle.styleNo
    : (scanResult.value as CuttingScanResult).cutting.styleNo
})
const allSelected = computed(() => {
  if (scanResult.value?.scanType !== 'CUTTING') return false
  const matrix = (scanResult.value as CuttingScanResult).colorSizeMatrix
  return matrix.length > 0 && matrix.every(cs => isColorSizeSelected(cs))
})

// Employee search
function onSearchInput(e: any) {
  const val = e.detail.value
  searchKeyword.value = val
  searchResults.value = []
  if (searchTimer) clearTimeout(searchTimer)
  if (!val.trim()) return
  searchTimer = setTimeout(async () => {
    searching.value = true
    try {
      const res = await getEmployees(val.trim(), 1, 10)
      searchResults.value = res?.records || []
    } catch { searchResults.value = [] }
    finally { searching.value = false }
  }, 300)
}

function selectEmployee(emp: EmployeeRecord) {
  selectedEmployee.value = emp
  searchKeyword.value = ''
  searchResults.value = []
  searchFocused.value = false
}

function clearSelected() {
  selectedEmployee.value = null
  searchKeyword.value = ''
  searchResults.value = []
}

// Scan
async function handleScan() {
  try {
    const res = await Taro.scanCode({ scanType: ['qrCode', 'barCode'] })
    const code = res.result
    if (!code) return
    scanCode.value = code
    await resolveCode(code)
  } catch {}
}

async function onManualResolve() {
  const code = scanCode.value.trim()
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
    scanResult.value = result
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
    // handled by request util
    scanResult.value = null
  } finally {
    resolving.value = false
  }
}

// Color/Size selection (cutting)
function isColorSizeSelected(cs: { color: string; size: string }): boolean {
  return selectedColorSizes.value.some(s => s.color === cs.color && s.size === cs.size)
}

function toggleColorSize(cs: { color: string; size: string; bundleIds: number[] }) {
  const idx = selectedColorSizes.value.findIndex(s => s.color === cs.color && s.size === cs.size)
  if (idx > -1) {
    selectedColorSizes.value.splice(idx, 1)
    cs.bundleIds.forEach(id => {
      const bIdx = selectedBundleIds.value.indexOf(id)
      if (bIdx > -1) selectedBundleIds.value.splice(bIdx, 1)
    })
  } else {
    selectedColorSizes.value.push({ color: cs.color, size: cs.size })
    cs.bundleIds.forEach(id => {
      if (!selectedBundleIds.value.includes(id)) selectedBundleIds.value.push(id)
    })
  }
}

function toggleSelectAll() {
  if (scanResult.value?.scanType !== 'CUTTING') return
  const matrix = (scanResult.value as CuttingScanResult).colorSizeMatrix
  if (allSelected.value) {
    selectedColorSizes.value = []
    selectedBundleIds.value = []
  } else {
    selectedColorSizes.value = matrix.map(cs => ({ color: cs.color, size: cs.size }))
    selectedBundleIds.value = matrix.flatMap(cs => cs.bundleIds)
  }
}

// Process
function onProcessChange(ids: number[]) {
  selectedProcessIds.value = ids
}

// Submit
async function handleSubmit() {
  if (!selectedEmployee.value) {
    Taro.showToast({ title: '请选择员工', icon: 'none' })
    return
  }
  if (selectedBundleIds.value.length === 0) {
    Taro.showToast({ title: '请至少选择一个颜色尺码', icon: 'none' })
    return
  }
  if (selectedProcessIds.value.length === 0) {
    Taro.showToast({ title: '请至少选择一个工序', icon: 'none' })
    return
  }

  submitting.value = true
  resultItems.value = []
  failedItems.value = []

  const employeeNo = selectedEmployee.value.employeeNo
  const processNames: Record<number, string> = {}
  scanResult.value!.processes.forEach(p => { processNames[p.processId] = p.processName })

  // 构建 bundleId -> qty 映射
  let bundleQtyMap: Record<number, number> = {}
  if (scanResult.value!.scanType === 'CUTTING') {
    const cutting = scanResult.value as CuttingScanResult
    cutting.bundles.forEach(b => { bundleQtyMap[b.id] = b.qty })
  } else {
    const bundle = (scanResult.value as BundleScanResult).bundle
    bundleQtyMap[bundle.id] = bundle.qty
  }

  // 对每个选中的扎号 × 每个选中的工序，调用补录接口
  for (const bundleId of selectedBundleIds.value) {
    const qty = bundleQtyMap[bundleId] || 1

    for (const processId of selectedProcessIds.value) {
      try {
        await supplementReport({
          employeeNo,
          bundleId,
          processId,
          qty,
        })
        resultItems.value.push({
          processName: processNames[processId] || String(processId),
          qty,
        })
      } catch {
        failedItems.value.push({
          processName: processNames[processId] || String(processId),
          reason: '补录失败',
        })
      }
    }
  }

  submitting.value = false
  step.value = 3

  if (resultItems.value.length > 0) {
    Taro.showToast({ title: `成功补录${resultItems.value.length}条`, icon: 'success' })
  }
}

function resetAll() {
  step.value = 1
  scanResult.value = null
  scanCode.value = ''
  selectedBundleIds.value = []
  selectedColorSizes.value = []
  selectedProcessIds.value = []
  resultItems.value = []
  failedItems.value = []
}

function goBack() {
  Taro.navigateBack()
}

useDidShow(() => {
  if (!checkAuthAndRedirect()) return
})
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  background: #f5f7fa;
}

/* ---- Step 1 ---- */
.step-1 {
  padding: 24px;
}

.section-title {
  display: block;
  font-size: 30px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
}

.required {
  color: #ef4444;
}

/* Scan */
.scan-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.scan-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0 32px;
}

.scan-icon-wrap {
  width: 160px;
  height: 160px;
  border-radius: 32px;
  background: #4A6FA5;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.scan-frame {
  width: 80px;
  height: 80px;
  position: relative;
}
.scan-corner {
  position: absolute;
  width: 20px;
  height: 20px;
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

.scan-desc {
  font-size: 26px;
  color: #9ca3af;
  margin-bottom: 24px;
}

.scan-btn {
  padding: 16px 56px;
  background: #4A6FA5;
  border-radius: 40px;
}

.scan-btn-text {
  font-size: 30px;
  font-weight: 600;
  color: #fff;
}

.manual-wrap {
  display: flex;
  gap: 12px;
}

.manual-input {
  flex: 1;
  height: 80px;
  padding: 0 20px;
  background: #f5f7fa;
  border-radius: 10px;
  font-size: 28px;
  color: #4A6FA5;
  border: 2px solid #e5e7eb;
}

.query-btn {
  height: 80px;
  padding: 0 32px;
  line-height: 80px;
  background: #4A6FA5;
  color: #fff;
  font-size: 28px;
  font-weight: 500;
  border-radius: 10px;
  border: none;
  flex-shrink: 0;
}
.query-btn::after { border: none; }

/* ---- Step 2 ---- */
.step-2 {
  padding: 24px;
  padding-bottom: 160px;
}

.emp-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.section-label {
  display: block;
  font-size: 28px;
  color: #374151;
  font-weight: 600;
  margin-bottom: 16px;
}

.emp-search-wrap {
  position: relative;
}

.emp-input {
  width: 100%;
  height: 80px;
  padding: 0 20px;
  background: #f5f7fa;
  border-radius: 10px;
  font-size: 28px;
  color: #4A6FA5;
  border: 2px solid #e5e7eb;
  box-sizing: border-box;
}

.emp-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 8px 16px;
  background: #e8f0fe;
  border-radius: 8px;
  align-self: flex-start;
}

.emp-tag-text { font-size: 26px; color: #4A6FA5; }
.emp-tag-close { padding: 4px; }
.close-x { font-size: 28px; color: #9ca3af; }

.emp-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-top: 4px;
  max-height: 300px;
  overflow-y: auto;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  z-index: 20;
}

.emp-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.emp-item:last-child { border-bottom: none; }
.emp-item:active { background: #f5f7fa; }

.emp-item-no { font-size: 26px; font-weight: 600; color: #4A6FA5; }
.emp-item-name { font-size: 24px; color: #4b5563; }

.search-empty { padding: 16px; text-align: center; }
.search-empty-text { font-size: 24px; color: #9ca3af; }

.info-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  border-left: 6px solid #4A6FA5;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}
.info-row:last-child { border-bottom: none; }

.info-label { font-size: 28px; color: #6b7280; }
.info-value { font-size: 28px; color: #4A6FA5; font-weight: 500; }

.color-size-summary {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.summary-label {
  display: block;
  font-size: 26px;
  color: #6b7280;
  margin-bottom: 8px;
}

.color-size-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.color-size-tag {
  font-size: 24px;
  color: #4A6FA5;
  background: #f0f0ff;
  padding: 6px 12px;
  border-radius: 6px;
}

/* Color/Size selection */
.cs-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.section-header .section-title { margin-bottom: 0; }

.select-all-btn {
  padding: 8px 20px;
  border-radius: 8px;
  background: #f0f0ff;
}

.select-all-text {
  font-size: 24px;
  color: #4A6FA5;
  font-weight: 500;
}

.cs-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cs-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #f9fafb;
  border-radius: 10px;
  border: 2px solid transparent;
}
.cs-item.selected {
  border-color: #4A6FA5;
  background: #f0f0ff;
}

.cs-check { margin-right: 16px; flex-shrink: 0; }

.check-box {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.checked-box { background: #4A6FA5; }
.check-icon { color: #fff; font-size: 24px; }
.unchecked-box { background: #fff; border: 2px solid #d1d5db; }

.cs-info {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.cs-name { font-size: 28px; font-weight: 500; color: #4A6FA5; }
.cs-qty { font-size: 26px; color: #6b7280; }

/* Process */
.process-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

/* Action Bar */
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 24px;
  padding-bottom: calc(16px + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2px 12px rgba(0,0,0,0.06);
  display: flex;
  gap: 12px;
  align-items: center;
}

.back-btn {
  height: 80px;
  padding: 0 24px;
  line-height: 80px;
  background: #f3f4f6;
  color: #374151;
  font-size: 28px;
  border-radius: 10px;
  border: none;
  flex-shrink: 0;
}
.back-btn::after { border: none; }

.confirm-btn {
  height: 80px;
  padding: 0 32px;
  line-height: 80px;
  background: #4A6FA5;
  color: #fff;
  font-size: 28px;
  font-weight: 600;
  border-radius: 10px;
  border: none;
  flex-shrink: 0;
}
.confirm-btn::after { border: none; }

/* ---- Step 3 ---- */
.step-3 {
  padding: 40px 24px;
}

.result-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0 32px;
}

.success-icon { font-size: 80px; margin-bottom: 16px; }
.success-text { font-size: 36px; font-weight: 700; color: #10b981; }

.result-summary {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.result-employee {
  display: block;
  font-size: 28px;
  color: #4A6FA5;
  font-weight: 600;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.result-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
}

.result-name { font-size: 28px; color: #374151; }
.result-detail { font-size: 28px; color: #4A6FA5; font-weight: 500; }

.result-failed {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #fef2f2;
}

.failed-title {
  font-size: 24px;
  color: #ef4444;
  display: block;
  margin-bottom: 8px;
}

.failed-text {
  font-size: 24px;
  color: #dc2626;
}

.result-btns {
  display: flex;
  gap: 16px;
}

.continue-btn {
  flex: 1;
  height: 88px;
  line-height: 88px;
  background: #4A6FA5;
  color: #fff;
  font-size: 30px;
  font-weight: 600;
  border-radius: 12px;
  border: none;
}
.continue-btn::after { border: none; }

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
.home-btn::after { border: none; }
</style>
