<template>
  <div id="bank-app">
    <!-- Top Global Dark Navigation Bar -->
    <header v-if="currentUser" class="bank-header">
      <div class="header-inner">
        <div class="header-left">
          <router-link to="/" class="logo">
            <Landmark :size="22" class="logo-icon-svg" />
            <span class="logo-bold">BANK</span> SYSTEM
          </router-link>
          
          <nav class="top-nav">
            <router-link v-if="currentUser?.role !== 'ADMIN'" to="/" class="nav-link" active-class="active">홈</router-link>
            <router-link v-if="currentUser?.role === 'ADMIN'" to="/admin" class="nav-link" active-class="active">
              <ShieldCheck :size="16" />
              <span>관리자 센터</span>
            </router-link>
            <router-link to="/payment" class="nav-link" active-class="active">결제 서비스</router-link>
            <router-link to="/history" class="nav-link" active-class="active">거래 내역</router-link>
          </nav>
        </div>

        <div class="header-right">
          <div class="user-chip">
            <User :size="14" class="user-avatar-svg" />
            <span class="user-name">{{ currentUser.name }}</span>
            <span :class="['role-badge', currentUser.role?.toLowerCase()]">
              {{ currentUser.role }}
            </span>
          </div>
          <button @click="handleLogout" class="logout-btn" title="로그아웃">
            <LogOut :size="16" />
            <span>로그아웃</span>
          </button>
        </div>
      </div>
    </header>

    <!-- Main Content Container -->
    <main :class="['container', { 'full-screen': !currentUser }]">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Landmark, User, ShieldCheck, LogOut } from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()

const currentUser = ref<any>(null)

const checkAuth = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      currentUser.value = JSON.parse(userStr)
    } catch (e) {
      currentUser.value = null
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

#bank-app {
  min-height: 100vh;
  background-color: #0b0e14;
}

.container.full-screen {
  padding: 0;
  max-width: 100%;
}

.bank-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background-color: rgba(11, 14, 20, 0.92);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.header-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 40px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 800;
  text-decoration: none;
  color: #ffffff;
  letter-spacing: -0.5px;
}

.logo-icon-svg {
  color: #3182f6;
}

.logo-bold {
  color: #3182f6;
}

.top-nav {
  display: flex;
  gap: 24px;
}

.nav-link {
  color: #94a3b8;
  text-decoration: none;
  font-size: 15px;
  font-weight: 600;
  transition: color 0.2s;
  padding: 6px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-link:hover {
  color: #ffffff;
}

.nav-link.active {
  color: #3182f6;
  border-bottom: 2px solid #3182f6;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #141b26;
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 14px;
  font-weight: 600;
}

.user-avatar-svg {
  color: #94a3b8;
}

.user-name {
  color: #f8fafc;
}

.role-badge {
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 6px;
  font-weight: 700;
}

.role-badge.admin {
  background-color: #ef4444;
  color: #ffffff;
}

.role-badge.user {
  background-color: #3182f6;
  color: #ffffff;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #141b26;
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #94a3b8;
  padding: 8px 14px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background-color: rgba(239, 68, 68, 0.15);
  color: #f87171;
  border-color: rgba(239, 68, 68, 0.3);
}
</style>
