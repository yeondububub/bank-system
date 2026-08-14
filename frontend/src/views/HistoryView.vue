<template>
  <div class="history-container">
    <!-- Header Row -->
    <div class="history-header">
      <h1 class="page-title">Transactions</h1>
      <p class="page-subtitle">계좌별 전체 입출금 및 송금 내역을 확인하세요.</p>
    </div>

    <!-- Filter Card -->
    <div class="bank-card filter-card">
      <div class="filter-grid">
        <!-- Account Select -->
        <div class="filter-field">
          <label class="filter-label">조회 계좌 선택</label>
          <div class="select-wrapper">
            <select v-model="selectedAccountNumber" class="toss-select" @change="fetchTransactions">
              <option value="ALL">전체 계좌 내역</option>
              <option 
                v-for="acc in userAccounts" 
                :key="acc.accountNumber" 
                :value="acc.accountNumber"
              >
                {{ acc.isPrimary ? '[대표]' : '[서브]' }} {{ formatAccNum(acc.accountNumber) }} (잔액: {{ acc.balance.toLocaleString() }}원)
              </option>
            </select>
          </div>
        </div>

        <!-- Type Filter Chips -->
        <div class="filter-field">
          <label class="filter-label">거래 유형 필터</label>
          <div class="chip-group">
            <button 
              v-for="filter in typeFilters" 
              :key="filter.value"
              :class="['toss-chip', { active: currentFilter === filter.value }]"
              @click="currentFilter = filter.value"
            >
              {{ filter.label }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Transactions List Container -->
    <div class="bank-card list-card">
      <!-- Loading State -->
      <div v-if="loading" class="state-box">
        <RefreshCw :size="28" class="spin-icon" />
        <span>거래 이력을 조회하는 중입니다...</span>
      </div>

      <!-- Empty State -->
      <div v-else-if="filteredTransactions.length === 0" class="state-box">
        <Inbox :size="42" class="empty-icon" />
        <h3 class="empty-title">거래 내역이 없습니다</h3>
        <p class="empty-sub">선택한 조건에 일치하는 입출금 이력이 존재하지 않습니다.</p>
      </div>

      <!-- Transaction Items List -->
      <div v-else class="tx-timeline-list">
        <div 
          v-for="item in filteredTransactions" 
          :key="item.id" 
          class="tx-item-card"
        >
          <div class="tx-item-left">
            <div :class="['tx-type-icon', getTypeClass(item.type)]">
              <ArrowDownLeft v-if="isPositiveType(item.type)" :size="20" />
              <ArrowUpRight v-else-if="item.type === 'TRANSFER_OUT'" :size="20" />
              <CreditCard v-else :size="20" />
            </div>

            <div class="tx-main-info">
              <div class="tx-header-line">
                <span class="tx-title-text">{{ getTransactionTitle(item) }}</span>
                <span :class="['tx-badge', getTypeClass(item.type)]">
                  {{ getTypeLabel(item.type) }}
                </span>
              </div>

              <div class="tx-sub-details">
                <span>계좌: {{ formatAccNum(item.accountNumber) }}</span>
                <span v-if="item.memo" class="memo-text">| {{ item.memo }}</span>
              </div>

              <div class="tx-timestamp">{{ item.createdAt }}</div>
            </div>
          </div>

          <div class="tx-item-right">
            <div :class="['tx-amount-text', isPositiveType(item.type) ? 'positive' : 'negative']">
              {{ isPositiveType(item.type) ? '+' : '-' }}{{ item.amount.toLocaleString() }} 원
            </div>
            <div class="tx-after-balance">
              잔액 {{ item.balanceAfter.toLocaleString() }} 원
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ArrowDownLeft, ArrowUpRight, CreditCard, RefreshCw, Inbox } from 'lucide-vue-next'
import axios from 'axios'

const userAccounts = ref<any[]>([])
const transactions = ref<any[]>([])
const selectedAccountNumber = ref<string>('ALL')
const currentFilter = ref<string>('ALL')
const loading = ref(false)

const typeFilters = [
  { label: '전체', value: 'ALL' },
  { label: '입금 (+)', value: 'IN' },
  { label: '출금/송금 (-)', value: 'OUT' },
  { label: '결제/환불', value: 'PAYMENT' }
]

const formatAccNum = (numStr?: string) => {
  if (!numStr) return ''
  const str = String(numStr).replace(/-/g, '')
  if (str.length === 13) {
    return `${str.slice(0, 3)}-${str.slice(3, 7)}-${str.slice(7, 11)}-${str.slice(11)}`
  }
  return str
}

const isPositiveType = (type: string) => {
  return type === 'TRANSFER_IN' || type === 'REFUND'
}

const getTypeClass = (type: string) => {
  switch (type) {
    case 'TRANSFER_IN': return 'type-in'
    case 'TRANSFER_OUT': return 'type-out'
    case 'PAYMENT': return 'type-payment'
    case 'REFUND': return 'type-refund'
    default: return ''
  }
}

const getTypeLabel = (type: string) => {
  switch (type) {
    case 'TRANSFER_IN': return '입금'
    case 'TRANSFER_OUT': return '송금'
    case 'PAYMENT': return '결제'
    case 'REFUND': return '환불'
    default: return type
  }
}

const getTransactionTitle = (item: any) => {
  switch (item.type) {
    case 'TRANSFER_IN':
      return item.counterpartyName ? `${item.counterpartyName}님 입금` : '입금'
    case 'TRANSFER_OUT':
      return item.counterpartyName ? `${item.counterpartyName}님에게 송금` : '송금 출금'
    case 'PAYMENT':
      return item.memo || '주문 결제'
    case 'REFUND':
      return item.memo || '결제 환불'
    default:
      return '거래 내역'
  }
}

const filteredTransactions = computed(() => {
  return transactions.value.filter((item: any) => {
    if (currentFilter.value === 'IN') {
      return item.type === 'TRANSFER_IN' || item.type === 'REFUND'
    }
    if (currentFilter.value === 'OUT') {
      return item.type === 'TRANSFER_OUT'
    }
    if (currentFilter.value === 'PAYMENT') {
      return item.type === 'PAYMENT' || item.type === 'REFUND'
    }
    return true
  })
})

const fetchUserAccounts = async (userId: number) => {
  try {
    const res = await axios.get(`/api/v1/accounts/user/${userId}`)
    userAccounts.value = res.data
  } catch (e) {
    userAccounts.value = []
  }
}

const fetchTransactions = async () => {
  loading.value = true
  const userStr = localStorage.getItem('user')
  if (!userStr) {
    loading.value = false
    return
  }
  const user = JSON.parse(userStr)

  try {
    if (selectedAccountNumber.value === 'ALL') {
      const res = await axios.get(`/api/v1/accounts/user/${user.id}/transactions`)
      transactions.value = res.data
    } else {
      const res = await axios.get(`/api/v1/accounts/${selectedAccountNumber.value}/transactions`)
      transactions.value = res.data
    }
  } catch (e) {
    transactions.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const token = localStorage.getItem('accessToken')
  let user: any = null
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      user = JSON.parse(userStr)
    } catch (e) {}
  }

  if ((!user || !user.id) && token) {
    try {
      const res = await fetch('/api/v1/auth/me', {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      if (res.ok) {
        user = await res.json()
        localStorage.setItem('user', JSON.stringify(user))
      }
    } catch (e) {}
  }

  if (user && user.id) {
    await fetchUserAccounts(user.id)
  }
  await fetchTransactions()
})
</script>

<style scoped>
.history-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-title {
  font-size: 32px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.8px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* Filter Card */
.filter-card {
  padding: 24px;
}

.filter-grid {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 24px;
  align-items: flex-end;
}

@media (max-width: 768px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
}

.toss-select {
  width: 100%;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  background-color: var(--surface-subtle);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
  outline: none;
  transition: all 0.2s ease;
}

.toss-select:focus {
  border-color: var(--toss-blue);
  box-shadow: 0 0 0 3px var(--toss-blue-light);
}

.chip-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.toss-chip {
  background-color: var(--surface-subtle);
  border: 1px solid var(--border-light);
  color: var(--text-secondary);
  padding: 8px 16px;
  border-radius: var(--radius-pill);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.toss-chip:hover {
  background-color: var(--surface-hover);
  color: var(--text-primary);
}

.toss-chip.active {
  background-color: var(--toss-gray);
  color: #ffffff;
  border-color: var(--toss-gray);
  font-weight: 700;
}

/* List Card */
.list-card {
  min-height: 360px;
  padding: 24px;
}

.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: var(--text-tertiary);
  gap: 12px;
}

.spin-icon {
  animation: spin 1s linear infinite;
  color: var(--toss-blue);
}

@keyframes spin {
  100% { transform: rotate(360deg); }
}

.empty-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.empty-sub {
  font-size: 13px;
  color: var(--text-tertiary);
}

/* Timeline List */
.tx-timeline-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tx-item-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-radius: var(--radius-md);
  background-color: var(--surface-subtle);
  border: 1px solid var(--border-light);
  transition: all 0.2s ease;
}

.tx-item-card:hover {
  border-color: var(--toss-blue-light);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.tx-item-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.tx-type-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tx-type-icon.type-in {
  background-color: var(--bank-green-light);
  color: var(--bank-green);
}

.tx-type-icon.type-out {
  background-color: var(--toss-blue-light);
  color: var(--toss-blue);
}

.tx-type-icon.type-payment {
  background-color: rgba(168, 85, 247, 0.1);
  color: #a855f7;
}

.tx-type-icon.type-refund {
  background-color: var(--bank-yellow-light);
  color: var(--bank-yellow);
}

.tx-main-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.tx-header-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tx-title-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.tx-badge {
  font-size: 11px;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: var(--radius-pill);
}

.tx-badge.type-in { background: var(--bank-green-light); color: var(--bank-green); }
.tx-badge.type-out { background: var(--toss-blue-light); color: var(--toss-blue); }
.tx-badge.type-payment { background: rgba(168, 85, 247, 0.15); color: #a855f7; }
.tx-badge.type-refund { background: var(--bank-yellow-light); color: var(--bank-yellow); }

.tx-sub-details {
  font-size: 13px;
  color: var(--text-secondary);
  display: flex;
  gap: 6px;
}

.memo-text {
  color: var(--text-tertiary);
}

.tx-timestamp {
  font-size: 11px;
  color: var(--text-tertiary);
}

.tx-item-right {
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.tx-amount-text {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.4px;
}

.tx-amount-text.positive {
  color: var(--bank-green);
}

.tx-amount-text.negative {
  color: var(--text-primary);
}

.tx-after-balance {
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>
