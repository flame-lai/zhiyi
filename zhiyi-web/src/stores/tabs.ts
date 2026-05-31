import { defineStore } from 'pinia'
import { ref } from 'vue'

interface TabItem {
  path: string
  title: string
  name: string
  menuPath?: string
}

export const useTabStore = defineStore('tabs', () => {
  const tabs = ref<TabItem[]>([])
  const activeTab = ref('')

  function addTab(tab: TabItem, menuPath?: string) {
    // 如果是详情页，更新已有标签的路径
    if (menuPath && tab.path !== menuPath) {
      const existingTab = tabs.value.find(t => t.path === menuPath || t.menuPath === menuPath)
      if (existingTab) {
        existingTab.path = tab.path
        activeTab.value = tab.path
        return
      }
    }

    // 检查是否已存在相同路径的标签
    const exists = tabs.value.find(t => t.path === tab.path)
    if (!exists) {
      tabs.value.push({ ...tab, menuPath: menuPath || tab.path })
    }
    activeTab.value = tab.path
  }

  function removeTab(path: string) {
    const index = tabs.value.findIndex(t => t.path === path)
    if (index > -1) {
      tabs.value.splice(index, 1)
      // 如果关闭的是当前激活的tab，切换到最后一个tab
      if (activeTab.value === path && tabs.value.length > 0) {
        activeTab.value = tabs.value[tabs.value.length - 1].path
      }
    }
  }

  function setActiveTab(path: string) {
    activeTab.value = path
  }

  function clearTabs() {
    tabs.value = []
    activeTab.value = ''
  }

  return {
    tabs,
    activeTab,
    addTab,
    removeTab,
    setActiveTab,
    clearTabs,
  }
})
