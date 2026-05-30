import { defineStore } from 'pinia'
import { ref } from 'vue'
import { menuApi } from '../api/system'

export interface MenuItem {
  id: number
  name: string
  code: string
  path: string
  component: string | null
  icon: string | null
  type: number // 1=目录, 2=菜单
  children?: MenuItem[]
}

export const useMenuStore = defineStore('menu', () => {
  const menus = ref<MenuItem[]>([])
  const loaded = ref(false)

  async function fetchMenus() {
    if (loaded.value) return
    const res: any = await menuApi.userTree('web')
    menus.value = res.data || []
    loaded.value = true
  }

  function clearMenus() {
    menus.value = []
    loaded.value = false
  }

  return { menus, loaded, fetchMenus, clearMenus }
})
