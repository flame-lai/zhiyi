<template>
  <View class="login-page">
    <View class="login-header">
      <Text class="login-logo">智衣ERP</Text>
      <Text class="login-subtitle">制衣厂生产管理系统</Text>
    </View>

    <View class="login-tabs">
      <View class="tab-item" :class="{ active: activeTab === 'employee' }" @tap="activeTab = 'employee'">
        <Text class="tab-text">员工登录</Text>
      </View>
      <View class="tab-item" :class="{ active: activeTab === 'admin' }" @tap="activeTab = 'admin'">
        <Text class="tab-text">管理员登录</Text>
      </View>
    </View>

    <View class="login-card">
      <View v-if="activeTab === 'employee'" class="login-form">
        <View class="input-group">
          <Text class="input-label">工号 / 登录码</Text>
          <Input class="login-input" type="text" placeholder="请输入工号"
            :value="loginCode" @input="loginCode = $event.detail.value" @confirm="handleLogin" />
        </View>
        <Button class="login-btn" :loading="loading" @tap="handleLogin">登 录</Button>
      </View>

      <View v-if="activeTab === 'admin'" class="login-form">
        <View class="input-group">
          <Text class="input-label">账号</Text>
          <Input class="login-input" type="text" placeholder="请输入账号"
            :value="account" @input="account = $event.detail.value" />
        </View>
        <View class="input-group">
          <Text class="input-label">密码</Text>
          <Input class="login-input" type="password" placeholder="请输入密码"
            :value="password" @input="password = $event.detail.value" @confirm="handleLogin" />
        </View>
        <Button class="login-btn" :loading="loading" @tap="handleLogin">登 录</Button>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro from '@tarojs/taro'
import { loginByCode, loginByAccount } from '../../api/mobileAuth'
import { saveAuth } from '../../utils/request'

definePageConfig({ navigationBarTitleText: '登录' })

const activeTab = ref<'employee' | 'admin'>('employee')
const loginCode = ref('')
const account = ref('')
const password = ref('')
const loading = ref(false)

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
.login-page { min-height: 100vh; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 0 40px; box-sizing: border-box; display: flex; flex-direction: column; }
.login-header { padding-top: 120px; margin-bottom: 48px; text-align: center; }
.login-logo { display: block; font-size: 56px; font-weight: 700; color: #fff; margin-bottom: 12px; }
.login-subtitle { display: block; font-size: 28px; color: rgba(255,255,255,0.8); }
.login-tabs { display: flex; background: rgba(255,255,255,0.15); border-radius: 16px 16px 0 0; overflow: hidden; }
.tab-item { flex: 1; text-align: center; padding: 24px 0; }
.tab-item.active { background: #fff; }
.tab-text { font-size: 30px; font-weight: 500; color: rgba(255,255,255,0.7); }
.tab-item.active .tab-text { color: #667eea; }
.login-card { background: #fff; border-radius: 0 0 16px 16px; padding: 40px 32px; }
.login-form { display: flex; flex-direction: column; gap: 24px; }
.input-group { display: flex; flex-direction: column; gap: 8px; }
.input-label { font-size: 26px; color: #6b7280; font-weight: 500; }
.login-input { width: 100%; height: 88px; border: 2px solid #e5e7eb; border-radius: 12px; padding: 0 24px; font-size: 30px; box-sizing: border-box; }
.login-btn { width: 100%; height: 88px; line-height: 88px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; font-size: 32px; font-weight: 600; border-radius: 12px; border: none; margin-top: 16px; }
</style>
