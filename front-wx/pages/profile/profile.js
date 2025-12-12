// pages/profile/profile.js
const api = require('../../utils/api.js');
const util = require('../../utils/util.js');

Page({
  data: {
    userInfo: null,
    avatarUrl: ''
  },

  onLoad() {
    this.loadUserInfo();
  },

  onShow() {
    this.loadUserInfo();
  },

  // 加载用户信息
  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo');
    this.setData({
      userInfo: userInfo
    });

    // 加载头像（功能暂未实现）
    // 头像上传功能需要后端文件服务支持
  },

  // 上传头像
  uploadAvatar() {
    wx.showModal({
      title: '功能开发中',
      content: '头像上传功能正在开发中，敬请期待',
      showCancel: false,
      confirmText: '我知道了'
    });
  },

  // 跳转到个人信息设置
  goToSettings() {
    wx.navigateTo({
      url: '/pages/profile/settings'
    });
  },

  // 跳转到修改密码
  goToChangePassword() {
    wx.navigateTo({
      url: '/pages/profile/change-password'
    });
  },

  // 跳转到帮助与反馈
  goToHelp() {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },

  // 跳转到关于我们
  goToAbout() {
    wx.showModal({
      title: '关于我们',
      content: '泰来云员工管理系统 v1.2.0\n\n保安公司入职离职管理平台\n\n© 2025 Tailai Cloud',
      showCancel: false,
      confirmText: '知道了'
    });
  },

  // 退出登录
  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          util.showLoading('退出中...');
          
          api.logout().then(() => {
            util.hideLoading();
            
            // 清除本地存储
            wx.removeStorageSync('token');
            wx.removeStorageSync('userInfo');
            
            util.showSuccess('已退出登录');
            
            // 跳转到登录页
            setTimeout(() => {
              wx.reLaunch({
                url: '/pages/login/login'
              });
            }, 1500);
          }).catch(() => {
            util.hideLoading();
            
            // 即使接口失败也清除本地数据
            wx.removeStorageSync('token');
            wx.removeStorageSync('userInfo');
            
            wx.reLaunch({
              url: '/pages/login/login'
            });
          });
        }
      }
    });
  }
});
