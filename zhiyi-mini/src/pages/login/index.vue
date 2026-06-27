<template>
  <View class="login-page">
    <View class="login-header">
      <Text class="login-logo">智衣ERP</Text>
      <Text class="login-subtitle">制衣厂生产管理系统</Text>
    </View>

    <View class="login-card">
      <View class="login-form">
        <View class="input-group">
          <Text class="input-label">工号</Text>
          <Input
            class="login-input"
            type="text"
            placeholder="请输入工号"
            :value="account"
            @input="account = $event.detail.value"
          />
        </View>
        <View class="input-group">
          <Text class="input-label">密码</Text>
          <Input
            class="login-input"
            type="text"
            password
            placeholder="请输入密码"
            :value="password"
            @input="password = $event.detail.value"
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
import { loginByAccount } from '../../api/mobileAuth'
import { saveAuth } from '../../utils/request'

definePageConfig({ navigationBarTitleText: '登录' })

const account = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  const acc = account.value.trim()
  const pwd = password.value.trim()
  if (!acc) { Taro.showToast({ title: '请输入工号', icon: 'none' }); return }
  if (!pwd) { Taro.showToast({ title: '请输入密码', icon: 'none' }); return }
  loading.value = true
  try {
    const res = await loginByAccount(acc, pwd)
    saveAuth(res)
    Taro.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => { Taro.switchTab({ url: '/pages/index/index' }) }, 800)
  } catch {} finally { loading.value = false }
}
</script>

<style lang="scss">
.login-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 48px 48px;
  box-sizing: border-box;
}

.login-header {
  padding-top: 180px;
  margin-bottom: 72px;
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
  height: 88px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 0 24px;
  font-size: 30px;
  color: #4A6FA5;
  box-sizing: border-box;
  background: #ffffff;
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
