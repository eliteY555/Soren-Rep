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
      webSocketURL: {
        hostname: 'localhost',
        pathname: '/ws',
        port: 8080,
      },
      progress: false,
    },
    proxy: {
      '/api': {
        target: 'http://localhost:9999',
        changeOrigin: true,
        ws: true,
        pathRewrite: {
          '^/api': ''
        },
        // === SSE 流式响应：禁用代理缓冲 ===
        onProxyReq: (proxyReq, req, res) => {
          if (req.url && req.url.includes('/agent/chat')) {
            proxyReq.setHeader('X-Accel-Buffering', 'no');
            proxyReq.setHeader('Cache-Control', 'no-cache');
          }
        },
        onProxyRes: (proxyRes, req, res) => {
          if (req.url && req.url.includes('/agent/chat')) {
            proxyRes.headers['cache-control'] = 'no-cache, no-transform';
            proxyRes.headers['x-accel-buffering'] = 'no';
            proxyRes.headers['connection'] = 'keep-alive';
          }
        }
      }
    },
  },
  lintOnSave: false,
}
