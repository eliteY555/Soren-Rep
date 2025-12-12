<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration decoration-1"></div>
    <div class="bg-decoration decoration-2"></div>
    
    <!-- 登录卡片 -->
    <div class="login-card">
      <!-- 左侧：品牌展示区 -->
      <div class="brand-area">
        <div class="decoration decoration-3"></div>
        <div class="decoration decoration-4"></div>
        
        <div class="brand-content">
          <!-- Logo -->
          <img class="logo" src="/images/logo.jpg" alt="泰来云Logo">
          
          <h1 class="title">泰来云</h1>
          <h2 class="subtitle">企业数字化管理平台</h2>
        </div>
      </div>
      
      <!-- 右侧：登录表单区 -->
      <div class="form-area">
        <div class="form-header">
          <h3>欢迎登录</h3>
          <p>请使用管理员账号登录系统</p>
        </div>
        
        <el-form ref="loginFormRef" :model="loginForm" :rules="rules" @keyup.enter="handleLogin">
          <el-form-item prop="phone">
            <el-input
              v-model="loginForm.phone"
              placeholder="请输入手机号"
              prefix-icon="User"
              size="large"
              autocomplete="off"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              autocomplete="new-password"
            />
          </el-form-item>
          
          <div class="form-options">
            <el-checkbox v-model="rememberMe">记住密码</el-checkbox>
            <a href="#" class="forgot-link">忘记密码？</a>
          </div>
          
          <el-button
            :loading="loading"
            type="primary"
            size="large"
            class="login-button"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form>
        
        
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(true)

const loginForm = reactive({
  phone: '',
  password: ''
})

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login(loginForm)
        ElMessage.success('登录成功')
        router.push('/')
      } catch (error) {
        console.error('登录失败:', error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px;
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  
  &.decoration-1 {
    top: -100px;
    right: -100px;
    width: 400px;
    height: 400px;
  }
  
  &.decoration-2 {
    bottom: -150px;
    left: -150px;
    width: 500px;
    height: 500px;
    background: rgba(255, 255, 255, 0.08);
  }
}

.login-card {
  width: 900px;
  height: 500px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 2;
  display: flex;
  overflow: hidden;
}

.brand-area {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 50px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
  
  .decoration {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);
    
    &.decoration-3 {
      top: -50px;
      right: -50px;
      width: 200px;
      height: 200px;
    }
    
    &.decoration-4 {
      bottom: -30px;
      left: -30px;
      width: 150px;
      height: 150px;
      background: rgba(255, 255, 255, 0.08);
    }
  }
}

.brand-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
}

.logo {
  width: 120px;
  height: 120px;
  margin: 0 auto 24px;
  display: block;
  border-radius: 20px;
  background: #fff;
  padding: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  border: 3px solid rgba(255, 255, 255, 0.3);
}

.title {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 16px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.subtitle {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 12px;
  opacity: 0.95;
}

.description {
  font-size: 15px;
  opacity: 0.85;
  line-height: 1.8;
  margin-top: 20px;
}

.features {
  margin-top: 40px;
  text-align: left;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  color: rgba(255, 255, 255, 0.9);
  
  .feature-icon {
    width: 32px;
    height: 32px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
  }
  
  div:last-child {
    font-size: 14px;
  }
}

.form-area {
  flex: 1;
  padding: 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: #fff;
}

.form-header {
  margin-bottom: 36px;
  
  h3 {
    font-size: 26px;
    color: #333;
    font-weight: 700;
    margin-bottom: 8px;
  }
  
  p {
    font-size: 14px;
    color: #999;
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  .forgot-link {
    font-size: 13px;
    color: #667eea;
    text-decoration: none;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

.login-button {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
  }
}

.test-info {
  margin-top: 20px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  text-align: center;
  
  p {
    font-size: 12px;
    color: #999;
    
    strong {
      color: #667eea;
    }
  }
}
</style>

