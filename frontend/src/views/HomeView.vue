<template>
  <div class="home-view">
    <!-- Greeting Header -->
    <div class="header-section">
      <span class="user-tag">메인 계좌</span>
      <h1 class="toss-title">사용자님의 입출금 통장</h1>
    </div>

    <!-- Main Account Card -->
    <div class="toss-card account-card">
      <div class="account-header">
        <span class="account-number">뱅크 1000-1234-5678</span>
        <button class="icon-btn" title="설정" @click="fetchAccountInfo">
          <Settings :size="20" />
        </button>
      </div>
      <div class="amount-wrapper">
        <div v-if="loading" class="toss-amount loading-text">조회 중...</div>
        <div v-else-if="errorMessage" class="toss-amount error-text">{{ errorMessage }}</div>
        <div v-else class="toss-amount">{{ formattedBalance }} 원</div>
      </div>
      <div class="card-actions">
        <button class="toss-btn toss-btn-subtle" @click="handleDeposit" :disabled="loading">
          <ArrowDownLeft :size="18" /> 입금
        </button>
        <button class="toss-btn toss-btn-primary" @click="$router.push('/payment')">
          <Send :size="18" /> 결제하기
        </button>
      </div>
    </div>

    <!-- Quick Features Section -->
    <div class="toss-card feature-card">
      <h3 class="card-title">빠른 메뉴</h3>
      <div class="menu-grid">
        <router-link to="/payment" class="menu-item">
          <div class="menu-icon bg-blue">
            <CreditCard :size="22" />
          </div>
          <span>결제 승인</span>
        </router-link>

        <router-link to="/history" class="menu-item">
          <div class="menu-icon bg-green">
            <Receipt :size="22" />
          </div>
          <span>이력 조회</span>
        </router-link>

        <div class="menu-item" @click="fetchAccountInfo">
          <div class="menu-icon bg-purple">
            <RefreshCw :size="22" :class="{ 'spin-icon': loading }" />
          </div>
          <span>잔액 갱신</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Settings, ArrowDownLeft, Send, CreditCard, Receipt, RefreshCw } from 'lucide-vue-next'
import axios from 'axios'

const ownerId = ref(1)
const balance = ref(0)
const loading = ref(false)
const errorMessage = ref('')

const formattedBalance = computed(() => {
  return balance.value.toLocaleString('ko-KR')
})

const fetchAccountInfo = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await axios.get(`/api/v1/accounts/${ownerId.value}`)
    balance.value = response.data.balance
  } catch (error: any) {
    if (error.response && error.response.status === 404) {
      errorMessage.value = '등록된 계좌가 없습니다.'
      balance.value = 0
    } else {
      errorMessage.value = '계좌 조회 실패'
      console.error('계좌 조회 중 오류 발생:', error)
    }
  } finally {
    loading.value = false
  }
}

const handleDeposit = async () => {
  balance.value += 100000
}

onMounted(() => {
  fetchAccountInfo()
})
</script>

<style scoped>
.header-section {
  margin-bottom: 20px;
}

.user-tag {
  font-size: 13px;
  font-weight: 600;
  color: var(--toss-blue-text);
  background-color: var(--toss-blue-light);
  padding: 4px 10px;
  border-radius: 12px;
}

.account-card {
  background: linear-gradient(145deg, var(--surface-color), var(--surface-subtle));
}

.account-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.account-number {
  font-size: 13px;
  color: var(--text-tertiary);
  font-weight: 500;
}

.icon-btn {
  background: none;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
}

.amount-wrapper {
  margin: 12px 0 24px;
}

.loading-text {
  font-size: 24px;
  color: var(--text-tertiary);
}

.error-text {
  font-size: 20px;
  color: var(--toss-red);
}

.card-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.card-title {
  font-size: 17px;
  font-weight: 700;
  margin-bottom: 16px;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  text-align: center;
}

.menu-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: var(--text-primary);
  cursor: pointer;
}

.menu-icon {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bg-blue { background-color: var(--toss-blue-light); color: var(--toss-blue); }
.bg-green { background-color: var(--toss-green-light); color: var(--toss-green); }
.bg-purple { background-color: #f3f0ff; color: #7950f2; }

.spin-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  100% { transform: rotate(360deg); }
}
</style>
