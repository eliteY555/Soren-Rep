import request from './request'

export interface DashboardData {
  totalEmployees: number
  totalManagers: number
  activeContracts: number
  pendingApprovals: number
  [key: string]: any
}

export interface TrendData {
  months: string[]
  onboardData: number[]
  offboardData: number[]
}

export interface DepartmentDistribution {
  departmentName: string
  employeeCount: number
}

// 获取数据概览
export function getDashboard() {
  return request<DashboardData>({
    url: '/api/hr/statistics/dashboard',
    method: 'get'
  })
}

// 获取入离职趋势
export function getTrend() {
  return request<TrendData>({
    url: '/api/hr/statistics/trend',
    method: 'get'
  })
}

// 获取部门分布
export function getDepartmentDistribution() {
  return request<DepartmentDistribution[]>({
    url: '/api/hr/statistics/department',
    method: 'get'
  })
}

