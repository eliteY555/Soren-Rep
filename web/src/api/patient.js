import request from '@/utils/request'
import { isRequestCanceled } from '@/utils/request'

// 更新患者基础信息
export function updatePatientInfo(data) {
  return request({
    url: '/patient/update',
    method: 'post',
    data
  }).catch(error => {
    // 如果是请求取消错误，静默处理
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
    // 如果是请求取消错误，静默处理
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('创建患者信息失败:', error);
    return Promise.reject(error);
  });
}

// 获取患者基本信息
export function getPatientInfo(userId) {
  if (!userId) {
    return Promise.reject('用户ID不能为空');
  }
  
  return request({
    url: `/patient/get/${userId}`,
    method: 'get'
  }).catch(error => {
    // 如果是请求取消错误，静默处理
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error(`获取患者(ID: ${userId})信息失败:`, error);
    return Promise.reject(error);
  });
}