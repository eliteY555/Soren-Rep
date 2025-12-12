<template>
  <div class="main-container">
    <div class="overlay"></div>
    <div class="login-container">
      <div class="login-warp-main">
        <div class="login-warp-main-title">
          诊易通-在线问诊平台
        </div>
        <div class="login-warp-main-form">
          <login
            v-if="isLogin"
            @switchToRegister="
              isLogin = false;
              isForget = false;
            "
            @switchToForget="
              isLogin = false;
              isForget = true;
            "
          />
          <register
            v-if="!isLogin && !isForget"
            @switchToLogin="isLogin = true"
          />
          <forget
            v-if="isForget"
            @switchToLogin="
              isLogin = true;
              isForget = false;
            "
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import login from "./components/login.vue";
import register from "./components/register.vue";
import forget from "./components/forget.vue";
export default {
  components: {
    login,
    register,
    forget,
  },
  data() {
    return {
      isLogin: true,
      isForget: false,
    };
  },
  mounted() {
  },
  methods: {},
};
</script>
<style scoped lang="scss">
.main-container {
  height: 100vh;
  width: 100%;
  background: url("@/assets/images/loginBg.jpg") no-repeat;
  background-size: cover;
  background-position: center center;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(1.5px);
}

.login-container {
  width: 400px;
  min-height: 400px;
  max-height: 85vh;
  background-color: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  position: relative;
  overflow: hidden;
  animation: fadeIn 0.8s ease-in-out;
  padding: 20px 0;
  
  .login-warp-main {
    display: flex;
    flex-direction: column;
    height: 100%;
    
    .login-warp-main-title {
      height: 80px;
      line-height: 80px;
      font-size: 26px;
      font-weight: 800;
      text-align: center;
      color: var(--base-color);
      text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
      position: relative;
      
      &::after {
        content: '';
        position: absolute;
        bottom: 15px;
        left: 50%;
        transform: translateX(-50%);
        width: 100px;
        height: 3px;
        background-color: var(--base-color);
        border-radius: 3px;
      }
    }
    
    .login-warp-main-form {
      padding: 0 40px 20px;
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: center;
    }
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
