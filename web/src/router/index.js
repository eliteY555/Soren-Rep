import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '@/store'
import { Message } from "element-ui";
import { cancelAllRequests } from '@/utils/request';

Vue.use(VueRouter)

// 解决重复导航报错问题
const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => {
    // 忽略NavigationDuplicated错误
    if (err.name !== 'NavigationDuplicated') {
      return Promise.reject(err)
    }
    return Promise.resolve()
  })
}

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue')
  },
  {
    path: '/',
    redirect: '/login' // 根路径重定向到登录页
  },
  {
    path: '/home',
    name: 'home',
    redirect:"/home/addRecord",
    component: () => import('@/views/home/index.vue'),
    children: [
      {
        path:"/home/addRecord",
        name:"病症诊断",
        icon: "el-icon-service",
        component: () => import('@/views/medicalRecord/addRecord.vue'),
        meta: { roles: [0] } // 只有患者可以访问
      },
      {
        path:"/home/recordDetail",
        name:"recordDetail",
        component: () => import('@/views/medicalRecord/recordDetail.vue'),
      },
      {
        path:"/home/allRecords",
        name:"咨询列表",
        icon: "el-icon-document",
        component: () => import('@/views/medicalRecord/allRecords.vue'),
        meta: { roles: [0] }
      },
      {
        path:"/home/docList",
        name:"医师列表",
        icon: "el-icon-user",
        component: () => import('@/views/medicalRecord/docList.vue'),
        meta: { roles: [0] }
      },
      {
        path:"/home/aiDiagnosis",
        name:"智能问诊",
        icon: "el-icon-chat-dot-round",
        component: () => import('@/views/diagnosis/aiDiagnosis.vue'),
        meta: { roles: [0] } // 只有患者可以访问
      },
      // 医生
      {
        path:"/home/diagnosisList",
        name:"诊断列表",
        icon: "el-icon-user",
        component: () => import('@/views/diagnosis/diagnosisList.vue'),
        meta: { roles: [1] }
      },
      {
        path:"/diagnosis/detail",
        name:"diagnosisDetail",
        component: () => import('@/views/diagnosis/detail.vue'),
      },
      {
        path:"/home/communicate",
        name:"医患交流",
        icon: "el-icon-chat-line-round",
        component: () => import('@/views/communicate/index.vue'),
        meta: { roles: [1] }
      },
      {
        path:"/home/statistics",
        name:"统计分析",
        icon: "el-icon-c-scale-to-original",
        component: () => import('@/views/statistics/index.vue'),
        meta: { roles: [1] }
      },
    ]
  },
]

const router = new VueRouter({
  routes
})

// 全局路由钩子，用于取消请求
router.beforeEach((to, from, next) => {
  // 取消所有进行中的请求，避免路由切换导致的请求错误
  cancelAllRequests();
    next();
})

// 不需要在这里重复添加路由守卫，已在main.js中实现

export default router
