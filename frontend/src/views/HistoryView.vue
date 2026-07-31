<template>
  <div class="history-view">
    <!-- 상단 네비게이션 -->
    <div class="header-nav">
      <button class="back-btn" @click="$router.push('/')">
        <ArrowLeft :size="22" />
      </button>
      <h2 class="nav-title">거래 내역 조회</h2>
    </div>

    <!-- 필터 및 계좌 선택 컨트롤 패널 -->
    <div class="bank-card filter-card">
      <div class="filter-row">
        <!-- 계좌 선택 드롭다운 -->
        <div class="account-select-box">
          <label class="select-label">조회 계좌</label>
          <select v-model="selectedAccountNumber" class="account-select" @change="fetchTransactions">
            <option value="ALL">전체 계좌</option>
            <option 
              v-for="acc in userAccounts" 
              :key="acc.accountNumber" 
              :value="acc.accountNumber"
            >
              {{ acc.isPrimary ? '[대표]' : '[서브]' }} {{ formatAccNum(acc.accountNumber) }} (잔액: {{ acc.balance.toLocaleString() }}원)
            </option>
          </select>
        </div>

        <!-- 거래 유형 필터 탭 -->
        <div class="type-filter-chips">
          <button 
            v-for="filter in typeFilters" 
            :key="filter.value"
            :class="['filter-chip', { active: currentFilter === filter.value }]"
            @click="currentFilter = filter.value"
          >
            {{ filter.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- 거래 이력 타임라인 리스트 카드 -->
    <div class="bank-card list-card">
      <!-- 로딩 상태 -->
      <div v-if="loading" class="loading-box">
        <RefreshCw :size="32" class="spin-icon" />
        <span>거래 내역을 불러오는 중입니다...</span>
      </div>

      <!-- 내역 없음 (Empty State) -->
      <div v-else-if="filteredTransactions.length === 0" class="empty-box">
        <Inbox :size="48" class="empty-icon" />
        <h4 class="empty-title">거래 내역이 없습니다</h4>
        <p class="empty-desc">선택하신 조건에 해당하는 입출금 및 결제 내역이 존재하지 않습니다.</p>
      </div>

      <!-- 거래 이력 목록 -->
      <div v-else class="history-list">
        <div 
          v-for="item in filteredTransactions" 
          :key="item.id" 
          class="history-item"
        >
          <div class="item-left">
            <!-- 거래 유형 아이콘 뱃지 -->
            <div :class="['item-icon-box', getTypeClass(item.type)]">
              <ArrowDownLeft v-if="item.type === 'TRANSFER_IN' || item.type === 'REFUND'" :size="20" />
              <ArrowUpRight v-else-if="item.type === 'TRANSFER_OUT'" :size="20" />
              <CreditCard v-else :size="20" />
            </div>

            <div class="item-info">
              <div class="item-title-row">
                <span class="item-title">{{ getTransactionTitle(item) }}</span>
                <span :class="['type-badge', getTypeClass(item.type)]">
                  {{ getTypeLabel(item.type) }}
                </span>
              </div>

              <div class="item-sub-info">
                <span class="item-account-tag">계좌: {{ formatAccNum(item.accountNumber) }}</span>
                <span v-if="item.memo" class="item-memo-tag">| {{ item.memo }}</span>
              </div>

              <div class="item-date">{{ item.createdAt }}</div>
            </div>
          </div>

          <div class="item-right">
            <!-- 금액 (입금/환불: 초록색 +, 출금/결제: 파란색 -) -->
            <div :class="['item-amount', isPositiveType(item.type) ? 'amount-plus' : 'amount-minus']">
              {{ isPositiveType(item.type) ? '+' : '-' }}{{ item.amount.toLocaleString() }} 원
            </div>
            <div class="item-balance">
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
import { ArrowLeft, ArrowDownLeft, ArrowUpRight, CreditCard, RefreshCw, Inbox } from 'lucide-vue-next'
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
    case 'TRANSFER_IN': return 'type-deposit'
    case 'TRANSFER_OUT': return 'type-transfer'
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
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      if (user.id) {
        await fetchUserAccounts(user.id)
      }
    } catch (e) {}
  }
  await fetchTransactions()
})
</script>

<style scoped>
.history-view {
  max-width: 720px;
  margin: 0 auto;
  padding-top: 10px;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.back-btn {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #ffffff;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: scale(1.05);
}

.nav-title {
  font-size: 22px;
  font-weight: 800;
  color: #ffffff;
}

/* 필터 카드 */
.filter-card {
  background: linear-gradient(145deg, #151c28 0%, #1e293b 100%);
  border-radius: 20px;
  padding: 20px 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.account-select-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.select-label {
  font-size: 12px;
  font-weight: 700;
  color: #94a3b8;
}

.account-select {
  background: rgba(15, 23, 42, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  outline: none;
}

.type-filter-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-chip {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #94a3b8;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-chip:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
}

.filter-chip.active {
  background: #3182f6;
  color: #ffffff;
  border-color: #3182f6;
  font-weight: 700;
}

/* 리스트 카드 */
.list-card {
  background: linear-gradient(145deg, #151c28 0%, #1e293b 100%);
  border-radius: 24px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
  min-height: 320px;
}

.loading-box, .empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 60px 0;
  color: #94a3b8;
}

.spin-icon {
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
  color: #3182f6;
}

@keyframes spin {
  100% { transform: rotate(360deg); }
}

.empty-icon {
  color: #475569;
  margin-bottom: 12px;
}

.empty-title {
  font-size: 18px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 6px;
}

.empty-desc {
  font-size: 13px;
  color: #64748b;
}

/* 거래 아이템 */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  transition: all 0.2s ease;
}

.history-item:hover {
  background: rgba(15, 23, 42, 0.8);
  border-color: rgba(49, 130, 246, 0.3);
  transform: translateY(-2px);
}

.item-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.item-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.item-icon-box.type-deposit {
  background: rgba(16, 185, 129, 0.15);
  color: #34d399;
}

.item-icon-box.type-transfer {
  background: rgba(49, 130, 246, 0.15);
  color: #60a5fa;
}

.item-icon-box.type-payment {
  background: rgba(168, 85, 247, 0.15);
  color: #c084fc;
}

.item-icon-box.type-refund {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-title {
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
}

.type-badge {
  font-size: 11px;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 8px;
}

.type-badge.type-deposit { background: rgba(16, 185, 129, 0.2); color: #34d399; }
.type-badge.type-transfer { background: rgba(49, 130, 246, 0.2); color: #60a5fa; }
.type-badge.type-payment { background: rgba(168, 85, 247, 0.2); color: #c084fc; }
.type-badge.type-refund { background: rgba(245, 158, 11, 0.2); color: #fbbf24; }

.item-sub-info {
  font-size: 12px;
  color: #94a3b8;
  display: flex;
  gap: 6px;
}

.item-date {
  font-size: 11px;
  color: #64748b;
}

.item-right {
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-amount {
  font-size: 17px;
  font-weight: 800;
}

.amount-plus {
  color: #34d399;
}

.amount-minus {
  color: #60a5fa;
}

.item-balance {
  font-size: 12px;
  color: #94a3b8;
}
</style>
