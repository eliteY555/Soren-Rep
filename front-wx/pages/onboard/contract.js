// 合同填写页面
const api = require('../../utils/api.js');

Page({
  data: {
    applicationId: null,
    contractData: {
      employeeName: '',
      position: '',
      startDate: '',
      endDate: '',
      baseSalary: '',
      department: ''
    },
    templates: [],
    selectedTemplateIndex: -1,
    selectedTemplateId: null
  },

  onLoad(options) {
    if (options.id) {
      this.setData({
        applicationId: options.id
      });
      this.loadTemplates();
      this.loadContractData(options.id);
    } else {
      wx.showToast({
        title: '参数错误',
        icon: 'none'
      });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }
  },

  // 加载模板列表
  loadTemplates() {
    api.getTemplateList().then(data => {
      this.setData({
        templates: data.data || []
      });
      // 如果只有一个模板，自动选中
      if (data.data && data.data.length === 1) {
        this.setData({
          selectedTemplateIndex: 0,
          selectedTemplateId: data.data[0].id
        });
      }
    }).catch(err => {
      console.error('加载模板列表失败', err);
    });
  },

  // 模板选择改变
  onTemplateChange(e) {
    const index = parseInt(e.detail.value);
    this.setData({
      selectedTemplateIndex: index,
      selectedTemplateId: this.data.templates[index].id
    });
  },

  // 加载合同数据
  loadContractData(id) {
    wx.showLoading({ title: '加载中...' });
    
    api.getApplicationDetail(id).then(data => {
      wx.hideLoading();
      const app = data.data;
      this.setData({
        contractData: {
          employeeName: app.employeeName,
          position: app.position,
          startDate: app.startDate,
          endDate: app.endDate,
          baseSalary: app.baseSalary,
          department: app.department
        }
      });
    }).catch(err => {
      wx.hideLoading();
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    });
  },

  // 返回修改
  goBack() {
    wx.navigateBack();
  },

  // 前往签名页面
  goToSign() {
    // 验证是否选择了模板
    if (!this.data.selectedTemplateId) {
      wx.showToast({
        title: '请选择合同模板',
        icon: 'none'
      });
      return;
    }

    // 保存合同信息
    wx.showLoading({ title: '保存中...' });
    
    const contractData = {
      applicationId: this.data.applicationId,
      ...this.data.contractData,
      templateId: this.data.selectedTemplateId
    };
    
    api.fillContract(contractData).then(data => {
      wx.hideLoading();
      wx.navigateTo({
        url: '/pages/onboard/sign?id=' + this.data.applicationId
      });
    }).catch(err => {
      wx.hideLoading();
    });
  }
});

