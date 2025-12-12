import request from './request'

export interface ContractTemplate {
  id: number
  templateName: string
  templateCode: string
  templateType: string
  contractCategory?: string
  ossFilePath?: string
  version?: string
  status: number
  remark?: string
}

export interface ContractIssueRequest {
  templateId: number
  employeeId: number
  position?: string
  baseSalary?: number
  startDate: string
  endDate?: string
  signatureBase64: string  // 经理签名（必填）
  ipAddress?: string
  deviceInfo?: string
  sendNotification?: boolean
}

export interface ContractSignRequest {
  contractId: number
  signatureBase64: string
  ipAddress?: string
  deviceInfo?: string
}

export interface ContractDetailVO {
  id: number
  contractNo: string
  contractType: string
  contractTypeDesc?: string
  employeeId: number
  employeeName: string
  initiatorId: number
  initiatorName: string
  templateId?: number
  position?: string
  baseSalary?: number
  startDate: string
  endDate?: string
  contractPdfUrl?: string
  status: number  // 1-待员工签字, 2-已生效, 3-已终止
  statusDesc?: string
  initiatorSignTime?: string
  issueTime?: string
  employeeSignTime?: string
  effectiveTime?: string
  managerSignature?: string
  employeeSignature?: string
  createdAt: string
}

// 下发合同给员工
export function issueContract(data: ContractIssueRequest) {
  return request({
    url: '/api/manager/contract/issue',
    method: 'post',
    data
  })
}

// 经理签署合同
export function signContract(data: ContractSignRequest) {
  return request({
    url: '/api/manager/contract/sign',
    method: 'post',
    data
  })
}

// 获取我发起的合同列表
export function getMyContracts(status?: number) {
  return request<ContractDetailVO[]>({
    url: '/api/manager/contract/my-list',
    method: 'get',
    params: { status }
  })
}

// 获取合同详情
export function getContractDetail(id: number) {
  return request<ContractDetailVO>({
    url: `/api/manager/contract/detail/${id}`,
    method: 'get'
  })
}

// 查看合同签署状态
export function getContractStatus(id: number) {
  return request<string>({
    url: `/api/manager/contract/status/${id}`,
    method: 'get'
  })
}

// 获取可用合同模板列表
export function getAvailableTemplates() {
  return request<ContractTemplate[]>({
    url: `/api/manager/contract/templates`,
    method: 'get'
  })
}

// 获取合同PDF临时访问URL
export function getContractPdfUrl(id: number) {
  return request<string>({
    url: `/api/manager/contract/pdf-url/${id}`,
    method: 'get'
  })
}

// 终止已生效的合同
export function terminateContract(id: number) {
  return request({
    url: `/api/manager/contract/terminate/${id}`,
    method: 'post'
  })
}

