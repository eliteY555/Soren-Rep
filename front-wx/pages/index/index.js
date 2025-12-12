// 功能页（首页）
const api = require('../../utils/api.js');

Page({
  data: {
    userName: '员工',
    userPhone: '',
    pendingCount: 0,
    signedCount: 0
  },

  onLoad() {
    this.loadUserInfo();
    this.loadContractStats();
  },

  onShow() {
    // 每次显示时刷新数据
    this.loadUserInfo();
    this.loadContractStats();
  },

  // 加载用户信息
  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo) {
      this.setData({
        userName: userInfo.realName || '员工',
        userPhone: userInfo.phone || ''
      });
    }
  },

  // 加载合同统计数据
  loadContractStats() {
    // 获取待签署合同
    api.getPendingContracts().then(res => {
      console.log('待签署合同返回:', res);
      if (res.code === 200 && res.data) {
        this.setData({
          pendingCount: Array.isArray(res.data) ? res.data.length : 0
        });
      }
    }).catch(err => {
      console.error('获取待签署合同失败', err);
    });

    // 获取已签署合同
    api.getMyContracts(2).then(res => {
      console.log('已签署合同返回:', res);
      if (res.code === 200 && res.data) {
        this.setData({
          signedCount: Array.isArray(res.data) ? res.data.length : 0
        });
      }
    }).catch(err => {
      console.error('获取已签署合同失败', err);
    });
  },

  // 前往待签署合同
  goToPendingContracts() {
    wx.navigateTo({
      url: '/pages/contract/contract?tab=0'
    });
  },

  // 前往已签署合同
  goToSignedContracts() {
    wx.navigateTo({
      url: '/pages/contract/contract?tab=1'
    });
  }
});
