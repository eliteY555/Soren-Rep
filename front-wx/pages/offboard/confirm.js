// 离职确认页面
const api = require('../../utils/api.js');

Page({
  data: {
    applicationId: null,
    offboardData: {}
  },

  onLoad(options) {
    if (options.id) {
      this.setData({
        applicationId: options.id
      });
      this.loadData(options.id);
    }
  },

  // 加载数据
  loadData(id) {
    wx.showLoading({ title: '加载中...' });
    
    api.getOffboardDetail(id).then(data => {
      wx.hideLoading();
      this.setData({
        offboardData: data.data
      });
    }).catch(err => {
      wx.hideLoading();
    });
  },

  // 返回修改
  goBack() {
    wx.navigateBack();
  },

  // 前往签名页面
  goToSign() {
    wx.navigateTo({
      url: '/pages/offboard/sign?id=' + this.data.applicationId
    });
  }
});

