import request from '@/utils/request'
import { isRequestCanceled } from '@/utils/request'

// 医生信息缓存
const doctorCache = {
  info: new Map(),
  list: null,
  timestamp: 0,
  EXPIRY: 10 * 60 * 1000,
  isExpired() {
    return Date.now() - this.timestamp > this.EXPIRY;
  },
  reset() {
    this.info.clear();
    this.list = null;
    this.timestamp = 0;
  }
};

// 医生注册
export function doctorRegister(data) {
  return request({
    url: '/doctor/register',
    method: 'post',
    data
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('医生注册失败:', error);
    return Promise.reject(error);
  });
}

// 更新医生基础信息
export function updateDoctorInfo(data) {
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
export function getDoctorInfo(doctorId) {
  if (!doctorId) {
    return Promise.reject(new Error('医生ID不能为空'));
  }
  if (doctorCache.info.has(doctorId) && !doctorCache.isExpired()) {
    return Promise.resolve(doctorCache.info.get(doctorId));
  }
  return request({
    url: `/doctor/get/${doctorId}`,
    method: 'get',
    retryTimes: 3,
    timeout: 15000,
    headers: {
      'Cache-Control': 'no-cache',
      'Pragma': 'no-cache',
      'Expires': '0'
    },
    params: {
      _t: new Date().getTime()
    }
  }).then(data => {
    if (data) {
      doctorCache.info.set(doctorId, data);
      doctorCache.timestamp = Date.now();
    }
    return data;
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error(`获取医生信息(ID: ${doctorId})失败:`, error);
    return Promise.reject(error);
  });
}

export function getDoctorList(retries = 2) {
  doctorCache.reset();
  return request({
    url: `/doctor`,
    method: 'get',
    retryTimes: retries,
    headers: {
      'Cache-Control': 'no-cache',
      'Pragma': 'no-cache',
      'Expires': '0'
    },
    params: {
      _t: new Date().getTime()
    }
  }).then(data => {
    if (data) {
      console.log(`成功获取${data.length || 0}条医生数据`);
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
      return Promise.reject(error);
    }
    if (error.name === 'CanceledError' || error.code === 'ERR_CANCELED') {
      return Promise.reject(error);
    }
    console.error('获取医生列表失败:', error);
    return Promise.reject(error);
  });
}

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
