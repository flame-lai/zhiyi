import Taro from '@tarojs/taro'

// TODO: 上线前改为真实域名
const BASE_URL = 'http://192.168.1.7:8080/api'

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  params?: Record<string, any>
}

export function request<T = any>(options: RequestOptions): Promise<T> {
  const token = Taro.getStorageSync('satoken')
  const headers: Record<string, string> = {}
  if (token) {
    headers.satoken = token
  }

  let url = BASE_URL + options.url
  if (options.params) {
    const query = Object.entries(options.params)
      .filter(([, v]) => v !== undefined && v !== null)
      .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`)
      .join('&')
    if (query) url += '?' + query
  }

  return new Promise((resolve, reject) => {
    Taro.request({
      url,
      method: options.method || 'GET',
      data: options.data,
      header: headers,
      timeout: 30000,
      success(res) {
        if (res.data.code === 200) {
          resolve(res.data.data)
        } else if (res.data.code === 401) {
          clearAuth()
          Taro.redirectTo({ url: '/pages/login/index' })
          reject(res.data)
        } else {
          Taro.showToast({ title: res.data.message || '请求失败', icon: 'none' })
          reject(res.data)
        }
      },
      fail(err) {
        Taro.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
        reject(err)
      },
    })
  })
}

export function get<T = any>(url: string, params?: Record<string, any>) {
  return request<T>({ url, method: 'GET', params })
}

export function post<T = any>(url: string, data?: any) {
  return request<T>({ url, method: 'POST', data })
}

export function put<T = any>(url: string, data?: any) {
  return request<T>({ url, method: 'PUT', data })
}

export function del<T = any>(url: string, params?: Record<string, any>) {
  return request<T>({ url, method: 'DELETE', params })
}

export function clearAuth() {
  Taro.removeStorageSync('satoken')
  Taro.removeStorageSync('userInfo')
  Taro.removeStorageSync('menus')
  Taro.removeStorageSync('permissions')
  Taro.removeStorageSync('defaultHomeType')
}

export function saveAuth(data: {
  token: string
  employee: any
  role: any
  menus: any[]
  permissions: string[]
  defaultHomeType: string
}) {
  Taro.setStorageSync('satoken', data.token)
  Taro.setStorageSync('userInfo', JSON.stringify({
    employee: data.employee,
    role: data.role,
  }))
  Taro.setStorageSync('menus', JSON.stringify(data.menus))
  Taro.setStorageSync('permissions', JSON.stringify(data.permissions))
  Taro.setStorageSync('defaultHomeType', data.defaultHomeType)
}

export function getToken(): string {
  return Taro.getStorageSync('satoken') || ''
}

export function getUserInfo() {
  const raw = Taro.getStorageSync('userInfo')
  return raw ? JSON.parse(raw) : null
}

export function getMenus() {
  const raw = Taro.getStorageSync('menus')
  return raw ? JSON.parse(raw) : []
}

export function getPermissions(): string[] {
  const raw = Taro.getStorageSync('permissions')
  return raw ? JSON.parse(raw) : []
}

export function hasPermission(code: string): boolean {
  return getPermissions().includes(code)
}
