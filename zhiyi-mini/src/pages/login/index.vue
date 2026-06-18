<template>
  <View class="login-page">
    <View class="login-header">
      <Text class="login-logo">智衣ERP</Text>
      <Text class="login-subtitle">制衣厂生产管理系统</Text>
    </View>

    <View class="login-tabs">
      <View class="tab-item" :class="{ active: activeTab === 'employee' }" @tap="switchTab('employee')">
        <Text class="tab-text">员工登录</Text>
      </View>
      <View class="tab-item" :class="{ active: activeTab === 'admin' }" @tap="switchTab('admin')">
        <Text class="tab-text">管理员登录</Text>
      </View>
    </View>

    <View class="login-card">
      <View v-if="activeTab === 'employee'" class="login-form">
        <View class="input-group">
          <Text class="input-label">工号 / 登录码</Text>
          <Input
            class="login-input"
            type="text"
            placeholder="请输入工号"
            placeholder-class="input-placeholder"
            :value="loginCode"
            @input="onCodeInput"
            @confirm="handleLogin"
          />
        </View>
        <Button class="login-btn" :loading="loading" @tap="handleLogin">登 录</Button>
      </View>

      <View v-if="activeTab === 'admin'" class="login-form">
        <View class="input-group">
          <Text class="input-label">账号</Text>
          <Input
            class="login-input"
            type="text"
            placeholder="请输入账号"
            placeholder-class="input-placeholder"
            :value="account"
            @input="onAccountInput"
          />
        </View>
        <View class="input-group">
          <Text class="input-label">密码</Text>
          <Input
            class="login-input"
            password
            placeholder="请输入密码"
            placeholder-class="input-placeholder"
            :value="password"
            @input="onPasswordInput"
            @confirm="handleLogin"
          />
        </View>
        <Button class="login-btn" :loading="loading" @tap="handleLogin">登 录</Button>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro from '@tarojs/taro'
import { View, Text, Input, Button } from '@tarojs/components'
import { loginByCode, loginByAccount } from '../../api/mobileAuth'
import { saveAuth } from '../../utils/request'

definePageConfig({ navigationBarTitleText: '登录' })

const activeTab = ref<'employee' | 'admin'>('employee')
const loginCode = ref('')
const account = ref('')
const password = ref('')
const loading = ref(false)

function switchTab(tab: 'employee' | 'admin') {
  if (activeTab.value !== tab) activeTab.value = tab
}

// 用显式事件处理函数，避免 $event.detail.value 在部分机型/渲染时机下取不到值
function onCodeInput(e: any) {
  loginCode.value = e.detail.value
}
function onAccountInput(e: any) {
  account.value = e.detail.value
}
function onPasswordInput(e: any) {
  password.value = e.detail.value
}

async function handleLogin() {
  if (activeTab.value === 'employee') {
    const code = loginCode.value.trim()
    if (!code) { Taro.showToast({ title: '请输入工号', icon: 'none' }); return }
    loading.value = true
    try {
      const res = await loginByCode(code)
      saveAuth(res)
      Taro.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => { Taro.switchTab({ url: '/pages/index/index' }) }, 800)
    } catch {} finally { loading.value = false }
  } else {
    const acc = account.value.trim()
    const pwd = password.value.trim()
    if (!acc || !pwd) { Taro.showToast({ title: '请输入账号和密码', icon: 'none' }); return }
    loading.value = true
    try {
      const res = await loginByAccount(acc, pwd)
      saveAuth(res)
      Taro.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => { Taro.switchTab({ url: '/pages/index/index' }) }, 800)
    } catch {} finally { loading.value = false }
  }
}
</script>

<style lang="scss">
.login-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 48px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.login-header {
  padding-top: 140px;
  margin-bottom: 56px;
  text-align: center;
}

.login-logo {
  display: block;
  font-size: 56px;
  font-weight: 700;
  color: #4A6FA5;
  margin-bottom: 14px;
}

.login-subtitle {
  display: block;
  font-size: 26px;
  color: #9ca3af;
}

.login-tabs {
  display: flex;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 32px;
  border: 2px solid #e5e7eb;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 24px 0;
}

.tab-item.active {
  background: #4A6FA5;
}

.tab-text {
  font-size: 30px;
  font-weight: 500;
  color: #6b7280;
}

.tab-item.active .tab-text {
  color: #ffffff;
}

.login-card {
  background: #fff;
  border-radius: 16px;
  padding: 48px 36px;
  border: 2px solid #e5e7eb;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-label {
  font-size: 26px;
  color: #4b5563;
  font-weight: 500;
}

.login-input {
  width: 100%;
  height: 96px;
  line-height: 96px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 0 24px;
  font-size: 30px;
  color: #4A6FA5;
  box-sizing: border-box;
  background: #ffffff;
}

.input-placeholder {
  color: #c0c4cc;
  font-size: 28px;
}

.login-btn {
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

.login-btn::after {
  border: none;
}
</style>
