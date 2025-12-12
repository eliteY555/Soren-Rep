import request from './request'

export interface EmployeeRegisterRequest {
  realName: string
  phone: string
  idCard: string
  gender?: number
}

export interface EmployeeRegisterVO {
  employeeId: number
  realName: string
  phone: string
  defaultPassword: string
}

export interface Employee {
  id: number
  realName: string
  phone: string
  idCard?: string
  gender?: number
  role: string
  managedBy?: number
  departmentId?: number
  status: number
  lastLoginTime?: string
  lastLoginIp?: string
  createdAt?: string
  updatedAt?: string
}

// 注册员工
export function registerEmployee(data: EmployeeRegisterRequest) {
  return request<EmployeeRegisterVO>({
    url: '/api/manager/employee/register',
    method: 'post',
    params: data
  })
}

// 获取我的员工列表
export function getMyEmployees(pageNum: number = 1, pageSize: number = 10) {
  return request({
    url: '/api/manager/employee/my-list',
    method: 'get',
    params: {
      pageNum,
      pageSize
    }
  })
}

// 冻结员工
export function freezeEmployee(id: number) {
  return request({
    url: `/api/manager/employee/freeze/${id}`,
    method: 'post'
  })
}

// 解冻员工
export function unfreezeEmployee(id: number) {
  return request({
    url: `/api/manager/employee/unfreeze/${id}`,
    method: 'post'
  })
}

// 获取员工详情
export function getEmployeeDetail(id: number) {
  return request<Employee>({
    url: `/api/manager/employee/detail/${id}`,
    method: 'get'
  })
}

// 重置员工密码
export function resetPassword(id: number) {
  return request({
    url: `/api/manager/employee/reset-password/${id}`,
    method: 'post'
  })
}

