import axios from "axios";
import { Message } from "element-ui";

// 创建一个自定义的取消请求错误类型
class RequestCancelError extends Error {
  constructor(message) {
    super(message);
    this.name = 'RequestCancelError';
    this.isCanceled = true;
    this.code = 'ERR_CANCELED'; // 与axios的CanceledError保持一致
  }
}

// 创建一个Map来存储请求取消器
export const pendingRequests = new Map();

// 生成请求的唯一键
function generateRequestKey(config) {
  const { url, method, params, data } = config;
  return [url, method, JSON.stringify(params), JSON.stringify(data)].join('&');
}

// 添加请求到pendingRequests
function addPendingRequest(config) {
  const requestKey = generateRequestKey(config);
  config.cancelToken = config.cancelToken || new axios.CancelToken(cancel => {
    if (!pendingRequests.has(requestKey)) {
      pendingRequests.set(requestKey, cancel);
    }
  });
}

// 移除请求从pendingRequests
function removePendingRequest(config) {
  const requestKey = generateRequestKey(config);
  if (pendingRequests.has(requestKey)) {
    pendingRequests.delete(requestKey);
  }
}

// 取消所有请求
export function cancelAllRequests() {
  pendingRequests.forEach(cancel => {
    cancel('请求被取消');
  });
  pendingRequests.clear();
  console.log('所有请求已取消');
}

// 检查是否是请求取消错误
export function isRequestCanceled(error) {
  return axios.isCancel(error) || 
    error.name === 'CanceledError' || 
    error.name === 'AbortError' ||
    error.code === 'ERR_CANCELED';
}

// 创建axios实例
const instance = axios.create({
  baseURL: "/api",
  timeout: 15000, // 增加超时时间
  responseType: "json",
  retryTimes: 1, // 请求失败后的重试次数
  retryDelay: 1000, // 重试间隔时间
});

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    // 在发送请求之前做什么
    // 1.添加通用请求头
    config.headers["Authorization"] = `Bearer ${localStorage.getItem("token")}`;
    
    // 2.添加防止缓存的参数
    if (config.method === 'get') {
      config.params = config.params || {};
      config.params._t = Date.now();
    }
    
    // 3.添加请求到队列中
    addPendingRequest(config);
    
    return config;
  },
  (error) => {
    // 请求错误作什么
    return Promise.reject(error);
  }
);

// 响应拦截器
instance.interceptors.response.use(
  (response) => {
    // 请求成功后，从队列中移除
    removePendingRequest(response.config);
    
    // 如果是流式响应，直接返回整个response对象
    if (response.config.responseType === 'blob') {
      return response;
    }
    
    const { data } = response
    const { code, msg, result } = data
    if (code === 200) {
      return result;
    } else {
      Message.error(msg ? msg : '出错啦，请重试！')
      // 返回一个被拒绝的Promise，这样可以在调用处捕获错误
      return Promise.reject(new Error(msg || '请求失败'));
    }
  },
  (error) => {
    // 请求失败后，从队列中移除
    if (error.config) {
      removePendingRequest(error.config);
      
      // 实现请求重试机制
      const { retryTimes = 1, retryCount = 0, retryDelay = 1000 } = error.config;
      
      // 如果是取消请求或已达到最大重试次数，不再重试
      if (isRequestCanceled(error) || retryCount >= retryTimes) {
        // 如果是取消请求的错误，不显示错误信息，返回自定义错误
        if (isRequestCanceled(error)) {
          console.log('请求已被取消:', error.message);
          return Promise.reject(new RequestCancelError('请求已取消'));
        }
      } else {
        // 重试请求
        return new Promise(resolve => {
          setTimeout(() => {
            console.log(`请求重试: ${retryCount + 1}/${retryTimes}`);
            // 创建新的配置，增加重试计数
            const newConfig = { ...error.config, retryCount: retryCount + 1 };
            resolve(instance(newConfig));
          }, retryDelay);
        });
      }
    }
    
    // 错误统一处理
    let errorMessage = '网络错误，请稍后重试';
    
    if (error.response) {
      switch (error.response.status) {
        case 401:
          errorMessage = "未授权，请重新登录";
          // 可以在这里处理登录过期的情况，例如跳转到登录页
          break;
        case 403:
          errorMessage = "禁止访问";
          break;
        case 404:
          errorMessage = "请求的资源不存在";
          break;
        case 500:
          errorMessage = "服务器错误";
          break;
        default:
          errorMessage = `请求错误 (${error.response.status})`;
      }
    } else if (error.request) {
      errorMessage = "服务器无响应，请检查网络连接";
    } else if (error.message && error.message.includes('timeout')) {
      errorMessage = "请求超时，请稍后再试";
    }
    
    // 如果不是取消请求错误，才显示错误信息
    if (!isRequestCanceled(error)) {
      Message.error(errorMessage);
    }
    
    return Promise.reject(error);
  }
);

// 防抖函数，用于减少频繁请求
export const debounce = (fn, delay = 300) => {
  let timer = null;
  return function(...args) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      fn.apply(this, args);
    }, delay);
  };
};

export default instance;
