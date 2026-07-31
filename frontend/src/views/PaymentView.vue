<template>
  <div class="transfer-container">
    <!-- 상단 네비게이션 -->
    <div class="header-nav">
      <button class="back-btn" @click="handleBack">
        <ArrowLeft :size="22" />
      </button>
      <h2 class="nav-title">송금하기</h2>
    </div>

    <!-- 단계별 프로그레스 스텝 바 -->
    <div class="step-progress-bar">
      <div :class="['step-item', { active: currentStep >= 1, completed: currentStep > 1 }]">
        <span class="step-num">1</span>
        <span class="step-label">정보 입력</span>
      </div>
      <div class="step-line" :class="{ active: currentStep >= 2 }"></div>
      <div :class="['step-item', { active: currentStep >= 2, completed: currentStep > 2 }]">
        <span class="step-num">2</span>
        <span class="step-label">송금 확인</span>
      </div>
      <div class="step-line" :class="{ active: currentStep >= 3 }"></div>
      <div :class="['step-item', { active: currentStep >= 3 }]">
        <span class="step-num">3</span>
        <span class="step-label">송금 완료</span>
      </div>
    </div>

    <!-- STEP 1: 정보 입력 (받는 분 계좌번호, 보낼 금액 디자인) -->
    <div v-if="currentStep === 1" class="bank-card transfer-card step-fade">
      <h3 class="bank-title">어디로 얼마를 보낼까요?</h3>
      <p class="bank-subtitle">받는 분의 계좌번호와 송금하실 금액을 입력해 주세요.</p>

      <form @submit.prevent="goToConfirmStep" class="transfer-form">
        <!-- 출금 계좌 간편 표시 (기본 선택) -->
        <div v-if="selectedAccount" class="source-account-badge">
          <span class="badge-label">출금 계좌</span>
          <span class="badge-acc">{{ formatAccNum(selectedAccount.accountNumber) }}</span>
          <span class="badge-bal">(잔액: {{ selectedAccount.balance.toLocaleString() }}원)</span>
        </div>

        <div class="bank-input-group">
          <label class="bank-label">받는 분 계좌번호</label>
          <input
            v-model="toAccountNumber"
            type="text"
            class="bank-input account-num-input"
            placeholder="13자리 계좌번호 입력 (예: 100-2004-5678-90)"
            required
            autofocus
          />
        </div>

        <div class="bank-input-group">
          <label class="bank-label">보낼 금액 (원)</label>
          <div class="amount-input-wrapper">
            <input
              v-model.number="amount"
              type="number"
              class="bank-input amount-input"
              placeholder="0"
              required
              min="1"
            />
            <span class="currency-unit">원</span>
          </div>
          <!-- 빠른 금액 선택 칩 -->
          <div class="quick-amount-chips">
            <button type="button" class="chip-btn" @click="addAmount(10000)">+1만</button>
            <button type="button" class="chip-btn" @click="addAmount(50000)">+5만</button>
            <button type="button" class="chip-btn" @click="addAmount(100000)">+10만</button>
            <button type="button" class="chip-btn" @click="addAmount(500000)">+50만</button>
            <button type="button" class="chip-btn max-btn" @click="setFullAmount">전액</button>
          </div>
        </div>

        <div v-if="errorMessage" class="error-banner">
          <AlertCircle :size="16" />
          <span>{{ errorMessage }}</span>
        </div>

        <div class="action-btn-group">
          <button
            type="submit"
            class="bank-btn bank-btn-primary next-btn"
            :disabled="!toAccountNumber || !amount || amount <= 0 || fetchingHolder"
          >
            <span v-if="!fetchingHolder">다음</span>
            <span v-else>계좌 확인 중...</span>
          </button>
        </div>
      </form>
    </div>

    <!-- STEP 2: 송금 확인 (계좌 주인 이름 및 전송 확인 문구) -->
    <div v-else-if="currentStep === 2" class="bank-card transfer-card step-fade">
      <div class="confirm-header">
        <div class="recipient-avatar">
          <User :size="36" />
        </div>
        <div class="recipient-badge-tag">받는 사람</div>
        <h3 class="recipient-name-title">{{ recipientName }} 님</h3>
      </div>

      <div class="confirm-message-box">
        <p class="confirm-text">
          정말 <strong class="highlight-name">{{ recipientName }}</strong>님에게 <br />
          <strong class="highlight-amount">{{ (amount || 0).toLocaleString() }}원</strong>을 전송하시겠습니까?
        </p>
      </div>

      <div class="transfer-detail-summary">
        <div class="summary-row">
          <span class="summary-label">출금 계좌</span>
          <span class="summary-val">{{ formatAccNum(selectedAccount?.accountNumber) }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">입금 계좌</span>
          <span class="summary-val">{{ formatAccNum(toAccountNumber) }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">송금 후 잔액</span>
          <span class="summary-val remaining-balance">{{ ((selectedAccount?.balance || 0) - (amount || 0)).toLocaleString() }}원</span>
        </div>
      </div>

      <div v-if="errorMessage" class="error-banner">
        <AlertCircle :size="16" />
        <span>{{ errorMessage }}</span>
      </div>

      <div class="confirm-actions">
        <button type="button" class="bank-btn prev-step-btn" @click="currentStep = 1" :disabled="loading">
          이전
        </button>
        <button
          type="button"
          class="bank-btn bank-btn-primary execute-transfer-btn"
          @click="submitTransfer"
          :disabled="loading"
        >
          <span v-if="!loading">송금하기</span>
          <span v-else>송금 처리 중...</span>
        </button>
      </div>
    </div>

    <!-- STEP 3: 송금 완료 -->
    <div v-else-if="currentStep === 3" class="bank-card transfer-card step-fade success-card">
      <div class="success-icon-box">
        <CheckCircle2 :size="64" class="check-icon" />
      </div>
      <h3 class="success-title">송금되었습니다.</h3>
      <p class="success-subdesc">요청하신 금액이 안전하게 전송 완료되었습니다.</p>

      <div class="receipt-box">
        <div class="receipt-row">
          <span>받은 사람</span>
          <strong>{{ recipientName }} 님</strong>
        </div>
        <div class="receipt-row">
          <span>보낸 금액</span>
          <strong class="sent-amount">{{ (amount || 0).toLocaleString() }} 원</strong>
        </div>
        <div class="receipt-row">
          <span>출금 후 잔액</span>
          <strong>{{ balanceAfter.toLocaleString() }} 원</strong>
        </div>
      </div>

      <div class="success-actions">
        <button class="bank-btn bank-btn-primary finish-btn" @click="$router.push('/')">
          홈으로 돌아가기
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, User, AlertCircle, CheckCircle2 } from 'lucide-vue-next'
import axios from 'axios'

const router = useRouter()

const currentStep = ref(1)
const toAccountNumber = ref('')
const amount = ref<number | ''>('')
const recipientName = ref('')
const userAccounts = ref<any[]>([])
const selectedAccount = ref<any>(null)

const fetchingHolder = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const balanceAfter = ref(0)

const formatAccNum = (numStr?: string) => {
  if (!numStr) return ''
  const str = String(numStr).replace(/-/g, '')
  if (str.length === 13) {
    return `${str.slice(0, 3)}-${str.slice(3, 7)}-${str.slice(7, 11)}-${str.slice(11)}`
  }
  return str
}

const addAmount = (addVal: number) => {
  const current = typeof amount.value === 'number' ? amount.value : 0
  amount.value = current + addVal
}

const setFullAmount = () => {
  if (selectedAccount.value) {
    amount.value = selectedAccount.value.balance
  }
}

const fetchAccounts = async (userId: number) => {
  try {
    const res = await axios.get(`/api/v1/accounts/user/${userId}`)
    userAccounts.value = res.data
    // 대표 계좌(isPrimary = true)를 기본 선택값으로 지정
    const primary = res.data.find((a: any) => a.isPrimary)
    if (primary) {
      selectedAccount.value = primary
    } else if (res.data.length > 0) {
      selectedAccount.value = res.data[0]
    }
  } catch (e) {
    userAccounts.value = []
  }
}

const goToConfirmStep = async () => {
  errorMessage.value = ''
  if (!toAccountNumber.value) {
    errorMessage.value = '받는 분 계좌번호를 입력해 주세요.'
    return
  }
  if (!amount.value || amount.value <= 0) {
    errorMessage.value = '송금하실 금액을 올바르게 입력해 주세요.'
    return
  }
  if (selectedAccount.value && amount.value > selectedAccount.value.balance) {
    errorMessage.value = '출금 가능 잔액이 부족합니다.'
    return
  }

  // 본인 계좌 송금 방지
  const cleanTo = toAccountNumber.value.replace(/-/g, '').trim()
  const cleanFrom = selectedAccount.value?.accountNumber.replace(/-/g, '').trim()
  if (cleanTo === cleanFrom) {
    errorMessage.value = '동일한 계좌로는 송금할 수 없습니다.'
    return
  }

  fetchingHolder.value = true
  try {
    const res = await axios.get(`/api/v1/accounts/holder/${cleanTo}`)
    recipientName.value = res.data.ownerName || '고객'
    currentStep.value = 2
  } catch (e: any) {
    // 계좌를 찾을 수 없을 때도 친절 안내
    if (e.response?.status === 404) {
      errorMessage.value = '입력하신 계좌번호를 찾을 수 없습니다. 다시 확인해 주세요.'
    } else {
      recipientName.value = '예금주'
      currentStep.value = 2
    }
  } finally {
    fetchingHolder.value = false
  }
}

const submitTransfer = async () => {
  if (!selectedAccount.value || !toAccountNumber.value || !amount.value) return

  loading.value = true
  errorMessage.value = ''

  try {
    const response = await axios.post('/api/v1/accounts/transfer', {
      fromAccountNumber: selectedAccount.value.accountNumber,
      toAccountNumber: toAccountNumber.value.replace(/-/g, '').trim(),
      amount: amount.value
    })

    balanceAfter.value = response.data.balanceAfterFrom
    currentStep.value = 3
  } catch (error: any) {
    errorMessage.value = error.response?.data?.message || '송금 처리 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  if (currentStep.value > 1 && currentStep.value < 3) {
    currentStep.value--
  } else {
    router.push('/')
  }
}

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      if (user.id) {
        fetchAccounts(user.id)
      }
    } catch (e) {}
  }
})
</script>

<style scoped>
.transfer-container {
  max-width: 540px;
  margin: 0 auto;
  padding-top: 10px;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.back-btn {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #ffffff;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: scale(1.05);
}

.nav-title {
  font-size: 22px;
  font-weight: 800;
  color: #ffffff;
}

/* 스텝 프로그레스 바 */
.step-progress-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
  padding: 0 16px;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.step-num {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #1e293b;
  border: 2px solid #475569;
  color: #94a3b8;
  font-weight: 800;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.step-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  transition: all 0.3s ease;
}

.step-item.active .step-num {
  background: #3182f6;
  border-color: #3182f6;
  color: #ffffff;
  box-shadow: 0 0 14px rgba(49, 130, 246, 0.5);
}

.step-item.active .step-label {
  color: #ffffff;
}

.step-item.completed .step-num {
  background: #10b981;
  border-color: #10b981;
  color: #ffffff;
}

.step-line {
  flex: 1;
  height: 2px;
  background: #334155;
  margin: 0 12px;
  margin-bottom: 20px;
  transition: all 0.3s ease;
}

.step-line.active {
  background: #3182f6;
}

/* 카드 기본 스타일 */
.transfer-card {
  background: linear-gradient(145deg, #151c28 0%, #1e293b 100%);
  border-radius: 28px;
  padding: 36px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
}

.step-fade {
  animation: fadeIn 0.35s ease-out forwards;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.bank-title {
  font-size: 22px;
  font-weight: 800;
  color: #ffffff;
  margin-bottom: 6px;
}

.bank-subtitle {
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 24px;
}

/* 출금 계좌 배지 */
.source-account-badge {
  background: rgba(49, 130, 246, 0.12);
  border: 1px solid rgba(49, 130, 246, 0.3);
  padding: 10px 16px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  font-size: 13px;
}

.badge-label {
  color: #3182f6;
  font-weight: 800;
}

.badge-acc {
  color: #ffffff;
  font-weight: 700;
}

.badge-bal {
  color: #94a3b8;
  font-size: 12px;
}

/* 폼 요소 */
.bank-input-group {
  margin-bottom: 24px;
}

.bank-label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #cbd5e1;
  margin-bottom: 8px;
}

.bank-input {
  width: 100%;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 14px;
  padding: 14px 18px;
  font-size: 15px;
  color: #ffffff;
  outline: none;
  box-sizing: border-box;
  transition: all 0.2s ease;
}

.bank-input:focus {
  border-color: #3182f6;
  box-shadow: 0 0 0 3px rgba(49, 130, 246, 0.25);
}

.account-num-input {
  font-family: monospace;
  font-size: 17px;
  letter-spacing: 0.5px;
}

.amount-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.amount-input {
  font-size: 24px;
  font-weight: 800;
  color: #3182f6;
  padding-right: 42px;
}

.currency-unit {
  position: absolute;
  right: 18px;
  font-size: 18px;
  font-weight: 700;
  color: #94a3b8;
}

/* 빠른 금액 칩 */
.quick-amount-chips {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.chip-btn {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #cbd5e1;
  padding: 6px 12px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.chip-btn:hover {
  background: rgba(49, 130, 246, 0.2);
  color: #ffffff;
  border-color: #3182f6;
}

.max-btn {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
  border-color: rgba(245, 158, 11, 0.3);
}

.max-btn:hover {
  background: rgba(245, 158, 11, 0.3);
}

/* 에러 배너 */
.error-banner {
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.4);
  color: #fca5a5;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.action-btn-group {
  margin-top: 28px;
}

.next-btn {
  width: 100%;
  padding: 16px;
  font-size: 17px;
  font-weight: 800;
  border-radius: 16px;
}

/* STEP 2: 확인 화면 디자인 */
.confirm-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;
}

.recipient-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.4);
}

.recipient-badge-tag {
  font-size: 12px;
  font-weight: 700;
  color: #3182f6;
  background: rgba(49, 130, 246, 0.15);
  padding: 3px 10px;
  border-radius: 12px;
  margin-bottom: 6px;
}

.recipient-name-title {
  font-size: 24px;
  font-weight: 800;
  color: #ffffff;
}

.confirm-message-box {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  padding: 24px;
  text-align: center;
  margin-bottom: 24px;
}

.confirm-text {
  font-size: 18px;
  line-height: 1.6;
  color: #cbd5e1;
}

.highlight-name {
  color: #ffffff;
  font-size: 22px;
  font-weight: 800;
}

.highlight-amount {
  color: #3182f6;
  font-size: 24px;
  font-weight: 800;
}

.transfer-detail-summary {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: rgba(255, 255, 255, 0.03);
  padding: 16px 20px;
  border-radius: 14px;
  margin-bottom: 28px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.summary-label {
  color: #94a3b8;
}

.summary-val {
  color: #ffffff;
  font-weight: 600;
}

.remaining-balance {
  color: #34d399;
  font-weight: 700;
}

.confirm-actions {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 12px;
}

.prev-step-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #cbd5e1;
  padding: 16px;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
}

.prev-step-btn:hover {
  background: rgba(255, 255, 255, 0.18);
  color: #ffffff;
}

.execute-transfer-btn {
  padding: 16px;
  font-size: 17px;
  font-weight: 800;
  border-radius: 16px;
}

/* STEP 3: 성공 화면 디자인 */
.success-card {
  text-align: center;
  padding: 48px 36px;
}

.success-icon-box {
  color: #10b981;
  margin-bottom: 16px;
  animation: popIn 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes popIn {
  0% { transform: scale(0); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

.success-title {
  font-size: 28px;
  font-weight: 800;
  color: #ffffff;
  margin-bottom: 8px;
}

.success-subdesc {
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 32px;
}

.receipt-box {
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 32px;
}

.receipt-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #cbd5e1;
}

.sent-amount {
  color: #3182f6;
  font-size: 18px;
}

.finish-btn {
  width: 100%;
  padding: 16px;
  font-size: 17px;
  font-weight: 800;
  border-radius: 16px;
}
</style>
