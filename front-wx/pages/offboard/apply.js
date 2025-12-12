// 离职申请页面
const api = require('../../utils/api.js');

Page({
  data: {
    formData: {
      employeeName: '',
      position: '',
      offboardDate: '',
      reason: '',
      reasonDetail: '',
      phone: ''
    },
    reasons: ['个人原因', '家庭原因', '职业发展', '健康原因', '薪资待遇', '工作环境', '其他'],
    todayDate: '',
    applicationId: null
  },

  onLoad(options) {
    // 设置今天日期
    const today = new Date();
    const dateStr = this.formatDate(today);
    this.setData({
      todayDate: dateStr
    });

    // 加载用户信息
    this.loadUserInfo();

    // 如果有申请ID，加载草稿数据
    if (options.id) {
      this.setData({
        applicationId: options.id
      });
      this.loadDraft(options.id);
    }
  },

  // 加载用户信息
  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo) {
      this.setData({
        'formData.employeeName': userInfo.realName || '',
        'formData.phone': userInfo.phone || ''
      });
    }
  },

  // 姓名输入
  onEmployeeNameInput(e) {
    this.setData({
      'formData.employeeName': e.detail.value
    });
  },

  // 岗位输入
  onPositionInput(e) {
    this.setData({
      'formData.position': e.detail.value
    });
  },

  // 离职日期选择
  onOffboardDateChange(e) {
    this.setData({
      'formData.offboardDate': e.detail.value
    });
  },

  // 离职原因选择
  onReasonChange(e) {
    this.setData({
      'formData.reason': this.data.reasons[e.detail.value]
    });
  },

  // 详细说明输入
  onReasonDetailInput(e) {
    this.setData({
      'formData.reasonDetail': e.detail.value
    });
  },

  // 联系电话输入
  onPhoneInput(e) {
    this.setData({
      'formData.phone': e.detail.value
    });
  },

  // 保存草稿
  saveDraft() {
    if (!this.validateBasicInfo()) {
      return;
    }

    wx.showLoading({ title: '保存中...' });

    const that = this;
    
    if (this.data.applicationId) {
      // 更新草稿
      api.saveOffboardDraft(this.data.applicationId, this.data.formData).then(data => {
        wx.hideLoading();
        wx.showToast({
          title: '草稿已保存',
          icon: 'success'
        });
      }).catch(err => {
        wx.hideLoading();
      });
    } else {
      // 创建新申请
      api.createOffboardApplication(this.data.formData).then(data => {
        wx.hideLoading();
        wx.showToast({
          title: '草稿已保存',
          icon: 'success'
        });
        that.setData({
          applicationId: data.data
        });
      }).catch(err => {
        wx.hideLoading();
      });
    }
  },

  // 下一步
  nextStep() {
    if (!this.validateForm()) {
      return;
    }

    wx.showLoading({ title: '提交中...' });

    const that = this;

    if (this.data.applicationId) {
      // 更新并跳转
      api.saveOffboardDraft(this.data.applicationId, this.data.formData).then(data => {
        wx.hideLoading();
        wx.navigateTo({
          url: '/pages/offboard/confirm?id=' + that.data.applicationId
        });
      }).catch(err => {
        wx.hideLoading();
      });
    } else {
      // 创建并跳转
      api.createOffboardApplication(this.data.formData).then(data => {
        wx.hideLoading();
        const applicationId = data.data;
        wx.navigateTo({
          url: '/pages/offboard/confirm?id=' + applicationId
        });
      }).catch(err => {
        wx.hideLoading();
      });
    }
  },

  // 验证基本信息（保存草稿用）
  validateBasicInfo() {
    const { employeeName } = this.data.formData;
    
    if (!employeeName) {
      wx.showToast({
        title: '请输入姓名',
        icon: 'none'
      });
      return false;
    }
    
    return true;
  },

  // 验证表单
  validateForm() {
    const { employeeName, position, offboardDate, reason } = this.data.formData;
    
    if (!employeeName) {
      wx.showToast({ title: '请输入姓名', icon: 'none' });
      return false;
    }
    
    if (!position) {
      wx.showToast({ title: '请输入当前岗位', icon: 'none' });
      return false;
    }
    
    if (!offboardDate) {
      wx.showToast({ title: '请选择离职日期', icon: 'none' });
      return false;
    }
    
    if (!reason) {
      wx.showToast({ title: '请选择离职原因', icon: 'none' });
      return false;
    }
    
    return true;
  },

  // 加载草稿
  loadDraft(id) {
    wx.showLoading({ title: '加载中...' });
    
    api.getOffboardDetail(id).then(data => {
      wx.hideLoading();
      const app = data.data;
      this.setData({
        formData: {
          employeeName: app.employeeName,
          position: app.position,
          offboardDate: app.offboardDate,
          reason: app.reason,
          reasonDetail: app.reasonDetail || '',
          phone: app.phone || ''
        }
      });
    }).catch(err => {
      wx.hideLoading();
    });
  },

  // 格式化日期
  formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
});

