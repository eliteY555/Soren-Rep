<template>
  <div id="app">
    <router-view/>
  </div>
</template>

<script>
export default {
  name: 'App',
  mounted() {
    // 全局处理未捕获的Promise错误
    window.addEventListener('unhandledrejection', this.handleUnhandledRejection);
  },
  beforeDestroy() {
    // 移除事件监听器
    window.removeEventListener('unhandledrejection', this.handleUnhandledRejection);
  },
  methods: {
    handleUnhandledRejection(event) {
      // 阻止默认行为（防止控制台错误）
      event.preventDefault();
      
      // 处理取消的请求，不显示错误
      if (event.reason && event.reason.message === 'cancel' || 
          (typeof event.reason === 'string' && event.reason.includes('cancel'))) {
        console.log('请求被取消，这是正常的行为');
        return;
      }
      
      // 处理其他类型的错误
      console.error('未捕获的Promise错误:', event.reason);
      
      // 可以根据需要添加全局错误通知
      // this.$message.error('系统遇到了一个错误，请刷新页面重试');
    }
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100%;
}
</style> 