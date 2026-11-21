<template>
  <div class="admin-container">
    <!-- 1. Admin Top Header -->
    <div class="admin-header">
      <div>
        <span class="admin-badge">관리자 거래 감사</span>
        <h1 class="page-title">전체 계좌 거래 내역</h1>
        <p class="page-subtitle">지정된 기간 및 계좌 조건으로 전체 고객의 입출금, 송금, 결제 내역을 모니터링합니다.</p>
      </div>

      <button class="refresh-btn" @click="resetAndFetchTransactions" :disabled="txLoading">
        <RefreshCw :size="16" :class="{ 'spin-icon': txLoading }" />
        <span>내역 갱신</span>
      </button>
    </div>

    <!-- 2. Summary Stat -->
    <div class="section-header-row">
      <div class="section-title-box">
        <h2 class="section-title">통합 거래 원장</h2>
        <span class="count-badge blue">{{ filteredTransactions.length }}</span>
      </div>
      <p class="section-desc">서버 페이징 기반 무한 스크롤로 최신 거래 내역을 순차 조회합니다.</p>
    </div>

    <!-- 3. Advanced Filter & Date Range Card -->
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

    <!-- 4. Transactions Table Container -->
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

      <!-- Infinite Scroll Sentinel Loading Bar -->
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
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import {
  RefreshCw,
  CheckCircle2,
  Search,
  Inbox,
  Calendar,
  CreditCard
} from 'lucide-vue-next'

const allTransactions = ref<any[]>([])
const txLoading = ref(false)
const loadingMore = ref(false)

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

const isPositiveTx = (type: string) => {
  return type === 'TRANSFER_IN' || type === 'REFUND' || type === 'DEPOSIT'
}

const getTxTypeLabel = (type: string) => {
  switch (type) {
    case 'TRANSFER_IN': return '송금 입금'
    case 'TRANSFER_OUT': return '송금 출금'
    case 'PAY_OUT':
    case 'PAYMENT': return 'PG 결제'
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
    case 'PAY_OUT':
    case 'PAYMENT': return 'badge-purple'
    case 'REFUND': return 'badge-orange'
    default: return 'badge-gray'
  }
}

const extractDateOnly = (dateTimeStr?: string) => {
  if (!dateTimeStr) return ''
  return String(dateTimeStr).replace('T', ' ').split(' ')[0]
}

const filteredTransactions = computed(() => {
  let list = allTransactions.value

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

  if (selectedFilter.value === 'TRANSFER') {
    list = list.filter(t => t.type === 'TRANSFER_IN' || t.type === 'TRANSFER_OUT')
  } else if (selectedFilter.value === 'PAYMENT') {
    list = list.filter(t => t.type === 'PAY_OUT' || t.type === 'PAYMENT' || t.type === 'REFUND')
  }

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
  await resetAndFetchTransactions()
  await nextTick()
  setupObserver()
})

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
})
</script>

<style scoped>
.admin-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
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
  background: var(--surface-white);
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

.spin-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  100% { transform: rotate(360deg); }
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

.count-badge.blue {
  background: var(--toss-blue);
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
  padding: 2px 10px;
  border-radius: var(--radius-pill);
}

.section-desc {
  font-size: 13px;
  color: var(--text-secondary);
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

/* Empty State */
.empty-state {
  background: var(--surface-white);
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

.empty-icon-gray { color: var(--text-tertiary); }

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

@media (max-width: 900px) {
  .filter-controls-grid {
    grid-template-columns: 1fr;
  }

  .filter-bar-bottom {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .search-keyword-box {
    width: 100%;
  }
}
</style>
