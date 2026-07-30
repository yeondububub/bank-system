<template>
  <div class="admin-dashboard">
    <!-- 어드민 헤더 타이틀 -->
    <div class="admin-header">
      <div class="header-title-box">
        <span class="admin-badge">ADMIN CENTER</span>
        <h1 class="bank-title">관리자 전용 계좌 심사 센터</h1>
        <p class="bank-subtitle">신규 개설 요청된 계좌를 심사하고 승인 또는 거절 처리합니다.</p>
      </div>

      <button class="refresh-btn" @click="fetchPendingAccounts" :disabled="loading">
        <RefreshCw :size="16" :class="{ 'spin-icon': loading }" />
        <span>목록 갱신</span>
      </button>
    </div>

    <!-- 요약 통계 카드 그리드 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon-box bg-yellow">
          <Clock :size="24" />
        </div>
        <div class="stat-info">
          <span class="stat-label">승인 대기 건수</span>
          <span class="stat-value text-yellow">{{ pendingAccounts.length }} 건</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-box bg-green">
          <CheckCircle2 :size="24" />
        </div>
        <div class="stat-info">
          <span class="stat-label">오늘 승인 처리</span>
          <span class="stat-value text-green">{{ approvedCount }} 건</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-box bg-red">
          <XCircle :size="24" />
        </div>
        <div class="stat-info">
          <span class="stat-label">오늘 거절 처리</span>
          <span class="stat-value text-red">{{ rejectedCount }} 건</span>
        </div>
      </div>
    </div>

    <!-- 승인 대기 계좌 목록 섹션 -->
    <div class="pending-section bank-card">
      <div class="section-top">
        <h3 class="section-title">승인 대기 계좌 목록 ({{ pendingAccounts.length }})</h3>
        <span class="section-desc">계좌 상태가 PENDING인 건에 대해 승인 처리 시 거래가 즉시 활성화(ACTIVE)됩니다.</span>
      </div>

      <!-- 로딩 중 -->
      <div v-if="loading" class="empty-box">
        <RefreshCw :size="32" class="spin-icon" />
        <p>승인 대기 목록을 불러오는 중입니다...</p>
      </div>

      <!-- 데이터 없음 -->
      <div v-else-if="pendingAccounts.length === 0" class="empty-box">
        <CheckCircle2 :size="40" class="check-icon" />
        <h4>현재 심사 대기 중인 계좌가 없습니다.</h4>
        <p>모든 신규 계좌 개설 요청이 처리되었습니다.</p>
      </div>

      <!-- 승인 대기 목록 테이블/카드 -->
      <div v-else class="pending-list">
        <div v-for="acc in pendingAccounts" :key="acc.id" class="pending-card">
          <div class="card-info">
            <div class="info-primary">
              <span class="owner-tag">소유자 ID: {{ acc.ownerId }}</span>
              <span class="acc-number">{{ formatAccountNumber(acc.accountNumber) }}</span>
            </div>
            <div class="info-secondary">
              <span class="balance-label">초기 신청 잔액:</span>
              <span class="balance-val">{{ acc.balance.toLocaleString() }} 원</span>
              <span class="status-tag pending">PENDING</span>
            </div>
          </div>

          <div class="card-actions">
            <button 
              class="action-btn approve-btn" 
              @click="handleApprove(acc.id)"
              :disabled="processingId === acc.id"
            >
              <CheckCircle2 :size="16" />
              <span>{{ processingId === acc.id ? '처리 중...' : '계좌 승인 (ACTIVE)' }}</span>
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

const formatAccountNumber = (numStr: string) => {
  if (!numStr) return ''
  const str = String(numStr)
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
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    if (res.ok) {
      pendingAccounts.value = await res.json()
    } else {
      console.error('승인 대기 목록 조회 실패:', res.status)
    }
  } catch (e) {
    console.error('네트워크 오류:', e)
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
      headers: {
        'Authorization': `Bearer ${token}`
      }
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
      headers: {
        'Authorization': `Bearer ${token}`
      }
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
.admin-dashboard {
  padding-top: 10px;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 28px;
}

.admin-badge {
  font-size: 11px;
  font-weight: 800;
  color: #ef4444;
  background: rgba(239, 68, 68, 0.15);
  padding: 4px 10px;
  border-radius: 20px;
  letter-spacing: 0.8px;
  display: inline-block;
  margin-bottom: 8px;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #141b26;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #94a3b8;
  padding: 10px 16px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn:hover {
  color: #ffffff;
  background: #1e2638;
}

/* 요약 통계 카드 그리드 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  background: #141b26;
  border-radius: 20px;
  padding: 22px 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  gap: 18px;
}

.stat-icon-box {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bg-yellow { background: rgba(245, 158, 11, 0.15); color: #fbbf24; }
.bg-green { background: rgba(16, 185, 129, 0.15); color: #34d399; }
.bg-red { background: rgba(239, 68, 68, 0.15); color: #f87171; }

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
}

.text-yellow { color: #fbbf24; }
.text-green { color: #34d399; }
.text-red { color: #f87171; }

/* 승인 대기 섹션 */
.section-top {
  margin-bottom: 24px;
}

.section-title {
  font-size: 20px;
  font-weight: 800;
  color: #ffffff;
  margin-bottom: 4px;
}

.section-desc {
  font-size: 13px;
  color: #64748b;
}

.empty-box {
  padding: 60px 20px;
  text-align: center;
  color: #64748b;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.check-icon {
  color: #34d399;
}

.pending-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pending-card {
  background: #1e2638;
  border-radius: 18px;
  padding: 20px 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

@media (max-width: 768px) {
  .pending-card {
    flex-direction: column;
    align-items: flex-start;
  }
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-primary {
  display: flex;
  align-items: center;
  gap: 12px;
}

.owner-tag {
  font-size: 12px;
  color: #94a3b8;
  background: rgba(255, 255, 255, 0.06);
  padding: 3px 8px;
  border-radius: 6px;
}

.acc-number {
  font-size: 18px;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: 0.5px;
}

.info-secondary {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.balance-label {
  color: #64748b;
}

.balance-val {
  color: #3182f6;
  font-weight: 700;
}

.status-tag {
  font-size: 10px;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 6px;
}

.status-tag.pending {
  background: rgba(245, 158, 11, 0.2);
  color: #fbbf24;
}

.card-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 12px 18px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.approve-btn {
  background: #3182f6;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(49, 130, 246, 0.3);
}

.approve-btn:hover {
  background: #1b64da;
}

.reject-btn {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.reject-btn:hover {
  background: rgba(239, 68, 68, 0.3);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spin-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  100% { transform: rotate(360deg); }
}
</style>
