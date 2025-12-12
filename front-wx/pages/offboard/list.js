// 离职申请列表页面
const api = require('../../utils/api.js');

Page({
  data: {
    applications: [],
    stats: {
      pending: 0,
      approved: 0,
      rejected: 0
    },
    pageNum: 1,
    pageSize: 20,
    hasMore: true
  },

  onLoad() {
    this.loadApplications();
  },

  onShow() {
    // 每次显示时刷新列表
    this.loadApplications();
  },

  // 加载申请列表
  loadApplications() {
    wx.showLoading({ title: '加载中...' });
    
    const userInfo = wx.getStorageSync('userInfo');
    const employeeName = userInfo ? userInfo.realName : '';
    
    api.getMyOffboardApplications(employeeName, this.data.pageNum, this.data.pageSize).then(data => {
      wx.hideLoading();
      
      const records = data.data.records || [];
      
      // 格式化时间
      records.forEach(item => {
        if (item.createTime) {
          item.createTime = this.formatDateTime(item.createTime);
        }
      });
      
      // 计算统计数据
      const stats = {
        pending: 0,
        approved: 0,
        rejected: 0
      };
      
      records.forEach(item => {
        if (item.status === 'PENDING_APPROVAL' || item.status === 'PENDING_SIGN' || item.status === 'PENDING') {
          stats.pending++;
        } else if (item.status === 'APPROVED') {
          stats.approved++;
        } else if (item.status === 'REJECTED') {
          stats.rejected++;
        }
      });
      
      this.setData({
        applications: records,
        stats: stats
      });
    }).catch(err => {
      wx.hideLoading();
    });
  },

  // 前往详情页
  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: '/pages/offboard/detail?id=' + id
    });
  },

  // 前往申请页
  goToApply() {
    wx.navigateTo({
      url: '/pages/offboard/apply'
    });
  },

  // 格式化日期时间
  formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '';
    
    const date = new Date(dateTimeStr);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hour}:${minute}`;
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadApplications();
    wx.stopPullDownRefresh();
  }
});

