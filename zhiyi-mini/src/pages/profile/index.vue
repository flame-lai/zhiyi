<template>
  <View class="profile-page">
    <View class="info-card">
      <View class="info-row">
        <Text class="info-label">姓名</Text>
        <Text class="info-value">{{ userInfo?.employee?.name || '-' }}</Text>
      </View>
      <View class="info-row">
        <Text class="info-label">工号</Text>
        <Text class="info-value">{{ userInfo?.employee?.code || '-' }}</Text>
      </View>
      <View class="info-row">
        <Text class="info-label">角色</Text>
        <Text class="info-value">{{ userInfo?.role?.name || '-' }}</Text>
      </View>
    </View>

    <View class="menu-section">
      <View class="menu-item" @tap="goChangePassword">
        <Text class="menu-text">修改密码</Text>
        <Text class="menu-arrow">›</Text>
      </View>
      <View class="menu-item" @tap="handleLogout">
        <Text class="menu-text menu-text-danger">退出登录</Text>
        <Text class="menu-arrow">›</Text>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro from '@tarojs/taro'
import { View, Text } from '@tarojs/components'
import { logout } from '../../api/mobileAuth'
import { getUserInfo } from '../../utils/auth'
import { clearAuth } from '../../utils/request'

definePageConfig({ navigationBarTitleText: '个人中心' })

const userInfo = ref<any>(null)

function goChangePassword() {
  Taro.navigateTo({ url: '/pages/change-password/index' })
}

async function handleLogout() {
  try { await logout() } catch {}
  clearAuth()
  Taro.redirectTo({ url: '/pages/login/index' })
}

// 每次进入页面刷新用户信息
Taro.useDidShow(() => {
  userInfo.value = getUserInfo()
})
</script>

<style lang="scss">
.profile-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32px 24px;
  box-sizing: border-box;
}

.info-card {
  background: #fff;
  border-radius: 16px;
  border: 2px solid #e5e7eb;
  overflow: hidden;
  margin-bottom: 32px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28px 32px;

  &:not(:last-child) {
    border-bottom: 1px solid #f3f4f6;
  }
}

.info-label {
  font-size: 28px;
  color: #6b7280;
}

.info-value {
  font-size: 28px;
  color: #374151;
  font-weight: 500;
}

.menu-section {
  background: #ffffff;
  border-radius: 16px;
  border: 2px solid #e5e7eb;
  overflow: hidden;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28px 32px;

  &:not(:last-child) {
    border-bottom: 1px solid #f3f4f6;
  }
}

.menu-text {
  font-size: 30px;
  color: #374151;
}

.menu-text-danger {
  color: #ef4444;
}

.menu-arrow {
  font-size: 32px;
  color: #9ca3af;
}
</style>
