import request from './request'

export interface LoginRequest {
  phone: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: number
  phone: string
  realName: string
  role: string
}

export interface AdminUserVO {
  id: number
  phone: string
  realName: string
  role: string
  status: number
  lastLoginTime?: string
  lastLoginIp?: string
}

// 人事登录
export function login(data: LoginRequest) {
  return request({
    url: '/api/hr/auth/login',
    method: 'post',
    params: data  // HR端使用params而不是data
  })
}

// 退出登录
export function logout() {
  return request({
    url: '/api/hr/auth/logout',
    method: 'post'
  })
}

