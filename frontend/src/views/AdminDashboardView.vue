<template>
  <div class="admin-container">
    <!-- 1. Admin Top Header -->
    <div class="admin-header">
      <div>
        <span class="admin-badge">관리자 심사 센터</span>
        <h1 class="page-title">계좌 개설 승인 관리</h1>
        <p class="page-subtitle">신규 개설 요청된 13자리 계좌를 심사하고 승인 또는 거절 처리합니다.</p>
      </div>

      <button class="refresh-btn" @click="fetchPendingAccounts" :disabled="loading">
        <RefreshCw :size="16" :class="{ 'spin-icon': loading }" />
        <span>목록 갱신</span>
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

    <!-- 3. Pending Accounts Approval Section -->
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RefreshCw, Clock, CheckCircle2, XCircle } from 'lucide-vue-next'

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

onMounted(() => {
  fetchPendingAccounts()
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

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
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

.section-desc {
  font-size: 13px;
  color: var(--text-secondary);
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

.empty-icon-green { color: #10b981; }

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

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
