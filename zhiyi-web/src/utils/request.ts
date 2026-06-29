import axios from 'axios'
import { message } from 'ant-design-vue'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('satoken')
  if (token) {
    config.headers.satoken = token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const { data } = response
    if (data.code === 200) {
      return data
    }
    if (data.code === 401) {
      localStorage.removeItem('satoken')
      router.push('/login')
      message.error('登录已过期，请重新登录')
      return Promise.reject(data)
    }
    message.error(data.message || '请求失败')
    return Promise.reject(data)
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('satoken')
      router.push('/login')
      message.error('登录已过期，请重新登录')
    } else {
      message.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
