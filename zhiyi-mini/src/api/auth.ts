import { get, post } from '../utils/request'

export function loginByAccount(data: { account: string; password: string }) {
  return post('/system/auth/login', data)
}

export function loginByCode(data: { loginCode: string }) {
  return post('/system/auth/login-by-code', data)
}

export function logout() {
  return post('/system/auth/logout')
}

export function getUserInfo() {
  return get('/system/auth/info')
}
