import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
// 引入element-ui
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import './assets/css/element-ui-reset.scss'
import { cancelAllRequests, debounce, isRequestCanceled } from './utils/request'
import { Message } from 'element-ui'

// 覆盖Element UI的默认配置
Vue.prototype.$message = {
  success(message, options = {}) {
    return Message.success({
      message,
      duration: 5000,
      showClose: true,
      customClass: 'custom-message-box',
      ...options
    });
  },
  warning(message, options = {}) {
    return Message.warning({
      message,
      duration: 5000,
      showClose: true,
      customClass: 'custom-message-box',
      ...options
    });
  },
  error(message, options = {}) {
    return Message.error({
      message,
      duration: 5000,
      showClose: true,
      customClass: 'custom-message-box',
      ...options
    });
  },
  info(message, options = {}) {
    return Message.info({
      message,
      duration: 5000,
      showClose: true,
      customClass: 'custom-message-box',
      ...options
    });
  },
};

Vue.use(ElementUI, {
  size: 'medium' // 设置组件默认尺寸
});

Vue.config.productionTip = false

// 初始化事件总线
const EventBus = new Vue();
Vue.prototype.$bus = EventBus;

// 路由变化计数器
let navigationCounter = 0;

// 添加全局路由守卫，处理请求取消
router.beforeEach((to, from, next) => {
  // 立即取消所有未完成的请求
  cancelAllRequests();
  
  // 增加路由变化计数
  navigationCounter++;
  const currentNavigation = navigationCounter;
  
  // 记录路由变化
  console.log(`路由从 ${from.path} 变化到 ${to.path}`);
  
  // 通知组件路由变化
  Vue.nextTick(() => {
    // 确保是最新的导航
    if (currentNavigation === navigationCounter) {
      EventBus.$emit('route-changed', { to, from });
    }
  });
  
  const userInfo = store.state.user.userInfo; // 从 Vuex 中获取用户角色
  const requiredRoles = to.meta.roles; // 获取路由所需的角色
  if (to.path === '/login') {
    next();
  } else {
    if (!userInfo) {
      Message.error('未登录');
      next('/login');
      return;
    }

    if (requiredRoles) {
      const userRole = userInfo.role;
      // 检查用户是否有权限访问该路由
      if (requiredRoles.includes(userRole)) {
        next(); // 允许访问
      } else {
        Message.error('无权限访问');
        next(false); 
      }
    } else {
      next(); // 无需权限，允许访问
    }
  }
});

// 路由后置钩子，用于通知组件重新加载数据
router.afterEach((to, from) => {
  Vue.nextTick(() => {
    // 通知组件路由已完成变化
    EventBus.$emit('route-complete', { to, from });
  });
});

// 全局路由错误处理
router.onError((error) => {
  // 使用isRequestCanceled函数判断是否为请求取消错误
  if (isRequestCanceled(error)) {
    console.log('路由切换时取消请求，这是正常行为');
    return;
  }
  
  // 其他路由错误
  console.error('路由错误:', error);
  Message.error('页面加载失败，请刷新重试');
});

// 添加全局未捕获Promise错误处理
window.addEventListener('unhandledrejection', event => {
  // 如果不是请求取消错误，则显示错误消息
  if (!isRequestCanceled(event.reason)) {
    console.error('未处理的Promise错误:', event.reason);
    Message.error('操作失败，请稍后再试');
  } else {
    // 取消的请求，静默处理
    console.log('请求已取消，这是正常行为');
  }
  
  // 阻止默认处理
  event.preventDefault();
});

// 检查用户信息并发布事件
const userInfo = store.state.user.userInfo;
if (userInfo) {
  // 延迟发布事件，确保组件已经挂载
  setTimeout(() => {
    EventBus.$emit('user-info-loaded', userInfo);
  }, 0);
}

// 初始化用户信息
store.dispatch('initUserInfo');

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
