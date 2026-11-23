<template>
  <div class="history-container">
    <section class="bank-card history-card">
      <div class="history-head">
        <div>
          <h1 class="page-title">거래 내역</h1>
          <p class="page-subtitle">계좌별 입출금, 송금 및 결제 내역을 확인할 수 있어요.</p>
        </div>

        <div class="tab-group" role="group" aria-label="거래 유형 필터">
          <button
            v-for="filter in typeFilters"
            :key="filter.value"
            type="button"
            :class="['tab-chip', { active: currentFilter === filter.value }]"
            @click="currentFilter = filter.value"
          >
            {{ filter.label }}
          </button>
        </div>
      </div>

      <div class="account-select-row">
        <label for="history-account-select" class="select-label">조회 계좌</label>
        <select
          id="history-account-select"
          v-model="selectedAccountNumber"
          class="bank-input account-select"
          @change="fetchTransactions"
        >
          <option value="ALL">전체 계좌 내역</option>
          <option
            v-for="acc in userAccounts"
            :key="acc.accountNumber"
            :value="acc.accountNumber"
          >
            {{ acc.isPrimary ? '[대표]' : '[서브]' }} {{ formatAccNum(acc.accountNumber) }} (잔액 {{ acc.balance.toLocaleString() }}원)
          </option>
        </select>
      </div>

      <div v-if="loading" class="state-box">
        <RefreshCw :size="24" class="spin-icon" />
        <span>거래 내역을 불러오는 중입니다…</span>
      </div>

      <div v-else-if="filteredTransactions.length === 0" class="state-box">
        <Inbox :size="32" class="muted-icon" />
        <p class="empty-title">거래 내역이 없습니다</p>
        <p class="empty-sub">선택한 조건에 해당하는 입출금 기록이 아직 없어요.</p>
      </div>

      <ul v-else class="tx-list">
        <li v-for="item in filteredTransactions" :key="item.id" class="tx-row">
          <div class="tx-row-left">
            <div class="tx-title-line">
              <span class="tx-title">{{ getTransactionTitle(item) }}</span>
              <span :class="['tx-type-tag', getTypeClass(item.type)]">
                {{ getTypeLabel(item.type) }}
              </span>
            </div>

            <div class="tx-meta-line num">
              <span>{{ formatAccNum(item.accountNumber) }}</span>
              <span v-if="item.memo">· {{ item.memo }}</span>
              <span>· {{ item.createdAt }}</span>
            </div>
          </div>

          <div class="tx-row-right num">
            <div :class="['tx-amount', isPositiveType(item.type) ? 'plus' : 'minus']">
              {{ isPositiveType(item.type) ? '+' : '-' }}{{ item.amount.toLocaleString() }}원
            </div>
            <div class="tx-after-balance">
              잔액 {{ item.balanceAfter.toLocaleString() }}원
            </div>
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RefreshCw, Inbox } from 'lucide-vue-next'
import axios from 'axios'

const userAccounts = ref<any[]>([])
const transactions = ref<any[]>([])
const selectedAccountNumber = ref<string>('ALL')
const currentFilter = ref<string>('ALL')
const loading = ref(false)

const typeFilters = [
  { label: '전체', value: 'ALL' },
  { label: '입금', value: 'IN' },
  { label: '출금', value: 'OUT' },
  { label: '결제·환불', value: 'PAYMENT' }
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
  max-width: 820px;
  margin-inline: auto;
}

.history-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.history-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--text-primary);
}

.page-subtitle {
  margin-top: 4px;
  font-size: 14px;
  color: var(--text-secondary);
}

.tab-group {
  display: flex;
  background-color: var(--bg-color);
  padding: 3px;
  border-radius: var(--radius-sm);
}

.tab-chip {
  padding: 7px 14px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.tab-chip.active {
  background-color: var(--surface-white);
  color: var(--text-primary);
  font-weight: 700;
  box-shadow: var(--shadow-xs);
}

.account-select-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.select-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  white-space: nowrap;
}

.account-select {
  max-width: 360px;
  padding: 10px 14px;
  font-size: 14px;
}

.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 56px 16px;
  color: var(--text-tertiary);
  font-size: 14px;
  text-align: center;
}

.empty-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.empty-sub {
  font-size: 13px;
  color: var(--text-tertiary);
}

.muted-icon {
  color: var(--text-disabled);
}

.spin-icon {
  animation: spin 1s linear infinite;
  color: var(--toss-blue);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.tx-list {
  list-style: none;
  display: flex;
  flex-direction: column;
}

.tx-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 16px 8px;
  border-radius: var(--radius-md);
  transition: background-color 0.15s ease;
}

.tx-row:hover {
  background-color: var(--surface-subtle);
}

.tx-row-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.tx-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tx-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.tx-type-tag {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 6px;
}

.tx-type-tag.type-in {
  background-color: var(--toss-blue-light);
  color: var(--toss-blue-hover);
}

.tx-type-tag.type-out,
.tx-type-tag.type-payment {
  background-color: var(--bg-color);
  color: var(--text-secondary);
}

.tx-type-tag.type-refund {
  background-color: var(--bank-green-light);
  color: #028A53;
}

.tx-meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.tx-row-right {
  text-align: right;
  flex-shrink: 0;
}

.tx-amount {
  font-size: 16px;
  font-weight: 700;
}

.tx-amount.plus {
  color: var(--toss-blue);
}

.tx-amount.minus {
  color: var(--text-primary);
}

.tx-after-balance {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 2px;
}

@media (max-width: 640px) {
  .account-select-row {
    flex-direction: column;
    align-items: stretch;
  }

  .account-select {
    max-width: none;
  }
}
</style>
