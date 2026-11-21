<template>
  <div class="home-container">
    <!-- 1. 총 자산 요약 카드 -->
    <section class="bank-card asset-summary-card" aria-label="총 자산 요약">
      <div class="asset-summary-top">
        <div>
          <p class="asset-owner-label">{{ userName }}님의 총 자산</p>
          <h1 class="asset-total-amount num">{{ totalBalance.toLocaleString() }}원</h1>
          <p class="asset-count-sub">
            보유 계좌 {{ accounts.length }}개 · 사용 가능 {{ activeAccountCount }}개
          </p>
        </div>

        <div class="asset-actions">
          <button
            type="button"
            class="bank-btn bank-btn-subtle icon-text-btn"
            @click="refreshAll"
            :disabled="loading"
          >
            <RefreshCw :size="15" :class="{ 'spin-icon': loading }" />
            <span>새로고침</span>
          </button>

          <button
            type="button"
            class="bank-btn bank-btn-primary icon-text-btn"
            @click="handleCreateAccount"
            :disabled="creatingAccount"
          >
            <Plus :size="16" />
            <span>{{ creatingAccount ? '신청 중…' : '새 계좌 개설' }}</span>
          </button>
        </div>
      </div>
    </section>

    <!-- 2. 내 계좌 목록 (토스 스타일 리스트형) -->
    <section class="bank-card accounts-card" aria-label="내 계좌 목록">
      <div class="card-header-row">
        <h2 class="section-title">계좌</h2>

        <div class="tab-group" role="tablist">
          <button
            v-for="tab in accountTabs"
            :key="tab.id"
            type="button"
            role="tab"
            :aria-selected="activeTab === tab.id"
            :class="['tab-chip', { active: activeTab === tab.id }]"
            @click="activeTab = tab.id"
          >
            {{ tab.name }}
          </button>
        </div>
      </div>

      <div v-if="loading" class="state-box">
        <RefreshCw :size="22" class="spin-icon" />
        <span>계좌 목록을 불러오는 중입니다…</span>
      </div>

      <div v-else-if="filteredAccounts.length === 0" class="state-box">
        <Inbox :size="28" class="muted-icon" />
        <span>표시할 계좌가 없습니다.</span>
      </div>

      <ul v-else class="account-list">
        <li v-for="acc in filteredAccounts" :key="acc.id" class="account-row">
          <div class="account-row-left">
            <button
              type="button"
              class="star-btn"
              @click="handleStarClick(acc)"
              :aria-label="isAccountPrimary(acc) ? '현재 대표 계좌' : '대표 계좌로 지정'"
              :title="isAccountPrimary(acc) ? '현재 대표 계좌입니다' : '클릭하여 대표 계좌로 지정'"
            >
              <Star
                :size="18"
                :fill="isAccountPrimary(acc) ? '#F59E0B' : 'none'"
                :color="isAccountPrimary(acc) ? '#F59E0B' : '#B0B8C1'"
              />
            </button>

            <div class="account-info">
              <div class="account-title-line">
                <span class="account-name">입출금 통장</span>
                <span v-if="isAccountPrimary(acc)" class="badge-primary">대표</span>
                <span v-if="acc.status === 'PENDING'" class="badge-pending">승인 대기</span>
              </div>

              <div class="account-number-line">
                <span class="account-number num">{{ formatAccNum(acc.accountNumber) }}</span>
                <button
                  type="button"
                  class="copy-btn"
                  @click="copyAccNum(acc.accountNumber)"
                  aria-label="계좌번호 복사"
                  title="계좌번호 복사"
                >
                  <Copy :size="13" />
                </button>
              </div>
            </div>
          </div>

          <div class="account-row-right">
            <div class="account-balance num">
              <span v-if="acc.status === 'PENDING'" class="pending-status-text">심사 대기 중</span>
              <span v-else>{{ acc.balance.toLocaleString() }}원</span>
            </div>

            <button
              v-if="acc.status === 'ACTIVE'"
              type="button"
              class="bank-btn bank-btn-soft transfer-btn"
              @click="goToTransfer(acc)"
            >
              송금
            </button>
          </div>
        </li>
      </ul>
    </section>

    <!-- 3. 최근 거래 내역 -->
    <section class="bank-card recent-tx-card" aria-label="최근 거래 내역">
      <div class="card-header-row">
        <h2 class="section-title">최근 거래 내역</h2>
        <router-link to="/history" class="view-all-link">
          <span>전체 보기</span>
          <ChevronRight :size="16" />
        </router-link>
      </div>

      <div v-if="txLoading" class="state-box">
        <RefreshCw :size="20" class="spin-icon" />
        <span>거래 내역을 불러오는 중입니다…</span>
      </div>

      <div v-else-if="recentTransactions.length === 0" class="state-box">
        <Inbox :size="28" class="muted-icon" />
        <span>최근 입출금 거래 내역이 없습니다.</span>
      </div>

      <ul v-else class="tx-list">
        <li v-for="tx in recentTransactions.slice(0, 5)" :key="tx.id" class="tx-row">
          <div class="tx-row-left">
            <span class="tx-title">{{ getTxTitle(tx) }}</span>
            <span class="tx-date num">{{ tx.createdAt }}</span>
          </div>

          <div :class="['tx-amount', 'num', isPositiveTx(tx.type) ? 'plus' : 'minus']">
            {{ isPositiveTx(tx.type) ? '+' : '-' }}{{ tx.amount.toLocaleString() }}원
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Plus,
  Star,
  ChevronRight,
  Copy,
  RefreshCw,
  Inbox
} from 'lucide-vue-next'

const router = useRouter()

const userName = ref('사용자')
const userRole = ref('USER')
const userId = ref<number | null>(null)

const accounts = ref<any[]>([])
const recentTransactions = ref<any[]>([])
const loading = ref(false)
const txLoading = ref(false)
const creatingAccount = ref(false)
const activeTab = ref('ALL')

const accountTabs = [
  { id: 'ALL', name: '전체' },
  { id: 'PRIMARY', name: '대표' },
  { id: 'PENDING', name: '승인 대기' }
]

const authHeaders = (extra: Record<string, string> = {}): Record<string, string> => {
  const token = localStorage.getItem('accessToken')
  return token ? { Authorization: `Bearer ${token}`, ...extra } : { ...extra }
}

const checkUser = async () => {
  const token = localStorage.getItem('accessToken')
  const userStr = localStorage.getItem('user')

  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      if (user && user.id) {
        userName.value = user.name
        userRole.value = user.role || 'USER'
        userId.value = user.id
        return
      }
    } catch (e) {
      console.error(e)
    }
  }

  if (token) {
    try {
      const res = await fetch('/api/v1/auth/me', {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      if (res.ok) {
        const user = await res.json()
        userName.value = user.name
        userRole.value = user.role || 'USER'
        userId.value = user.id
        localStorage.setItem('user', JSON.stringify(user))
      }
    } catch (e) {
      console.error('사용자 정보 조회 실패:', e)
    }
  }
}

const isAccountPrimary = (acc: any) => {
  if (!acc) return false
  return Boolean(acc.isPrimary ?? acc.primary ?? false)
}

const goToTransfer = (acc: any) => {
  if (!acc) return
  if (acc.status !== 'ACTIVE') {
    alert('관리자 승인 완료(ACTIVE)된 계좌만 송금이 가능합니다.')
    return
  }
  router.push({
    path: '/payment',
    query: { from: acc.accountNumber }
  })
}

const totalBalance = computed(() => {
  return accounts.value.reduce((sum, acc) => {
    if (acc.status === 'ACTIVE') {
      return sum + (acc.balance || 0)
    }
    return sum
  }, 0)
})

const activeAccountCount = computed(() => {
  return accounts.value.filter(a => a.status === 'ACTIVE').length
})

const sortedAccounts = computed(() => {
  const list = [...accounts.value]
  return list.sort((a, b) => {
    const aPrimary = isAccountPrimary(a) ? 1 : 0
    const bPrimary = isAccountPrimary(b) ? 1 : 0
    if (aPrimary !== bPrimary) {
      return bPrimary - aPrimary
    }
    return 0
  })
})

const filteredAccounts = computed(() => {
  const list = sortedAccounts.value
  if (activeTab.value === 'PRIMARY') {
    return list.filter(a => isAccountPrimary(a))
  }
  if (activeTab.value === 'PENDING') {
    return list.filter(a => a.status === 'PENDING')
  }
  return list
})

const formatAccNum = (accNum: any) => {
  if (!accNum) return ''
  const num = String(accNum)
  if (num.length === 13) {
    return `${num.slice(0, 3)}-${num.slice(3, 7)}-${num.slice(7, 11)}-${num.slice(11)}`
  }
  return num
}

const fetchAccounts = async () => {
  if (!userId.value) return
  loading.value = true

  try {
    const res = await fetch(`/api/v1/accounts/user/${userId.value}`, {
      headers: authHeaders()
    })
    if (res.ok) {
      accounts.value = await res.json()
    } else {
      accounts.value = []
    }
  } catch (e) {
    console.error('계좌 조회 실패:', e)
  } finally {
    loading.value = false
  }
}

const fetchRecentTransactions = async () => {
  if (!userId.value) return
  txLoading.value = true

  try {
    const res = await fetch(`/api/v1/accounts/user/${userId.value}/transactions`, {
      headers: authHeaders()
    })
    if (res.ok) {
      recentTransactions.value = await res.json()
    } else {
      recentTransactions.value = []
    }
  } catch (e) {
    recentTransactions.value = []
  } finally {
    txLoading.value = false
  }
}

const refreshAll = () => {
  fetchAccounts()
  fetchRecentTransactions()
}

const handleCreateAccount = async () => {
  if (!userId.value) return
  creatingAccount.value = true

  try {
    const res = await fetch('/api/v1/accounts', {
      method: 'POST',
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({
        ownerId: userId.value
      })
    })

    if (res.ok) {
      alert('13자리 신규 계좌 개설 신청이 완료되었습니다. (관리자 승인 대기)')
      await fetchAccounts()
    } else {
      alert('계좌 개설 신청 실패')
    }
  } catch (e) {
    alert('계좌 개설 중 오류가 발생했습니다.')
  } finally {
    creatingAccount.value = false
  }
}

const handleStarClick = async (acc: any) => {
  if (!acc || isAccountPrimary(acc)) return
  if (!confirm('이 계좌를 대표 계좌로 변경하시겠습니까?')) return

  try {
    const res = await fetch(`/api/v1/accounts/${acc.id}/primary`, {
      method: 'POST',
      headers: authHeaders()
    })
    if (res.ok) {
      accounts.value.forEach(a => {
        const isCurrent = String(a.id) === String(acc.id)
        a.isPrimary = isCurrent
        a.primary = isCurrent
      })
      alert('대표 계좌로 변경되었습니다.')
      await fetchAccounts()
    }
  } catch (e) {
    alert('대표 계좌 변경 중 오류 발생')
  }
}

const copyAccNum = (accNum: string) => {
  if (accNum) {
    navigator.clipboard.writeText(accNum)
    alert('계좌번호가 복사되었습니다.')
  }
}

const isPositiveTx = (type: string) => {
  return type === 'TRANSFER_IN' || type === 'REFUND'
}

const getTxTitle = (item: any) => {
  switch (item.type) {
    case 'TRANSFER_IN':
      return item.counterpartyName ? `${item.counterpartyName}님 입금` : '입금'
    case 'TRANSFER_OUT':
      return item.counterpartyName ? `${item.counterpartyName}님에게 송금` : '송금 출금'
    case 'PAYMENT':
      return item.memo || '온라인 결제'
    case 'REFUND':
      return item.memo || '결제 환불'
    default:
      return '거래 이력'
  }
}

onMounted(async () => {
  await checkUser()
  await fetchAccounts()
  await fetchRecentTransactions()
})
</script>

<style scoped>
.home-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 780px;
  margin-inline: auto;
}

/* 1. 총 자산 요약 카드 */
.asset-summary-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  flex-wrap: wrap;
}

.asset-owner-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.asset-total-amount {
  font-size: 34px;
  font-weight: 800;
  letter-spacing: -1px;
  color: var(--text-primary);
  line-height: 1.2;
}

.asset-count-sub {
  margin-top: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-tertiary);
}

.asset-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-text-btn {
  padding: 10px 16px;
  font-size: 14px;
}

/* 2. 공통 카드 헤더 */
.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: -0.3px;
  color: var(--text-primary);
}

.tab-group {
  display: flex;
  background-color: var(--bg-color);
  padding: 3px;
  border-radius: var(--radius-sm);
}

.tab-chip {
  padding: 6px 12px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.tab-chip.active {
  background-color: var(--surface-white);
  color: var(--text-primary);
  font-weight: 700;
  box-shadow: var(--shadow-xs);
}

/* 3. 계좌 리스트 */
.account-list {
  list-style: none;
  display: flex;
  flex-direction: column;
}

.account-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 8px;
  border-radius: var(--radius-md);
  transition: background-color 0.15s ease;
}

.account-row:hover {
  background-color: var(--surface-subtle);
}

.account-row-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.star-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: background-color 0.15s ease;
}

.star-btn:hover {
  background-color: var(--bg-color);
}

.account-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.account-title-line {
  display: flex;
  align-items: center;
  gap: 6px;
}

.account-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.badge-primary {
  font-size: 11px;
  font-weight: 700;
  color: var(--toss-blue-hover);
  background-color: var(--toss-blue-light);
  padding: 2px 7px;
  border-radius: 6px;
}

.badge-pending {
  font-size: 11px;
  font-weight: 700;
  color: #B45309;
  background-color: var(--bank-yellow-light);
  padding: 2px 7px;
  border-radius: 6px;
}

.account-number-line {
  display: flex;
  align-items: center;
  gap: 4px;
}

.account-number {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-tertiary);
}

.copy-btn {
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  padding: 4px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease;
}

.copy-btn:hover {
  color: var(--text-primary);
  background-color: var(--bg-color);
}

.account-row-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.account-balance {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
}

.pending-status-text {
  font-size: 13px;
  font-weight: 600;
  color: #B45309;
}

.transfer-btn {
  padding: 8px 14px;
  font-size: 13px;
  border-radius: var(--radius-sm);
}

/* 4. 최근 거래 내역 */
.view-all-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  text-decoration: none;
  transition: color 0.15s ease;
}

.view-all-link:hover {
  color: var(--toss-blue);
}

.tx-list {
  list-style: none;
  display: flex;
  flex-direction: column;
}

.tx-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 8px;
  border-radius: var(--radius-md);
  transition: background-color 0.15s ease;
}

.tx-row:hover {
  background-color: var(--surface-subtle);
}

.tx-row-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.tx-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.tx-date {
  font-size: 12px;
  color: var(--text-tertiary);
}

.tx-amount {
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
}

.tx-amount.plus {
  color: var(--toss-blue);
}

.tx-amount.minus {
  color: var(--text-primary);
}

/* 5. 상태 박스 */
.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px 16px;
  color: var(--text-tertiary);
  font-size: 14px;
  font-weight: 500;
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

@media (max-width: 640px) {
  .asset-total-amount {
    font-size: 28px;
  }

  .account-row {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .account-row-right {
    justify-content: space-between;
    padding-left: 44px;
  }
}
</style>
