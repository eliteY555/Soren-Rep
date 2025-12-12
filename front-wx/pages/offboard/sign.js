// 离职电子签名页面
const api = require('../../utils/api.js');

Page({
  data: {
    applicationId: null,
    applicationNo: '',
    employeeName: '',
    offboardDate: '',
    canvasContext: null,
    hasDrawn: false,
    lastPoint: null
  },

  onLoad(options) {
    if (options.id) {
      this.setData({
        applicationId: options.id
      });
      this.loadApplicationData(options.id);
      this.initCanvas();
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

  // 加载申请数据
  loadApplicationData(id) {
    api.getOffboardDetail(id).then(data => {
      const app = data.data;
      this.setData({
        applicationNo: app.applicationNo || '待生成',
        employeeName: app.employeeName,
        offboardDate: app.offboardDate
      });
    }).catch(err => {
      console.error('加载失败', err);
    });
  },

  // 初始化画布
  initCanvas() {
    const ctx = wx.createCanvasContext('signCanvas', this);
    ctx.setStrokeStyle('#000000');
    ctx.setLineWidth(4);
    ctx.setLineCap('round');
    ctx.setLineJoin('round');
    
    this.setData({
      canvasContext: ctx
    });
  },

  // 触摸开始
  touchStart(e) {
    const ctx = this.data.canvasContext;
    const touch = e.touches[0];
    
    this.setData({
      lastPoint: { x: touch.x, y: touch.y },
      hasDrawn: true
    });
  },

  // 触摸移动
  touchMove(e) {
    const ctx = this.data.canvasContext;
    const touch = e.touches[0];
    const lastPoint = this.data.lastPoint;
    
    if (lastPoint) {
      ctx.beginPath();
      ctx.moveTo(lastPoint.x, lastPoint.y);
      ctx.lineTo(touch.x, touch.y);
      ctx.stroke();
      ctx.draw(true);
      
      this.setData({
        lastPoint: { x: touch.x, y: touch.y }
      });
    }
  },

  // 触摸结束
  touchEnd(e) {
    this.setData({
      lastPoint: null
    });
  },

  // 清空签名
  clearSignature() {
    const ctx = this.data.canvasContext;
    ctx.clearRect(0, 0, 700, 400);
    ctx.draw();
    
    this.setData({
      hasDrawn: false
    });
    
    wx.showToast({
      title: '已清空',
      icon: 'success',
      duration: 1000
    });
  },

  // 预览签名
  previewSignature() {
    if (!this.data.hasDrawn) {
      wx.showToast({
        title: '请先签名',
        icon: 'none'
      });
      return;
    }
    
    wx.canvasToTempFilePath({
      canvasId: 'signCanvas',
      success: (res) => {
        wx.previewImage({
          urls: [res.tempFilePath],
          current: res.tempFilePath
        });
      }
    }, this);
  },

  // 提交签名
  submitSignature() {
    if (!this.data.hasDrawn) {
      wx.showToast({
        title: '请先签名',
        icon: 'none'
      });
      return;
    }
    
    wx.showModal({
      title: '确认签名',
      content: '签名后将提交审批，确认签名吗？',
      confirmColor: '#1890ff',
      success: (res) => {
        if (res.confirm) {
          this.saveSignature();
        }
      }
    });
  },

  // 保存签名
  saveSignature() {
    wx.showLoading({ title: '提交中...' });
    
    wx.canvasToTempFilePath({
      canvasId: 'signCanvas',
      success: (res) => {
        wx.getFileSystemManager().readFile({
          filePath: res.tempFilePath,
          encoding: 'base64',
          success: (fileRes) => {
            const signatureBase64 = 'data:image/png;base64,' + fileRes.data;
            
            // 调用离职签名接口
            api.offboardSign({
              applicationId: this.data.applicationId,
              signatureBase64: signatureBase64,
              signerName: this.data.employeeName
            }).then(data => {
              wx.hideLoading();
              wx.showToast({
                title: '签名成功',
                icon: 'success'
              });
              
              setTimeout(() => {
                wx.redirectTo({
                  url: '/pages/offboard/list'
                });
              }, 1500);
            }).catch(err => {
              wx.hideLoading();
            });
          },
          fail: (err) => {
            wx.hideLoading();
            wx.showToast({
              title: '签名失败',
              icon: 'none'
            });
          }
        });
      },
      fail: (err) => {
        wx.hideLoading();
        wx.showToast({
          title: '签名失败',
          icon: 'none'
        });
      }
    }, this);
  },

  // 返回
  goBack() {
    wx.navigateBack();
  }
});

