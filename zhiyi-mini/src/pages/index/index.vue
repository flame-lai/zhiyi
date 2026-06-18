<template>
  <View class="home-page">
    <View class="home-header">
      <View class="header-info">
        <Text class="header-name">{{ userInfo?.employee?.name || '用户' }}</Text>
        <Text class="header-code">{{ userInfo?.employee?.code || '' }}</Text>
      </View>
      <View class="header-logout" @tap="handleLogout">
        <Text class="logout-text">退出</Text>
      </View>
    </View>

    <View class="section" v-if="employeeMenus.length">
      <Text class="section-title">我的工作</Text>
      <View class="menu-grid">
        <View class="menu-item" v-for="menu in employeeMenus" :key="menu.code" @tap="navigateTo(menu.path)">
          <View class="menu-icon" :class="menu.code">
            <Text class="menu-icon-text">{{ getIcon(menu.code) }}</Text>
          </View>
          <Text class="menu-title">{{ menu.title }}</Text>
        </View>
      </View>
    </View>

    <View class="section" v-if="adminMenus.length">
      <Text class="section-title">管理工具</Text>
      <View class="menu-grid">
        <View class="menu-item" v-for="menu in adminMenus" :key="menu.code" @tap="navigateTo(menu.path)">
          <View class="menu-icon admin" :class="menu.code">
            <Text class="menu-icon-text">{{ getIcon(menu.code) }}</Text>
          </View>
          <Text class="menu-title">{{ menu.title }}</Text>
        </View>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'
import { getUserInfo, getMenus, clearAuth, isLoggedIn } from '../../utils/auth'
import { logout } from '../../api/mobileAuth'
import { getProfile } from '../../api/mobileAuth'
import { saveAuth } from '../../utils/request'
import type { MobileMenu } from '../../api/types'

definePageConfig({ navigationBarTitleText: '首页' })

const userInfo = ref<any>(null)
const menus = ref<MobileMenu[]>([])
const employeeMenus = computed(() => menus.value.filter(m => m.group === 'employee'))
const adminMenus = computed(() => menus.value.filter(m => m.group === 'admin'))

useDidShow(async () => {
  if (!isLoggedIn()) { Taro.redirectTo({ url: '/pages/login/index' }); return }
  userInfo.value = getUserInfo()
  menus.value = getMenus()
  try {
    const profile = await getProfile()
    saveAuth({ token: Taro.getStorageSync('satoken'), employee: profile.employee, role: profile.role, menus: profile.menus, permissions: profile.permissions, defaultHomeType: profile.defaultHomeType })
    userInfo.value = getUserInfo()
    menus.value = getMenus()
  } catch {}
})

function getIcon(code: string): string {
  const icons: Record<string, string> = { 'mobile:scan': '📷', 'mobile:wage': '💰', 'mobile:report-records': '📋', 'mobile:dashboard': '📊', 'mobile:anomaly': '⚠️', 'mobile:timed-wage': '⏰', 'mobile:bundle-query': '🔍', 'mobile:order-query': '📦' }
  return icons[code] || '📌'
}

function navigateTo(path: string) {
  if (path.startsWith('/pages/scan') || path.startsWith('/pages/index') || path.startsWith('/pages/wage')) {
    Taro.switchTab({ url: path })
  } else {
    Taro.navigateTo({ url: path })
  }
}

async function handleLogout() {
  try { await logout() } catch {}
  clearAuth()
  Taro.redirectTo({ url: '/pages/login/index' })
}
</script>

<style lang="scss">
.home-page { min-height: 100vh; background: #f5f7fa; padding-bottom: 120px; }
.home-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 48px 32px 32px; display: flex; align-items: center; justify-content: space-between; }
.header-info { flex: 1; }
.header-name { display: block; font-size: 40px; font-weight: 700; color: #fff; }
.header-code { display: block; font-size: 26px; color: rgba(255,255,255,0.8); margin-top: 4px; }
.header-logout { padding: 12px 24px; background: rgba(255,255,255,0.2); border-radius: 8px; }
.logout-text { font-size: 26px; color: #fff; }
.section { padding: 24px 24px 0; }
.section-title { display: block; font-size: 30px; font-weight: 600; color: #374151; margin-bottom: 20px; }
.menu-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
.menu-item { display: flex; flex-direction: column; align-items: center; padding: 20px 0; }
.menu-icon { width: 96px; height: 96px; border-radius: 24px; display: flex; align-items: center; justify-content: center; margin-bottom: 12px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.menu-icon.admin { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.menu-icon-text { font-size: 40px; }
.menu-title { font-size: 24px; color: #374151; font-weight: 500; }
</style>
