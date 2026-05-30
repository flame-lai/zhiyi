<template>
  <a-layout class="layout">
    <a-layout-sider v-model:collapsed="collapsed" :trigger="null" collapsible width="220" theme="light"
      style="border-right: 1px solid #e8e8e8">
      <div class="logo">
        <span v-if="!collapsed">智衣ERP</span>
        <span v-else>ZY</span>
      </div>
      <a-menu v-model:selectedKeys="selectedKeys" mode="inline" @click="handleMenuClick">
        <a-menu-item key="dashboard">
          <template #icon><DashboardOutlined /></template>
          生产看板
        </a-menu-item>

        <a-sub-menu key="system">
          <template #icon><SettingOutlined /></template>
          <template #title>系统管理</template>
          <a-menu-item key="system/employees">员工档案</a-menu-item>
          <a-menu-item key="system/roles">角色管理</a-menu-item>
          <a-menu-item key="system/menus">菜单管理</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="base">
          <template #icon><AppstoreOutlined /></template>
          <template #title>基础配置</template>
          <a-menu-item key="base/processes">工序库</a-menu-item>
          <a-menu-item key="base/styles">款号管理</a-menu-item>
          <a-menu-item key="base/customers">客户档案</a-menu-item>
          <a-menu-item key="base/suppliers">供应商档案</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="order">
          <template #icon><FileTextOutlined /></template>
          <template #title>订单管理</template>
          <a-menu-item key="order/list">订单列表</a-menu-item>
          <a-menu-item key="order/shipments">出货管理</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="cutting">
          <template #icon><ScissorOutlined /></template>
          <template #title>裁床管理</template>
          <a-menu-item key="cutting/plans">裁床计划</a-menu-item>
          <a-menu-item key="cutting/bundles">扎号管理</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="admin">
          <template #icon><AlertOutlined /></template>
          <template #title>管理后台</template>
          <a-menu-item key="admin/anomalies">异常预警</a-menu-item>
          <a-menu-item key="admin/timed-wages">计时工资</a-menu-item>
          <a-menu-item key="admin/wages">工资管理</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="finance">
          <template #icon><AccountBookOutlined /></template>
          <template #title>财务管理</template>
          <a-menu-item key="finance/payments">收支管理</a-menu-item>
          <a-menu-item key="finance/profit">利润分析</a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="header">
        <div class="header-left">
          <MenuFoldOutlined v-if="!collapsed" class="trigger" @click="collapsed = !collapsed" />
          <MenuUnfoldOutlined v-else class="trigger" @click="collapsed = !collapsed" />
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <a-dropdown>
            <span class="user-info">
              <UserOutlined />
              <span style="margin-left: 8px">{{ userStore.employeeId || '管理员' }}</span>
            </span>
            <template #overlay>
              <a-menu @click="handleUserMenu">
                <a-menu-item key="logout">
                  <LogoutOutlined />
                  <span style="margin-left: 8px">退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import {
  DashboardOutlined,
  SettingOutlined,
  AppstoreOutlined,
  FileTextOutlined,
  ScissorOutlined,
  AlertOutlined,
  AccountBookOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const collapsed = ref(false)
const selectedKeys = ref<string[]>(['dashboard'])

const currentTitle = computed(() => {
  return (route.meta?.title as string) || '智衣ERP'
})

watch(
  () => route.path,
  (path) => {
    const key = path.replace(/^\//, '')
    selectedKeys.value = [key]
  },
  { immediate: true }
)

function handleMenuClick({ key }: { key: string }) {
  router.push(`/${key}`)
}

function handleUserMenu({ key }: { key: string }) {
  if (key === 'logout') {
    userStore.logout()
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: #4A6FA5;
  border-bottom: 1px solid #e8e8e8;
}

.header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.trigger {
  font-size: 18px;
  cursor: pointer;
  color: #666;
  transition: color 0.3s;
}

.trigger:hover {
  color: #4A6FA5;
}

.page-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  cursor: pointer;
  color: #666;
  display: flex;
  align-items: center;
}

.user-info:hover {
  color: #4A6FA5;
}

.content {
  margin: 20px;
  padding: 20px;
  background: #F5F7FA;
  min-height: calc(100vh - 64px - 40px);
}
</style>
