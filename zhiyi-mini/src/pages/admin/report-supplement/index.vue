<template>
  <View class="report-page">
    <View class="form-card">
      <!-- 工号 -->
      <View class="form-item">
        <Text class="form-label">工号 <Text class="required">*</Text></Text>
        <Input class="form-input" placeholder="请输入工号" :value="form.employeeNo" @input="(e: any) => (form.employeeNo = e.detail.value)" />
      </View>

      <!-- 扫码区域 -->
      <View class="form-item">
        <Text class="form-label">扎号/条码 <Text class="required">*</Text></Text>
        <View class="scan-input-wrap">
          <Input
            class="scan-input"
            placeholder="扫码或手动输入"
            :value="scanCode"
            @input="scanCode = $event.detail.value"
            @confirm="onManualResolve"
          />
          <View class="scan-icon" @tap="handleScan">
            <View class="scan-icon-inner">
              <View class="scan-line-h" />
              <View class="scan-line-v" />
            </View>
          </View>
        </View>
      </View>

      <!-- 扫码结果信息 -->
      <View v-if="bundleInfo" class="info-card">
        <View class="info-row">
          <Text class="info-label">扎号</Text>
          <Text class="info-value">{{ bundleInfo.bundleNo }}</Text>
        </View>
        <View class="info-row">
          <Text class="info-label">订单号</Text>
          <Text class="info-value">{{ bundleInfo.orderNo || '-' }}</Text>
        </View>
        <View class="info-row">
          <Text class="info-label">数量</Text>
          <Text class="info-value">{{ bundleInfo.qty }}</Text>
        </View>
      </View>

      <!-- 工序选择 -->
      <View v-if="processes.length > 0" class="form-item">
        <Text class="form-label">选择工序 <Text class="required">*</Text></Text>
        <View class="process-list">
          <View
            v-for="p in processes"
            :key="p.processId"
            :class="['process-item', selectedProcessId === p.processId && 'process-active', p.status === 'DONE' && 'process-done']"
            @tap="onSelectProcess(p)"
          >
            <Text class="process-name">{{ p.processName }}</Text>
            <Text v-if="p.status === 'DONE'" class="process-status">已报工</Text>
            <Text v-else-if="p.hint" class="process-hint">{{ p.hint }}</Text>
          </View>
        </View>
      </View>

      <!-- 数量 -->
      <View class="form-item">
        <Text class="form-label">数量 <Text class="required">*</Text></Text>
        <Input class="form-input" type="number" placeholder="请输入数量" :value="form.qty" @input="(e: any) => (form.qty = e.detail.value)" />
      </View>

      <Button class="submit-btn" :loading="submitting" :disabled="!canSubmit" @tap="handleSubmit">确认补录</Button>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Input, Button } from '@tarojs/components'
import { resolveScan } from '../../../api/mobileReport'
import { supplementReport } from '../../../api/mobileAdmin'
import { checkAuthAndRedirect } from '../../../utils/auth'
import type { BundleScanResult, ProcessReportStatus } from '../../../api/types'

definePageConfig({ navigationBarTitleText: '补录报工' })

const submitting = ref(false)
const resolving = ref(false)
const scanCode = ref('')
const bundleInfo = ref<{ id: number; bundleNo: string; orderNo: string; qty: number } | null>(null)
const processes = ref<ProcessReportStatus[]>([])
const selectedProcessId = ref(0)

const form = reactive({
  employeeNo: '',
  qty: '',
})

const canSubmit = computed(() => {
  return form.employeeNo.trim() && bundleInfo.value && selectedProcessId.value && form.qty && Number(form.qty) > 0
})

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
  if (!code) return
  await resolveCode(code)
}

async function resolveCode(code: string) {
  resolving.value = true
  try {
    const result = await resolveScan(code)
    if (result.scanType === 'BUNDLE') {
      const bundle = (result as BundleScanResult).bundle
      bundleInfo.value = {
        id: bundle.id,
        bundleNo: bundle.bundleNo,
        orderNo: bundle.orderNo || '',
        qty: bundle.qty,
      }
      processes.value = (result as BundleScanResult).processes || []
      selectedProcessId.value = 0
      // 自动填入数量
      form.qty = String(bundle.qty)
    } else {
      Taro.showToast({ title: '请扫描扎号二维码', icon: 'none' })
    }
  } catch {
    bundleInfo.value = null
    processes.value = []
    selectedProcessId.value = 0
  } finally {
    resolving.value = false
  }
}

function onSelectProcess(p: ProcessReportStatus) {
  if (p.status === 'DONE') {
    Taro.showToast({ title: '该工序已报工', icon: 'none' })
    return
  }
  selectedProcessId.value = p.processId
}

async function handleSubmit() {
  if (!canSubmit.value || submitting.value) return

  submitting.value = true
  try {
    await supplementReport({
      employeeNo: form.employeeNo.trim(),
      bundleId: bundleInfo.value!.id,
      processId: selectedProcessId.value,
      qty: Number(form.qty),
    })
    Taro.showToast({ title: '补录成功', icon: 'success' })
    // 重置
    scanCode.value = ''
    bundleInfo.value = null
    processes.value = []
    selectedProcessId.value = 0
    form.qty = ''
  } catch {} finally {
    submitting.value = false
  }
}

useDidShow(() => {
  if (!checkAuthAndRedirect()) return
})
</script>

<style lang="scss">
.report-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}



.form-card {
  margin: 24px;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.form-label {
  font-size: 28px;
  color: #374151;
  font-weight: 500;
}

.required {
  color: #ef4444;
}

.form-input {
  height: 88px;
  padding: 0 24px;
  background: #f5f7fa;
  border-radius: 12px;
  font-size: 30px;
  color: #4A6FA5;
  border: 2px solid #e5e7eb;
}

.scan-input-wrap {
  display: flex;
  gap: 12px;
  align-items: center;
}

.scan-input {
  flex: 1;
  height: 88px;
  padding: 0 24px;
  background: #f5f7fa;
  border-radius: 12px;
  font-size: 30px;
  color: #4A6FA5;
  border: 2px solid #e5e7eb;
}

.scan-icon {
  width: 88px;
  height: 88px;
  background: #4A6FA5;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.scan-icon-inner {
  width: 48px;
  height: 48px;
  position: relative;
}

.scan-line-h {
  position: absolute;
  top: 50%;
  left: 4px;
  right: 4px;
  height: 3px;
  background: #fff;
  transform: translateY(-50%);
}

.scan-line-v {
  position: absolute;
  left: 50%;
  top: 4px;
  bottom: 4px;
  width: 3px;
  background: #fff;
  transform: translateX(-50%);
}

.info-card {
  background: #f0f9ff;
  border: 2px solid #bae6fd;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.info-row {
  display: flex;
  margin-bottom: 8px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-label {
  width: 120px;
  font-size: 26px;
  color: #0369a1;
  flex-shrink: 0;
}

.info-value {
  flex: 1;
  font-size: 26px;
  color: #0c4a6e;
  font-weight: 500;
}

.process-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.process-item {
  padding: 16px 24px;
  background: #f5f7fa;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.process-active {
  background: #dbeafe;
  border-color: #4A6FA5;
}

.process-done {
  opacity: 0.5;
}

.process-name {
  font-size: 26px;
  color: #374151;
}

.process-active .process-name {
  color: #4A6FA5;
  font-weight: 600;
}

.process-status {
  font-size: 22px;
  color: #16a34a;
}

.process-hint {
  font-size: 22px;
  color: #9ca3af;
}

.submit-btn {
  width: 100%;
  height: 96px;
  line-height: 96px;
  background: #4A6FA5;
  color: #ffffff;
  font-size: 32px;
  font-weight: 600;
  border-radius: 12px;
  border: none;
  margin-top: 16px;
}

.submit-btn::after {
  border: none;
}

.submit-btn[disabled] {
  background: #94a3b8;
}
</style>
