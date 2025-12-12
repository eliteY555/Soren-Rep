import request from '@/utils/request'
import { isRequestCanceled } from '@/utils/request'

// 医生信息缓存
const doctorCache = {
  info: new Map(), // 存储医生基本信息，键为userId
  list: null,      // 存储医生列表
  timestamp: 0,    // 缓存时间戳
  
  // 缓存过期时间（10分钟）
  EXPIRY: 10 * 60 * 1000,
  
  // 检查缓存是否过期
  isExpired() {
    return Date.now() - this.timestamp > this.EXPIRY;
  },
  
  // 重置缓存
  reset() {
    this.info.clear();
    this.list = null;
    this.timestamp = 0;
  }
};

// 更新医生基础信息
export function updateDoctorInfo(data) {
  // 更新后清除缓存
  doctorCache.reset();
  
  return request({
    url: '/doctor/update',
    method: 'post',
    data
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('更新医生信息失败:', error);
    return Promise.reject(error);
  });
}

// 获取医生基础信息
export function getDoctorInfo(userId) {
  if (!userId) {
    return Promise.reject(new Error('用户ID不能为空'));
  }
  
  // 检查缓存
  if (doctorCache.info.has(userId) && !doctorCache.isExpired()) {
    console.log(`使用缓存的医生信息 (userId: ${userId})`);
    return Promise.resolve(doctorCache.info.get(userId));
  }
  
  console.log(`从服务器获取医生信息 (userId: ${userId})...`);
  
  // 禁用缓存，确保获取最新数据
  return request({
    url: `/doctor/get/${userId}`,
    method: 'get',
    retryTimes: 3, // 设置重试次数
    timeout: 15000, // 增加超时时间
    headers: {
      'Cache-Control': 'no-cache',
      'Pragma': 'no-cache',
      'Expires': '0'
    },
    // 添加时间戳防止浏览器缓存
    params: {
      _t: new Date().getTime()
    }
  }).then(data => {
    // 缓存结果
    if (data) {
      console.log(`成功获取医生信息 (userId: ${userId}):`, data);
      doctorCache.info.set(userId, data);
      doctorCache.timestamp = Date.now();
    } else {
      console.warn(`医生信息返回空数据 (userId: ${userId})`);
    }
    return data;
  }).catch(error => {
    if (isRequestCanceled(error)) {
      console.log(`获取医生信息请求被取消 (userId: ${userId})`);
      return Promise.reject(error);
    }
    console.error(`获取医生信息(ID: ${userId})失败:`, error);
    return Promise.reject(error);
  });
}

export function getDoctorList(retries = 2) {
  // 禁用缓存，确保每次获取最新数据
  console.log('正在获取所有医生列表...');
  doctorCache.reset(); // 完全清除缓存
  
  return request({
    url: `/doctor`,
    method: 'get',
    retryTimes: retries, // 设置重试次数
    headers: {
      'Cache-Control': 'no-cache',
      'Pragma': 'no-cache',
      'Expires': '0'
    },
    // 添加时间戳防止浏览器缓存
    params: {
      _t: new Date().getTime()
    }
  }).then(data => {
    if (data) {
      // 记录数据但不缓存
      console.log(`成功获取${data.length || 0}条医生数据`);
      // 打印医生详细信息用于调试
      if (data.length > 0) {
        console.log('医生列表预览:', data.map(d => ({
          id: d.doctorId,
          name: d.doctorName,
          hospital: d.hospitalName,
          department: d.departmentName
        })));
      }
    } else {
      console.warn('获取医生列表返回空数据');
    }
    return data;
  }).catch(error => {
    if (isRequestCanceled(error)) {
      console.log('医生列表请求被取消');
      return Promise.reject(error);
    }
    // 针对CanceledError的特殊处理
    if (error.name === 'CanceledError' || error.code === 'ERR_CANCELED') {
      console.log('医生列表请求被取消 (CanceledError)');
      return Promise.reject(error);
    }
    console.error('获取医生列表失败:', error);
    return Promise.reject(error);
  });
}

// 分页查询
export function getDoctorListByPage(data) {
  return request({
    url: `/doctor/page`,
    method: 'post',
    data
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('分页获取医生列表失败:', error);
    return Promise.reject(error);
  });
}

// 创建医生信息 (专用于注册流程)
export function createDoctorInfo(data) {
  // 清除缓存
  doctorCache.reset();
  
  return request({
    url: '/doctor/create',
    method: 'post',
    data
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('创建医生信息失败:', error);
    return Promise.reject(error);
  });
}