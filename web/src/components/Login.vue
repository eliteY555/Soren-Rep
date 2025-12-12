<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <div class="logo-container">
          <i class="el-icon-s-help"></i>
        </div>
        <h2 class="system-title">诊易通-在线问诊平台</h2>
        <p class="system-subtitle">智慧医疗，便捷诊疗</p>
      </div>
      
      <el-form ref="loginForm" :model="loginForm" :rules="loginRules">
        <el-tabs v-model="activeTab" class="login-tabs">
          <el-tab-pane label="账号登录" name="account">
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                prefix-icon="el-icon-user"
                placeholder="请输入用户名"
                clearable
              ></el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                prefix-icon="el-icon-lock"
                placeholder="请输入密码"
                show-password
                @keyup.enter.native="handleLogin"
              ></el-input>
            </el-form-item>
          </el-tab-pane>
          <el-tab-pane label="手机登录" name="phone">
            <el-form-item prop="phone">
              <el-input
                v-model="loginForm.phone"
                prefix-icon="el-icon-mobile-phone"
                placeholder="请输入手机号"
                clearable
              ></el-input>
            </el-form-item>
            <el-form-item prop="code" class="verification-code-item">
              <el-input
                v-model="loginForm.code"
                prefix-icon="el-icon-key"
                placeholder="请输入验证码"
                clearable
                @keyup.enter.native="handleLogin"
              ></el-input>
              <el-button
                type="primary"
                :disabled="isGetting"
                @click="getCode"
                class="verification-code-btn"
              >{{ buttonText }}</el-button>
            </el-form-item>
          </el-tab-pane>
        </el-tabs>
        
        <el-form-item prop="role">
          <el-radio-group v-model="loginForm.role" class="role-select">
            <el-radio :label="0">患者</el-radio>
            <el-radio :label="1">医师</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >登 录</el-button>
        </el-form-item>
        
        <div class="login-options">
          <el-link type="primary" @click="goRegister">注册账号</el-link>
          <el-link type="primary">忘记密码</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      activeTab: 'account',
      loginForm: {
        username: '',
        password: '',
        phone: '',
        code: '',
        role: 0
      },
      loginRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' }
        ],
        code: [
          { required: true, message: '请输入验证码', trigger: 'blur' }
        ],
        role: [
          { required: true, message: '请选择角色', trigger: 'change' }
        ]
      },
      loading: false,
      isGetting: false,
      buttonText: '获取验证码'
    };
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate((valid) => {
        if (valid) {
          this.loading = true;
          // 这里应该调用后端接口进行登录验证
          setTimeout(() => {
            this.loading = false;
            this.$message.success('登录成功');
          }, 1000);
        } else {
          this.$message.error('请填写完整信息');
        }
      });
    },
    getCode() {
      this.isGetting = true;
      // 这里应该调用后端接口获取验证码
      setTimeout(() => {
        this.isGetting = false;
        this.buttonText = '已发送';
      }, 1000);
    },
    goRegister() {
      // 这里应该跳转到注册页面
      console.log('跳转到注册页面');
    }
  }
};
</script>

<style lang="scss" scoped>
.login-container {
  height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff9f4 0%, #f3ece2 100%);
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: -10%;
    right: -10%;
    width: 60%;
    height: 70%;
    background: rgba(139, 111, 78, 0.05);
    border-radius: 50%;
    transform: rotate(-20deg);
  }
  
  &::after {
    content: '';
    position: absolute;
    bottom: -10%;
    left: -10%;
    width: 70%;
    height: 60%;
    background: rgba(139, 111, 78, 0.05);
    border-radius: 50%;
    transform: rotate(20deg);
  }
}

.login-box {
  width: 400px;
  padding: 40px;
  background-color: #ffffff;
  box-shadow: var(--shadow-lg);
  border-radius: var(--border-radius-lg);
  z-index: 10;
  
  .login-header {
    text-align: center;
    margin-bottom: 30px;
    
    .logo-container {
      margin-bottom: 15px;
      
      i {
        font-size: 60px;
        color: var(--primary-color);
      }
    }
    
    .system-title {
      font-size: 24px;
      font-weight: 600;
      color: var(--primary-color-dark);
      margin: 0;
      margin-bottom: 10px;
    }
    
    .system-subtitle {
      font-size: 14px;
      color: var(--text-muted);
      margin: 0;
    }
  }
  
  .login-tabs {
    margin-bottom: 20px;
    
    :deep(.el-tabs__item) {
      font-size: 16px;
      
      &.is-active {
        color: var(--primary-color);
      }
      
      &:hover {
        color: var(--primary-color-light);
      }
    }
    
    :deep(.el-tabs__active-bar) {
      background-color: var(--primary-color);
    }
  }
  
  :deep(.el-input) {
    .el-input__inner {
      height: 46px;
      border-radius: var(--border-radius);
      
      &:focus {
        border-color: var(--primary-color);
      }
    }
    
    .el-input__prefix {
      left: 10px;
      
      i {
        font-size: 18px;
        color: var(--gray-500);
      }
    }
  }
  
  .verification-code-item {
    display: flex;
    
    :deep(.el-form-item__content) {
      display: flex;
    }
    
    .el-input {
      flex: 1;
      margin-right: 10px;
    }
    
    .verification-code-btn {
      width: 120px;
    }
  }
  
  .role-select {
    display: flex;
    justify-content: center;
    margin-bottom: 20px;
    
    :deep(.el-radio__input.is-checked) {
      .el-radio__inner {
        background-color: var(--primary-color);
        border-color: var(--primary-color);
      }
    }
    
    :deep(.el-radio__input.is-checked+.el-radio__label) {
      color: var(--primary-color);
    }
    
    :deep(.el-radio) {
      margin-right: 30px;
      
      &:last-child {
        margin-right: 0;
      }
    }
  }
  
  .login-button {
    width: 100%;
    height: 46px;
    font-size: 16px;
    border-radius: var(--border-radius);
  }
  
  .login-options {
    display: flex;
    justify-content: space-between;
    margin-top: 15px;
    
    :deep(.el-link) {
      font-size: 14px;
      
      &:hover {
        color: var(--primary-color-light);
      }
    }
  }
}

/* Responsive adjustments */
@media (max-width: 576px) {
  .login-box {
    width: 90%;
    padding: 30px 20px;
  }
}
</style> 