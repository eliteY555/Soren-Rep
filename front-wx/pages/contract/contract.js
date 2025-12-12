// pages/contract/contract.js
const api = require('../../utils/api.js');
const util = require('../../utils/util.js');

Page({
  data: {
    activeTab: 0, // 0: 待签署, 1: 已签署
    contractList: [],
    loading: false,
    showDetailModal: false,
    currentContract: null,
    showSignModal: false,
    signatureData: '', // 签名数据
    canvasWidth: 300,
    canvasHeight: 200,
    isDrawing: false,
    lastPoint: null
  },

  onLoad(options) {
    // 如果有传递 tab 参数，设置当前标签
    if (options.tab !== undefined) {
      const tabIndex = parseInt(options.tab);
      this.setData({
        activeTab: tabIndex
      });
    }
    this.loadContracts();
  },

  onShow() {
    // 每次显示页面时刷新合同列表
    this.loadContracts();
  },

  // 切换标签
  switchTab(e) {
    const index = parseInt(e.currentTarget.dataset.index);
    console.log('切换到标签:', index);
    
    // 如果点击的是当前标签，不处理
    if (this.data.activeTab === index) {
      return;
    }
    
    this.setData({
      activeTab: index,
      contractList: [] // 先清空列表，显示加载状态
    });
    
    this.loadContracts();
  },

  // 加载合同列表
  loadContracts() {
    const { activeTab, loading } = this.data;
    
    // 防抖：如果正在加载，取消上一次请求
    if (loading) {
      console.log('取消上一次请求');
    }
    
    this.setData({ loading: true });

    let promise;

    if (activeTab === 0) {
      // 待签署
      console.log('===== 加载待签署合同 =====');
      promise = api.getPendingContracts();
    } else if (activeTab === 1) {
      // 已签署
      console.log('===== 加载已签署合同 =====');
      promise = api.getMyContracts(2);
    } else {
      console.error('未知的标签索引:', activeTab);
      this.setData({ loading: false });
      return;
    }

    promise.then(res => {
      console.log('API返回:', res);
      const contracts = res.data || [];
      console.log('合同数量:', contracts.length);
      console.log('合同列表:', contracts);
      
      this.setData({
        contractList: contracts,
        loading: false
      });
    }).catch(err => {
      console.error('加载合同列表失败:', err);
      this.setData({ 
        contractList: [],
        loading: false 
      });
      wx.showToast({
        title: '加载失败',
        icon: 'none',
        duration: 1500
      });
    });
  },

  // 查看合同详情
  viewContract(e) {
    const id = e.currentTarget.dataset.id;
    
    util.showLoading('加载中...');
    api.getContractDetail(id).then(res => {
      util.hideLoading();
      this.setData({
        currentContract: res.data,
        showDetailModal: true
      });
    }).catch(err => {
      util.hideLoading();
      console.error('获取合同详情失败:', err);
    });
  },

  // 关闭详情弹窗
  closeDetailModal() {
    this.setData({
      showDetailModal: false,
      currentContract: null
    });
  },

  // 查看PDF（预览）
  viewPdf() {
    const contract = this.data.currentContract;
    if (!contract || !contract.contractPdfUrl) {
      util.showError('合同PDF不存在');
      return;
    }

    util.showLoading('加载中...');
    api.getContractPdfUrl(contract.id).then(res => {
      util.hideLoading();
      const pdfUrl = res.data;
      
      // 下载并打开PDF（预览）
      wx.downloadFile({
        url: pdfUrl,
        success: (res) => {
          const filePath = res.tempFilePath;
          wx.openDocument({
            filePath: filePath,
            fileType: 'pdf',
            success: function () {
              console.log('打开文档成功');
            },
            fail: function(err) {
              console.error('打开文档失败:', err);
              util.showError('打开文档失败');
            }
          });
        },
        fail: (err) => {
          console.error('下载文件失败:', err);
          util.showError('下载文件失败');
        }
      });
    }).catch(err => {
      util.hideLoading();
      console.error('获取PDF URL失败:', err);
    });
  },

  // 下载PDF（保存到本地）
  downloadPdf() {
    const contract = this.data.currentContract;
    if (!contract || !contract.contractPdfUrl) {
      util.showError('合同PDF不存在');
      return;
    }

    if (contract.status !== 2) {
      util.showError('只能下载已生效的合同');
      return;
    }

    util.showLoading('下载中...');
    api.getContractPdfUrl(contract.id).then(res => {
      util.hideLoading();
      const pdfUrl = res.data;
      
      // 下载文件到本地
      wx.downloadFile({
        url: pdfUrl,
        success: (res) => {
          if (res.statusCode === 200) {
            const filePath = res.tempFilePath;
            
            // 保存文件到相册或本地
            wx.saveFile({
              tempFilePath: filePath,
              success: (saveRes) => {
                const savedPath = saveRes.savedFilePath;
                wx.showModal({
                  title: '下载成功',
                  content: '合同已保存，是否立即打开？',
                  success: (modalRes) => {
                    if (modalRes.confirm) {
                      wx.openDocument({
                        filePath: savedPath,
                        fileType: 'pdf',
                        success: () => {
                          console.log('打开文档成功');
                        },
                        fail: (err) => {
                          console.error('打开文档失败:', err);
                          util.showError('打开文档失败');
                        }
                      });
                    }
                  }
                });
              },
              fail: (err) => {
                console.error('保存文件失败:', err);
                util.showError('保存文件失败');
              }
            });
          }
        },
        fail: (err) => {
          console.error('下载文件失败:', err);
          util.showError('下载文件失败');
        }
      });
    }).catch(err => {
      util.hideLoading();
      console.error('获取PDF URL失败:', err);
    });
  },

  // 打开签名弹窗
  openSignModal() {
    this.setData({
      showSignModal: true,
      showDetailModal: false
    }, () => {
      // 初始化签名画布
      this.initCanvas();
    });
  },

  // 初始化签名画布
  initCanvas() {
    const ctx = wx.createCanvasContext('signCanvas', this);
    ctx.setStrokeStyle('#000000');
    ctx.setLineWidth(3);
    ctx.setLineCap('round');
    ctx.setLineJoin('round');
    this.ctx = ctx;
  },

  // 开始绘制
  touchStart(e) {
    this.setData({
      isDrawing: true,
      lastPoint: {
        x: e.touches[0].x,
        y: e.touches[0].y
      }
    });
  },

  // 绘制中
  touchMove(e) {
    if (!this.data.isDrawing) return;

    const point = {
      x: e.touches[0].x,
      y: e.touches[0].y
    };

    this.ctx.moveTo(this.data.lastPoint.x, this.data.lastPoint.y);
    this.ctx.lineTo(point.x, point.y);
    this.ctx.stroke();
    this.ctx.draw(true);

    this.setData({
      lastPoint: point
    });
  },

  // 结束绘制
  touchEnd() {
    this.setData({
      isDrawing: false
    });
  },

  // 清空签名
  clearSignature() {
    const ctx = this.ctx;
    ctx.clearRect(0, 0, this.data.canvasWidth, this.data.canvasHeight);
    ctx.draw();
    this.setData({
      signatureData: ''
    });
  },

  // 确认签名
  confirmSignature() {
    // 将画布转为图片
    wx.canvasToTempFilePath({
      canvasId: 'signCanvas',
      success: (res) => {
        // 将图片转为base64
        wx.getFileSystemManager().readFile({
          filePath: res.tempFilePath,
          encoding: 'base64',
          success: (fileRes) => {
            const base64 = 'data:image/png;base64,' + fileRes.data;
            this.setData({
              signatureData: base64
            });
            this.submitSignature(base64);
          },
          fail: (err) => {
            console.error('读取文件失败:', err);
            util.showError('获取签名失败');
          }
        });
      },
      fail: (err) => {
        console.error('生成图片失败:', err);
        util.showError('生成签名失败');
      }
    }, this);
  },

  // 提交签名
  submitSignature(signatureBase64) {
    const contract = this.data.currentContract;
    
    util.showLoading('提交中...');
    api.signContract({
      contractId: contract.id,
      signatureBase64: signatureBase64,
      ipAddress: '',
      deviceInfo: ''
    }).then(res => {
      util.hideLoading();
      util.showSuccess('签署成功');
      
      // 关闭弹窗并刷新列表
      this.setData({
        showSignModal: false,
        currentContract: null
      });
      
      setTimeout(() => {
        this.loadContracts();
      }, 1500);
    }).catch(err => {
      util.hideLoading();
      console.error('签署失败:', err);
    });
  },

  // 关闭签名弹窗
  closeSignModal() {
    this.setData({
      showSignModal: false,
      showDetailModal: true
    });
  },

  // 阻止触摸滑动（防止签名时页面滚动）
  preventTouchMove() {
    // 空函数，阻止事件冒泡
    return false;
  },

  // 格式化日期
  formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return util.formatTime(date);
  },

  // 获取状态文本
  getStatusText(status) {
    const statusMap = {
      0: '草稿',
      1: '待签署',
      2: '已生效',
      3: '已拒绝',
      4: '已过期'
    };
    return statusMap[status] || '未知';
  },

  // 获取状态样式类
  getStatusClass(status) {
    const classMap = {
      0: 'status-draft',
      1: 'status-pending',
      2: 'status-effective',
      3: 'status-rejected',
      4: 'status-expired'
    };
    return classMap[status] || '';
  }
});
