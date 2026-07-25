<template>
  <div id="bank-app">
    <!-- Top Navigation Bar -->
    <header class="bank-header">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <span class="logo-blue">BANK</span> SYSTEM
        </router-link>
        <div class="user-profile">
          <User :size="20" />
        </div>
      </div>
    </header>

    <!-- Main Content Container -->
    <main class="container">
      <router-view />
    </main>

    <!-- Bottom Tab Navigation -->
    <nav class="bank-bottom-nav">
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
import { Home, CreditCard, History, User } from 'lucide-vue-next'
</script>

<style>
@import './assets/toss-design-system.css';

#bank-app {
  min-height: 100vh;
  padding-bottom: 70px;
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

.user-profile {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: var(--surface-color);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  box-shadow: var(--shadow-sm);
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
