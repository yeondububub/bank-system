<template>
  <div class="dashboard-view">
    <!-- 대시보드 메인 레이아웃 (7:3 2단 구조) -->
    <div class="dashboard-grid">
      
      <!-- 좌측 메인 영역 (70%) -->
      <div class="main-content">

        <!-- 1. 히어로 뱅킹 카드 섹션 (좌우 슬라이더 & 마지막 + 새 계좌 개설 카드) -->
        <div class="hero-card-container">
          <div class="slider-controls-wrapper">
            <!-- 좌측 이전 슬라이드 버튼 -->
            <button 
              class="slide-nav-btn prev" 
              @click="prevSlide" 
              :disabled="currentSlideIndex === 0"
              title="이전 계좌"
            >
              <ChevronLeft :size="24" />
            </button>

            <!-- 히어로 메인 카드 (슬라이드 뷰) -->
            <div class="hero-card">
              <!-- A. 일반 계좌 슬라이드 뷰 -->
              <template v-if="currentSlideIndex < accounts.length && selectedAccount">
                <!-- 계좌 우상단 대표 계좌 별표 토글 버튼 (:key 트래킹으로 가상 DOM 즉시 리렌더링) -->
                <button 
                  class="star-toggle-btn" 
                  @click="handleStarClick"
                  :disabled="settingPrimary"
                  :title="isAccountPrimary(selectedAccount) ? '현재 대표 계좌입니다' : '클릭하여 대표 계좌로 지정'"
                >
                  <Star 
                    :key="`star-${selectedAccount.id}-${isAccountPrimary(selectedAccount)}`"
                    :size="24" 
                    :fill="isAccountPrimary(selectedAccount) ? '#fbbf24' : 'none'" 
                    :color="isAccountPrimary(selectedAccount) ? '#fbbf24' : '#64748b'" 
                    class="star-icon" 
                  />
                </button>

                <div class="hero-left">
                  <div class="account-tag-row">
                    <span class="account-type-tag">입출금 통장</span>
                    <span v-if="isAccountPrimary(selectedAccount)" class="primary-tag">
                      <Star :size="12" fill="#fbbf24" color="#fbbf24" class="fill-star" />
                      <span>대표 계좌</span>
                    </span>
                    <span :class="['status-chip', selectedAccount.status?.toLowerCase()]">
                      <Clock v-if="selectedAccount.status === 'PENDING'" :size="12" />
                      <CheckCircle2 v-else :size="12" />
                      <span>{{ selectedAccount.status === 'PENDING' ? '승인 대기' : '활성화' }}</span>
                    </span>
                  </div>

                  <h2 class="hero-user-title">{{ userName }}님의 계좌</h2>
                  
                  <div class="hero-account-num">
                    <span>{{ formattedAccountNumber }}</span>
                    <button class="copy-btn" @click="copyAccountNumber">
                      <Copy :size="12" />
                      <span>{{ copied ? '복사됨' : '복사' }}</span>
                    </button>
                  </div>

                  <div class="hero-amount-wrapper">
                    <div v-if="loading" class="bank-amount loading-text">잔액 조회 중...</div>
                    
                    <!-- 승인 대기 (PENDING) -->
                    <div v-else-if="selectedAccount.status === 'PENDING'" class="pending-notice-box">
                      <div class="bank-amount dimmed">{{ formattedBalance }} 원</div>
                      <div class="pending-banner">
                        <Info :size="18" class="info-icon" />
                        <span>현재 관리자 승인 대기 중입니다. 승인 처리 후 입출금 및 송금이 활성화됩니다.</span>
                      </div>
                    </div>

                    <!-- 활성화 (ACTIVE) -->
                    <div v-else class="bank-amount bold-amount">{{ formattedBalance }} 원</div>
                  </div>

                  <!-- 계좌 제어 액션 버튼 (송금하기로 신설) -->
                  <div v-if="selectedAccount.status === 'ACTIVE'" class="hero-actions">
                    <button class="bank-btn bank-btn-primary transfer-btn" @click="$router.push('/payment')">
                      <Send :size="18" />
                      <span>송금하기</span>
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
              </template>

              <!-- B. 마지막 슬라이드: + 새 계좌 개설 카드 뷰 -->
              <template v-else>
                <div class="create-card-slide">
                  <div class="create-icon-circle">
                    <PlusCircle :size="42" class="create-big-icon" />
                  </div>
                  <h3 class="create-slide-title">새로운 13자리 계좌 추가 개설</h3>
                  <p class="create-slide-desc">새로운 뱅킹 계좌를 신청하여 지출과 자산을 스마트하게 관리해보세요.</p>
                  <button class="bank-btn bank-btn-primary create-action-btn" @click="handleCreateAccount" :disabled="creatingAccount">
                    <PlusCircle :size="18" />
                    <span>{{ creatingAccount ? '계좌 생성 신청 중...' : '13자리 계좌 개설 신청하기' }}</span>
                  </button>
                </div>
              </template>
            </div>

            <!-- 우측 다음 슬라이드 버튼 -->
            <button 
              class="slide-nav-btn next" 
              @click="nextSlide" 
              :disabled="currentSlideIndex >= accounts.length"
              title="다음 계좌"
            >
              <ChevronRight :size="24" />
            </button>
          </div>

          <!-- 하단 슬라이더 인디케이터 Dot -->
          <div class="slider-indicators">
            <button 
              v-for="(acc, i) in accounts" 
              :key="acc.id" 
              :class="['indicator-dot', { active: currentSlideIndex === i, isPrimary: isAccountPrimary(acc) }]"
              @click="currentSlideIndex = i"
              :title="`${i + 1}번째 계좌`"
            ></button>
            <button 
              :class="['indicator-dot add-dot', { active: currentSlideIndex === accounts.length }]"
              @click="currentSlideIndex = accounts.length"
              title="+ 계좌 추가 개설"
            >
              +
            </button>
          </div>
        </div>

        <!-- 2. 스마트 뱅킹 서비스 (4열 3D 카드 그리드) -->
        <div class="section-header">
          <h3 class="section-title">스마트 뱅킹 서비스</h3>
          <span class="section-sub">안전하고 신속하게 관리하는 뱅크 시스템 기능</span>
        </div>

        <div class="services-grid">
          <!-- 카드 1: 송금 / 결제 -->
          <router-link to="/payment" class="service-card">
            <div class="card-thumb bg-gradient-blue">
              <div class="thumb-icon-box">
                <CreditCard :size="28" />
              </div>
              <span class="thumb-tag">TRANSFER</span>
            </div>
            <div class="card-body">
              <h4 class="card-name">송금하기</h4>
              <p class="card-desc">안전한 멱등성 헤더 및 동시성 제어 송금</p>
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
              <p class="card-desc">실시간 입출금 및 송금 내역 타임라인</p>
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
        <!-- 관심 계좌 & 서비스 현황 위젯 (정돈된 내역) -->
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
              <span class="info-label">보유 계좌 개수</span>
              <span class="info-value highlight-num">{{ accounts.length }} 개</span>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ArrowDownLeft, Send, CreditCard, Receipt, RefreshCw, ShieldCheck, Copy, Clock, CheckCircle2, PlusCircle, Info, Star, ChevronLeft, ChevronRight } from 'lucide-vue-next'

const userName = ref('사용자')
const userRole = ref('USER')
const userId = ref<number | null>(null)

const accounts = ref<any[]>([])
const currentSlideIndex = ref(0)
const loading = ref(false)
const creatingAccount = ref(false)
const settingPrimary = ref(false)
const copied = ref(false)

const prevSlide = () => {
  if (currentSlideIndex.value > 0) {
    currentSlideIndex.value--
  }
}

const nextSlide = () => {
  if (currentSlideIndex.value < accounts.value.length) {
    currentSlideIndex.value++
  }
}

const isAccountPrimary = (acc: any) => {
  if (!acc) return false
  return Boolean(acc.isPrimary ?? acc.primary ?? false)
}

const selectedAccount = computed(() => {
  if (accounts.value.length === 0 || currentSlideIndex.value >= accounts.value.length) {
    return null
  }
  return accounts.value[currentSlideIndex.value]
})

const formattedBalance = computed(() => {
  return (selectedAccount.value?.balance || 0).toLocaleString('ko-KR')
})

const formattedAccountNumber = computed(() => {
  if (!selectedAccount.value?.accountNumber) return ''
  const num = String(selectedAccount.value.accountNumber)
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
    const response = await fetch(`/api/v1/accounts/user/${userId.value}`)
    if (response.ok) {
      const list = await response.json()
      accounts.value = list

      // 계좌가 아예 없을 때는 마감 +생성 슬라이드로 이동
      if (list.length === 0) {
        currentSlideIndex.value = 0
      }
    } else {
      accounts.value = []
    }
  } catch (error: any) {
    console.error('계좌 목록 조회 중 오류:', error)
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
      alert('13자리 계좌 개설 신청이 완료되었습니다! (관리자 승인 대기)')
      await fetchAccountInfo()
      // 새로 개설된 계좌 슬라이드로 자동 이동
      currentSlideIndex.value = accounts.value.length - 1
    } else {
      alert('계좌 개설 신청에 실패했습니다.')
    }
  } catch (e) {
    alert('계좌 개설 신청 중 오류가 발생했습니다.')
  } finally {
    creatingAccount.value = false
  }
}

const handleStarClick = () => {
  if (!selectedAccount.value) return
  if (isAccountPrimary(selectedAccount.value)) {
    return
  }
  handleSetPrimary(selectedAccount.value.id)
}

const handleSetPrimary = async (accountId: number) => {
  if (!accountId) return
  if (!confirm('이 계좌를 나의 대표 계좌로 변경하시겠습니까?')) return

  settingPrimary.value = true
  try {
    const res = await fetch(`/api/v1/accounts/${accountId}/primary`, {
      method: 'POST'
    })

    if (res.ok) {
      // 로컬 반응형 상태 즉시 갱신 (Optimistic Update)
      accounts.value.forEach((a: any) => {
        if (String(a.id) === String(accountId)) {
          a.isPrimary = true
          a.primary = true
        } else {
          a.isPrimary = false
          a.primary = false
        }
      })
      alert('대표 계좌가 정상 변경되었습니다!')

      // 백엔드 데이터 동기화
      await fetchAccountInfo()
    } else {
      alert('대표 계좌 변경에 실패했습니다.')
    }
  } catch (e) {
    alert('대표 계좌 변경 중 오류가 발생했습니다.')
  } finally {
    settingPrimary.value = false
  }
}

const copyAccountNumber = () => {
  if (selectedAccount.value?.accountNumber) {
    navigator.clipboard.writeText(selectedAccount.value.accountNumber)
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  }
}

const handleAdminClick = () => {
  if (userRole.value !== 'ADMIN') {
    alert('관리자 전용 기능입니다. ADMIN 권한 계정으로 로그인해 주세요.')
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

/* 히어로 뱅킹 카드 슬라이더 */
.hero-card-container {
  margin-bottom: 36px;
}

.slider-controls-wrapper {
  display: flex;
  align-items: center;
  gap: 16px;
}

.slide-nav-btn {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #94a3b8;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.25s ease;
}

.slide-nav-btn:hover:not(:disabled) {
  background: rgba(49, 130, 246, 0.25);
  color: #ffffff;
  border-color: #3182f6;
  transform: scale(1.1);
}

.slide-nav-btn:disabled {
  opacity: 0.25;
  cursor: not-allowed;
}

.hero-card {
  flex: 1;
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
  min-height: 280px;
  box-sizing: border-box;
}

/* 계좌 카드 우상단 별표 대표계좌 토글 버튼 */
.star-toggle-btn {
  position: absolute;
  top: 24px;
  right: 28px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 10px;
  border-radius: 50%;
  cursor: pointer;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.star-toggle-btn:hover:not(:disabled) {
  background: rgba(245, 158, 11, 0.18);
  border-color: rgba(245, 158, 11, 0.4);
  transform: scale(1.12);
}

.star-icon {
  transition: all 0.25s ease;
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

.primary-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 700;
  color: #fbbf24;
  background: rgba(245, 158, 11, 0.15);
  padding: 4px 10px;
  border-radius: 20px;
}

.fill-star {
  fill: #fbbf24;
}

.status-chip {
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 4px;
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

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.transfer-btn {
  padding: 12px 28px;
  font-size: 15px;
  border-radius: 14px;
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

/* B. 마지막 슬라이드: + 새 계좌 개설 카드 */
.create-card-slide {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 20px 0;
}

.create-icon-circle {
  width: 64px;
  height: 64px;
  background: rgba(49, 130, 246, 0.15);
  border: 1px solid rgba(49, 130, 246, 0.3);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3182f6;
  margin-bottom: 16px;
}

.create-slide-title {
  font-size: 20px;
  font-weight: 800;
  color: #ffffff;
  margin-bottom: 8px;
}

.create-slide-desc {
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 20px;
  max-width: 420px;
}

.create-action-btn {
  padding: 14px 28px;
  font-size: 15px;
  border-radius: 16px;
}

/* 슬라이더 하단 인디케이터 점(Dots) */
.slider-indicators {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}

.indicator-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  border: none;
  cursor: pointer;
  transition: all 0.25s ease;
}

.indicator-dot.active {
  background: #3182f6;
  width: 24px;
  border-radius: 10px;
}

.indicator-dot.isPrimary {
  background: #fbbf24;
}

.indicator-dot.add-dot {
  color: #3182f6;
  font-weight: 800;
  font-size: 10px;
  line-height: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(49, 130, 246, 0.2);
}

.indicator-dot.add-dot.active {
  background: #3182f6;
  color: #ffffff;
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

.highlight-num {
  color: #3182f6;
  font-weight: 800;
}
</style>
