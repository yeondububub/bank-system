<template>
  <div id="bank-app">
    <!-- Logged in state: Left Sidebar + Top Header Layout -->
    <div v-if="currentUser" class="app-layout">
      <!-- 1. Left Sidebar Navigation (#202632 Toss Gray) -->
      <aside class="sidebar">
        <div class="sidebar-top">
          <router-link to="/" class="sidebar-brand">
            <div class="sidebar-brand-icon">
              <Landmark :size="20" />
            </div>
            <span class="sidebar-brand-title"><span>BANK</span> SYSTEM</span>
          </router-link>

          <nav class="sidebar-nav">
            <template v-if="currentUser?.role !== 'ADMIN'">
              <router-link to="/" class="nav-item" active-class="active" exact>
                <LayoutDashboard :size="18" class="nav-icon" />
                <span>대시보드</span>
              </router-link>

              <router-link to="/history" class="nav-item" active-class="active">
                <ArrowLeftRight :size="18" class="nav-icon" />
                <span>거래 내역</span>
              </router-link>
            </template>

            <template v-else>
              <router-link to="/admin" class="nav-item" active-class="active">
                <ShieldCheck :size="18" class="nav-icon" />
                <span>관리자 센터</span>
              </router-link>
            </template>
          </nav>
        </div>

        <!-- Sidebar Bottom User Profile -->
        <div class="sidebar-footer">
          <div class="sidebar-user-card">
            <div class="user-info-left">
              <div class="user-avatar-circle">
                <User :size="18" />
              </div>
              <div class="user-text-details">
                <div class="user-name-text">{{ currentUser.name }}님</div>
                <div class="user-sub-text">{{ currentUser.email || '인증회원' }}</div>
              </div>
            </div>
          </div>

          <button @click="handleLogout" class="sidebar-logout-btn" title="로그아웃">
            <LogOut :size="15" />
            <span>로그아웃</span>
          </button>
        </div>
      </aside>

      <!-- 2. Main Content Wrapper -->
      <div class="main-wrapper">
        <!-- Top Header Bar -->
        <header class="top-header-bar">
          <div class="header-search-box">
            <Search :size="18" class="search-icon" />
            <input type="text" placeholder="계좌번호, 이체 내역 검색..." />
          </div>

          <div class="header-actions-right">
            <button class="header-icon-btn" title="알림">
              <Bell :size="18" />
            </button>
          </div>
        </header>

        <!-- Main View Routing Container -->
        <main class="content-body">
          <router-view />
        </main>
      </div>
    </div>

    <!-- Logged out state: Full Screen (e.g. Login / Register) -->
    <div v-else class="full-screen-container">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  Landmark, 
  LayoutDashboard, 
  ArrowLeftRight, 
  ShieldCheck, 
  User, 
  LogOut, 
  Search, 
  Bell 
} from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()

const currentUser = ref<any>(null)

const checkAuth = async () => {
  const token = localStorage.getItem('accessToken')
  const userStr = localStorage.getItem('user')

  if (userStr) {
    try {
      const parsed = JSON.parse(userStr)
      if (parsed && parsed.id) {
        currentUser.value = parsed
      }
    } catch (e) {}
  }

  if (token) {
    try {
      const res = await fetch('/api/v1/auth/me', {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      if (res.ok) {
        const userResponse = await res.json()
        currentUser.value = userResponse
        localStorage.setItem('user', JSON.stringify(userResponse))

        const roleUpper = String(userResponse.role || '').toUpperCase()
        if (roleUpper === 'ADMIN' && (route.path === '/' || route.path === '/login')) {
          router.push('/admin')
        }
      } else if (res.status === 401) {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('user')
        currentUser.value = null
        if (route.path !== '/login') {
          router.push('/login')
        }
      }
    } catch (e) {
      console.error('인증 상태 동기화 중 오류:', e)
    }
  } else {
    currentUser.value = null
  }
}

onMounted(() => {
  checkAuth()
})

watch(() => route.path, () => {
  checkAuth()
})

const handleLogout = () => {
  if (confirm('로그아웃 하시겠습니까?')) {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('user')
    currentUser.value = null
    router.push('/login')
  }
}
</script>

<style>
@import './assets/bank-design-system.css';

.full-screen-container {
  width: 100vw;
  min-height: 100vh;
  background-color: var(--bg-color);
}

.user-sub-text {
  font-size: 11px;
  color: var(--text-muted-dark);
  margin-top: 1px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 130px;
}
</style>
