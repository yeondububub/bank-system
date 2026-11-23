<template>
  <div class="admin-container">
    <!-- 1. 상단 요약 카드 -->
    <section class="bank-card summary-card" aria-label="계좌 심사 현황">
      <div class="summary-top">
        <div>
          <h1 class="page-title">계좌 개설 승인</h1>
          <p class="page-subtitle">신규 개설 신청된 계좌를 확인하고 승인 또는 거절할 수 있어요.</p>
        </div>

        <button
          type="button"
          class="bank-btn bank-btn-subtle refresh-btn"
          @click="fetchPendingAccounts"
          :disabled="loading"
        >
          <RefreshCw :size="15" :class="{ 'spin-icon': loading }" />
          <span>새로고침</span>
        </button>
      </div>

      <dl class="metrics-row">
        <div class="metric-item">
          <dt>승인 대기</dt>
          <dd class="num text-blue">{{ pendingAccounts.length }}건</dd>
        </div>
        <div class="metric-item">
          <dt>이번 세션 승인</dt>
          <dd class="num text-green">{{ approvedCount }}건</dd>
        </div>
        <div class="metric-item">
          <dt>이번 세션 거절</dt>
          <dd class="num text-red">{{ rejectedCount }}건</dd>
        </div>
      </dl>
    </section>

    <!-- 2. 승인 대기 목록 카드 -->
    <section class="bank-card list-card" aria-label="승인 대기 계좌 목록">
      <div class="list-head">
        <h2 class="section-title">승인 대기 목록</h2>
        <span class="count-chip num">{{ pendingAccounts.length }}건</span>
      </div>

      <div v-if="loading" class="state-box">
        <RefreshCw :size="24" class="spin-icon" />
        <span>승인 대기 계좌를 불러오는 중입니다…</span>
      </div>

      <div v-else-if="pendingAccounts.length === 0" class="state-box">
        <CheckCircle2 :size="32" class="check-empty-icon" />
        <p class="empty-title">대기 중인 계좌가 없어요</p>
        <p class="empty-sub">모든 신규 계좌 개설 신청이 처리되었습니다.</p>
      </div>

      <ul v-else class="pending-list">
        <li v-for="acc in pendingAccounts" :key="acc.id" class="pending-row">
          <div class="pending-left">
            <div class="pending-title-line">
              <span class="acc-number num">{{ formatAccountNumber(acc.accountNumber) }}</span>
              <span class="pending-tag">승인 대기</span>
            </div>

            <div class="pending-meta-line num">
              <span>소유자 ID {{ acc.ownerId }}</span>
              <span>·</span>
              <span>초기 잔액 {{ acc.balance.toLocaleString() }}원</span>
            </div>
          </div>

          <div class="pending-actions">
            <button
              type="button"
              class="bank-btn reject-btn"
              @click="handleReject(acc.id)"
              :disabled="processingId === acc.id"
            >
              {{ processingId === acc.id ? '처리 중…' : '거절' }}
            </button>

            <button
              type="button"
              class="bank-btn bank-btn-primary approve-btn"
              @click="handleApprove(acc.id)"
              :disabled="processingId === acc.id"
            >
              {{ processingId === acc.id ? '처리 중…' : '승인' }}
            </button>
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RefreshCw, CheckCircle2 } from 'lucide-vue-next'

const pendingAccounts = ref<any[]>([])
const loading = ref(false)
const processingId = ref<number | null>(null)
const approvedCount = ref(0)
const rejectedCount = ref(0)

const formatAccountNumber = (numStr?: string) => {
  if (!numStr) return ''
  const str = String(numStr).replace(/-/g, '')
  if (str.length === 13) {
    return `${str.slice(0, 3)}-${str.slice(3, 7)}-${str.slice(7, 11)}-${str.slice(11)}`
  }
  return str
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
      alert('계좌 승인이 완료되었습니다.')
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

onMounted(() => {
  fetchPendingAccounts()
})
</script>

<style scoped>
.admin-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 860px;
  margin-inline: auto;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.summary-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

.refresh-btn {
  padding: 10px 16px;
  font-size: 14px;
}

.metrics-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 18px 20px;
  border-radius: var(--radius-lg);
  background-color: var(--surface-subtle);
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-item dt {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.metric-item dd {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: var(--text-primary);
}

.text-blue {
  color: var(--toss-blue);
}

.text-green {
  color: var(--bank-green);
}

.text-red {
  color: var(--bank-red);
}

/* 승인 대기 목록 */
.list-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.list-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
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

.pending-list {
  list-style: none;
  display: flex;
  flex-direction: column;
}

.pending-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 8px;
  border-radius: var(--radius-md);
  transition: background-color 0.15s ease;
}

.pending-row:hover {
  background-color: var(--surface-subtle);
}

.pending-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pending-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.acc-number {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
}

.pending-tag {
  font-size: 11px;
  font-weight: 700;
  color: #B45309;
  background-color: var(--bank-yellow-light);
  padding: 2px 7px;
  border-radius: 6px;
}

.pending-meta-line {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.pending-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.approve-btn,
.reject-btn {
  padding: 9px 16px;
  font-size: 13px;
  border-radius: var(--radius-sm);
}

.reject-btn {
  background-color: var(--bank-red-light);
  color: var(--bank-red);
}

.reject-btn:hover:not(:disabled) {
  background-color: #FFD6DB;
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

.check-empty-icon {
  color: var(--toss-blue);
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
  .metrics-row {
    grid-template-columns: 1fr;
  }

  .pending-row {
    flex-direction: column;
    align-items: stretch;
  }

  .pending-actions {
    justify-content: flex-end;
  }
}
</style>
