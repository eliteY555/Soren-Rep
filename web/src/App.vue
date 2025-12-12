<template>
  <div id="app">
    <router-view v-if="!hasError"/>
    <div v-else class="error-container">
      <h2>页面加载错误</h2>
      <p>{{ errorMessage }}</p>
      <el-button type="primary" @click="reloadPage">刷新页面</el-button>
    </div>
  </div>
</template>

<script>
import { isRequestCanceled } from '@/utils/request';

export default {
  name: 'App',
  data() {
    return {
      hasError: false,
      errorMessage: '系统遇到了一个错误，请刷新页面重试'
    }
  },
  computed: {
    userInfo() {
      return this.$store.state.user.userInfo;
    }
  },
  watch: {
    // 监听用户信息变化，向其他组件广播
    userInfo: {
      handler(newVal) {
        if (newVal && newVal.userId) {
          // 通知其他组件用户信息已加载
          this.$nextTick(() => {
            this.$bus.$emit('user-info-loaded', newVal);
          });
        }
      },
      immediate: true
    }
  },
  mounted() {
    // 全局处理未捕获的Promise错误
    window.addEventListener('unhandledrejection', this.handleUnhandledRejection);
    
    // 添加全局错误处理
    window.addEventListener('error', this.handleGlobalError);
    
    // 监听路由错误
    this.$router.onError(this.handleRouterError);
    
    // 监听路由变化
    this.$router.afterEach(() => {
      this.$nextTick(() => {
        if (this.userInfo && this.userInfo.userId) {
          this.$bus.$emit('user-info-loaded', this.userInfo);
        }
      });
    });
    
    // 确保用户信息已加载
    this.ensureUserInfoLoaded();
  },
  beforeDestroy() {
    // 移除事件监听器
    window.removeEventListener('unhandledrejection', this.handleUnhandledRejection);
    window.removeEventListener('error', this.handleGlobalError);
  },
  methods: {
    handleUnhandledRejection(event) {
      // 阻止默认行为（防止控制台错误）
      event.preventDefault();
      
      // 处理取消的请求，不显示错误
      if (isRequestCanceled(event.reason) || 
          (event.reason && (
            (event.reason.message && 
              (event.reason.message.includes('cancel') || 
               event.reason.message.includes('取消'))) ||
            (event.reason.name && 
              (event.reason.name === 'CanceledError' || 
               event.reason.name === 'RequestCancelError'))
          ))) {
        // 静默处理，不输出到控制台
        return;
      }
      
      // 处理其他类型的错误
      console.error('未捕获的Promise错误:', event.reason);
      
      // 可以根据需要添加全局错误通知
      // this.$message.error('系统遇到了一个错误，请刷新页面重试');
    },
    
    handleGlobalError(event) {
      // 检查是否是请求取消相关的错误
      if (event.message && (
          event.message.includes('cancel') || 
          event.message.includes('取消') ||
          event.message.includes('RequestCancelError') ||
          event.message.includes('CanceledError')
        )) {
        // 阻止默认行为，不在控制台显示
        event.preventDefault();
        return;
      }
    },
    
    handleRouterError(error) {
      // 如果是请求取消错误，忽略
      if (isRequestCanceled(error)) {
        return;
      }
      
      console.error('路由错误:', error);
      this.errorMessage = '页面加载失败，请刷新重试';
      this.hasError = true;
    },
    
    reloadPage() {
      // 重置错误状态
      this.hasError = false;
      
      // 刷新当前页面
      window.location.reload();
    },
    
    // 确保用户信息已加载
    ensureUserInfoLoaded() {
      // 如果已经有用户信息，则广播通知
      if (this.userInfo && this.userInfo.userId) {
        console.log('用户信息已加载，通知所有组件...');
        setTimeout(() => {
          this.$bus.$emit('user-info-loaded', this.userInfo);
        }, 500);
        return;
      }
      
      // 如果没有用户信息，但有token，则尝试初始化用户信息
      const token = localStorage.getItem('token');
      if (token) {
        console.log('尝试初始化用户信息...');
        this.$store.dispatch('initUserInfo');
        
        // 延迟检查用户信息是否加载成功
        setTimeout(() => {
          if (this.userInfo && this.userInfo.userId) {
            console.log('用户信息初始化成功，通知所有组件...');
            this.$bus.$emit('user-info-loaded', this.userInfo);
          } else {
            console.warn('用户信息初始化失败');
          }
        }, 1000);
      }
    }
  }
}
</script>

<style>
@import '@/assets/css/app.scss';

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100%;
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  text-align: center;
  padding: 20px;
}
</style>
