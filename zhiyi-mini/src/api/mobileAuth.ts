import { get, post } from '../utils/request'
import type { MobileLoginResult, MobileProfile } from './types'

export function loginByCode(loginCode: string) {
  return post<MobileLoginResult>('/mobile/auth/login-by-code', { loginCode })
}

export function loginByAccount(account: string, password: string) {
  return post<MobileLoginResult>('/mobile/auth/login-by-account', { account, password })
}

export function logout() {
  return post<void>('/mobile/auth/logout')
}

export function getProfile() {
  return get<MobileProfile>('/mobile/auth/profile')
}
