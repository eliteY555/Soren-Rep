// pages/profile/settings.js
const api = require('../../utils/api.js');
const util = require('../../utils/util.js');

Page({
  data: {
    formData: {
      realName: '',
      phone: '',
      idCard: '',
      gender: null
    },
    originalData: {},
    genderOptions: [
      { label: '未设置', value: null },
      { label: '男', value: 1 },
      { label: '女', value: 2 }
    ],
    genderIndex: 0
  },

  onLoad() {
    this.loadUserInfo();
  },

  // 加载用户信息（优化：先从缓存加载，再请求最新数据）
  loadUserInfo() {
    // 先从本地缓存快速加载
    const cachedUserInfo = wx.getStorageSync('userInfo');
    if (cachedUserInfo) {
      this.setUserInfoData(cachedUserInfo);
    }
    
    // 再请求最新数据
    api.getUserInfo().then(res => {
      if (res.code === 200 && res.data) {
        const userInfo = res.data;
        // 更新缓存
        wx.setStorageSync('userInfo', userInfo);
        this.setUserInfoData(userInfo);
      }
    }).catch(err => {
      console.error('获取用户信息失败', err);
      // 如果有缓存数据就不提示错误
      if (!cachedUserInfo) {
        util.showError('加载用户信息失败');
      }
    });
  },
  
  // 设置用户信息到页面
  setUserInfoData(userInfo) {
    // 设置性别选择器索引
    let genderIndex = 0;
    if (userInfo.gender === 1) {
      genderIndex = 1;
    } else if (userInfo.gender === 2) {
      genderIndex = 2;
    }
    
    const formData = {
      realName: userInfo.realName || '',
      phone: userInfo.phone || '',
      idCard: userInfo.idCard || '',
      gender: userInfo.gender
    };
    
    this.setData({
      formData: formData,
      originalData: JSON.parse(JSON.stringify(formData)),
      genderIndex: genderIndex
    });
  },

  // 性别选择改变
  onGenderChange(e) {
    const index = e.detail.value;
    this.setData({
      genderIndex: index,
      'formData.gender': this.data.genderOptions[index].value
    });
  },

  // 提交表单
  handleSubmit(e) {
    const formValue = e.detail.value;
    
    // 校验手机号
    if (!formValue.phone) {
      util.showError('请输入手机号码');
      return;
    }
    
    if (!/^1[3-9]\d{9}$/.test(formValue.phone)) {
      util.showError('请输入正确的手机号码');
      return;
    }
    
    // 只提交手机号
    const updateData = {
      phone: formValue.phone
    };
    
    // 提交更新
    util.showLoading('保存中...');
    
    api.updateUserInfo(updateData).then(res => {
      util.hideLoading();
      
      if (res.code === 200) {
        util.showSuccess('手机号修改成功');
        
        // 更新本地存储的用户信息
        const userInfo = wx.getStorageSync('userInfo') || {};
        userInfo.phone = formValue.phone;
        wx.setStorageSync('userInfo', userInfo);
        
        // 延迟返回
        setTimeout(() => {
          wx.navigateBack();
        }, 1500);
      }
    }).catch(err => {
      util.hideLoading();
      util.showError(err.msg || '保存失败');
    });
  },

  // 跳转到修改密码页面
  goToChangePassword() {
    wx.navigateTo({
      url: '/pages/profile/change-password'
    });
  }
});

