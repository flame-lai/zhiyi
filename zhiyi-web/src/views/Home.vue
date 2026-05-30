<template>
  <div class="home-loading">
    <a-spin size="large" tip="加载中..." />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMenuStore } from '../stores/menu'

const router = useRouter()
const menuStore = useMenuStore()

onMounted(async () => {
  await menuStore.fetchMenus()

  // 找到第一个有效的叶子菜单
  const firstPath = findFirstLeafMenu(menuStore.menus)
  if (firstPath) {
    router.replace(firstPath)
  } else {
    // 没有菜单权限，提示无权限
    router.replace('/login')
  }
})

function findFirstLeafMenu(menus: any[]): string | null {
  for (const menu of menus) {
    if (menu.children && menu.children.length > 0) {
      const childPath = findFirstLeafMenu(menu.children)
      if (childPath) return childPath
    } else if (menu.path) {
      return menu.path.startsWith('/') ? menu.path : '/' + menu.path
    }
  }
  return null
}
</script>

<style scoped>
.home-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 60vh;
}
</style>
