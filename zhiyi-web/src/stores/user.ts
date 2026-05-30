import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo, logout as logoutApi } from '../api/auth'
import router from '../router'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('satoken') || '')
  const factoryId = ref<number | null>(null)
  const factoryName = ref('')
  const employeeId = ref<number | null>(null)
  const roleId = ref<number | null>(null)

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('satoken', t)
  }

  async function fetchUserInfo() {
    const res: any = await getUserInfo()
    factoryId.value = res.data.factoryId
    factoryName.value = res.data.factoryName || ''
    employeeId.value = res.data.employeeId
    roleId.value = res.data.roleId
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      factoryId.value = null
      factoryName.value = ''
      employeeId.value = null
      roleId.value = null
      localStorage.removeItem('satoken')
      router.push('/login')
    }
  }

  return { token, factoryId, factoryName, employeeId, roleId, setToken, fetchUserInfo, logout }
})
