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
    const actualMenuPath = menuPath || tab.path

    // 查找是否已有相同菜单的标签
    const existingTabIndex = tabs.value.findIndex(
      t => t.menuPath === actualMenuPath || t.path === actualMenuPath
    )

    if (existingTabIndex > -1) {
      // 更新已有标签的路径和标题
      tabs.value[existingTabIndex].path = tab.path
      tabs.value[existingTabIndex].title = tab.title
      activeTab.value = tab.path
    } else {
      // 添加新标签
      tabs.value.push({ ...tab, menuPath: actualMenuPath })
      activeTab.value = tab.path
    }
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
