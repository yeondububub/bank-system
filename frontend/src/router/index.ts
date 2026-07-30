import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/admin',
      name: 'admin',
      component: AdminDashboardView
    },
    {
      path: '/payment',
      name: 'payment',
      component: () => import('../views/PaymentView.vue')
    },
    {
      path: '/history',
      name: 'history',
      component: () => import('../views/HistoryView.vue')
    }
  ]
})

// Role-Based Navigation Guard
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('accessToken')
  const userStr = localStorage.getItem('user')
  let user: any = null

  if (userStr) {
    try {
      user = JSON.parse(userStr)
    } catch (e) {
      user = null
    }
  }

  // 1. 미인증 유저 처리
  if (to.name !== 'login' && !token) {
    next({ name: 'login' })
    return
  }

  // 2. 이미 로그인 상태에서 /login 진입 시 역할별 안내
  if (to.name === 'login' && token) {
    if (user?.role === 'ADMIN') {
      next({ name: 'admin' })
    } else {
      next({ name: 'home' })
    }
    return
  }

  // 3. 관리자 전용 /admin 경로 보호 (ADMIN만 가능)
  if (to.name === 'admin' && user?.role !== 'ADMIN') {
    alert('⚠️ 접근 권한이 없습니다. 관리자(ADMIN) 계정만 접근할 수 있습니다.')
    next({ name: 'home' })
    return
  }

  // 4. ADMIN 계정이 루트 / 진입 시 관리자 대시보드 /admin 으로 자동 이동
  if (to.name === 'home' && user?.role === 'ADMIN') {
    next({ name: 'admin' })
    return
  }

  next()
})

export default router
