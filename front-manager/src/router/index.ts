import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/employees',
    children: [
      {
        path: 'employees',
        name: 'Employees',
        component: () => import('@/views/employee/EmployeeList.vue'),
        meta: { title: '员工管理', icon: 'User' }
      },
      {
        path: 'contracts',
        name: 'Contracts',
        component: () => import('@/views/contract/ContractList.vue'),
        meta: { title: '合同管理', icon: 'Document' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.path !== '/login' && !userStore.token) {
    next('/login')
  } else if (to.path === '/login' && userStore.token) {
    next('/')
  } else {
    next()
  }
})

export default router

