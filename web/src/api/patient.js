import request from '@/utils/request'
import { isRequestCanceled } from '@/utils/request'

// 患者注册
export function patientRegister(data) {
  return request({
    url: '/patient/register',
    method: 'post',
    data
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('患者注册失败:', error);
    return Promise.reject(error);
  });
}

// 更新患者基础信息
export function updatePatientInfo(data) {
  return request({
    url: '/patient/update',
    method: 'post',
    data
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('更新患者信息失败:', error);
    return Promise.reject(error);
  });
}

// 创建患者基础信息
export function createPatientInfo(data) {
  return request({
    url: '/patient/create',
    method: 'post',
    data
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('创建患者信息失败:', error);
    return Promise.reject(error);
  });
}

// 获取患者基本信息
export function getPatientInfo(patientId) {
  if (!patientId) {
    return Promise.reject('患者ID不能为空');
  }

  return request({
    url: `/patient/get/${patientId}`,
    method: 'get'
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error(`获取患者(ID: ${patientId})信息失败:`, error);
    return Promise.reject(error);
  });
}
