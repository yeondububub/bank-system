<template>
  <div id="bank-app">
    <!-- Logged in state: Clean White Top Navigation Bar -->
    <div v-if="currentUser" class="app-layout">
      <header class="top-nav">
        <div class="top-nav-inner">
          <div class="nav-left">
            <router-link :to="currentUser?.role === 'ADMIN' ? '/admin' : '/'" class="nav-brand">
              BANK SYSTEM
            </router-link>

            <nav class="nav-links" aria-label="주요 메뉴">
              <template v-if="currentUser?.role !== 'ADMIN'">
                <router-link to="/" class="nav-link" exact-active-class="active">
                  대시보드
                </router-link>

                <router-link to="/history" class="nav-link" active-class="active">
                  거래 내역
                </router-link>
              </template>

              <template v-else>
                <router-link to="/admin" class="nav-link" exact-active-class="active">
                  계좌 승인
                </router-link>

                <router-link to="/admin/transactions" class="nav-link" active-class="active">
                  전체 거래 내역
                </router-link>
              </template>
            </nav>
          </div>

          <div class="nav-right">
            <span class="nav-user-name">{{ currentUser.name }}님</span>

            <button type="button" @click="handleLogout" class="nav-logout-btn">
              로그아웃
            </button>
          </div>
        </div>
      </header>

      <main class="content-body">
        <router-view />
      </main>
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
</style>
