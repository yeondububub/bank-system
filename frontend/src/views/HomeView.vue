<template>
  <div class="home-view">
    <!-- Greeting Header -->
    <div class="header-section">
      <span class="user-tag">메인 계좌</span>
      <h1 class="toss-title">{{ userName }}님의 입출금 통장</h1>
    </div>

    <!-- Main Account Card -->
    <div class="toss-card account-card">
      <div v-if="account" class="account-header">
        <span class="account-number">352-계좌 ({{ account.accountNumber }})</span>
        <span :class="['status-badge', account.status?.toLowerCase()]">
          {{ account.status === 'PENDING' ? '⏳ 승인 대기' : '✅ 활성화 (ACTIVE)' }}
        </span>
      </div>

      <div class="amount-wrapper">
        <div v-if="loading" class="toss-amount loading-text">조회 중...</div>
        
        <!-- 계좌 미존재 시 개설 신청 카드 -->
        <div v-else-if="!account" class="no-account-box">
          <p class="no-account-msg">아직 개설된 계좌가 없습니다.</p>
          <button class="toss-btn toss-btn-primary create-account-btn" @click="handleCreateAccount" :disabled="creatingAccount">
            <span v-if="creatingAccount">계좌 신청 중...</span>
            <span v-else>✨ 13자리 계좌 개설 신청하기</span>
          </button>
        </div>

        <!-- 승인 대기(PENDING) 상태 -->
        <div v-else-if="account.status === 'PENDING'" class="pending-box">
          <div class="toss-amount dimmed">{{ formattedBalance }} 원</div>
          <div class="pending-alert">
            ℹ️ 현재 관리자 계좌 승인 대기 중입니다. 관리자 승인 후 거래가 가능해집니다.
          </div>
        </div>

        <!-- 활성(ACTIVE) 상태 -->
        <div v-else class="toss-amount">{{ formattedBalance }} 원</div>
      </div>

      <div v-if="account && account.status === 'ACTIVE'" class="card-actions">
        <button class="toss-btn toss-btn-subtle" @click="handleDeposit" :disabled="loading">
          <ArrowDownLeft :size="18" /> 입금 (테스트)
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
import { ArrowDownLeft, Send, CreditCard, Receipt, RefreshCw } from 'lucide-vue-next'

const userName = ref('사용자')
const userId = ref<number | null>(null)

const account = ref<any>(null)
const loading = ref(false)
const creatingAccount = ref(false)
const errorMessage = ref('')

const formattedBalance = computed(() => {
  return (account.value?.balance || 0).toLocaleString('ko-KR')
})

const checkUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      userName.value = user.name
      userId.value = user.id
    } catch (e) {
      console.error(e)
    }
  }
}

const fetchAccountInfo = async () => {
  if (!userId.value) return
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await fetch(`/api/v1/accounts/${userId.value}`)
    if (response.ok) {
      account.value = await response.json()
    } else if (response.status === 404) {
      account.value = null
    } else {
      errorMessage.value = '계좌 조회 실패'
    }
  } catch (error: any) {
    errorMessage.value = '계좌 조회 중 오류 발생'
  } finally {
    loading.value = false
  }
}

const handleCreateAccount = async () => {
  if (!userId.value) return
  creatingAccount.value = true

  try {
    const res = await fetch('/api/v1/accounts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ownerId: userId.value,
        initialBalance: 1000000
      })
    })

    if (res.ok) {
      const data = await res.json()
      account.value = data
      alert('🎉 계좌 개설 신청이 완료되었습니다! (관리자 승인 대기)')
    } else {
      alert('계좌 개설 신청에 실패했습니다.')
    }
  } catch (e) {
    alert('계좌 개설 신청 중 오류가 발생했습니다.')
  } finally {
    creatingAccount.value = false
  }
}

const handleDeposit = () => {
  if (account.value) {
    account.value.balance += 100000
  }
}

onMounted(() => {
  checkUser()
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

.status-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 8px;
}

.status-badge.pending {
  background: #fef3c7;
  color: #d97706;
}

.status-badge.active {
  background: #dcfce7;
  color: #16a34a;
}

.amount-wrapper {
  margin: 12px 0 24px;
}

.no-account-box {
  text-align: center;
  padding: 16px 0;
}

.no-account-msg {
  color: var(--text-secondary);
  font-size: 15px;
  margin-bottom: 14px;
}

.create-account-btn {
  width: 100%;
  padding: 14px;
  font-size: 15px;
}

.pending-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dimmed {
  opacity: 0.6;
}

.pending-alert {
  background: rgba(217, 119, 6, 0.1);
  border: 1px solid rgba(217, 119, 6, 0.2);
  color: #b45309;
  font-size: 13px;
  padding: 10px 12px;
  border-radius: 10px;
  line-height: 1.4;
}

.loading-text {
  font-size: 24px;
  color: var(--text-tertiary);
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
