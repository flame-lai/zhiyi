<template>
  <a-layout class="layout">
    <a-layout-sider v-model:collapsed="collapsed" :trigger="null" collapsible width="220" theme="light"
      style="border-right: 1px solid #e8e8e8">
      <div class="logo">
        <span v-if="!collapsed">智衣ERP</span>
        <span v-else>ZY</span>
      </div>
      <a-menu v-model:selectedKeys="selectedKeys" v-model:openKeys="openKeys" mode="inline" @click="handleMenuClick">
        <template v-for="menu in menuStore.menus" :key="menu.code">
          <!-- 一级菜单（目录）有子菜单 -->
          <a-sub-menu v-if="menu.type === 1 && menu.children && menu.children.length > 0" :key="menu.code">
            <template #icon>
              <component :is="iconMap[menu.icon || ''] || 'span'" />
            </template>
            <template #title>{{ menu.name }}</template>
            <a-menu-item v-for="child in menu.children" :key="child.path">
              {{ child.name }}
            </a-menu-item>
          </a-sub-menu>
          <!-- 一级菜单（直接页面） -->
          <a-menu-item v-else :key="menu.path">
            <template #icon>
              <component :is="iconMap[menu.icon || ''] || 'span'" />
            </template>
            {{ menu.name }}
          </a-menu-item>
        </template>
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
          <span v-if="userStore.factoryName" class="factory-name">{{ userStore.factoryName }}</span>
          <a-dropdown>
            <span class="user-info">
              <UserOutlined />
              <span style="margin-left: 8px">{{ userStore.userName || '管理员' }}</span>
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

      <!-- 标签页导航 -->
      <div class="tab-bar">
        <div class="tab-list">
          <div
            v-for="tab in tabStore.tabs"
            :key="tab.path"
            class="tab-item"
            :class="{ active: tabStore.activeTab === tab.path }"
            @click="switchTab(tab.path)"
          >
            <span class="tab-title">{{ tab.title }}</span>
            <CloseOutlined
              v-if="tabStore.tabs.length > 1"
              class="tab-close"
              @click.stop="closeTab(tab.path)"
            />
          </div>
        </div>
      </div>

      <a-layout-content class="content">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, markRaw } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useMenuStore } from '../stores/menu'
import { useTabStore } from '../stores/tabs'
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
  CloseOutlined,
} from '@ant-design/icons-vue'

// 图标映射
const iconMap: Record<string, any> = {
  DashboardOutlined: markRaw(DashboardOutlined),
  SettingOutlined: markRaw(SettingOutlined),
  AppstoreOutlined: markRaw(AppstoreOutlined),
  FileTextOutlined: markRaw(FileTextOutlined),
  ScissorOutlined: markRaw(ScissorOutlined),
  AlertOutlined: markRaw(AlertOutlined),
  AccountBookOutlined: markRaw(AccountBookOutlined),
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const menuStore = useMenuStore()
const tabStore = useTabStore()
const collapsed = ref(false)
const selectedKeys = ref<string[]>([])
const openKeys = ref<string[]>([])

onMounted(async () => {
  await userStore.fetchUserInfo()
  await menuStore.fetchMenus()
  // 根据当前路由设置展开的菜单
  updateOpenKeys()
  // 添加当前页面到标签页
  addCurrentTab()
})

const currentTitle = computed(() => {
  return (route.meta?.title as string) || '智衣ERP'
})

watch(
  () => route.path,
  () => {
    updateOpenKeys()
    addCurrentTab()
  }
)

function updateOpenKeys() {
  const path = route.path
  // 找到当前路径所属的父菜单
  for (const menu of menuStore.menus) {
    if (menu.children) {
      const found = menu.children.find((c: any) => c.path === path)
      if (found) {
        selectedKeys.value = [path]
        openKeys.value = [menu.code]
        return
      }
    }
    if (menu.path === path) {
      selectedKeys.value = [path]
      return
    }
  }
  selectedKeys.value = [path]
}

function addCurrentTab() {
  const menuPath = findMenuPath(route.path)
  if (!menuPath) return

  // 获取菜单标题
  const title = getMenuTitle(menuPath) || (route.meta?.title as string) || '未知页面'

  // 如果是详情页，更新已有标签的路径；否则添加新标签
  tabStore.addTab({
    path: route.path,
    title,
    name: route.name as string,
  }, menuPath)
}

function findMenuPath(path: string): string | null {
  for (const menu of menuStore.menus) {
    if (menu.children) {
      const found = menu.children.find((c: any) => path.startsWith(c.path))
      if (found) return found.path
    }
    if (path.startsWith(menu.path)) return menu.path
  }
  return null
}

function getMenuTitle(menuPath: string): string | null {
  for (const menu of menuStore.menus) {
    if (menu.children) {
      const found = menu.children.find((c: any) => c.path === menuPath)
      if (found) return found.name
    }
    if (menu.path === menuPath) return menu.name
  }
  return null
}

function handleMenuClick({ key }: { key: string }) {
  router.push(key)
}

function switchTab(path: string) {
  tabStore.setActiveTab(path)
  router.push(path)
}

function closeTab(path: string) {
  tabStore.removeTab(path)
  // 如果关闭后还有标签，导航到当前激活的标签
  if (tabStore.activeTab) {
    router.push(tabStore.activeTab)
  }
}

function handleUserMenu({ key }: { key: string }) {
  if (key === 'logout') {
    menuStore.clearMenus()
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
  gap: 16px;
}

.factory-name {
  font-size: 14px;
  color: #4A6FA5;
  font-weight: 500;
  padding: 4px 12px;
  background: rgba(74, 111, 165, 0.1);
  border-radius: 4px;
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

.tab-bar {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 16px;
  height: 40px;
  display: flex;
  align-items: center;
}

.tab-list {
  display: flex;
  gap: 4px;
  overflow-x: auto;
  white-space: nowrap;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  background: #f5f5f5;
  transition: all 0.2s;
}

.tab-item:hover {
  color: #4A6FA5;
  background: rgba(74, 111, 165, 0.1);
}

.tab-item.active {
  color: #4A6FA5;
  background: rgba(74, 111, 165, 0.15);
  font-weight: 500;
}

.tab-title {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tab-close {
  font-size: 10px;
  color: #999;
  cursor: pointer;
  padding: 2px;
  border-radius: 50%;
  transition: all 0.2s;
}

.tab-close:hover {
  color: #ff4d4f;
  background: rgba(255, 77, 79, 0.1);
}

.content {
  margin: 16px;
  padding: 16px;
  background: #F5F7FA;
  min-height: calc(100vh - 64px - 40px - 32px);
}
</style>
