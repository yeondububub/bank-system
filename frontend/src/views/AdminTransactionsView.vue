<template>
  <div class="admin-tx-container">
    <!-- 1. 상단 헤더 & 필터 카드 -->
    <section class="bank-card filter-card" aria-label="거래 내역 검색 및 필터">
      <div class="head-row">
        <div>
          <div class="title-with-count">
            <h1 class="page-title">전체 거래 내역</h1>
            <span class="count-chip num">{{ filteredTransactions.length }}건</span>
          </div>
          <p class="page-subtitle">기간, 계좌번호, 거래 유형별로 전체 원장 기록을 조회합니다.</p>
        </div>

        <button
          type="button"
          class="bank-btn bank-btn-subtle refresh-btn"
          @click="resetAndFetchTransactions"
          :disabled="txLoading"
        >
          <RefreshCw :size="15" :class="{ 'spin-icon': txLoading }" />
          <span>새로고침</span>
        </button>
      </div>

      <div class="filter-grid">
        <!-- 기간 필터 -->
        <div class="control-group">
          <label class="control-label">조회 기간</label>
          <div class="date-row">
            <input type="date" v-model="startDate" class="bank-input date-input num" @change="onDateInputChange" />
            <span class="date-sep">~</span>
            <input type="date" v-model="endDate" class="bank-input date-input num" @change="onDateInputChange" />

            <div class="preset-group">
              <button
                type="button"
                :class="['preset-chip', { active: activePreset === 0 }]"
                @click="setPresetDays(0)"
              >오늘</button>
              <button
                type="button"
                :class="['preset-chip', { active: activePreset === 7 }]"
                @click="setPresetDays(7)"
              >1주일</button>
              <button
                type="button"
                :class="['preset-chip', { active: activePreset === 30 }]"
                @click="setPresetDays(30)"
              >1개월</button>
              <button
                type="button"
                :class="['preset-chip', { active: activePreset === 90 }]"
                @click="setPresetDays(90)"
              >3개월</button>
              <button
                type="button"
                :class="['preset-chip', { active: activePreset === 'ALL' }]"
                @click="clearDates"
              >전체</button>
            </div>
          </div>
        </div>

        <!-- 계좌번호 검색 -->
        <div class="control-group">
          <label class="control-label">계좌번호 조회</label>
          <div class="account-search-row">
            <input
              v-model="targetAccountNumber"
              type="text"
              placeholder="13자리 계좌번호 입력"
              class="bank-input num"
              @keyup.enter="handleAccountSearch"
            />
            <button type="button" class="bank-btn bank-btn-soft search-btn" @click="handleAccountSearch">
              조회
            </button>
          </div>
        </div>
      </div>

      <div class="filter-bottom-row">
        <div class="tab-group" role="group" aria-label="거래 유형 필터">
          <button
            type="button"
            :class="['tab-chip', { active: selectedFilter === 'ALL' }]"
            @click="selectedFilter = 'ALL'"
          >
            전체
          </button>
          <button
            type="button"
            :class="['tab-chip', { active: selectedFilter === 'TRANSFER' }]"
            @click="selectedFilter = 'TRANSFER'"
          >
            송금·이체
          </button>
          <button
            type="button"
            :class="['tab-chip', { active: selectedFilter === 'PAYMENT' }]"
            @click="selectedFilter = 'PAYMENT'"
          >
            PG 결제·환불
          </button>
        </div>

        <div class="keyword-box">
          <Search :size="15" class="search-icon" />
          <input
            v-model="searchQuery"
            type="text"
            placeholder="예금주, 메모 검색"
          />
        </div>
      </div>
    </section>

    <!-- 2. 거래 내역 테이블 카드 -->
    <section class="bank-card table-card" aria-label="통합 거래 원장">
      <div v-if="txLoading && currentPage === 0" class="state-box">
        <RefreshCw :size="24" class="spin-icon" />
        <span>거래 내역을 불러오는 중입니다…</span>
      </div>

      <div v-else-if="filteredTransactions.length === 0" class="state-box">
        <Inbox :size="32" class="muted-icon" />
        <p class="empty-title">조회된 거래 내역이 없어요</p>
        <p class="empty-sub">선택한 기간이나 검색 조건에 맞는 거래 기록이 없습니다.</p>
      </div>

      <template v-else>
        <div class="tx-table-wrapper">
          <table class="tx-table">
            <thead>
              <tr>
                <th>일시</th>
                <th>계좌번호</th>
                <th>구분</th>
                <th>상대방 · 메모</th>
                <th class="text-right">거래 금액</th>
                <th class="text-right">거래 후 잔액</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="tx in filteredTransactions" :key="tx.id">
                <td class="tx-date-cell num">{{ tx.createdAt || '-' }}</td>
                <td class="tx-acc-cell num">{{ formatAccountNumber(tx.accountNumber) }}</td>
                <td>
                  <span :class="['tx-type-badge', getTxBadgeClass(tx.type)]">
                    {{ getTxTypeLabel(tx.type) }}
                  </span>
                </td>
                <td>
                  <div class="counterparty-info">
                    <span class="counterparty-name">{{ tx.counterpartyName || tx.memo || '일반' }}</span>
                    <span v-if="tx.counterpartyAccountNumber" class="counterparty-acc num">
                      {{ formatAccountNumber(tx.counterpartyAccountNumber) }}
                    </span>
                  </div>
                </td>
                <td :class="['text-right', 'amount-cell', 'num', isPositiveTx(tx.type) ? 'plus' : 'minus']">
                  {{ isPositiveTx(tx.type) ? '+' : '-' }}{{ (tx.amount || 0).toLocaleString() }}원
                </td>
                <td class="text-right balance-cell num">
                  {{ (tx.balanceAfter || 0).toLocaleString() }}원
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div ref="sentinelRef" class="infinite-scroll-sentinel">
          <div v-if="loadingMore" class="sentinel-loading">
            <RefreshCw :size="16" class="spin-icon" />
            <span>거래 내역을 더 불러오는 중입니다…</span>
          </div>
          <div v-else-if="isLastPage && filteredTransactions.length > 0" class="sentinel-end">
            <CheckCircle2 :size="15" class="check-mini" />
            <span>마지막 내역입니다 (총 {{ filteredTransactions.length }}건)</span>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import {
  RefreshCw,
  CheckCircle2,
  Search,
  Inbox
} from 'lucide-vue-next'

const allTransactions = ref<any[]>([])
const txLoading = ref(false)
const loadingMore = ref(false)

const activePreset = ref<number | 'ALL' | 'CUSTOM'>('ALL')
const startDate = ref('')
const endDate = ref('')
const targetAccountNumber = ref('')
const selectedFilter = ref('ALL')
const searchQuery = ref('')

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
    case 'TRANSFER_IN': return '입금'
    case 'TRANSFER_OUT': return '송금'
    case 'PAY_OUT':
    case 'PAYMENT': return '결제'
    case 'REFUND': return '환불'
    case 'DEPOSIT': return '입금'
    case 'WITHDRAWAL': return '출금'
    default: return type
  }
}

const getTxBadgeClass = (type: string) => {
  switch (type) {
    case 'TRANSFER_IN': return 'badge-blue'
    case 'TRANSFER_OUT':
    case 'PAY_OUT':
    case 'PAYMENT': return 'badge-gray'
    case 'REFUND': return 'badge-green'
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
.admin-tx-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1080px;
  margin-inline: auto;
}

.filter-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.title-with-count {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--text-primary);
}

.count-chip {
  font-size: 12px;
  font-weight: 700;
  color: var(--toss-blue-hover);
  background-color: var(--toss-blue-light);
  padding: 3px 9px;
  border-radius: var(--radius-pill);
}

.page-subtitle {
  margin-top: 4px;
  font-size: 14px;
  color: var(--text-secondary);
}

.refresh-btn {
  padding: 10px 16px;
  font-size: 14px;
}

.filter-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 20px;
  align-items: end;
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.control-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.date-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.date-input {
  width: auto;
  padding: 10px 12px;
  font-size: 13px;
}

.date-sep {
  color: var(--text-tertiary);
  font-weight: 600;
}

.preset-group {
  display: flex;
  background-color: var(--bg-color);
  padding: 3px;
  border-radius: var(--radius-sm);
}

.preset-chip {
  padding: 6px 11px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.preset-chip.active {
  background-color: var(--surface-white);
  color: var(--text-primary);
  font-weight: 700;
  box-shadow: var(--shadow-xs);
}

.account-search-row {
  display: flex;
  gap: 8px;
}

.account-search-row .bank-input {
  padding: 10px 14px;
  font-size: 14px;
}

.search-btn {
  padding: 10px 18px;
  font-size: 14px;
}

.filter-bottom-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding-top: 16px;
  border-top: 1px solid var(--border-subtle);
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

.keyword-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: var(--surface-subtle);
  padding: 9px 14px;
  border-radius: var(--radius-md);
  width: 260px;
}

.search-icon {
  color: var(--text-tertiary);
}

.keyword-box input {
  border: none;
  background: transparent;
  outline: none;
  font-size: 13px;
  color: var(--text-primary);
  width: 100%;
}

/* 테이블 */
.table-card {
  padding: 8px 0 0;
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
  color: var(--text-tertiary);
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  padding: 12px 24px;
  border-bottom: 1px solid var(--border-subtle);
  white-space: nowrap;
}

.tx-table td {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-subtle);
  color: var(--text-primary);
}

.tx-table tbody tr:hover {
  background-color: var(--surface-subtle);
}

.tx-table tr:last-child td {
  border-bottom: none;
}

.text-right {
  text-align: right;
  white-space: nowrap;
}

.tx-date-cell {
  color: var(--text-tertiary);
  font-size: 13px;
  white-space: nowrap;
}

.tx-acc-cell {
  font-weight: 600;
  color: var(--text-secondary);
  white-space: nowrap;
}

.tx-type-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
}

.badge-blue {
  background-color: var(--toss-blue-light);
  color: var(--toss-blue-hover);
}

.badge-green {
  background-color: var(--bank-green-light);
  color: #028A53;
}

.badge-gray {
  background-color: var(--bg-color);
  color: var(--text-secondary);
}

.counterparty-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.counterparty-name {
  font-weight: 600;
}

.counterparty-acc {
  color: var(--text-tertiary);
  font-size: 12px;
}

.amount-cell {
  font-weight: 700;
}

.amount-cell.plus {
  color: var(--toss-blue);
}

.amount-cell.minus {
  color: var(--text-primary);
}

.balance-cell {
  color: var(--text-tertiary);
  font-weight: 500;
}

.infinite-scroll-sentinel {
  padding: 18px;
  text-align: center;
  border-top: 1px solid var(--border-subtle);
}

.sentinel-loading,
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
  color: var(--toss-blue);
}

.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 64px 16px;
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

@media (max-width: 900px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .filter-bottom-row {
    flex-direction: column;
    align-items: stretch;
  }

  .keyword-box {
    width: 100%;
  }
}
</style>
