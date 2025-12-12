import request from '@/utils/request'
import { isRequestCanceled } from '@/utils/request'

// 记录信息缓存
const recordCache = {
  info: new Map(), // 存储记录详情，键为recordId
  timestamp: 0,    // 缓存时间戳
  
  // 缓存过期时间（5分钟）
  EXPIRY: 5 * 60 * 1000,
  
  // 检查缓存是否过期
  isExpired() {
    return Date.now() - this.timestamp > this.EXPIRY;
  },
  
  // 重置缓存
  reset() {
    this.info.clear();
    this.timestamp = 0;
  }
};

export function addRecord(data) {
    // 添加记录后清除缓存
    recordCache.reset();
    
    return request({
        url: '/record/add',
        method: 'post',
        data
    }).catch(error => {
        if (isRequestCanceled(error)) {
            return Promise.reject(error);
        }
        console.error('添加病历失败:', error);
        return Promise.reject(error);
    });
}

export function getRecordList(data) {
    return request({
        url: `/record/page`,
        method: 'post',
        data
    }).catch(error => {
        if (isRequestCanceled(error)) {
            return Promise.reject(error);
        }
        console.error('获取病历列表失败:', error);
        return Promise.reject(error);
    });
}

export function getRecordInfo(recordId) {
  if (!recordId) {
    return Promise.reject(new Error('记录ID不能为空'));
  }
  
  // 完全禁用缓存，确保每次都获取最新数据
  recordCache.info.delete(recordId);
  
  // 添加时间戳参数，避免浏览器缓存
  const timestamp = Date.now();
  
  return request({
    url: `/record/${recordId}?_t=${timestamp}`,
    method: 'get',
    // 对这个特定请求添加自定义重试配置
    retryTimes: 2, // 最多尝试3次(初始请求+2次重试)
    retryDelay: 1000, // 1秒后重试
    timeout: 10000, // 增加超时时间
    headers: {
      'Cache-Control': 'no-cache, no-store',
      'Pragma': 'no-cache'
    }
  }).then(data => {
    // 不再缓存结果
    return data;
  }).catch(error => {
    if (isRequestCanceled(error)) {
        return Promise.reject(error);
    }
    console.error(`获取病历 ${recordId} 失败:`, error);
    // 清除此记录的可能存在的缓存
    recordCache.info.delete(recordId);
    return Promise.reject(error);
  });
}

export function updateRecord(data) {
  // 更新记录后清除缓存
  if (data && data.recordId) {
    recordCache.info.delete(data.recordId);
  } else {
    recordCache.reset();
  }
  
  return request({
    url: `/record/update`,
    method: 'post',
    data
  }).catch(error => {
    if (isRequestCanceled(error)) {
        return Promise.reject(error);
    }
    console.error('更新病历失败:', error);
    return Promise.reject(error);
  });
}

/**
 * 获取诊断推荐列表
 * @param {string} diagnosisText 诊断结果文本
 * @param {number} limit 返回结果数量限制，默认5条
 * @returns {Promise} 推荐列表
 */
export function getDiagnosisRecommendations(diagnosisText, limit = 5) {
  return request({
    url: '/record/recommend',
    method: 'post',
    data: {
      diagnosisText,
      limit
    }
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('获取诊断推荐失败:', error);
    return Promise.reject(error);
  });
}