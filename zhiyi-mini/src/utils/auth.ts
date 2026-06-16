import Taro from '@tarojs/taro'
import { clearAuth as clearAuthRequest, getToken, getMenus, getPermissions, getUserInfo } from './request'

export { getToken, getMenus, getPermissions, getUserInfo }

export function isLoggedIn(): boolean {
  return !!getToken()
}

export function hasMenu(code: string): boolean {
  const menus = getMenus()
  return menus.some((m: any) => m.code === code)
}

export function hasPermission(code: string): boolean {
  return getPermissions().includes(code)
}

export function getDefaultHomeType(): string {
  return Taro.getStorageSync('defaultHomeType') || 'employee'
}

export function clearAuth() {
  clearAuthRequest()
}

export function checkAuthAndRedirect() {
  if (!isLoggedIn()) {
    Taro.redirectTo({ url: '/pages/login/index' })
    return false
  }
  return true
}
