// 入职申请页面
const api = require('../../utils/api.js');

Page({
  data: {
    formData: {
      employeeName: '',
      position: '',
      startDate: '',
      endDate: '',
      baseSalary: '',
      templateId: null
    },
    positions: ['保安员', '队长', '管理岗', '实习生'],
    todayDate: '',
    contractDuration: '',
    idCardFrontUrl: '',
    idCardBackUrl: '',
    idCardFrontPath: '',
    idCardBackPath: '',
    applicationId: null
  },

  onLoad(options) {
    // 设置今天日期
    const today = new Date();
    const dateStr = this.formatDate(today);
    this.setData({
      todayDate: dateStr
    });

    // 如果有申请ID，加载草稿数据
    if (options.id) {
      this.setData({
        applicationId: options.id
      });
      this.loadDraft(options.id);
    }
  },

  // 姓名输入
  onEmployeeNameInput(e) {
    this.setData({
      'formData.employeeName': e.detail.value
    });
  },

  // 岗位选择
  onPositionChange(e) {
    this.setData({
      'formData.position': this.data.positions[e.detail.value]
    });
  },

  // 开始日期选择
  onStartDateChange(e) {
    this.setData({
      'formData.startDate': e.detail.value
    });
    this.calculateDuration();
  },

  // 结束日期选择
  onEndDateChange(e) {
    this.setData({
      'formData.endDate': e.detail.value
    });
    this.calculateDuration();
  },

  // 基本工资输入
  onBaseSalaryInput(e) {
    this.setData({
      'formData.baseSalary': e.detail.value
    });
  },

  // 计算合同期限
  calculateDuration() {
    const { startDate, endDate } = this.data.formData;
    if (startDate && endDate) {
      const start = new Date(startDate);
      const end = new Date(endDate);
      const days = Math.floor((end - start) / (1000 * 60 * 60 * 24));
      const years = Math.floor(days / 365);
      const months = Math.floor((days % 365) / 30);
      
      let duration = '';
      if (years > 0) duration += years + '年';
      if (months > 0) duration += months + '个月';
      
      this.setData({
        contractDuration: duration || days + '天'
      });
    }
  },

  // 上传身份证正面
  uploadIdCardFront() {
    this.chooseImage('id-card-front');
  },

  // 上传身份证反面
  uploadIdCardBack() {
    this.chooseImage('id-card-back');
  },

  // 选择图片
  chooseImage(type) {
    const that = this;
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success(res) {
        const tempFilePath = res.tempFilePaths[0];
        
        wx.showLoading({ title: '上传中...' });
        
        // 调用上传接口
        api.uploadCertificate(tempFilePath, type).then(data => {
          wx.hideLoading();
          wx.showToast({
            title: '上传成功',
            icon: 'success'
          });
          
          if (type === 'id-card-front') {
            that.setData({
              idCardFrontUrl: tempFilePath,
              idCardFrontPath: data.data.ossPath
            });
          } else {
            that.setData({
              idCardBackUrl: tempFilePath,
              idCardBackPath: data.data.ossPath
            });
          }
        }).catch(err => {
          wx.hideLoading();
          console.error('上传失败', err);
        });
      }
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
      api.saveDraft(this.data.applicationId, this.data.formData).then(data => {
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
      api.createOnboardApplication(this.data.formData).then(data => {
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
      api.saveDraft(this.data.applicationId, this.data.formData).then(data => {
        wx.hideLoading();
        wx.navigateTo({
          url: '/pages/onboard/contract?id=' + that.data.applicationId
        });
      }).catch(err => {
        wx.hideLoading();
      });
    } else {
      // 创建并跳转
      api.createOnboardApplication(this.data.formData).then(data => {
        wx.hideLoading();
        const applicationId = data.data;
        wx.navigateTo({
          url: '/pages/onboard/contract?id=' + applicationId
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
    const { employeeName, position, startDate, endDate, baseSalary } = this.data.formData;
    
    if (!employeeName) {
      wx.showToast({ title: '请输入姓名', icon: 'none' });
      return false;
    }
    
    if (!position) {
      wx.showToast({ title: '请选择岗位', icon: 'none' });
      return false;
    }
    
    if (!startDate) {
      wx.showToast({ title: '请选择合同开始日期', icon: 'none' });
      return false;
    }
    
    if (!endDate) {
      wx.showToast({ title: '请选择合同结束日期', icon: 'none' });
      return false;
    }
    
    if (!baseSalary) {
      wx.showToast({ title: '请输入基本工资', icon: 'none' });
      return false;
    }
    
    if (!this.data.idCardFrontPath || !this.data.idCardBackPath) {
      wx.showToast({ title: '请上传身份证照片', icon: 'none' });
      return false;
    }
    
    return true;
  },

  // 加载草稿
  loadDraft(id) {
    wx.showLoading({ title: '加载中...' });
    
    api.getApplicationDetail(id).then(data => {
      wx.hideLoading();
      const app = data.data;
      this.setData({
        formData: {
          employeeName: app.employeeName,
          position: app.position,
          startDate: app.startDate,
          endDate: app.endDate,
          baseSalary: app.baseSalary,
          templateId: app.templateId
        }
      });
      this.calculateDuration();
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

