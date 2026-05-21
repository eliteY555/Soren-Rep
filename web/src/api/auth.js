import request from '@/utils/request'

// 统一登录
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

// 找回密码-验证身份
export function findPassword(identity) {
  return request({
    url: `/auth/findPassword?identity=${identity}`,
    method: 'get',
  })
}

// 更新信息（旧接口兼容，实际已分开）
export function update(data) {
  return request({
    url: '/auth/update',
    method: 'post',
    data
  })
}
