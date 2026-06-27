<template>
  <View class="home-page">
    <View class="home-header">
      <View class="header-info">
        <Text class="header-name">{{ userInfo?.employee?.name || '用户' }}</Text>
        <Text class="header-code">工号 {{ userInfo?.employee?.code || '-' }}</Text>
      </View>
      <View class="header-logout" @tap="handleLogout">
        <Text class="logout-text">退出</Text>
      </View>
    </View>

    <!-- 员工功能 -->
    <View class="section" v-if="featureMenus.length">
      <Text class="section-title">常用功能</Text>
      <View class="menu-grid">
        <View class="menu-item" v-for="menu in featureMenus" :key="menu.code" @tap="navigateTo(menu.path)">
          <View class="menu-icon" :style="{ background: getIconBg(menu.code) }">
            <View class="icon-inner">
              <View class="icon-shape" :class="getIconShape(menu.code)" />
            </View>
          </View>
          <Text class="menu-title">{{ menu.title }}</Text>
        </View>
      </View>
    </View>

    <!-- 管理功能 -->
    <View class="section" v-if="adminMenus.length">
      <Text class="section-title">管理工具</Text>
      <View class="menu-grid">
        <View class="menu-item" v-for="menu in adminMenus" :key="menu.code" @tap="navigateTo(menu.path)">
          <View class="menu-icon" :style="{ background: getIconBg(menu.code) }">
            <View class="icon-inner">
              <View class="icon-shape" :class="getIconShape(menu.code)" />
            </View>
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
const featureMenus = computed(() => menus.value.filter(m => m.group === 'employee' && m.code.startsWith('mobile:')))
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

function getIconBg(code: string): string {
  const bg: Record<string, string> = {
    'mobile:scan': 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
    'mobile:wage': 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
    'mobile:report-records': 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
    'mobile:dashboard': 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)',
    'mobile:anomaly': 'linear-gradient(135deg, #f97316 0%, #ea580c 100%)',
    'mobile:timed-wage': 'linear-gradient(135deg, #06b6d4 0%, #0891b2 100%)',
    'mobile:bundle-query': 'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)',
    'mobile:order-query': 'linear-gradient(135deg, #6366f1 0%, #4f46e5 100%)',
    'mobile:employee': 'linear-gradient(135deg, #ec4899 0%, #db2777 100%)',
  }
  // 兼容 mobile:admin:xxx 格式
  const short = code.replace('mobile:admin:', 'mobile:')
  return bg[code] || bg[short] || 'linear-gradient(135deg, #6b7280 0%, #4b5563 100%)'
}

function getIconShape(code: string): string {
  const shapes: Record<string, string> = {
    'mobile:scan': 'icon-scan',
    'mobile:wage': 'icon-wallet',
    'mobile:report-records': 'icon-clipboard',
    'mobile:admin:dashboard': 'icon-chart-bar',
    'mobile:admin:anomaly': 'icon-alert',
    'mobile:admin:timed-wage': 'icon-clock',
    'mobile:admin:bundle-query': 'icon-search',
    'mobile:admin:order-query': 'icon-package',
    'mobile:admin:employee': 'icon-users',
    'mobile:admin:report-clear': 'icon-trash',
    'mobile:admin:report-supplement': 'icon-plus',
    'mobile:dashboard': 'icon-chart-bar',
    'mobile:anomaly': 'icon-alert',
    'mobile:timed-wage': 'icon-clock',
    'mobile:bundle-query': 'icon-search',
    'mobile:order-query': 'icon-package',
  }
  return shapes[code] || 'icon-default'
}

function navigateTo(path: string) {
  if (!path) { Taro.showToast({ title: '功能开发中', icon: 'none' }); return }
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
.home-header { background: #4A6FA5; padding: 48px 32px 36px; display: flex; align-items: center; justify-content: space-between; }
.header-info { flex: 1; }
.header-name { display: block; font-size: 40px; font-weight: 700; color: #fff; }
.header-code { display: block; font-size: 26px; color: rgba(255,255,255,0.8); margin-top: 6px; }
.header-logout { padding: 12px 28px; background: rgba(255,255,255,0.2); border-radius: 24px; }
.logout-text { font-size: 26px; color: #fff; }

.section { padding: 28px 24px 0; }
.section-title { display: block; font-size: 30px; font-weight: 600; color: #1f2937; margin-bottom: 20px; padding-left: 4px; }

.menu-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px 16px; }
.menu-item { display: flex; flex-direction: column; align-items: center; }

.menu-icon {
  width: 104px;
  height: 104px;
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
}

.icon-inner {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ===== 用纯 CSS 画的图标 ===== */

/* 扫码：十字准星 */
.icon-scan {
  width: 36px; height: 36px;
  border: 4px solid #fff;
  border-radius: 4px;
  position: relative;
}
.icon-scan::before {
  content: '';
  position: absolute;
  top: 50%; left: 50%;
  width: 16px; height: 2px;
  background: #fff;
  transform: translate(-50%, -50%);
}
.icon-scan::after {
  content: '';
  position: absolute;
  top: 50%; left: 50%;
  width: 2px; height: 16px;
  background: #fff;
  transform: translate(-50%, -50%);
}

/* 工资：钱包 */
.icon-wallet {
  width: 38px; height: 28px;
  border: 3px solid #fff;
  border-radius: 6px;
  position: relative;
}
.icon-wallet::before {
  content: '';
  position: absolute;
  top: 4px; right: -3px;
  width: 12px; height: 12px;
  border: 3px solid #fff;
  border-radius: 0 6px 6px 0;
  border-left: none;
}

/* 记录：文档 */
.icon-clipboard {
  width: 30px; height: 36px;
  border: 3px solid #fff;
  border-radius: 4px;
  position: relative;
}
.icon-clipboard::before {
  content: '';
  position: absolute;
  top: 10px; left: 6px;
  width: 14px; height: 2px;
  background: #fff;
  box-shadow: 0 6px 0 #fff, 0 12px 0 #fff;
}

/* 看板：柱状图 */
.icon-chart-bar {
  width: 36px; height: 36px;
  position: relative;
}
.icon-chart-bar::before {
  content: '';
  position: absolute;
  bottom: 0; left: 2px;
  width: 8px; height: 16px;
  background: rgba(255,255,255,0.6);
  border-radius: 2px 2px 0 0;
  box-shadow: 12px 4px 0 rgba(255,255,255,0.9), 24px -2px 0 rgba(255,255,255,0.7);
}

/* 预警：三角感叹号 */
.icon-alert {
  width: 0; height: 0;
  border-left: 20px solid transparent;
  border-right: 20px solid transparent;
  border-bottom: 34px solid #fff;
  position: relative;
  opacity: 0.9;
}
.icon-alert::after {
  content: '!';
  position: absolute;
  top: 12px; left: -4px;
  width: 8px; text-align: center;
  font-size: 20px; font-weight: 700;
  color: #ea580c;
}

/* 计时：时钟 */
.icon-clock {
  width: 36px; height: 36px;
  border: 3px solid #fff;
  border-radius: 50%;
  position: relative;
}
.icon-clock::before {
  content: '';
  position: absolute;
  top: 8px; left: 50%;
  width: 2px; height: 12px;
  background: #fff;
  transform-origin: bottom center;
  transform: translateX(-50%);
}
.icon-clock::after {
  content: '';
  position: absolute;
  top: 14px; left: 50%;
  width: 10px; height: 2px;
  background: #fff;
  transform-origin: left center;
}

/* 查询：放大镜 */
.icon-search {
  width: 28px; height: 28px;
  border: 4px solid #fff;
  border-radius: 50%;
  position: relative;
}
.icon-search::after {
  content: '';
  position: absolute;
  bottom: -12px; right: -8px;
  width: 4px; height: 14px;
  background: #fff;
  transform: rotate(-45deg);
  border-radius: 2px;
}

/* 订单：包裹 */
.icon-package {
  width: 34px; height: 28px;
  border: 3px solid #fff;
  border-radius: 4px;
  position: relative;
}
.icon-package::before {
  content: '';
  position: absolute;
  top: -3px; left: 50%;
  width: 34px; height: 3px;
  background: #fff;
  transform: translateX(-50%);
}
.icon-package::after {
  content: '';
  position: absolute;
  top: 50%; left: 50%;
  width: 14px; height: 10px;
  border: 2px solid #fff;
  border-radius: 2px;
  transform: translate(-50%, -50%);
}

/* 清除：垃圾桶 */
.icon-trash {
  width: 28px; height: 32px;
  border: 3px solid #fff;
  border-radius: 0 0 6px 6px;
  position: relative;
}
.icon-trash::before {
  content: '';
  position: absolute;
  top: -8px; left: 50%;
  width: 20px; height: 4px;
  background: #fff;
  transform: translateX(-50%);
  border-radius: 2px;
}
.icon-trash::after {
  content: '';
  position: absolute;
  top: -14px; left: 50%;
  width: 10px; height: 6px;
  border: 3px solid #fff;
  border-bottom: none;
  transform: translateX(-50%);
  border-radius: 3px 3px 0 0;
}

/* 补录：加号 */
.icon-plus {
  width: 32px; height: 32px;
  position: relative;
}
.icon-plus::before {
  content: '';
  position: absolute;
  top: 50%; left: 50%;
  width: 24px; height: 4px;
  background: #fff;
  transform: translate(-50%, -50%);
  border-radius: 2px;
}
.icon-plus::after {
  content: '';
  position: absolute;
  top: 50%; left: 50%;
  width: 4px; height: 24px;
  background: #fff;
  transform: translate(-50%, -50%);
  border-radius: 2px;
}

/* 员工：双人 */
.icon-users {
  width: 36px; height: 28px;
  position: relative;
}
.icon-users::before {
  content: '';
  position: absolute;
  top: 0; left: 50%;
  width: 14px; height: 14px;
  border: 3px solid #fff;
  border-radius: 50%;
  transform: translateX(-50%);
}
.icon-users::after {
  content: '';
  position: absolute;
  bottom: 0; left: 50%;
  width: 28px; height: 14px;
  border: 3px solid #fff;
  border-radius: 14px 14px 0 0;
  border-bottom: none;
  transform: translateX(-50%);
}

/* 默认 */
.icon-default {
  width: 24px; height: 24px;
  background: #fff;
  border-radius: 50%;
}

.menu-title { font-size: 24px; color: #374151; font-weight: 500; text-align: center; }
</style>
