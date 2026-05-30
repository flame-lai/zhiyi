<template>
  <ScrollView class="page" scroll-y :refresher-enabled="true" :refresher-triggered="refreshing"
    @refresherrefresh="onRefresh">
    <View class="section">
      <View v-if="list.length === 0 && !loading" class="empty">
        <Text class="empty-text">暂无异常预警</Text>
      </View>

      <View v-for="item in list" :key="item.id" class="card">
        <View class="card-header">
          <Text class="card-order">订单号: {{ item.orderNo }}</Text>
          <View :class="['tag', 'tag-' + item.status]">
            <Text class="tag-text">{{ statusMap[item.status] || item.status }}</Text>
          </View>
        </View>

        <View class="card-body">
          <View class="info-row">
            <Text class="info-label">扎号</Text>
            <Text class="info-value">{{ item.bundleNo }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">工序</Text>
            <Text class="info-value">{{ item.processName }}</Text>
          </View>
          <View class="info-row">
            <Text class="info-label">创建时间</Text>
            <Text class="info-value">{{ item.createdAt }}</Text>
          </View>
        </View>

        <View v-if="item.status === 'pending'" class="card-actions">
          <Button class="btn btn-ignore" @tap="onIgnore(item)">忽略</Button>
          <Button class="btn btn-clear" @tap="onClear(item)">清除</Button>
        </View>
      </View>
    </View>

    <View v-if="loading" class="loading">
      <Text class="loading-text">加载中...</Text>
    </View>
  </ScrollView>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro from '@tarojs/taro'
import { ScrollView, View, Text, Button } from '@tarojs/components'
import { getAnomalyList, ignoreAnomaly, clearAnomaly } from '../../../api/admin'

definePageConfig({
  navigationBarTitleText: '异常预警',
})

interface AnomalyItem {
  id: number
  orderNo: string
  bundleNo: string
  processName: string
  status: string
  createdAt: string
}

const statusMap: Record<string, string> = {
  pending: '待处理',
  ignored: '已忽略',
  cleared: '已清除',
}

const refreshing = ref(false)
const loading = ref(false)
const list = ref<AnomalyItem[]>([])

async function loadList() {
  loading.value = true
  try {
    const res = await getAnomalyList({})
    list.value = res.data || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  await loadList()
  refreshing.value = false
}

function onIgnore(item: AnomalyItem) {
  Taro.showModal({
    title: '确认忽略',
    content: `确定忽略订单 ${item.orderNo} 的异常预警?`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await ignoreAnomaly(item.id)
          Taro.showToast({ title: '已忽略', icon: 'success' })
          await loadList()
        } catch {}
      }
    },
  })
}

function onClear(item: AnomalyItem) {
  Taro.showModal({
    title: '确认清除',
    content: `确定清除订单 ${item.orderNo} 的异常预警?`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await clearAnomaly(item.id)
          Taro.showToast({ title: '已清除', icon: 'success' })
          await loadList()
        } catch {}
      }
    },
  })
}

Taro.useDidShow(() => {
  loadList()
})
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.card-order {
  font-size: 28px;
  font-weight: 600;
  color: #333;
}

.tag {
  padding: 4px 16px;
  border-radius: 8px;
  font-size: 22px;
}

.tag-text {
  font-size: 22px;
}

.tag-pending {
  background: #FFF7E6;
  .tag-text {
    color: #FA8C16;
  }
}

.tag-ignored {
  background: #F5F5F5;
  .tag-text {
    color: #999;
  }
}

.tag-cleared {
  background: #E6F7E6;
  .tag-text {
    color: #52C41A;
  }
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: 26px;
  color: #999;
}

.info-value {
  font-size: 26px;
  color: #333;
}

.card-actions {
  display: flex;
  gap: 20px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.btn {
  flex: 1;
  height: 64px;
  line-height: 64px;
  text-align: center;
  border-radius: 8px;
  font-size: 26px;
  border: none;
  padding: 0;
}

.btn::after {
  border: none;
}

.btn-ignore {
  background: #FFF7E6;
  color: #FA8C16;
}

.btn-clear {
  background: #E6F7E6;
  color: #52C41A;
}

.empty {
  text-align: center;
  padding: 80px 0;
}

.empty-text {
  font-size: 28px;
  color: #999;
}

.loading {
  text-align: center;
  padding: 40px 0;
}

.loading-text {
  font-size: 26px;
  color: #999;
}
</style>
