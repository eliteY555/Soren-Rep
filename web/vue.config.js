module.exports = {
  publicPath: '/',
  outputDir: 'dist',
  assetsDir: 'static',
  productionSourceMap: false,
  devServer: { 
    port: 8080,
    hot: true,
    client: {
      overlay: {
        errors: true,
        warnings: false,
      },
      // 解决 WebSocket 连接问题
      webSocketURL: {
        hostname: 'localhost',
        pathname: '/ws',
        port: 8080,
      },
      progress: false,
    },
    proxy: {
      '/api': {
        target: 'http://localhost:9999', // 配置好的后端接口地址
        // 允许跨域
        changeOrigin: true,
        ws: true,
        pathRewrite: {
          '^/api': '' // 以'/api'开头的url会进行接口转发
        }
      }
    },
  },
  // 禁用 eslint 检查
  lintOnSave: false,
}
