import Taro from '@tarojs/taro'

const BASE_URL = 'http://localhost:8080/api'

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  params?: Record<string, any>
}

export function request(options: RequestOptions): Promise<any> {
  const token = Taro.getStorageSync('satoken')
  const headers: Record<string, string> = {}
  if (token) {
    headers.satoken = token
  }

  let url = BASE_URL + options.url
  if (options.params) {
    const query = Object.entries(options.params)
      .filter(([, v]) => v !== undefined && v !== null)
      .map(([k, v]) => `${k}=${encodeURIComponent(v)}`)
      .join('&')
    if (query) url += '?' + query
  }

  return new Promise((resolve, reject) => {
    Taro.request({
      url,
      method: options.method || 'GET',
      data: options.data,
      header: headers,
      success(res) {
        if (res.data.code === 200) {
          resolve(res.data)
        } else if (res.data.code === 401) {
          Taro.removeStorageSync('satoken')
          Taro.redirectTo({ url: '/pages/login/index' })
          reject(res.data)
        } else {
          Taro.showToast({ title: res.data.message || '请求失败', icon: 'none' })
          reject(res.data)
        }
      },
      fail(err) {
        Taro.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      },
    })
  })
}

export function get(url: string, params?: Record<string, any>) {
  return request({ url, method: 'GET', params })
}

export function post(url: string, data?: any) {
  return request({ url, method: 'POST', data })
}

export function put(url: string, data?: any) {
  return request({ url, method: 'PUT', data })
}

export function del(url: string, params?: Record<string, any>) {
  return request({ url, method: 'DELETE', params })
}
