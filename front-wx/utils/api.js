// API请求工具类

// 后端API地址
// 开发环境：使用局域网IP（真机调试）
// 生产环境：使用正式域名
const API_BASE = 'http://192.168.40.102:8081'; // 后端API地址（使用真实WiFi局域网IP）

// 标记是否正在处理token失效（避免重复跳转）
let isHandlingTokenExpired = false;

/**
 * 处理token失效
 */
function handleTokenExpired() {
  if (isHandlingTokenExpired) {
    return;
  }
  
  isHandlingTokenExpired = true;
  
  // 清除本地存储
  wx.removeStorageSync('token');
  wx.removeStorageSync('userInfo');
  
  // 提示用户
  wx.showToast({
    title: '登录已过期，请重新登录',
    icon: 'none',
    duration: 2000
  });
  
  // 延迟跳转，让用户看到提示
  setTimeout(() => {
    wx.reLaunch({
      url: '/pages/login/login',
      complete: () => {
        isHandlingTokenExpired = false;
      }
    });
  }, 1000);
}

/**
 * 通用请求方法
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    
    wx.request({
      url: API_BASE + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : '',
        ...options.header
      },
      success: (res) => {
        if (res.statusCode === 200) {
          if (res.data && res.data.code === 200) {
            resolve(res.data);
          } else if (res.data && res.data.code === 401) {
            // 业务层返回401（Token无效或过期）
            handleTokenExpired();
            reject(new Error('Token已失效'));
          } else {
            // 显示后端返回的错误消息
            const errorMsg = (res.data && res.data.message) || '请求失败';
            wx.showToast({
              title: errorMsg,
              icon: 'none',
              duration: 2000
            });
            reject(res.data || {message: errorMsg});
          }
        } else if (res.statusCode === 401) {
          // HTTP状态码401（未授权）
          handleTokenExpired();
          reject(new Error('Token已失效'));
        } else if (res.statusCode === 403) {
          // 权限不足
          wx.showToast({
            title: '权限不足',
            icon: 'none',
            duration: 2000
          });
          reject({message: '权限不足'});
        } else if (res.statusCode === 404) {
          // 资源不存在
          wx.showToast({
            title: '请求的资源不存在',
            icon: 'none',
            duration: 2000
          });
          reject({message: '请求的资源不存在'});
        } else if (res.statusCode >= 500) {
          // 服务器错误
          wx.showToast({
            title: '服务器错误，请稍后重试',
            icon: 'none',
            duration: 2000
          });
          reject({message: '服务器错误'});
        } else {
          wx.showToast({
            title: '网络错误',
            icon: 'none',
            duration: 2000
          });
          reject({message: '网络错误'});
        }
      },
      fail: (err) => {
        wx.showToast({
          title: '网络请求失败',
          icon: 'none',
          duration: 2000
        });
        reject(err);
      }
    });
  });
}

/**
 * GET请求
 */
function get(url, data = {}) {
  return request({
    url: url,
    method: 'GET',
    data: data
  });
}

/**
 * POST请求
 */
function post(url, data = {}) {
  return request({
    url: url,
    method: 'POST',
    data: data
  });
}

/**
 * 用户登录（手机号+密码）
 */
function login(phone, password) {
  return post('/api/miniapp/auth/login', {
    phone: phone,
    password: password
  });
}

/**
 * 验证Token
 */
function validateToken() {
  return get('/api/miniapp/auth/validate');
}

/**
 * 获取用户信息
 */
function getUserInfo() {
  return get('/api/miniapp/auth/userinfo');
}

/**
 * 退出登录
 */
function logout() {
  return post('/api/miniapp/auth/logout');
}

/**
 * 更新用户信息
 */
function updateUserInfo(data) {
  return post('/api/miniapp/user/update', data);
}

/**
 * 修改密码
 */
function changePassword(data) {
  return post('/api/miniapp/user/change-password', data);
}

/**
 * 上传头像
 */
function uploadAvatar(filePath) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    
    wx.uploadFile({
      url: API_BASE + '/api/miniapp/user/upload-avatar',
      filePath: filePath,
      name: 'file',
      header: {
        'Authorization': token ? 'Bearer ' + token : ''
      },
      success: (res) => {
        const data = JSON.parse(res.data);
        if (data.code === 200) {
          resolve(data);
        } else {
          wx.showToast({
            title: data.message || '上传失败',
            icon: 'none'
          });
          reject(data);
        }
      },
      fail: (err) => {
        wx.showToast({
          title: '上传失败',
          icon: 'none'
        });
        reject(err);
      }
    });
  });
}

// ==================== 合同相关 ====================

/**
 * 获取待签合同列表
 */
function getPendingContracts() {
  return get('/api/miniapp/contract/pending');
}

/**
 * 获取我的合同列表
 */
function getMyContracts(status) {
  const params = {};
  // 只有当 status 有值时才添加到参数中
  if (status !== undefined && status !== null) {
    params.status = status;
  }
  return get('/api/miniapp/contract/my-list', params);
}

/**
 * 获取合同详情
 */
function getContractDetail(id) {
  return get('/api/miniapp/contract/detail/' + id);
}

/**
 * 员工签署合同
 */
function signContract(data) {
  return post('/api/miniapp/contract/sign', data);
}

/**
 * 获取合同PDF临时访问URL
 */
function getContractPdfUrl(id) {
  return get('/api/miniapp/contract/pdf-url/' + id);
}

module.exports = {
  request,
  get,
  post,
  login,
  validateToken,
  getUserInfo,
  logout,
  updateUserInfo,
  changePassword,
  uploadAvatar,
  // 合同管理
  getPendingContracts,
  getMyContracts,
  getContractDetail,
  signContract,
  getContractPdfUrl
};

