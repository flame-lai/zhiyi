<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>智衣ERP</h1>
        <p>制衣厂生产管理系统</p>
      </div>

      <a-tabs v-model:activeKey="loginType" centered>
        <a-tab-pane key="account" tab="账号密码登录">
          <a-form :model="accountForm" @finish="handleAccountLogin" layout="vertical">
            <a-form-item name="account" :rules="[{ required: true, message: '请输入账号' }]">
              <a-input v-model:value="accountForm.account" placeholder="请输入账号" size="large">
                <template #prefix><UserOutlined /></template>
              </a-input>
            </a-form-item>
            <a-form-item name="password" :rules="[{ required: true, message: '请输入密码' }]">
              <a-input-password v-model:value="accountForm.password" placeholder="请输入密码" size="large">
                <template #prefix><LockOutlined /></template>
              </a-input-password>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" :loading="loading" block size="large">
                登录
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="code" tab="工号登录">
          <a-form :model="codeForm" @finish="handleCodeLogin" layout="vertical">
            <a-form-item name="loginCode" :rules="[{ required: true, message: '请输入工号登录码' }]">
              <a-input v-model:value="codeForm.loginCode" placeholder="请输入工号登录码" size="large">
                <template #prefix><IdcardOutlined /></template>
              </a-input>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" :loading="loading" block size="large">
                登录
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, IdcardOutlined } from '@ant-design/icons-vue'
import { loginByAccount, loginByCode } from '../api/auth'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const loginType = ref('account')

const accountForm = ref({ account: '', password: '' })
const codeForm = ref({ loginCode: '' })

async function handleAccountLogin() {
  loading.value = true
  try {
    const res: any = await loginByAccount(accountForm.value)
    userStore.setToken(res.data.token)
    userStore.factoryName = res.data.factoryName || ''
    userStore.employeeId = res.data.userId
    userStore.userName = res.data.userName || ''
    message.success('登录成功')
    router.push('/')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleCodeLogin() {
  loading.value = true
  try {
    const res: any = await loginByCode(codeForm.value)
    userStore.setToken(res.data.token)
    userStore.factoryName = res.data.factoryName || ''
    userStore.employeeId = res.data.userId
    userStore.userName = res.data.userName || ''
    message.success('登录成功')
    router.push('/')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F5F7FA;
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h1 {
  font-size: 28px;
  color: #4A6FA5;
  margin-bottom: 8px;
}

.login-header p {
  color: #999;
  font-size: 14px;
}
</style>
