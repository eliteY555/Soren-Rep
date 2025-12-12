// app.js
App({
  onLaunch() {
    // 小程序启动
    console.log('========== 小程序启动 ==========');
    console.log('API地址:', this.globalData.apiBase);
  },

  globalData: {
    userInfo: null,
    apiBase: 'http://192.168.40.102:8081' // 后端API地址（使用真实WiFi局域网IP）
  }
},
);

