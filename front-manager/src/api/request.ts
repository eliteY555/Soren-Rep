import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://8.137.153.9:8082',
  timeout: 30000
})

// 标记是否正在处理token失效
let isRefreshing = false

// 处理token失效
const handleTokenExpired = () => {
  if (isRefreshing) return
  
  isRefreshing = true
  ElMessage.warning('登录已过期，请重新登录')
  
  const userStore = useUserStore()
  userStore.logout().finally(() => {
    isRefreshing = false
    // 跳转到登录页
    router.push('/login')
  })
}

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    
    // 如果返回的状态码不是200，则报错
    if (res.code !== 200) {
      // 401: Token过期或无效
      if (res.code === 401) {
        handleTokenExpired()
        return Promise.reject(new Error('Token已失效'))
      }
      
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    
    return res
  },
  (error) => {
    console.error('响应错误:', error)
    
    // 处理HTTP状态码401（未授权）
    if (error.response && error.response.status === 401) {
      handleTokenExpired()
    } else if (error.response && error.response.status === 403) {
      // 403: 权限不足
      ElMessage.error('权限不足，无法访问该资源')
    } else if (error.response && error.response.status === 404) {
      // 404: 资源不存在
      ElMessage.error('请求的资源不存在')
    } else if (error.response && error.response.status >= 500) {
      // 5xx: 服务器错误
      ElMessage.error('服务器错误，请稍后重试')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    
    return Promise.reject(error)
  }
)

export default service

