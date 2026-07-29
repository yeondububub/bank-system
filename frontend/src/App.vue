<template>
  <div id="bank-app">
    <!-- Top Navigation Bar (로그인 시에만 내비게이션 및 프로필 바 출력) -->
    <header v-if="currentUser" class="bank-header">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <span class="logo-blue">BANK</span> SYSTEM
        </router-link>
        <div class="user-actions">
          <div class="user-chip">
            <span class="user-name">{{ currentUser.name }}</span>
            <span :class="['role-badge', currentUser.role?.toLowerCase()]">
              {{ currentUser.role }}
            </span>
          </div>
          <button @click="handleLogout" class="logout-btn" title="로그아웃">
            <LogOut :size="18" />
          </button>
        </div>
      </div>
    </header>

    <!-- Main Content Container -->
    <main :class="['container', { 'full-screen': !currentUser }]">
      <router-view />
    </main>

    <!-- Bottom Tab Navigation (로그인 시에만 하단 바 출력) -->
    <nav v-if="currentUser" class="bank-bottom-nav">
      <router-link to="/" class="nav-item" active-class="active">
        <Home :size="22" />
        <span>홈</span>
      </router-link>
      <router-link to="/payment" class="nav-item" active-class="active">
        <CreditCard :size="22" />
        <span>결제</span>
      </router-link>
      <router-link to="/history" class="nav-item" active-class="active">
        <History :size="22" />
        <span>내역</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Home, CreditCard, History, LogOut } from 'lucide-vue-next'

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

// 라우터 경로 변경 시 유저 상태 갱신
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
@import './assets/toss-design-system.css';

#bank-app {
  min-height: 100vh;
  padding-bottom: 70px;
}

.container.full-screen {
  padding: 0;
  max-width: 100%;
}

.bank-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background-color: rgba(242, 244, 246, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-color);
}

.header-inner {
  max-width: 540px;
  margin: 0 auto;
  padding: 14px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  font-size: 20px;
  font-weight: 800;
  text-decoration: none;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.logo-blue {
  color: var(--toss-blue);
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--surface-color);
  padding: 6px 12px;
  border-radius: 20px;
  box-shadow: var(--shadow-sm);
  font-size: 13px;
  font-weight: 600;
}

.user-name {
  color: var(--text-primary);
}

.role-badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 6px;
  font-weight: 700;
}

.role-badge.admin {
  background-color: #ef4444;
  color: #ffffff;
}

.role-badge.user {
  background-color: #3b82f6;
  color: #ffffff;
}

.logout-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.logout-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
  color: #ef4444;
}

.bank-bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: var(--surface-color);
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: space-around;
  padding: 10px 0 14px;
  max-width: 540px;
  margin: 0 auto;
  z-index: 100;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-decoration: none;
}

.nav-item.active {
  color: var(--toss-blue);
}
</style>
