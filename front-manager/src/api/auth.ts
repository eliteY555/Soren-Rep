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

// 经理登录
export function login(data: LoginRequest) {
  return request({
    url: '/api/manager/auth/login',
    method: 'post',
    data
  })
}

// 退出登录
export function logout() {
  return request({
    url: '/api/manager/auth/logout',
    method: 'post'
  })
}

