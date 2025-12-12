import request from './request'

export interface ManagerCreateRequest {
  realName: string
  phone: string
  idCard: string
  departmentId?: number
}

export interface Manager {
  id: number
  realName: string
  phone: string
  idCard?: string
  role: string
  departmentId?: number
  status: number
  lastLoginTime?: string
  lastLoginIp?: string
  createdAt?: string
  updatedAt?: string
}

// 创建经理
export function createManager(data: ManagerCreateRequest) {
  return request({
    url: '/api/hr/manager/create',
    method: 'post',
    params: data
  })
}

// 获取经理列表
export function getManagerList(pageNum: number = 1, pageSize: number = 10) {
  return request({
    url: '/api/hr/manager/list',
    method: 'get',
    params: {
      pageNum,
      pageSize
    }
  })
}

// 冻结经理
export function freezeManager(id: number) {
  return request({
    url: `/api/hr/manager/freeze/${id}`,
    method: 'post'
  })
}

// 解冻经理
export function unfreezeManager(id: number) {
  return request({
    url: `/api/hr/manager/unfreeze/${id}`,
    method: 'post'
  })
}

// 更新经理信息
export function updateManager(id: number, data: {
  realName: string
  phone: string
  departmentId?: number
}) {
  return request({
    url: `/api/hr/manager/update/${id}`,
    method: 'put',
    params: data
  })
}

// 重置经理密码
export function resetPassword(id: number) {
  return request({
    url: `/api/hr/manager/reset-password/${id}`,
    method: 'post'
  })
}

