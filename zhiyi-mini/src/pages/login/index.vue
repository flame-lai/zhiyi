<template>
  <View class="login-page">
    <View class="login-card">
      <Text class="login-title">工号登录</Text>
      <Text class="login-subtitle">请输入您的工号进行登录</Text>
      <View class="input-group">
        <Input
          class="login-input"
          type="text"
          placeholder="请输入工号"
          :value="loginCode"
          @input="onInput"
          @confirm="handleLogin"
        />
      </View>
      <Button class="login-btn" :loading="loading" @tap="handleLogin">
        登 录
      </Button>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro from '@tarojs/taro'
import { loginByCode } from '../../api/auth'

definePageConfig({
  navigationBarTitleText: '员工登录',
})

const loginCode = ref('')
const loading = ref(false)

function onInput(e) {
  loginCode.value = e.detail.value
}

async function handleLogin() {
  const code = loginCode.value.trim()
  if (!code) {
    Taro.showToast({ title: '请输入工号', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const res = await loginByCode({ loginCode: code })
    const token = res.data?.satoken || res.data?.token || res.data
    Taro.setStorageSync('satoken', token)
    Taro.setStorageSync('employeeCode', code)
    Taro.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      Taro.switchTab({ url: '/pages/index/index' })
    }, 1000)
  } catch (err) {
    // error toast handled by request util
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss">
.login-page {
  min-height: 100vh;
  background: #F5F7FA;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  box-sizing: border-box;
}

.login-card {
  width: 100%;
  background: #ffffff;
  border-radius: 12px;
  padding: 48px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.login-title {
  display: block;
  text-align: center;
  font-size: 36px;
  font-weight: bold;
  color: #333333;
  margin-bottom: 8px;
}

.login-subtitle {
  display: block;
  text-align: center;
  font-size: 26px;
  color: #999999;
  margin-bottom: 48px;
}

.input-group {
  margin-bottom: 32px;
}

.login-input {
  width: 100%;
  height: 88px;
  border: 1px solid #E0E0E0;
  border-radius: 8px;
  padding: 0 24px;
  font-size: 30px;
  box-sizing: border-box;
}

.login-btn {
  width: 100%;
  height: 88px;
  line-height: 88px;
  background: #4A6FA5;
  color: #ffffff;
  font-size: 32px;
  border-radius: 8px;
  border: none;
}
</style>
