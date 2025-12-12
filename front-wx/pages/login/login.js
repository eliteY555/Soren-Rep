// pages/login/login.js
const api = require('../../utils/api.js');
const util = require('../../utils/util.js');

Page({
  data: {
    // 手机号密码登录
    phone: '',
    password: '',
    showPassword: false
  },

  onLoad() {
    // 页面加载
    const token = wx.getStorageSync('token');
    if (token) {
      wx.switchTab({
        url: '/pages/index/index'
      });
    }
  },
  
  onShow() {
    // 页面显示时打印日志
    console.log('========== 登录页面显示 ==========');
  },
  
  onReady() {
    // 页面渲染完成
    console.log('========== 登录页面渲染完成 ==========');
  },

  // 输入手机号
  onPhoneInput(e) {
    this.setData({
      phone: e.detail.value
    });
  },

  // 输入密码
  onPasswordInput(e) {
    this.setData({
      password: e.detail.value
    });
  },

  // 切换密码显示/隐藏
  togglePassword() {
    this.setData({
      showPassword: !this.data.showPassword
    });
  },

  // 手机号密码登录
  handleLogin() {
    wx.showToast({
      title: '按钮被点击了',
      icon: 'none'
    });
    
    const { phone, password } = this.data;

    // 表单验证
    if (!phone) {
      util.showError('请输入手机号');
      return;
    }

    if (!util.validatePhone(phone)) {
      util.showError('手机号格式不正确');
      return;
    }

    if (!password) {
      util.showError('请输入密码');
      return;
    }

    if (!util.validatePassword(password)) {
      util.showError('密码长度为6-20位');
      return;
    }

    util.showLoading('登录中...');

    api.login(phone, password).then(res => {
      util.hideLoading();
      
      // 保存Token和用户信息
      wx.setStorageSync('token', res.data.token);
      wx.setStorageSync('userInfo', {
        userId: res.data.userId,
        phone: res.data.phone,
        realName: res.data.realName
      });

      util.showSuccess('登录成功');
      
      // 跳转到首页
      setTimeout(() => {
        wx.switchTab({
          url: '/pages/index/index'
        });
      }, 1500);
    }).catch(err => {
      util.hideLoading();
      util.showError(err.message || '登录失败');
    });
  }
});
