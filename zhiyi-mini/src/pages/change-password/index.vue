<template>
  <View class="change-pwd-page">
    <View class="form-card">
      <View class="input-group">
        <Text class="input-label">旧密码</Text>
        <Input
          class="form-input"
          type="text"
          password
          placeholder="请输入旧密码"
          :value="oldPassword"
          @input="oldPassword = $event.detail.value"
        />
      </View>
      <View class="input-group">
        <Text class="input-label">新密码</Text>
        <Input
          class="form-input"
          type="text"
          password
          placeholder="请输入新密码（至少4位）"
          :value="newPassword"
          @input="newPassword = $event.detail.value"
        />
      </View>
      <View class="input-group">
        <Text class="input-label">确认新密码</Text>
        <Input
          class="form-input"
          type="text"
          password
          placeholder="请再次输入新密码"
          :value="confirmPassword"
          @input="confirmPassword = $event.detail.value"
          @confirm="handleSubmit"
        />
      </View>
      <Button class="submit-btn" :loading="loading" @tap="handleSubmit">确认修改</Button>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro from '@tarojs/taro'
import { View, Text, Input, Button } from '@tarojs/components'
import { changePassword } from '../../api/mobileAuth'

definePageConfig({ navigationBarTitleText: '修改密码' })

const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)

async function handleSubmit() {
  const oldPwd = oldPassword.value.trim()
  const newPwd = newPassword.value.trim()
  const confirmPwd = confirmPassword.value.trim()

  if (!oldPwd) { Taro.showToast({ title: '请输入旧密码', icon: 'none' }); return }
  if (!newPwd) { Taro.showToast({ title: '请输入新密码', icon: 'none' }); return }
  if (newPwd.length < 4) { Taro.showToast({ title: '新密码至少4位', icon: 'none' }); return }
  if (newPwd !== confirmPwd) { Taro.showToast({ title: '两次输入的密码不一致', icon: 'none' }); return }
  if (oldPwd === newPwd) { Taro.showToast({ title: '新密码不能与旧密码相同', icon: 'none' }); return }

  loading.value = true
  try {
    await changePassword(oldPwd, newPwd)
    Taro.showToast({ title: '密码修改成功', icon: 'success' })
    setTimeout(() => { Taro.navigateBack() }, 800)
  } catch {} finally { loading.value = false }
}
</script>

<style lang="scss">
.change-pwd-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32px 24px;
  box-sizing: border-box;
}

.form-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  border: 2px solid #e5e7eb;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 32px;
}

.input-label {
  font-size: 26px;
  color: #4b5563;
  font-weight: 500;
}

.form-input {
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
</style>
