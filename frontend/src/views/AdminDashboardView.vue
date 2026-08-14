<template>
  <div class="admin-container">
    <!-- 1. Admin Top Header -->
    <div class="admin-header">
      <div>
        <span class="admin-badge">관리자 심사 센터</span>
        <h1 class="page-title">계좌 심사 & 통합 거래 관리</h1>
        <p class="page-subtitle">신규 개설 요청된 계좌를 심사하고 서버 페이징 기반 무한 스크롤로 거래 내역을 모니터링합니다.</p>
      </div>

      <button class="refresh-btn" @click="refreshAllData" :disabled="loading || txLoading">
        <RefreshCw :size="16" :class="{ 'spin-icon': loading || txLoading }" />
        <span>전체 데이터 갱신</span>
      </button>
    </div>

    <!-- 2. Summary Stats Grid -->
    <div class="stats-grid">
      <div class="bank-card stat-card">
        <div class="stat-icon-box bg-yellow">
          <Clock :size="24" />
        </div>
        <div class="stat-info">
          <span class="stat-label">계좌 승인 대기</span>
          <span class="stat-value text-yellow">{{ pendingAccounts.length }} 건</span>
        </div>
      </div>

      <div class="bank-card stat-card">
        <div class="stat-icon-box bg-blue">
          <ArrowLeftRight :size="24" />
        </div>
        <div class="stat-info">
          <span class="stat-label">조회된 거래 건수</span>
          <span class="stat-value text-blue">{{ filteredTransactions.length }} 건</span>
        </div>
      </div>

      <div class="bank-card stat-card">
        <div class="stat-icon-box bg-green">
          <CheckCircle2 :size="24" />
        </div>
        <div class="stat-info">
          <span class="stat-label">이번 세션 승인</span>
          <span class="stat-value text-green">{{ approvedCount }} 건</span>
        </div>
      </div>

      <div class="bank-card stat-card">
        <div class="stat-icon-box bg-red">
          <XCircle :size="24" />
        </div>
        <div class="stat-info">
          <span class="stat-label">이번 세션 거절</span>
          <span class="stat-value text-red">{{ rejectedCount }} 건</span>
        </div>
      </div>
    </div>

    <!-- 3. TOP SECTION: Pending Accounts Approval Section -->
    <div class="section-container">
      <div class="section-header-row">
        <div class="section-title-box">
          <h2 class="section-title">신규 계좌 개설 승인 대기 목록</h2>
          <span class="count-badge">{{ pendingAccounts.length }}</span>
        </div>
        <p class="section-desc">본인 인증이 완료된 고객의 신규 13자리 계좌 개설 요청건입니다.</p>
      </div>

      <div v-if="loading" class="empty-state">
        <RefreshCw :size="32" class="spin-icon" />
        <p>승인 대기 계좌 목록을 조회 중입니다...</p>
      </div>

      <div v-else-if="pendingAccounts.length === 0" class="empty-state">
        <CheckCircle2 :size="48" class="empty-icon-green" />
        <h3>승인 대기 중인 계좌가 없습니다.</h3>
        <p>모든 신규 계좌 개설 요청이 정상 처리되었습니다.</p>
      </div>

      <div v-else class="pending-list-grid">
        <div 
          v-for="acc in pendingAccounts" 
          :key="acc.id" 
          class="bank-card pending-card"
        >
          <div class="card-header-row">
            <span class="acc-type-pill">신규 계좌 신청</span>
            <span class="status-tag pending">승인 대기</span>
          </div>

          <div class="card-main-info">
            <div class="info-primary">
              <span class="owner-tag">소유자 ID: {{ acc.ownerId }}</span>
              <span class="acc-number">{{ formatAccountNumber(acc.accountNumber) }}</span>
            </div>
            <div class="info-secondary">
              <span class="balance-label">신청 초기 잔액:</span>
              <strong class="balance-val">{{ acc.balance.toLocaleString() }} 원</strong>
            </div>
          </div>

          <div class="card-actions">
            <button 
              class="action-btn approve-btn" 
              @click="handleApprove(acc.id)"
              :disabled="processingId === acc.id"
            >
              <CheckCircle2 :size="16" />
              <span>{{ processingId === acc.id ? '처리 중...' : '승인 처리' }}</span>
            </button>

            <button 
              class="action-btn reject-btn" 
              @click="handleReject(acc.id)"
              :disabled="processingId === acc.id"
            >
              <XCircle :size="16" />
              <span>{{ processingId === acc.id ? '처리 중...' : '거절 처리' }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. BOTTOM SECTION: Integrated Transactions Section -->
    <div class="section-container transactions-section">
      <div class="section-header-row">
        <div class="section-title-box">
          <h2 class="section-title">통합 거래 내역</h2>
          <span class="count-badge blue">{{ filteredTransactions.length }}</span>
        </div>
        <p class="section-desc">지정된 기간 및 계좌 조건에 따른 전체 입출금, 송금 및 결제 내역입니다.</p>
      </div>

      <!-- Advanced Filter & Date Range Card -->
      <div class="bank-card safe-filter-card">
        <div class="filter-controls-grid">
          <!-- Date Range Control -->
          <div class="control-group">
            <label class="control-label">
              <Calendar :size="14" />
              <span>조회 기간 (시작일 ~ 종료일)</span>
            </label>
            <div class="date-input-group">
              <input type="date" v-model="startDate" class="bank-date-input" @change="onDateInputChange" />
              <span class="date-sep">~</span>
              <input type="date" v-model="endDate" class="bank-date-input" @change="onDateInputChange" />
              
              <div class="preset-chips">
                <button 
                  type="button" 
                  :class="['preset-btn', { active: activePreset === 0 }]" 
                  @click="setPresetDays(0)"
                >오늘</button>
                <button 
                  type="button" 
                  :class="['preset-btn', { active: activePreset === 7 }]" 
                  @click="setPresetDays(7)"
                >1주일</button>
                <button 
                  type="button" 
                  :class="['preset-btn', { active: activePreset === 30 }]" 
                  @click="setPresetDays(30)"
                >1개월</button>
                <button 
                  type="button" 
                  :class="['preset-btn', { active: activePreset === 90 }]" 
                  @click="setPresetDays(90)"
                >3개월</button>
                <button 
                  type="button" 
                  :class="['preset-btn', 'reset', { active: activePreset === 'ALL' }]" 
                  @click="clearDates"
                >전체</button>
              </div>
            </div>
          </div>

          <!-- Target Account Number Control -->
          <div class="control-group">
            <label class="control-label">
              <CreditCard :size="14" />
              <span>계좌번호 검색</span>
            </label>
            <div class="account-search-wrapper">
              <input 
                v-model="targetAccountNumber" 
                type="text" 
                placeholder="13자리 계좌번호 입력 (예: 100-2004-5678-90)" 
                class="bank-account-input"
                @keyup.enter="handleAccountSearch"
              />
              <button type="button" class="search-submit-btn" @click="handleAccountSearch">
                <Search :size="16" />
                <span>조회</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Filter Bar Bottom: Type & Keyword -->
        <div class="filter-bar-bottom">
          <div class="filter-chips">
            <button 
              :class="['filter-btn', { active: selectedFilter === 'ALL' }]" 
              @click="selectedFilter = 'ALL'"
            >
              전체 유형
            </button>
            <button 
              :class="['filter-btn', { active: selectedFilter === 'TRANSFER' }]" 
              @click="selectedFilter = 'TRANSFER'"
            >
              송금/이체
            </button>
            <button 
              :class="['filter-btn', { active: selectedFilter === 'PAYMENT' }]" 
              @click="selectedFilter = 'PAYMENT'"
            >
              PG 결제/환불
            </button>
          </div>

          <div class="search-keyword-box">
            <Search :size="16" class="search-icon" />
            <input 
              v-model="searchQuery" 
              type="text" 
              placeholder="예금주, 메모 키워드 검색..." 
            />
          </div>
        </div>
      </div>

      <!-- Transactions Table Container -->
      <div v-if="txLoading && currentPage === 0" class="empty-state">
        <RefreshCw :size="32" class="spin-icon" />
        <p>거래 내역을 조회 중입니다...</p>
      </div>

      <div v-else-if="filteredTransactions.length === 0" class="empty-state">
        <Inbox :size="48" class="empty-icon-gray" />
        <h3>조회된 거래 내역이 없습니다.</h3>
        <p>선택하신 조건에 맞는 거래 기록이 존재하지 않습니다.</p>
      </div>

      <div v-else class="bank-card tx-table-card">
        <div class="tx-table-wrapper">
          <table class="tx-table">
            <thead>
              <tr>
                <th>거래 일시</th>
                <th>당사 계좌번호</th>
                <th>거래 구분</th>
                <th>상대방 정보 / 적요</th>
                <th class="text-right">거래 금액</th>
                <th class="text-right">거래 후 잔액</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="tx in filteredTransactions" :key="tx.id">
                <td class="tx-date-cell">{{ tx.createdAt || '-' }}</td>
                <td class="tx-acc-cell">
                  <strong>{{ formatAccountNumber(tx.accountNumber) }}</strong>
                </td>
                <td>
                  <span :class="['tx-type-badge', getTxBadgeClass(tx.type)]">
                    {{ getTxTypeLabel(tx.type) }}
                  </span>
                </td>
                <td>
                  <div class="counterparty-info">
                    <span class="counterparty-name">{{ tx.counterpartyName || tx.memo || '일반' }}</span>
                    <span v-if="tx.counterpartyAccountNumber" class="counterparty-acc">
                      ({{ formatAccountNumber(tx.counterpartyAccountNumber) }})
                    </span>
                  </div>
                </td>
                <td :class="['text-right', 'amount-cell', isPositiveTx(tx.type) ? 'plus' : 'minus']">
                  {{ isPositiveTx(tx.type) ? '+' : '-' }}{{ (tx.amount || 0).toLocaleString() }} 원
                </td>
                <td class="text-right balance-cell">
                  {{ (tx.balanceAfter || 0).toLocaleString() }} 원
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 5. Infinite Scroll Sentinel Loading Bar -->
        <div ref="sentinelRef" class="infinite-scroll-sentinel">
          <div v-if="loadingMore" class="sentinel-loading">
            <RefreshCw :size="18" class="spin-icon" />
            <span>거래 내역을 불러오는 중입니다...</span>
          </div>
          <div v-else-if="isLastPage && filteredTransactions.length > 0" class="sentinel-end">
            <CheckCircle2 :size="16" class="check-mini" />
            <span>마지막 거래 내역입니다. (총 {{ filteredTransactions.length }}건)</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { 
  RefreshCw, 
  Clock, 
  CheckCircle2, 
  XCircle, 
  ArrowLeftRight, 
  Search, 
  Inbox, 
  Calendar, 
  CreditCard 
} from 'lucide-vue-next'

const pendingAccounts = ref<any[]>([])
const allTransactions = ref<any[]>([])

const loading = ref(false)
const txLoading = ref(false)
const loadingMore = ref(false)
const processingId = ref<number | null>(null)

const approvedCount = ref(0)
const rejectedCount = ref(0)

// Filters & Preset Active State
const activePreset = ref<number | 'ALL' | 'CUSTOM'>('ALL')
const startDate = ref('')
const endDate = ref('')
const targetAccountNumber = ref('')
const selectedFilter = ref('ALL')
const searchQuery = ref('')

// Server-side Infinite Scroll Pagination State
const currentPage = ref(0)
const PAGE_SIZE = 20
const isLastPage = ref(false)
const totalElements = ref(0)
const sentinelRef = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

const formatAccountNumber = (numStr?: string) => {
  if (!numStr) return ''
  const str = String(numStr).replace(/-/g, '')
  if (str.length === 13) {
    return `${str.slice(0, 3)}-${str.slice(3, 7)}-${str.slice(7, 11)}-${str.slice(11)}`
  }
  return str
}

const formatDateOnly = (d: Date) => {
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const setPresetDays = (days: number) => {
  activePreset.value = days
  const end = new Date()
  endDate.value = formatDateOnly(end)

  if (days === 0) {
    startDate.value = endDate.value
  } else {
    const start = new Date()
    start.setDate(end.getDate() - days)
    startDate.value = formatDateOnly(start)
  }
}

const clearDates = () => {
  activePreset.value = 'ALL'
  startDate.value = ''
  endDate.value = ''
  targetAccountNumber.value = ''
  searchQuery.value = ''
  selectedFilter.value = 'ALL'
  resetAndFetchTransactions()
}

const onDateInputChange = () => {
  activePreset.value = 'CUSTOM'
}

const resetAndFetchTransactions = async () => {
  currentPage.value = 0
  isLastPage.value = false
  allTransactions.value = []
  await fetchNextPage()
}

const fetchPendingAccounts = async () => {
  loading.value = true
  const token = localStorage.getItem('accessToken')

  try {
    const res = await fetch('/api/v1/admin/accounts/pending', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (res.ok) {
      pendingAccounts.value = await res.json()
    }
  } catch (e) {
    console.error('승인 대기 목록 조회 실패:', e)
  } finally {
    loading.value = false
  }
}

const handleAccountSearch = async () => {
  await resetAndFetchTransactions()
}

const fetchNextPage = async () => {
  if (isLastPage.value || loadingMore.value || (txLoading.value && currentPage.value > 0)) return

  const token = localStorage.getItem('accessToken')
  const cleanNum = targetAccountNumber.value.replace(/-/g, '').trim()

  let url = `/api/v1/admin/transactions?page=${currentPage.value}&size=${PAGE_SIZE}`
  if (cleanNum) {
    url += `&accountNumber=${encodeURIComponent(cleanNum)}`
  }

  if (currentPage.value === 0) {
    txLoading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const res = await fetch(url, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (res.ok) {
      const data = await res.json()
      if (data && Array.isArray(data.content)) {
        if (data.content.length === 0) {
          isLastPage.value = true
        } else {
          allTransactions.value = [...allTransactions.value, ...data.content]
          totalElements.value = data.totalElements || allTransactions.value.length
          isLastPage.value = Boolean(data.isLast) || data.content.length < PAGE_SIZE
          currentPage.value += 1
        }
      } else if (Array.isArray(data)) {
        allTransactions.value = data
        totalElements.value = data.length
        isLastPage.value = true
      } else {
        isLastPage.value = true
      }
    } else {
      isLastPage.value = true
    }
  } catch (e) {
    console.error('거래 내역 페이징 조회 실패:', e)
    isLastPage.value = true
  } finally {
    txLoading.value = false
    loadingMore.value = false
  }
}

const refreshAllData = () => {
  fetchPendingAccounts()
  resetAndFetchTransactions()
}

const handleApprove = async (accountId: number) => {
  if (!confirm('해당 계좌 개설을 승인(ACTIVE)하시겠습니까?')) return
  
  processingId.value = accountId
  const token = localStorage.getItem('accessToken')

  try {
    const res = await fetch(`/api/v1/admin/accounts/${accountId}/approve`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    })

    if (res.ok) {
      alert('계좌 승인이 정상 완료되었습니다.')
      approvedCount.value += 1
      await fetchPendingAccounts()
    } else {
      const err = await res.json()
      alert(`승인 실패: ${err.message || '오류가 발생했습니다.'}`)
    }
  } catch (e) {
    alert('계좌 승인 요청 중 오류가 발생했습니다.')
  } finally {
    processingId.value = null
  }
}

const handleReject = async (accountId: number) => {
  if (!confirm('해당 계좌 개설 요청을 거절(REJECTED)하시겠습니까?')) return

  processingId.value = accountId
  const token = localStorage.getItem('accessToken')

  try {
    const res = await fetch(`/api/v1/admin/accounts/${accountId}/reject`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    })

    if (res.ok) {
      alert('계좌 요청이 거절 처리되었습니다.')
      rejectedCount.value += 1
      await fetchPendingAccounts()
    } else {
      const err = await res.json()
      alert(`거절 실패: ${err.message || '오류가 발생했습니다.'}`)
    }
  } catch (e) {
    alert('계좌 거절 요청 중 오류가 발생했습니다.')
  } finally {
    processingId.value = null
  }
}

const isPositiveTx = (type: string) => {
  return type === 'TRANSFER_IN' || type === 'REFUND' || type === 'DEPOSIT'
}

const getTxTypeLabel = (type: string) => {
  switch (type) {
    case 'TRANSFER_IN': return '송금 입금'
    case 'TRANSFER_OUT': return '송금 출금'
    case 'PAY_OUT': return 'PG 결제'
    case 'REFUND': return 'PG 환불'
    case 'DEPOSIT': return '입금'
    case 'WITHDRAWAL': return '출금'
    default: return type
  }
}

const getTxBadgeClass = (type: string) => {
  switch (type) {
    case 'TRANSFER_IN': return 'badge-green'
    case 'TRANSFER_OUT': return 'badge-blue'
    case 'PAY_OUT': return 'badge-purple'
    case 'REFUND': return 'badge-orange'
    default: return 'badge-gray'
  }
}

const extractDateOnly = (dateTimeStr?: string) => {
  if (!dateTimeStr) return ''
  return String(dateTimeStr).replace('T', ' ').split(' ')[0]
}

// Client-side filtering logic with Date Range & Target Account Filter
const filteredTransactions = computed(() => {
  let list = allTransactions.value

  // 1. Date Range Filter
  if (startDate.value) {
    list = list.filter(t => {
      const txDate = extractDateOnly(t.createdAt)
      return !txDate || txDate >= startDate.value
    })
  }
  if (endDate.value) {
    list = list.filter(t => {
      const txDate = extractDateOnly(t.createdAt)
      return !txDate || txDate <= endDate.value
    })
  }

  // 2. Type Filter
  if (selectedFilter.value === 'TRANSFER') {
    list = list.filter(t => t.type === 'TRANSFER_IN' || t.type === 'TRANSFER_OUT')
  } else if (selectedFilter.value === 'PAYMENT') {
    list = list.filter(t => t.type === 'PAY_OUT' || t.type === 'REFUND')
  }

  // 3. Search Query
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(t => 
      String(t.accountNumber).includes(q) ||
      String(t.counterpartyName || '').toLowerCase().includes(q) ||
      String(t.counterpartyAccountNumber || '').includes(q) ||
      String(t.memo || '').toLowerCase().includes(q)
    )
  }

  return list
})

// Infinite Scroll Observer Setup
const setupObserver = () => {
  if (observer) observer.disconnect()
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting && !isLastPage.value && !loadingMore.value && !txLoading.value) {
        fetchNextPage()
      }
    },
    { rootMargin: '50px' }
  )

  if (sentinelRef.value) {
    observer.observe(sentinelRef.value)
  }
}

onMounted(async () => {
  refreshAllData()
  await nextTick()
  setupObserver()
})
</script>

<style scoped>
.admin-container {
  display: flex;
  flex-direction: column;
  gap: 28px;
  max-width: 1200px;
  margin: 0 auto;
}

.admin-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.admin-badge {
  display: inline-block;
  font-size: 12px;
  font-weight: 800;
  color: var(--toss-blue);
  background: var(--toss-blue-light);
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  margin-bottom: 8px;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  padding: 10px 18px;
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.refresh-btn:hover {
  background: var(--surface-subtle);
  border-color: var(--toss-blue);
  color: var(--toss-blue);
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.stat-icon-box {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.bg-yellow { background: rgba(245, 158, 11, 0.12); color: #d97706; }
.bg-blue { background: var(--toss-blue-light); color: var(--toss-blue); }
.bg-green { background: rgba(16, 185, 129, 0.12); color: #059669; }
.bg-red { background: var(--bank-red-light); color: var(--bank-red); }

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 600;
}

.stat-value {
  font-size: 22px;
  font-weight: 800;
}

.text-yellow { color: #d97706; }
.text-blue { color: var(--toss-blue); }
.text-green { color: #059669; }
.text-red { color: var(--bank-red); }

/* Section Container */
.section-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-header-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.section-title-box {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-primary);
}

.count-badge {
  background: #f59e0b;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
  padding: 2px 10px;
  border-radius: var(--radius-pill);
}

.count-badge.blue {
  background: var(--toss-blue);
}

.section-desc {
  font-size: 13px;
  color: var(--text-secondary);
}

/* Empty State */
.empty-state {
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 48px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
  color: var(--text-secondary);
}

.empty-icon-green { color: #10b981; }
.empty-icon-gray { color: var(--text-tertiary); }

/* Pending List Grid */
.pending-list-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

.pending-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.acc-type-pill {
  font-size: 12px;
  font-weight: 700;
  color: var(--toss-blue);
  background: var(--toss-blue-light);
  padding: 4px 10px;
  border-radius: var(--radius-pill);
}

.status-tag.pending {
  font-size: 12px;
  font-weight: 800;
  color: #d97706;
  background: rgba(245, 158, 11, 0.12);
  padding: 4px 10px;
  border-radius: var(--radius-pill);
}

.card-main-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-primary {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.owner-tag {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}

.acc-number {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.info-secondary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  background: var(--surface-subtle);
  padding: 10px 14px;
  border-radius: var(--radius-md);
}

.balance-label {
  color: var(--text-secondary);
}

.balance-val {
  color: var(--text-primary);
  font-size: 15px;
}

.card-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.approve-btn {
  background: var(--toss-blue);
  color: #ffffff;
}

.approve-btn:hover:not(:disabled) {
  background: #0052cc;
  box-shadow: 0 4px 12px var(--toss-blue-glow);
}

.reject-btn {
  background: var(--bank-red-light);
  color: var(--bank-red);
}

.reject-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.2);
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Safe Filter Card & Controls */
.safe-filter-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-controls-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.control-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
}

.date-input-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.bank-date-input {
  background: var(--surface-subtle);
  border: 1px solid var(--border-light);
  color: var(--text-primary);
  padding: 8px 12px;
  border-radius: var(--radius-md);
  font-size: 13px;
  outline: none;
}

.date-sep {
  font-weight: 700;
  color: var(--text-tertiary);
}

.preset-chips {
  display: flex;
  gap: 6px;
}

.preset-btn {
  background: var(--surface-subtle);
  border: 1px solid var(--border-light);
  color: var(--text-secondary);
  padding: 6px 12px;
  border-radius: var(--radius-pill);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.preset-btn:hover {
  background: var(--toss-blue-light);
  color: var(--toss-blue);
  border-color: var(--toss-blue-light);
}

.preset-btn.active {
  background: var(--toss-blue) !important;
  color: #ffffff !important;
  border-color: var(--toss-blue) !important;
  box-shadow: 0 2px 8px var(--toss-blue-glow);
}

.preset-btn.reset {
  background: var(--surface-subtle);
  color: var(--text-secondary);
}

.preset-btn.reset.active {
  background: var(--toss-gray) !important;
  color: #ffffff !important;
  border-color: var(--toss-gray) !important;
}

.account-search-wrapper {
  display: flex;
  gap: 8px;
}

.bank-account-input {
  flex: 1;
  background: var(--surface-subtle);
  border: 1px solid var(--border-light);
  color: var(--text-primary);
  padding: 10px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  outline: none;
}

.search-submit-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--toss-blue);
  color: #ffffff;
  border: none;
  padding: 10px 16px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.search-submit-btn:hover {
  background: #0052cc;
}

.filter-bar-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
}

.filter-chips {
  display: flex;
  gap: 8px;
}

.filter-btn {
  background: var(--surface-subtle);
  border: 1px solid var(--border-light);
  color: var(--text-secondary);
  padding: 8px 16px;
  border-radius: var(--radius-pill);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-btn.active {
  background: var(--toss-blue);
  color: #ffffff;
  border-color: var(--toss-blue);
}

.search-keyword-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--surface-subtle);
  border: 1px solid var(--border-light);
  padding: 8px 16px;
  border-radius: var(--radius-pill);
  width: 280px;
}

.search-icon {
  color: var(--text-tertiary);
}

.search-keyword-box input {
  border: none;
  background: transparent;
  outline: none;
  font-size: 13px;
  color: var(--text-primary);
  width: 100%;
}

/* Table Card */
.tx-table-card {
  padding: 0;
  overflow: hidden;
}

.tx-table-wrapper {
  overflow-x: auto;
}

.tx-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.tx-table th {
  background: var(--surface-subtle);
  color: var(--text-secondary);
  font-weight: 700;
  text-align: left;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-light);
}

.tx-table td {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
  color: var(--text-primary);
}

.tx-table tr:last-child td {
  border-bottom: none;
}

.text-right {
  text-align: right !important;
}

.tx-date-cell {
  color: var(--text-secondary);
  font-size: 13px;
}

.tx-type-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 800;
  padding: 3px 8px;
  border-radius: var(--radius-pill);
}

.badge-green { background: rgba(16, 185, 129, 0.12); color: #059669; }
.badge-blue { background: var(--toss-blue-light); color: var(--toss-blue); }
.badge-purple { background: rgba(139, 92, 246, 0.12); color: #7c3aed; }
.badge-orange { background: rgba(245, 158, 11, 0.12); color: #d97706; }
.badge-gray { background: var(--surface-subtle); color: var(--text-secondary); }

.counterparty-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.counterparty-acc {
  color: var(--text-tertiary);
  font-size: 12px;
}

.amount-cell {
  font-weight: 800;
}

.amount-cell.plus { color: #059669; }
.amount-cell.minus { color: var(--text-primary); }

.balance-cell {
  color: var(--text-secondary);
  font-weight: 600;
}

/* Infinite Scroll Sentinel Styles */
.infinite-scroll-sentinel {
  padding: 20px;
  text-align: center;
  background: var(--surface-subtle);
  border-top: 1px solid var(--border-light);
}

.sentinel-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 700;
  color: var(--toss-blue);
}

.sentinel-end {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-tertiary);
}

.check-mini {
  color: #10b981;
}
</style>
