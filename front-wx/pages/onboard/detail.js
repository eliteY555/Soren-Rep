// 申请详情页面
const api = require('../../utils/api.js');

Page({
  data: {
    applicationId: null,
    application: {},
    statusClass: '',
    statusIcon: '',
    statusTitle: '',
    statusDesc: '',
    showTimeline: false
  },

  onLoad(options) {
    if (options.id) {
      this.setData({
        applicationId: options.id
      });
      this.loadDetail(options.id);
    }
  },

  // 加载详情
  loadDetail(id) {
    wx.showLoading({ title: '加载中...' });
    
    api.getApplicationDetail(id).then(data => {
      wx.hideLoading();
      const app = data.data;
      
      // 格式化时间
      if (app.createTime) {
        app.createTime = this.formatDateTime(app.createTime);
      }
      if (app.approvalTime) {
        app.approvalTime = this.formatDateTime(app.approvalTime);
      }
      
      // 设置状态信息
      this.setStatusInfo(app.status);
      
      this.setData({
        application: app,
        showTimeline: app.status !== 'PENDING'
      });
    }).catch(err => {
      wx.hideLoading();
    });
  },

  // 设置状态信息
  setStatusInfo(status) {
    let statusClass = '';
    let statusIcon = '';
    let statusTitle = '';
    let statusDesc = '';
    
    switch(status) {
      case 'PENDING':
        statusClass = 'status-pending';
        statusIcon = '📝';
        statusTitle = '待填写合同';
        statusDesc = '请继续完成入职申请流程';
        break;
      case 'PENDING_SIGN':
        statusClass = 'status-pending';
        statusIcon = '✍️';
        statusTitle = '待员工签字';
        statusDesc = '请完成电子签名';
        break;
      case 'PENDING_APPROVAL':
        statusClass = 'status-pending';
        statusIcon = '⏰';
        statusTitle = '待领导审批并签字';
        statusDesc = '已提交审批，请耐心等待';
        break;
      case 'APPROVED':
        statusClass = 'status-success';
        statusIcon = '✅';
        statusTitle = '审批通过';
        statusDesc = '合同已生效，欢迎加入公司！';
        break;
      case 'REJECTED':
        statusClass = 'status-error';
        statusIcon = '❌';
        statusTitle = '已驳回';
        statusDesc = '申请未通过审批';
        break;
      default:
        statusClass = 'status-pending';
        statusIcon = '📋';
        statusTitle = status;
        statusDesc = '';
    }
    
    this.setData({
      statusClass,
      statusIcon,
      statusTitle,
      statusDesc
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
  }
});

