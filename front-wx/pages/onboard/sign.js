// 电子签名页面
const api = require('../../utils/api.js');

Page({
  data: {
    applicationId: null,
    applicationNo: '',
    employeeName: '',
    startDate: '',
    endDate: '',
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
    api.getApplicationDetail(id).then(data => {
      const app = data.data;
      this.setData({
        applicationNo: app.applicationNo,
        employeeName: app.employeeName,
        startDate: app.startDate,
        endDate: app.endDate
      });
    }).catch(err => {
      console.error('加载失败', err);
    });
  },

  // 初始化画布
  initCanvas() {
    const ctx = wx.createCanvasContext('signCanvas', this);
    ctx.setStrokeStyle('#000000');
    ctx.setLineWidth(4); // 增加线宽，使签名更清晰
    ctx.setLineCap('round');
    ctx.setLineJoin('round');
    // 确保是实线（默认就是实线，不需要设置lineDash）
    
    this.setData({
      canvasContext: ctx
    });
  },

  // 触摸开始
  touchStart(e) {
    const ctx = this.data.canvasContext;
    const touch = e.touches[0];
    
    // 保存起始点
    this.setData({
      lastPoint: { x: touch.x, y: touch.y },
      hasDrawn: true
    });
  },

  // 触摸移动 - 连续绘制实线
  touchMove(e) {
    const ctx = this.data.canvasContext;
    const touch = e.touches[0];
    const lastPoint = this.data.lastPoint;
    
    if (lastPoint) {
      // 绘制从上一个点到当前点的线段
      ctx.beginPath();
      ctx.moveTo(lastPoint.x, lastPoint.y);
      ctx.lineTo(touch.x, touch.y);
      ctx.stroke();
      ctx.draw(true); // 保留之前的内容
      
      // 更新最后一个点
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
      confirmColor: '#667eea',
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
    
    // 将Canvas转为图片
    wx.canvasToTempFilePath({
      canvasId: 'signCanvas',
      success: (res) => {
        // 将图片转为Base64
        wx.getFileSystemManager().readFile({
          filePath: res.tempFilePath,
          encoding: 'base64',
          success: (fileRes) => {
            const signatureBase64 = 'data:image/png;base64,' + fileRes.data;
            
            // 调用签名接口
            api.employeeSign({
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
                  url: '/pages/onboard/list'
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

