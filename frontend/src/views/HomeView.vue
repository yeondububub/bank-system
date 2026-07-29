<template>
  <div class="dashboard-view">
    <!-- 대시보드 메인 레이아웃 (7:3 2단 구조) -->
    <div class="dashboard-grid">
      
      <!-- 좌측 메인 영역 (70%) -->
      <div class="main-content">
        <!-- 1. 히어로 뱅킹 카드 섹션 -->
        <div class="hero-card-container">
          <div class="hero-card">
            <div class="hero-left">
              <div class="account-tag-row">
                <span class="account-type-tag">입출금 통장</span>
                <span v-if="account" :class="['status-chip', account.status?.toLowerCase()]">
                  {{ account.status === 'PENDING' ? '승인 대기' : '활성화' }}
                </span>
              </div>

              <h2 class="hero-user-title">{{ userName }}님의 메인 계좌</h2>
              
              <div v-if="account" class="hero-account-num">
                <span>{{ formattedAccountNumber }}</span>
                <button class="copy-btn" @click="copyAccountNumber">
                  <Copy :size="12" />
                  <span>{{ copied ? '복사됨' : '복사' }}</span>
                </button>
              </div>

              <div class="hero-amount-wrapper">
                <div v-if="loading" class="bank-amount loading-text">잔액 조회 중...</div>
                
                <!-- 계좌 미존재 시 개설 신청 카드 -->
                <div v-else-if="!account" class="no-account-action">
                  <p class="no-account-desc">아직 개설된 계좌가 없습니다. 메인 13자리 계좌를 신청해보세요.</p>
                  <button class="bank-btn bank-btn-primary create-btn" @click="handleCreateAccount" :disabled="creatingAccount">
                    <span v-if="creatingAccount">계좌 생성 중...</span>
                    <span v-else>13자리 계좌 개설 신청하기</span>
                  </button>
                </div>

                <!-- 승인 대기 (PENDING) -->
                <div v-else-if="account.status === 'PENDING'" class="pending-notice-box">
                  <div class="bank-amount dimmed">{{ formattedBalance }} 원</div>
                  <div class="pending-banner">
                    <ShieldCheck :size="18" class="info-icon" />
                    <span>현재 관리자 승인 대기 중입니다. 승인 처리 후 입출금 및 결제가 활성화됩니다.</span>
                  </div>
                </div>

                <!-- 활성화 (ACTIVE) -->
                <div v-else class="bank-amount bold-amount">{{ formattedBalance }} 원</div>
              </div>

              <div v-if="account && account.status === 'ACTIVE'" class="hero-actions">
                <button class="bank-btn bank-btn-subtle" @click="handleDeposit" :disabled="loading">
                  <ArrowDownLeft :size="18" /> 입금 (테스트)
                </button>
                <button class="bank-btn bank-btn-primary" @click="$router.push('/payment')">
                  <Send :size="18" /> 결제하기
                </button>
              </div>
            </div>

            <!-- 우측 3D 실물 카드 비주얼 그래픽 -->
            <div class="hero-right">
              <div class="bank-card-visual">
                <div class="card-chip"></div>
                <div class="card-logo">BANK SYSTEM</div>
                <div class="card-holder">{{ userName }}</div>
                <div class="card-brand-tag">EVERYDAY COMMUTERS</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 2. 스마트 뱅킹 서비스 (4열 3D 카드 그리드) -->
        <div class="section-header">
          <h3 class="section-title">스마트 뱅킹 서비스</h3>
          <span class="section-sub">안전하고 신속하게 관리하는 뱅크 시스템 기능</span>
        </div>

        <div class="services-grid">
          <!-- 카드 1: 결제 승인 -->
          <router-link to="/payment" class="service-card">
            <div class="card-thumb bg-gradient-blue">
              <div class="thumb-icon-box">
                <CreditCard :size="28" />
              </div>
              <span class="thumb-tag">PAYMENT</span>
            </div>
            <div class="card-body">
              <h4 class="card-name">결제 승인</h4>
              <p class="card-desc">안전한 멱등성 헤더 및 동시성 제어 결제</p>
            </div>
          </router-link>

          <!-- 카드 2: 이력 조회 -->
          <router-link to="/history" class="service-card">
            <div class="card-thumb bg-gradient-green">
              <div class="thumb-icon-box">
                <Receipt :size="28" />
              </div>
              <span class="thumb-tag">HISTORY</span>
            </div>
            <div class="card-body">
              <h4 class="card-name">거래 이력</h4>
              <p class="card-desc">실시간 입출금 및 결제 내역 타임라인</p>
            </div>
          </router-link>

          <!-- 카드 3: 잔액 갱신 -->
          <div class="service-card" @click="fetchAccountInfo">
            <div class="card-thumb bg-gradient-purple">
              <div class="thumb-icon-box" :class="{ 'spin-icon': loading }">
                <RefreshCw :size="28" />
              </div>
              <span class="thumb-tag">REFRESH</span>
            </div>
            <div class="card-body">
              <h4 class="card-name">잔액 갱신</h4>
              <p class="card-desc">실시간 통장 잔액 상태 최신화</p>
            </div>
          </div>

          <!-- 카드 4: 관리자 승인 센터 -->
          <div class="service-card" @click="handleAdminClick">
            <div class="card-thumb bg-gradient-pink">
              <div class="thumb-icon-box">
                <ShieldCheck :size="28" />
              </div>
              <span class="thumb-tag">ADMIN</span>
            </div>
            <div class="card-body">
              <h4 class="card-name">관리자 센터</h4>
              <p class="card-desc">승인 대기 계좌 심사 및 승인/거절</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 우측 사이드바 영역 (30%) -->
      <div class="sidebar-content">
        <!-- 관심 계좌 & 서비스 현황 위젯 -->
        <div class="sidebar-panel">
          <div class="panel-header">
            <h4 class="panel-title">내 계좌 상태 요약</h4>
            <button class="refresh-mini-btn" @click="fetchAccountInfo">갱신</button>
          </div>

          <div class="info-list">
            <div class="info-item">
              <span class="info-label">회원 이름</span>
              <span class="info-value">{{ userName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">회원 권한</span>
              <span :class="['role-badge-mini', userRole?.toLowerCase()]">{{ userRole }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">계좌 보유 여부</span>
              <span class="info-value">{{ account ? '보유중' : '미보유' }}</span>
            </div>
            <div v-if="account" class="info-item">
              <span class="info-label">계좌 상태</span>
              <span :class="['status-badge-mini', account.status?.toLowerCase()]">
                {{ account.status }}
              </span>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ArrowDownLeft, Send, CreditCard, Receipt, RefreshCw, ShieldCheck, Copy } from 'lucide-vue-next'

const userName = ref('사용자')
const userRole = ref('USER')
const userId = ref<number | null>(null)

const account = ref<any>(null)
const loading = ref(false)
const creatingAccount = ref(false)
const copied = ref(false)

const formattedBalance = computed(() => {
  return (account.value?.balance || 0).toLocaleString('ko-KR')
})

const formattedAccountNumber = computed(() => {
  if (!account.value?.accountNumber) return ''
  const num = String(account.value.accountNumber)
  if (num.length === 13) {
    return `${num.slice(0, 3)}-${num.slice(3, 7)}-${num.slice(7, 11)}-${num.slice(11)}`
  }
  return num
})

const checkUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      userName.value = user.name
      userRole.value = user.role || 'USER'
      userId.value = user.id
    } catch (e) {
      console.error(e)
    }
  }
}

const fetchAccountInfo = async () => {
  if (!userId.value) return
  loading.value = true

  try {
    const response = await fetch(`/api/v1/accounts/${userId.value}`)
    if (response.ok) {
      account.value = await response.json()
    } else if (response.status === 404) {
      account.value = null
    }
  } catch (error: any) {
    console.error('계좌 조회 중 오류:', error)
  } finally {
    loading.value = false
  }
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
      const data = await res.json()
      account.value = data
      alert('계좌 개설 신청이 완료되었습니다! (관리자 승인 대기)')
    } else {
      alert('계좌 개설 신청에 실패했습니다.')
    }
  } catch (e) {
    alert('계좌 개설 신청 중 오류가 발생했습니다.')
  } finally {
    creatingAccount.value = false
  }
}

const handleDeposit = () => {
  if (account.value) {
    account.value.balance += 100000
  }
}

const copyAccountNumber = () => {
  if (account.value?.accountNumber) {
    navigator.clipboard.writeText(account.value.accountNumber)
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  }
}

const handleAdminClick = () => {
  if (userRole.value !== 'ADMIN') {
    alert('⚠️ 관리자 전용 기능입니다. ADMIN 권한 계정으로 로그인해 주세요.')
  } else {
    alert('관리자 전용 계좌 승인 센터로 이동합니다.')
  }
}

onMounted(() => {
  checkUser()
  fetchAccountInfo()
})
</script>

<style scoped>
.dashboard-view {
  padding-top: 10px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 32px;
}

@media (max-width: 1024px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

/* 히어로 뱅킹 카드 */
.hero-card-container {
  margin-bottom: 36px;
}

.hero-card {
  background: linear-gradient(135deg, #151c28 0%, #1e293b 100%);
  border-radius: 28px;
  padding: 36px 40px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.hero-left {
  flex: 1;
  z-index: 2;
}

.account-tag-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.account-type-tag {
  font-size: 13px;
  font-weight: 700;
  color: #3182f6;
  background: rgba(49, 130, 246, 0.15);
  padding: 4px 12px;
  border-radius: 20px;
}

.status-chip {
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 20px;
}

.status-chip.pending {
  background: rgba(245, 158, 11, 0.2);
  color: #fbbf24;
}

.status-chip.active {
  background: rgba(16, 185, 129, 0.2);
  color: #34d399;
}

.hero-user-title {
  font-size: 22px;
  font-weight: 700;
  color: #94a3b8;
  margin-bottom: 6px;
}

.hero-account-num {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  color: #cbd5e1;
  font-weight: 600;
  margin-bottom: 18px;
}

.copy-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.08);
  border: none;
  color: #94a3b8;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.copy-btn:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.2);
}

.hero-amount-wrapper {
  margin-bottom: 24px;
}

.bold-amount {
  font-size: 42px;
  color: #ffffff;
}

.dimmed {
  opacity: 0.5;
}

.pending-notice-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pending-banner {
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.3);
  color: #fbbf24;
  font-size: 13px;
  padding: 12px 16px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1.4;
}

.no-account-action {
  background: rgba(255, 255, 255, 0.04);
  padding: 20px;
  border-radius: 18px;
  border: 1px dashed rgba(255, 255, 255, 0.15);
}

.no-account-desc {
  color: #94a3b8;
  font-size: 14px;
  margin-bottom: 12px;
}

.create-btn {
  width: 100%;
}

.hero-actions {
  display: flex;
  gap: 14px;
  max-width: 380px;
}

/* 우측 3D 카드 그래픽 비주얼 */
.hero-right {
  display: flex;
  justify-content: flex-end;
  z-index: 1;
}

.bank-card-visual {
  width: 220px;
  height: 140px;
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
  border-radius: 18px;
  padding: 18px;
  box-shadow: 0 16px 32px rgba(37, 99, 235, 0.35);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transform: rotate(-6deg) translateY(-8px);
  transition: transform 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.hero-card:hover .bank-card-visual {
  transform: rotate(0deg) scale(1.05);
}

.card-chip {
  width: 32px;
  height: 24px;
  background: #f59e0b;
  border-radius: 6px;
}

.card-logo {
  font-size: 12px;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -0.3px;
}

.card-holder {
  font-size: 13px;
  font-weight: 700;
  color: #ffffff;
}

.card-brand-tag {
  font-size: 8px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 1px;
}

/* 4열 3D 서비스 카드 그리드 */
.section-header {
  margin-bottom: 20px;
}

.section-title {
  font-size: 20px;
  font-weight: 800;
  color: #ffffff;
  margin-bottom: 4px;
}

.section-sub {
  font-size: 14px;
  color: #64748b;
}

.services-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

@media (max-width: 768px) {
  .services-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.service-card {
  background: #141b26;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 16px;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.service-card:hover {
  transform: translateY(-4px);
  border-color: rgba(49, 130, 246, 0.4);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.4);
}

.card-thumb {
  height: 110px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 16px;
  position: relative;
  overflow: hidden;
}

.thumb-icon-box {
  color: #ffffff;
  display: flex;
  align-items: center;
}

.thumb-tag {
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.8px;
  opacity: 0.85;
  color: #ffffff;
}

.bg-gradient-blue { background: linear-gradient(135deg, #1e3a8a, #3b82f6); }
.bg-gradient-green { background: linear-gradient(135deg, #064e3b, #10b981); }
.bg-gradient-purple { background: linear-gradient(135deg, #4c1d95, #8b5cf6); }
.bg-gradient-pink { background: linear-gradient(135deg, #831843, #ec4899); }

.card-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-name {
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
}

.card-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}

.spin-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  100% { transform: rotate(360deg); }
}

/* 사이드바 패널 (우측) */
.sidebar-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-panel {
  background: #141b26;
  border-radius: 20px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
}

.refresh-mini-btn {
  background: rgba(255, 255, 255, 0.06);
  border: none;
  color: #94a3b8;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 8px;
  cursor: pointer;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.info-label {
  color: #64748b;
}

.info-value {
  color: #f8fafc;
  font-weight: 600;
}

.role-badge-mini {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
}

.role-badge-mini.admin { background: #ef4444; color: #fff; }
.role-badge-mini.user { background: #3182f6; color: #fff; }

.status-badge-mini {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
}

.status-badge-mini.pending { background: rgba(245, 158, 11, 0.2); color: #fbbf24; }
.status-badge-mini.active { background: rgba(16, 185, 129, 0.2); color: #34d399; }

.highlight-panel {
  background: linear-gradient(135deg, #1e293b, #0f172a);
  border-color: rgba(49, 130, 246, 0.2);
}

.security-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.security-icon {
  color: #34d399;
}

.guide-text {
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.5;
  margin-top: 10px;
}
</style>
