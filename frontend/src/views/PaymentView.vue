<template>
  <div class="transfer-container">
    <!-- Header -->
    <div class="transfer-header">
      <h1 class="page-title">Transfer</h1>
      <p class="page-subtitle">쉽고 빠른 스마트 이체 서비스</p>
    </div>

    <!-- Step Progress Bar -->
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

    <!-- STEP 1: Form Input -->
    <div v-if="currentStep === 1" class="bank-card transfer-card step-fade">
      <h3 class="bank-title">어디로 얼마를 보낼까요?</h3>
      <p class="bank-subtitle">선택하신 출금 계좌에서 받는 분의 계좌번호와 금액을 입력해 주세요.</p>

      <form @submit.prevent="goToConfirmStep" class="transfer-form">
        <!-- Selected Withdrawal Account Badge -->
        <div v-if="selectedAccount" class="source-account-badge">
          <span class="badge-label">{{ isAccountPrimary(selectedAccount) ? '대표 출금 계좌' : '출금 계좌' }}</span>
          <span class="badge-acc">{{ formatAccNum(selectedAccount.accountNumber) }}</span>
          <span class="badge-bal">(출금 가능 잔액: {{ selectedAccount.balance.toLocaleString() }}원)</span>
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
          <!-- Quick Amount Chips -->
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
            :disabled="!selectedAccount || !toAccountNumber || !amount || amount <= 0 || fetchingHolder"
          >
            <span v-if="!fetchingHolder">다음</span>
            <span v-else>계좌 확인 중...</span>
          </button>
        </div>
      </form>
    </div>

    <!-- STEP 2: Confirmation -->
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
          <span class="summary-val">{{ formatAccNum(selectedAccount?.accountNumber) }} {{ isAccountPrimary(selectedAccount) ? '(대표)' : '' }}</span>
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

    <!-- STEP 3: Success Receipt -->
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
          <span>출금 계좌</span>
          <strong>{{ formatAccNum(selectedAccount?.accountNumber) }}</strong>
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
          대시보드로 돌아가기
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { User, AlertCircle, CheckCircle2 } from 'lucide-vue-next'
import axios from 'axios'

const route = useRoute()

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

const isAccountPrimary = (acc: any) => {
  if (!acc) return false
  return Boolean(acc.isPrimary ?? acc.primary ?? false)
}

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

    const fromQuery = String(route.query.from || '').replace(/-/g, '').trim()
    if (fromQuery) {
      const matched = res.data.find((a: any) => String(a.accountNumber).replace(/-/g, '').trim() === fromQuery)
      if (matched) {
        selectedAccount.value = matched
        return
      }
    }

    // Default to primary account or first active account
    const primary = res.data.find((a: any) => isAccountPrimary(a) && a.status === 'ACTIVE')
    if (primary) {
      selectedAccount.value = primary
    } else {
      const firstActive = res.data.find((a: any) => a.status === 'ACTIVE')
      if (firstActive) {
        selectedAccount.value = firstActive
      } else {
        selectedAccount.value = res.data[0] || null
      }
    }
  } catch (e) {
    userAccounts.value = []
  }
}

const goToConfirmStep = async () => {
  errorMessage.value = ''
  if (!selectedAccount.value) {
    errorMessage.value = '출금하실 계좌가 지정되지 않았습니다.'
    return
  }
  if (!toAccountNumber.value) {
    errorMessage.value = '받는 분 계좌번호를 입력해 주세요.'
    return
  }
  if (!amount.value || amount.value <= 0) {
    errorMessage.value = '송금하실 금액을 올바르게 입력해 주세요.'
    return
  }
  if (amount.value > selectedAccount.value.balance) {
    errorMessage.value = '출금 계좌의 잔액이 부족합니다.'
    return
  }

  const cleanTo = toAccountNumber.value.replace(/-/g, '').trim()
  const cleanFrom = String(selectedAccount.value.accountNumber).replace(/-/g, '').trim()
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

onMounted(async () => {
  const token = localStorage.getItem('accessToken')
  let user: any = null
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      user = JSON.parse(userStr)
    } catch (e) {}
  }

  if ((!user || !user.id) && token) {
    try {
      const res = await fetch('/api/v1/auth/me', {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      if (res.ok) {
        user = await res.json()
        localStorage.setItem('user', JSON.stringify(user))
      }
    } catch (e) {}
  }

  if (user && user.id) {
    fetchAccounts(user.id)
  }
})
</script>

<style scoped>
.transfer-container {
  max-width: 600px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
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

/* Step Progress Bar */
.step-progress-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.step-num {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--surface-subtle);
  border: 2px solid var(--border-light);
  color: var(--text-tertiary);
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
  color: var(--text-tertiary);
}

.step-item.active .step-num {
  background: var(--toss-blue);
  border-color: var(--toss-blue);
  color: #ffffff;
  box-shadow: 0 4px 12px var(--toss-blue-glow);
}

.step-item.active .step-label {
  color: var(--text-primary);
  font-weight: 700;
}

.step-item.completed .step-num {
  background: var(--bank-green);
  border-color: var(--bank-green);
  color: #ffffff;
}

.step-line {
  flex: 1;
  height: 2px;
  background: var(--border-light);
  margin: 0 12px;
  margin-bottom: 20px;
}

.step-line.active {
  background: var(--toss-blue);
}

/* Transfer Card */
.transfer-card {
  padding: 32px;
}

.step-fade {
  animation: fadeIn 0.3s ease-out forwards;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.source-account-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--toss-blue-light);
  border: 1px solid rgba(0, 100, 255, 0.2);
  padding: 12px 18px;
  border-radius: var(--radius-md);
  margin-top: 8px;
  font-size: 14px;
}

.badge-label {
  font-weight: 800;
  color: var(--toss-blue);
}

.badge-acc {
  font-weight: 700;
  color: var(--text-primary);
}

.badge-bal {
  color: var(--text-secondary);
}

.transfer-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 20px;
}

.bank-input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bank-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
}

.amount-input-wrapper {
  position: relative;
}

.amount-input {
  padding-right: 40px;
}

.currency-unit {
  position: absolute;
  right: 18px;
  top: 50%;
  transform: translateY(-50%);
  font-weight: 700;
  color: var(--text-tertiary);
}

.quick-amount-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 6px;
}

.chip-btn {
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

.chip-btn:hover {
  background: var(--toss-blue-light);
  color: var(--toss-blue);
  border-color: var(--toss-blue-light);
}

.chip-btn.max-btn {
  background: var(--toss-gray);
  color: #ffffff;
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bank-red-light);
  color: var(--bank-red);
  padding: 12px 16px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
}

.next-btn, .execute-transfer-btn, .finish-btn {
  width: 100%;
  padding: 16px;
  margin-top: 8px;
}

/* Step 2 Confirmation Styling */
.confirm-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}

.recipient-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--toss-blue-light);
  color: var(--toss-blue);
  display: flex;
  align-items: center;
  justify-content: center;
}

.recipient-badge-tag {
  font-size: 11px;
  font-weight: 800;
  color: var(--toss-blue);
  background: var(--toss-blue-light);
  padding: 2px 10px;
  border-radius: var(--radius-pill);
}

.recipient-name-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-primary);
}

.confirm-message-box {
  background: var(--surface-subtle);
  border-radius: var(--radius-md);
  padding: 20px;
  text-align: center;
  margin-bottom: 20px;
}

.confirm-text {
  font-size: 16px;
  color: var(--text-primary);
  line-height: 1.6;
}

.highlight-name {
  color: var(--toss-blue);
}

.highlight-amount {
  color: var(--text-primary);
  font-size: 20px;
}

.transfer-detail-summary {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
  border-top: 1px solid var(--border-light);
  border-bottom: 1px solid var(--border-light);
  padding: 16px 0;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.summary-label {
  color: var(--text-secondary);
}

.summary-val {
  font-weight: 700;
  color: var(--text-primary);
}

.confirm-actions {
  display: flex;
  gap: 12px;
}

.prev-step-btn {
  background: var(--surface-subtle);
  color: var(--text-secondary);
  border: 1px solid var(--border-light);
  width: 30%;
}

/* Step 3 Success */
.success-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 12px;
}

.success-icon-box {
  color: var(--bank-green);
  margin-bottom: 8px;
}

.success-title {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-primary);
}

.success-subdesc {
  font-size: 14px;
  color: var(--text-secondary);
}

.receipt-box {
  width: 100%;
  background: var(--surface-subtle);
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 16px 0;
}

.receipt-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.sent-amount {
  color: var(--toss-blue);
  font-size: 16px;
}
</style>
