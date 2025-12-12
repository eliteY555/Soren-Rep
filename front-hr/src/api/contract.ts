import request from './request'

export interface ContractDetailVO {
  id: number
  contractNo: string
  contractType: string
  contractTypeDesc: string
  employeeId: number
  employeeName: string
  initiatorId: number
  initiatorName: string
  position: string
  baseSalary: number
  startDate: string
  endDate: string
  contractPdfUrl: string
  status: number
  statusDesc: string
  initiatorSignTime?: string
  issueTime?: string
  employeeSignTime?: string
  effectiveTime?: string
  managerSignature?: string
  employeeSignature?: string
  createdAt: string
}

// 根据经理获取员工合同列表
export function getContractsByManager(
  managerId: number,
  pageNum: number = 1,
  pageSize: number = 10,
  status?: number
) {
  return request({
    url: `/api/hr/contract/by-manager/${managerId}`,
    method: 'get',
    params: {
      pageNum,
      pageSize,
      status
    }
  })
}

// 获取合同PDF临时访问URL
export function getContractPdfUrl(id: number) {
  return request<string>({
    url: `/api/hr/contract/pdf-url/${id}`,
    method: 'get'
  })
}

