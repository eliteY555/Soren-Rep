/**
 * 工具函数
 */

/**
 * 格式化时间
 */
const formatTime = date => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  return `${[year, month, day].map(formatNumber).join('-')} ${[hour, minute, second].map(formatNumber).join(':')}`
}

const formatNumber = n => {
  n = n.toString()
  return n[1] ? n : `0${n}`
}

/**
 * 验证手机号
 */
function validatePhone(phone) {
  return /^1[3-9]\d{9}$/.test(phone);
}

/**
 * 验证用户名
 */
function validateUsername(username) {
  return /^[a-zA-Z0-9_]{3,20}$/.test(username);
}

/**
 * 验证密码
 */
function validatePassword(password) {
  return password.length >= 6 && password.length <= 20;
}

/**
 * 显示加载提示
 */
function showLoading(title = '加载中...') {
  wx.showLoading({
    title: title,
    mask: true
  });
}

/**
 * 隐藏加载提示
 */
function hideLoading() {
  wx.hideLoading();
}

/**
 * 显示成功提示
 */
function showSuccess(message) {
  wx.showToast({
    title: message,
    icon: 'success',
    duration: 2000
  });
}

/**
 * 显示错误提示
 */
function showError(message) {
  wx.showToast({
    title: message,
    icon: 'none',
    duration: 2000
  });
}

module.exports = {
  formatTime,
  validatePhone,
  validateUsername,
  validatePassword,
  showLoading,
  hideLoading,
  showSuccess,
  showError
}

