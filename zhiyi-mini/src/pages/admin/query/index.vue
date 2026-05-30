<template>
  <View class="page">
    <View class="section">
      <!-- Query form -->
      <View class="card">
        <View class="card-header">
          <Text class="card-title">扎号查询</Text>
        </View>

        <View class="query-form">
          <View class="input-row">
            <Input class="query-input" type="number" placeholder="请输入扎号ID" :value="bundleId" @input="e => bundleId = e.detail.value" />
            <Button class="query-btn" @tap="onQuery">查询</Button>
          </View>
          <Button class="scan-btn" @tap="onScan">
            <Text class="scan-text">扫码查询</Text>
          </Button>
        </View>
      </View>

      <!-- Result -->
      <View v-if="bundle" class="card result-card">
        <View class="card-header">
          <Text class="card-title">扎号详情</Text>
        </View>

        <View class="detail-list">
          <View class="detail-item">
            <Text class="detail-label">扎号</Text>
            <Text class="detail-value">{{ bundle.id }}</Text>
          </View>
          <View class="detail-item">
            <Text class="detail-label">数量</Text>
            <Text class="detail-value">{{ bundle.quantity }}</Text>
          </View>
          <View class="detail-item">
            <Text class="detail-label">状态</Text>
            <View :class="['status-tag', 'status-' + bundle.status]">
              <Text class="status-text">{{ statusMap[bundle.status] || bundle.status }}</Text>
            </View>
          </View>
          <View class="detail-item">
            <Text class="detail-label">裁床计划ID</Text>
            <Text class="detail-value">{{ bundle.cuttingPlanId }}</Text>
          </View>
        </View>
      </View>

      <!-- Empty state after query -->
      <View v-if="queried && !bundle" class="card">
        <View class="empty">
          <Text class="empty-text">未找到该扎号信息</Text>
        </View>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro from '@tarojs/taro'
import { View, Text, Input, Button } from '@tarojs/components'
import { getBundleById } from '../../../api/admin'

definePageConfig({
  navigationBarTitleText: '扎号查询',
})

interface BundleInfo {
  id: number
  quantity: number
  status: string
  cuttingPlanId: number
}

const statusMap: Record<string, string> = {
  pending: '待生产',
  cutting: '裁剪中',
  completed: '已完成',
  issued: '已发料',
}

const bundleId = ref('')
const bundle = ref<BundleInfo | null>(null)
const queried = ref(false)

async function queryBundle(id: string) {
  const numId = parseInt(id)
  if (!numId || numId <= 0) {
    Taro.showToast({ title: '请输入有效的扎号ID', icon: 'none' })
    return
  }

  queried.value = false
  bundle.value = null

  try {
    const res = await getBundleById(numId)
    bundle.value = res.data || null
    queried.value = true
  } catch {
    bundle.value = null
    queried.value = true
  }
}

function onQuery() {
  queryBundle(bundleId.value)
}

function onScan() {
  Taro.scanCode({
    onlyFromCamera: false,
    scanType: ['qrCode', 'barCode'],
    success: (res) => {
      const scannedId = res.result
      if (scannedId) {
        bundleId.value = scannedId
        queryBundle(scannedId)
      }
    },
    fail: () => {
      Taro.showToast({ title: '扫码取消', icon: 'none' })
    },
  })
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  background: #F5F7FA;
}

.section {
  padding: 24px;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
}

.card-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.card-title {
  font-size: 30px;
  font-weight: 600;
  color: #333;
}

.query-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-row {
  display: flex;
  gap: 16px;
}

.query-input {
  flex: 1;
  height: 80px;
  padding: 0 20px;
  background: #F5F7FA;
  border-radius: 8px;
  font-size: 28px;
  color: #333;
}

.query-btn {
  width: 140px;
  height: 80px;
  line-height: 80px;
  background: #4A6FA5;
  color: #fff;
  font-size: 28px;
  font-weight: 500;
  border-radius: 8px;
  border: none;
  padding: 0;
}

.query-btn::after {
  border: none;
}

.scan-btn {
  height: 80px;
  line-height: 80px;
  background: #fff;
  border: 2px solid #4A6FA5;
  border-radius: 8px;
  padding: 0;
}

.scan-btn::after {
  border: none;
}

.scan-text {
  font-size: 28px;
  color: #4A6FA5;
  font-weight: 500;
}

.result-card {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.detail-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-label {
  font-size: 26px;
  color: #999;
}

.detail-value {
  font-size: 28px;
  color: #333;
  font-weight: 500;
}

.status-tag {
  padding: 4px 16px;
  border-radius: 8px;
}

.status-text {
  font-size: 24px;
}

.status-pending {
  background: #FFF7E6;
  .status-text {
    color: #FA8C16;
  }
}

.status-cutting {
  background: #E6F0FF;
  .status-text {
    color: #4A6FA5;
  }
}

.status-completed {
  background: #E6F7E6;
  .status-text {
    color: #52C41A;
  }
}

.status-issued {
  background: #F0F0F0;
  .status-text {
    color: #666;
  }
}

.empty {
  text-align: center;
  padding: 40px 0;
}

.empty-text {
  font-size: 28px;
  color: #999;
}
</style>
