<template>
  <div class="transfer-container">
    <!-- 상단 진행 표시 -->
    <div class="progress-track" aria-hidden="true">
      <div class="progress-fill" :style="{ width: `${(currentStep / 3) * 100}%` }"></div>
    </div>

    <!-- STEP 1: 정보 입력 -->
    <section v-if="currentStep === 1" class="bank-card transfer-card">
      <div class="card-head">
        <h1 class="step-title">어디로 돈을 보낼까요?</h1>
        <p class="step-subtitle">받는 분의 13자리 계좌번호와 금액을 입력해 주세요.</p>
      </div>

      <form @submit.prevent="goToConfirmStep" class="transfer-form">
        <div v-if="selectedAccount" class="source-account-box">
          <div class="source-left">
            <span class="source-tag">{{ isAccountPrimary(selectedAccount) ? '대표 계좌' : '출금 계좌' }}</span>
            <span class="source-num num">{{ formatAccNum(selectedAccount.accountNumber) }}</span>
          </div>
          <div class="source-right num">
            잔액 {{ selectedAccount.balance.toLocaleString() }}원
          </div>
        </div>

        <div class="field-group">
          <label for="to-account" class="field-label">받는 분 계좌번호</label>
          <input
            id="to-account"
            v-model="toAccountNumber"
            type="text"
            class="bank-input num"
            placeholder="계좌번호 13자리 입력"
            required
            autofocus
          />
        </div>

        <div class="field-group">
          <label for="transfer-amount" class="field-label">보낼 금액</label>
          <div class="amount-input-wrapper">
            <input
              id="transfer-amount"
              v-model.number="amount"
              type="number"
              inputmode="numeric"
              class="bank-input amount-input num"
              placeholder="얼마나 보낼까요?"
              required
              min="1"
            />
            <span class="currency-unit">원</span>
          </div>

          <div class="quick-chips">
            <button type="button" class="chip-btn" @click="addAmount(10000)">+1만</button>
            <button type="button" class="chip-btn" @click="addAmount(50000)">+5만</button>
            <button type="button" class="chip-btn" @click="addAmount(100000)">+10만</button>
            <button type="button" class="chip-btn" @click="addAmount(500000)">+50만</button>
            <button type="button" class="chip-btn soft-chip" @click="setFullAmount">전액</button>
          </div>
        </div>

        <div v-if="errorMessage" class="error-banner" role="alert">
          <AlertCircle :size="16" />
          <span>{{ errorMessage }}</span>
        </div>

        <button
          type="submit"
          class="bank-btn bank-btn-primary full-btn"
          :disabled="!selectedAccount || !toAccountNumber || !amount || amount <= 0 || fetchingHolder"
        >
          {{ fetchingHolder ? '계좌 확인 중…' : '다음' }}
        </button>
      </form>
    </section>

    <!-- STEP 2: 송금 확인 -->
    <section v-else-if="currentStep === 2" class="bank-card transfer-card">
      <div class="card-head">
        <h1 class="step-title">
          {{ recipientName }}님에게<br />
          <span class="amount-emphasis num">{{ (amount || 0).toLocaleString() }}원</span>을 보낼까요?
        </h1>
      </div>

      <dl class="summary-list">
        <div class="summary-row">
          <dt>출금 계좌</dt>
          <dd class="num">{{ formatAccNum(selectedAccount?.accountNumber) }}</dd>
        </div>
        <div class="summary-row">
          <dt>입금 계좌</dt>
          <dd class="num">{{ formatAccNum(toAccountNumber) }}</dd>
        </div>
        <div class="summary-row">
          <dt>송금 후 잔액</dt>
          <dd class="num">{{ ((selectedAccount?.balance || 0) - (amount || 0)).toLocaleString() }}원</dd>
        </div>
      </dl>

      <div v-if="errorMessage" class="error-banner" role="alert">
        <AlertCircle :size="16" />
        <span>{{ errorMessage }}</span>
      </div>

      <div class="confirm-actions">
        <button
          type="button"
          class="bank-btn bank-btn-subtle"
          @click="currentStep = 1"
          :disabled="loading"
        >
          이전
        </button>
        <button
          type="button"
          class="bank-btn bank-btn-primary"
          @click="submitTransfer"
          :disabled="loading"
        >
          {{ loading ? '보내는 중…' : '보내기' }}
        </button>
      </div>
    </section>

    <!-- STEP 3: 송금 완료 -->
    <section v-else-if="currentStep === 3" class="bank-card transfer-card success-card">
      <div class="success-head">
        <CheckCircle2 :size="48" class="check-icon" />
        <h1 class="step-title">
          {{ recipientName }}님에게<br />
          <span class="num">{{ (amount || 0).toLocaleString() }}원</span>을 보냈어요
        </h1>
      </div>

      <dl class="summary-list">
        <div class="summary-row">
          <dt>출금 계좌</dt>
          <dd class="num">{{ formatAccNum(selectedAccount?.accountNumber) }}</dd>
        </div>
        <div class="summary-row">
          <dt>출금 후 잔액</dt>
          <dd class="num strong-val">{{ balanceAfter.toLocaleString() }}원</dd>
        </div>
      </dl>

      <button type="button" class="bank-btn bank-btn-primary full-btn" @click="$router.push('/')">
        확인
      </button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { AlertCircle, CheckCircle2 } from 'lucide-vue-next'
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
  max-width: 520px;
  margin-inline: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-track {
  width: 100%;
  height: 4px;
  background-color: #E5E8EB;
  border-radius: var(--radius-pill);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: var(--toss-blue);
  border-radius: var(--radius-pill);
  transition: width 0.25s ease;
}

.transfer-card {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.card-head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.step-title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.6px;
  line-height: 1.35;
  color: var(--text-primary);
}

.step-subtitle {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
}

.amount-emphasis {
  color: var(--toss-blue);
}

.transfer-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.source-account-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background-color: var(--surface-subtle);
}

.source-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.source-tag {
  font-size: 12px;
  font-weight: 700;
  color: var(--toss-blue-hover);
  background-color: var(--toss-blue-light);
  padding: 3px 8px;
  border-radius: 6px;
}

.source-num {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
}

.source-right {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.amount-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.amount-input {
  padding-right: 40px;
  font-size: 18px;
  font-weight: 700;
}

.currency-unit {
  position: absolute;
  right: 16px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-secondary);
}

.quick-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.chip-btn {
  padding: 7px 12px;
  border: none;
  border-radius: var(--radius-sm);
  background-color: var(--bg-color);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.chip-btn:hover {
  background-color: #E5E8EB;
  color: var(--text-primary);
}

.soft-chip {
  background-color: var(--toss-blue-light);
  color: var(--toss-blue-hover);
}

.soft-chip:hover {
  background-color: #D6E7FF;
  color: var(--toss-blue-hover);
}

.error-banner {
  background-color: var(--bank-red-light);
  color: var(--bank-red);
  padding: 12px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
  border-radius: var(--radius-md);
  background-color: var(--surface-subtle);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.summary-row dt {
  color: var(--text-secondary);
  font-weight: 500;
}

.summary-row dd {
  color: var(--text-primary);
  font-weight: 600;
}

.strong-val {
  color: var(--toss-blue);
  font-weight: 700;
}

.full-btn {
  width: 100%;
  padding: 15px;
}

.confirm-actions {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 10px;
}

.success-head {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.check-icon {
  color: var(--toss-blue);
}
</style>
