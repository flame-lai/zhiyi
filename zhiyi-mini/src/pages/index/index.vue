<template>
  <View class="home-page">
    <View class="header">
      <Text class="welcome">欢迎回来</Text>
      <Text class="employee-code" v-if="employeeCode">{{ employeeCode }}</Text>
    </View>

    <View class="actions">
      <View class="action-card" @tap="goScan">
        <Text class="action-icon"> </Text>
        <Text class="action-title">扫码报工</Text>
        <Text class="action-desc">扫描扎号或裁床条形码</Text>
      </View>

      <View class="action-card" @tap="goWage">
        <Text class="action-icon"> </Text>
        <Text class="action-title">我的工资</Text>
        <Text class="action-desc">查看工资明细与汇总</Text>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'

definePageConfig({
  navigationBarTitleText: '首页',
})

const employeeCode = ref('')

useDidShow(() => {
  const token = Taro.getStorageSync('satoken')
  if (!token) {
    Taro.redirectTo({ url: '/pages/login/index' })
    return
  }
  employeeCode.value = Taro.getStorageSync('employeeCode') || ''
})

function goScan() {
  Taro.switchTab({ url: '/pages/scan/index' })
}

function goWage() {
  Taro.switchTab({ url: '/pages/wage/index' })
}
</script>

<style lang="scss">
.home-page {
  min-height: 100vh;
  background: #F5F7FA;
  padding: 24px;
  box-sizing: border-box;
}

.header {
  background: #4A6FA5;
  border-radius: 12px;
  padding: 40px 24px;
  margin-bottom: 24px;
}

.welcome {
  display: block;
  font-size: 40px;
  font-weight: bold;
  color: #ffffff;
  margin-bottom: 8px;
}

.employee-code {
  display: block;
  font-size: 28px;
  color: rgba(255, 255, 255, 0.8);
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.action-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 32px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.action-icon {
  display: block;
  font-size: 48px;
  margin-bottom: 12px;
}

.action-title {
  display: block;
  font-size: 32px;
  font-weight: bold;
  color: #333333;
  margin-bottom: 8px;
}

.action-desc {
  display: block;
  font-size: 24px;
  color: #999999;
}
</style>
