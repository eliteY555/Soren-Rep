import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin, logout as apiLogout, LoginRequest, LoginResponse, AdminUserVO } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  
  // 从 localStorage 恢复用户信息
  const savedUserInfo = localStorage.getItem('userInfo')
  const userInfo = ref<AdminUserVO | null>(savedUserInfo ? JSON.parse(savedUserInfo) : null)

  // 登录
  async function login(data: LoginRequest) {
    const res: any = await apiLogin(data)
    const loginData: LoginResponse = res.data
    
    token.value = loginData.token
    localStorage.setItem('token', loginData.token)
    
    userInfo.value = {
      id: loginData.userId,
      phone: loginData.phone,
      realName: loginData.realName,
      role: loginData.role,
      status: 1
    }
    
    // 持久化用户信息
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    
    return loginData
  }

  // 退出登录
  async function logout() {
    try {
      await apiLogout()
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }

  return {
    token,
    userInfo,
    login,
    logout
  }
})

