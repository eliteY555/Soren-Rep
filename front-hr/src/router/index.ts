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
    redirect: '/managers',
    children: [
      {
        path: 'managers',
        name: 'Managers',
        component: () => import('@/views/manager/ManagerList.vue'),
        meta: { title: '经理管理', icon: 'User' }
      },
      {
        path: 'templates',
        name: 'Templates',
        component: () => import('@/views/template/TemplateList.vue'),
        meta: { title: '模板管理', icon: 'Document' }
      },
      {
        path: 'contracts',
        name: 'Contracts',
        component: () => import('@/views/contract/ContractList.vue'),
        meta: { title: '合同管理', icon: 'Postcard' }
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

