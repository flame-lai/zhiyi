import request from '../utils/request'

export function loginByAccount(data: { account: string; password: string }) {
  return request.post('/system/auth/login', data)
}

export function loginByCode(data: { loginCode: string }) {
  return request.post('/system/auth/login-by-code', data)
}

export function logout() {
  return request.post('/system/auth/logout')
}

export function getUserInfo() {
  return request.get('/system/auth/info')
}
