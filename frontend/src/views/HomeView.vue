<template>
  <div class="dashboard-container">
    <!-- 1. Top Header & Asset Summary -->
    <div class="dashboard-top-section">
      <div class="section-header-row">
        <div>
          <h1 class="page-title">내 계좌</h1>
          <p class="page-subtitle">{{ userName }}님의 등록된 스마트 뱅킹 계좌 현황입니다.</p>
        </div>

        <div class="top-action-group">
          <button class="add-account-btn" @click="handleCreateAccount" :disabled="creatingAccount">
            <Plus :size="16" />
            <span>{{ creatingAccount ? '개설 신청 중...' : '+ 새 계좌 개설' }}</span>
          </button>
        </div>
      </div>

      <!-- Total Asset Overview Banner -->
      <div class="bank-card total-asset-banner">
        <div class="asset-info-left">
          <div class="asset-label">총 자산 (보유 계좌 잔액 합계)</div>
          <div class="asset-amount">{{ totalBalance.toLocaleString() }} 원</div>
        </div>
        <div class="asset-info-right">
          <div class="asset-stat-chip">
            <Wallet :size="16" />
            <span>보유 계좌 <strong>{{ accounts.length }}</strong>개 (활성 <strong>{{ activeAccountCount }}</strong>개)</span>
          </div>
          <button class="refresh-asset-btn" @click="refreshAll" :disabled="loading">
            <RefreshCw :size="14" :class="{ 'spin-icon': loading }" />
            <span>갱신</span>
          </button>
        </div>
      </div>

      <!-- Sub Navigation Tabs -->
      <div class="sub-nav-tabs">
        <button 
          v-for="tab in accountTabs" 
          :key="tab.id"
          :class="['tab-pill', { active: activeTab === tab.id }]"
          @click="activeTab = tab.id"
        >
          {{ tab.name }}
        </button>
      </div>
    </div>

    <!-- 2. Bank Accounts Showcase Grid -->
    <div v-if="loading" class="accounts-loading-state">
      <RefreshCw :size="28" class="spin-icon" />
      <span>계좌 목록을 불러오는 중입니다...</span>
    </div>

    <div v-else class="accounts-showcase-grid">
      <!-- Bank Account Cards Loop -->
      <div 
        v-for="(acc, index) in filteredAccounts" 
        :key="acc.id"
        :class="['account-card', getCardStyleClass(index, acc)]"
      >
        <div class="account-card-top">
          <div class="account-type-badge">
            <Landmark :size="14" />
            <span>입출금 통장</span>
          </div>

          <div class="card-top-right">
            <!-- Primary Star Toggle Button -->
            <button 
              class="account-star-btn"
              @click.stop="handleStarClick(acc)"
              :title="isAccountPrimary(acc) ? '현재 대표 계좌입니다' : '클릭하여 대표 계좌로 지정'"
            >
              <Star 
                :size="18" 
                :fill="isAccountPrimary(acc) ? '#FFC107' : 'none'" 
                :color="isAccountPrimary(acc) ? '#FFC107' : 'rgba(255,255,255,0.7)'" 
              />
            </button>
          </div>
        </div>

        <div class="account-card-body">
          <div class="account-num-row">
            <span class="account-num-text">{{ formatAccNum(acc.accountNumber) }}</span>
            <button class="account-copy-btn" @click.stop="copyAccNum(acc.accountNumber)" title="계좌번호 복사">
              <Copy :size="13" />
            </button>
          </div>

          <div class="account-balance-val">
            <span v-if="acc.status === 'PENDING'" class="pending-text">승인 대기 중</span>
            <span v-else>{{ acc.balance.toLocaleString() }} 원</span>
          </div>
        </div>

        <div class="account-card-bottom">
          <div class="account-owner">
            <span class="owner-name">{{ userName }}</span>
            <span v-if="isAccountPrimary(acc)" class="primary-badge">대표 계좌</span>
            <span v-else-if="acc.status === 'PENDING'" class="pending-badge">승인 대기</span>
          </div>

          <div class="account-card-actions">
            <button 
              v-if="acc.status === 'ACTIVE'"
              class="card-transfer-btn" 
              @click.stop="goToTransfer(acc)"
            >
              <Send :size="13" />
              <span>송금</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Add New Account Callout Card -->
      <div class="account-card card-add-placeholder" @click="handleCreateAccount">
        <div class="add-card-inner">
          <div class="add-icon-circle">
            <Plus :size="28" />
          </div>
          <div class="add-card-title">+ 새 계좌 개설 신청</div>
          <div class="add-card-desc">13자리 스마트 뱅킹 입출금 통장 추가</div>
        </div>
      </div>
    </div>

    <!-- 3. Bottom Recent Transactions Section -->
    <div class="bank-card transactions-panel">
      <div class="panel-header-row">
        <div class="panel-title-group">
          <h3 class="panel-title">최근 거래 내역</h3>
          <span class="panel-tag">실시간 거래 기록</span>
        </div>
        <router-link to="/history" class="external-link-btn" title="전체 거래 내역">
          <ArrowUpRight :size="20" />
        </router-link>
      </div>

      <div v-if="txLoading" class="tx-loading">
        <RefreshCw :size="20" class="spin-icon" />
        <span>거래 내역 조회 중...</span>
      </div>

      <div v-else-if="recentTransactions.length === 0" class="tx-empty">
        <Inbox :size="32" class="empty-icon" />
        <span>최근 입출금 거래 내역이 없습니다.</span>
      </div>

      <div v-else class="tx-list">
        <div 
          v-for="tx in recentTransactions.slice(0, 5)" 
          :key="tx.id"
          class="tx-row-item"
        >
          <div class="tx-row-left">
            <div :class="['tx-icon-circle', getTxIconClass(tx.type)]">
              <ArrowDownLeft v-if="isPositiveTx(tx.type)" :size="18" />
              <ArrowUpRight v-else-if="tx.type === 'TRANSFER_OUT'" :size="18" />
              <Receipt v-else :size="18" />
            </div>

            <div class="tx-details">
              <div class="tx-title">{{ getTxTitle(tx) }}</div>
              <div class="tx-date">{{ tx.createdAt }}</div>
            </div>
          </div>

          <div :class="['tx-amount', isPositiveTx(tx.type) ? 'plus' : 'minus']">
            {{ isPositiveTx(tx.type) ? '+' : '-' }}{{ tx.amount.toLocaleString() }} 원
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  Plus, 
  Star, 
  Send, 
  ArrowUpRight, 
  ArrowDownLeft, 
  Copy, 
  RefreshCw, 
  Inbox,
  Landmark,
  Wallet,
  Receipt
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
  { id: 'ALL', name: '전체 계좌' },
  { id: 'PRIMARY', name: '대표 계좌' },
  { id: 'PENDING', name: '승인 대기' }
]

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
  let list = sortedAccounts.value
  if (activeTab.value === 'PRIMARY') {
    return list.filter(a => isAccountPrimary(a))
  }
  if (activeTab.value === 'PENDING') {
    return list.filter(a => a.status === 'PENDING')
  }
  return list
})

const getCardStyleClass = (_index: number, acc: any) => {
  if (isAccountPrimary(acc)) {
    return 'card-toss-blue'
  } else if (acc.status === 'PENDING') {
    return 'card-toss-silver'
  } else {
    return 'card-toss-gray'
  }
}

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
    const res = await fetch(`/api/v1/accounts/user/${userId.value}`)
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
    const res = await fetch(`/api/v1/accounts/user/${userId.value}/transactions`)
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
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ownerId: userId.value,
        initialBalance: 1000000
      })
    })

    if (res.ok) {
      alert('13자리 신규 계좌 개설 신청이 완료되었습니다! (관리자 승인 대기)')
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
    const res = await fetch(`/api/v1/accounts/${acc.id}/primary`, { method: 'POST' })
    if (res.ok) {
      accounts.value.forEach(a => {
        const isCurrent = String(a.id) === String(acc.id)
        a.isPrimary = isCurrent
        a.primary = isCurrent
      })
      alert('대표 계좌로 변경되었습니다!')
      await fetchAccounts()
    }
  } catch (e) {
    alert('대표 계좌 변경 중 오류 발생')
  }
}

const copyAccNum = (accNum: string) => {
  if (accNum) {
    navigator.clipboard.writeText(accNum)
    alert('계좌번호가 복사되었습니다!')
  }
}

const isPositiveTx = (type: string) => {
  return type === 'TRANSFER_IN' || type === 'REFUND'
}

const getTxIconClass = (type: string) => {
  if (isPositiveTx(type)) return 'green'
  return 'blue'
}

const getTxTitle = (item: any) => {
  switch (item.type) {
    case 'TRANSFER_IN':
      return item.counterpartyName ? `${item.counterpartyName}님 입금` : '입금'
    case 'TRANSFER_OUT':
      return item.counterpartyName ? `${item.counterpartyName}님 송금` : '송금 출금'
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
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* Top Section Header & Sub Tabs */
.dashboard-top-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
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

.add-account-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: var(--toss-blue);
  color: #ffffff;
  border: none;
  padding: 12px 22px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 14px var(--toss-blue-glow);
  transition: all 0.2s ease;
}

.add-account-btn:hover:not(:disabled) {
  background-color: var(--toss-blue-hover);
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(0, 100, 255, 0.35);
}

/* Total Asset Overview Banner */
.total-asset-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 30px;
  background: linear-gradient(135deg, #ffffff 0%, #F4F7FC 100%);
  border: 1px solid var(--border-light);
  border-left: 6px solid var(--toss-blue);
  box-shadow: var(--shadow-sm);
}

.asset-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.asset-amount {
  font-size: 32px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -1px;
}

.asset-info-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.asset-stat-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--surface-white);
  padding: 8px 14px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border-light);
  font-size: 13px;
  color: var(--text-secondary);
}

.refresh-asset-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--surface-subtle);
  border: 1px solid var(--border-light);
  color: var(--text-secondary);
  padding: 8px 14px;
  border-radius: var(--radius-pill);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.refresh-asset-btn:hover {
  color: var(--toss-blue);
  border-color: var(--toss-blue-light);
  background: var(--toss-blue-light);
}

.sub-nav-tabs {
  display: flex;
  gap: 16px;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 12px;
}

.tab-pill {
  background: transparent;
  border: none;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 6px 4px;
  position: relative;
  transition: color 0.2s ease;
}

.tab-pill.active {
  color: var(--text-primary);
  font-weight: 800;
}

.tab-pill.active::after {
  content: '';
  position: absolute;
  bottom: -13px;
  left: 0;
  right: 0;
  height: 3px;
  background-color: var(--toss-blue);
  border-radius: 3px;
}

/* Accounts Showcase Grid */
.accounts-loading-state {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 40px;
  color: var(--text-secondary);
}

.spin-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  100% { transform: rotate(360deg); }
}

.accounts-showcase-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

@media (max-width: 1100px) {
  .accounts-showcase-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .accounts-showcase-grid {
    grid-template-columns: 1fr;
  }
}

.account-card {
  height: 190px;
  border-radius: var(--radius-xl);
  padding: 22px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
}

.account-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}

/* Card Visual Variants */
.card-toss-blue {
  background: linear-gradient(135deg, #0064FF 0%, #0042AD 100%);
  color: #ffffff;
}

.card-toss-gray {
  background: var(--toss-gray);
  color: #ffffff;
}

.card-toss-silver {
  background: #E5E8EB;
  color: var(--text-primary);
}

.card-add-placeholder {
  background: #ffffff;
  border: 2px dashed var(--border-light);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-add-placeholder:hover {
  border-color: var(--toss-blue);
  background: var(--toss-blue-light);
}

.add-card-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
}

.add-icon-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--surface-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--toss-blue);
}

.add-card-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.add-card-desc {
  font-size: 12px;
  color: var(--text-tertiary);
}

.account-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.account-type-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  opacity: 0.9;
}

.account-star-btn {
  background: rgba(255, 255, 255, 0.15);
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.account-star-btn:hover {
  transform: scale(1.1);
  background: rgba(255, 255, 255, 0.25);
}

.account-num-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.account-num-text {
  font-size: 13px;
  font-weight: 600;
  opacity: 0.85;
}

.account-copy-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: inherit;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s ease;
}

.account-copy-btn:hover {
  background: rgba(255, 255, 255, 0.35);
}

.account-balance-val {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.pending-text {
  font-size: 18px;
  color: var(--bank-yellow);
}

.account-card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.account-owner {
  display: flex;
  align-items: center;
  gap: 8px;
}

.owner-name {
  font-size: 13px;
  font-weight: 700;
}

.primary-badge {
  font-size: 10px;
  font-weight: 800;
  background: rgba(255, 255, 255, 0.25);
  padding: 2px 8px;
  border-radius: var(--radius-pill);
}

.pending-badge {
  font-size: 10px;
  font-weight: 800;
  background: var(--bank-yellow-light);
  color: var(--bank-yellow);
  padding: 2px 8px;
  border-radius: var(--radius-pill);
}

.card-transfer-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: inherit;
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.card-transfer-btn:hover {
  background: rgba(255, 255, 255, 0.35);
}

/* Dashboard Bottom 2-Column Grid */
.dashboard-bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 28px;
}

@media (max-width: 992px) {
  .dashboard-bottom-grid {
    grid-template-columns: 1fr;
  }
}

.panel-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.panel-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-title {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.4px;
}

.panel-tag {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-tertiary);
  background: var(--surface-subtle);
  padding: 2px 8px;
  border-radius: var(--radius-pill);
}

.external-link-btn {
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.external-link-btn:hover {
  background: var(--surface-hover);
  color: var(--toss-blue);
}

/* Management List */
.management-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mgmt-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background-color: var(--surface-subtle);
  border: 1px solid var(--border-light);
}

.mgmt-item-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.mgmt-icon-box {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--surface-white);
  border: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-primary);
}

.mgmt-icon-box.yellow {
  color: #F79009;
  background: rgba(247, 144, 9, 0.1);
  border-color: rgba(247, 144, 9, 0.2);
}

.mgmt-icon-box.blue {
  color: var(--toss-blue);
  background: var(--toss-blue-light);
  border-color: rgba(0, 100, 255, 0.2);
}

.mgmt-item-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.mgmt-item-sub {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.copy-mini-btn, .mgmt-action-btn {
  border: 1px solid var(--border-light);
  background: var(--surface-white);
  padding: 6px 14px;
  border-radius: var(--radius-pill);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-primary);
}

.mgmt-action-btn.primary {
  background: var(--toss-blue);
  color: #ffffff;
  border: none;
}

.mgmt-action-btn:hover:not(:disabled) {
  background: var(--surface-hover);
  border-color: var(--toss-blue);
}

.mgmt-action-btn.primary:hover {
  background: var(--toss-blue-hover);
}

.mgmt-action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Latest Transactions List */
.tx-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tx-row-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  transition: background-color 0.2s ease;
}

.tx-row-item:hover {
  background-color: var(--surface-subtle);
}

.tx-row-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.tx-icon-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tx-icon-circle.green {
  background-color: var(--bank-green-light);
  color: var(--bank-green);
}

.tx-icon-circle.blue {
  background-color: var(--toss-blue-light);
  color: var(--toss-blue);
}

.tx-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.tx-date {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 2px;
}

.tx-amount {
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.3px;
}

.tx-amount.plus {
  color: var(--bank-green);
}

.tx-amount.minus {
  color: var(--text-primary);
}

.tx-empty, .tx-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 30px;
  color: var(--text-tertiary);
  font-size: 14px;
}
</style>
