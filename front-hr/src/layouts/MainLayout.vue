<template>
  <el-container class="main-layout">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="sidebar">
      <div class="logo-area">
        <img class="logo-icon" src="/images/logo.jpg" alt="Logo">
        <div class="logo-title">人事管理系统</div>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        background-color="transparent"
        text-color="rgba(255,255,255,0.7)"
        active-text-color="#fff"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/managers">
          <el-icon><User /></el-icon>
          <span>经理管理</span>
        </el-menu-item>
        
        <el-menu-item index="/templates">
          <el-icon><Document /></el-icon>
          <span>模板管理</span>
        </el-menu-item>
        
        <el-menu-item index="/contracts">
          <el-icon><Postcard /></el-icon>
          <span>合同管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <!-- 主内容区 -->
    <el-container class="main-container">
      <!-- 顶部栏 -->
      <el-header class="topbar">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>{{ breadcrumbTitle }}</el-breadcrumb-item>
        </el-breadcrumb>
        
        <div class="user-info-bar">
          <el-dropdown @command="handleCommand">
            <div class="user-dropdown">
              <el-avatar class="user-avatar">{{ userStore.userInfo?.realName?.charAt(0) || '管' }}</el-avatar>
              <span>{{ userStore.userInfo?.realName || '管理员' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <!-- 内容区域 -->
      <el-main class="content-area">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const breadcrumbTitle = computed(() => {
  return route.meta.title as string || '首页'
})

const handleMenuSelect = (index: string) => {
  router.push(index)
}

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      await userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    }).catch(() => {})
  } else if (command === 'profile') {
    ElMessage.info('个人信息功能开发中...')
  }
}
</script>

<style scoped lang="scss">
.main-layout {
  height: 100vh;
}

.sidebar {
  background: linear-gradient(180deg, #2d3748 0%, #1a202c 100%);
  color: #fff;
  overflow-y: auto;
}

.logo-area {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  text-align: center;
}

.logo-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  margin: 0 auto 12px;
  display: block;
  background: #fff;
  padding: 5px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.logo-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.sidebar-menu {
  border: none;
  padding: 20px 0;
}

.menu-group-title {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  padding: 8px 20px;
  text-transform: uppercase;
  margin-top: 16px;
}

:deep(.el-menu-item) {
  &:hover {
    background: rgba(255, 255, 255, 0.1) !important;
  }
  
  &.is-active {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
    border-right: 3px solid #fff;
  }
}

.main-container {
  background: #f8f9fa;
}

.topbar {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
}

.user-info-bar {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  
  span {
    font-size: 14px;
    color: #333;
  }
}

.user-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.content-area {
  padding: 24px 30px;
  overflow-y: auto;
}
</style>

