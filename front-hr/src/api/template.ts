import request from './request'

export interface ContractTemplate {
  id: number
  templateName: string
  templateCode?: string
  templateType: string
  contractCategory: string
  templateContent?: string
  ossFilePath?: string
  variablesConfig?: string
  signaturePositions?: string
  version?: string
  status: number
  remark?: string
  createdBy?: number
  createdAt?: string
  updatedAt?: string
}

// 上传三方合同模板
export function uploadTemplate(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: '/api/hr/template/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取模板列表
export function getTemplateList(pageNum: number = 1, pageSize: number = 10, templateType?: string) {
  return request({
    url: '/api/hr/template/list',
    method: 'get',
    params: {
      pageNum,
      pageSize,
      templateType
    }
  })
}

// 获取模板详情
export function getTemplateDetail(id: number) {
  return request<ContractTemplate>({
    url: `/api/hr/template/detail/${id}`,
    method: 'get'
  })
}

// 删除模板
export function deleteTemplate(id: number) {
  return request({
    url: `/api/hr/template/delete/${id}`,
    method: 'delete'
  })
}

// 获取模板文件预览URL
export function getTemplatePreviewUrl(ossPath: string, expireMinutes: number = 60) {
  return request<string>({
    url: '/api/hr/template/preview-url',
    method: 'get',
    params: {
      ossPath,
      expireMinutes
    }
  })
}

