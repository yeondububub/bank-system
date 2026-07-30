<template>
  <div class="payment-view">
    <div class="header-nav">
      <button class="back-btn" @click="$router.push('/')">
        <ArrowLeft :size="22" />
      </button>
      <h2 class="nav-title">결제 승인 요청</h2>
    </div>

    <div class="bank-card">
      <h3 class="bank-title">얼마를 결제할까요?</h3>
      <p class="bank-subtitle">Idempotency-Key로 중복 결제가 안전하게 방지됩니다.</p>

      <form @submit.prevent="submitPayment" class="payment-form">
        <div class="bank-input-group">
          <label class="bank-label">주문 번호 (Order ID)</label>
          <input
            v-model="orderId"
            type="text"
            class="bank-input"
            placeholder="주문번호 입력 (예: ORD-1001)"
            required
          />
        </div>

        <div class="bank-input-group">
          <label class="bank-label">구매자 ID (Buyer ID)</label>
          <input
            v-model.number="buyerId"
            type="number"
            class="bank-input"
            placeholder="구매자 ID"
            required
            @change="fetchAccounts"
          />
        </div>

        <!-- 출금할 계좌 선택 (다계좌 지원) -->
        <div v-if="userAccounts.length > 0" class="bank-input-group">
          <label class="bank-label">출금할 계좌 선택 (보유 계좌 목록)</label>
          <select v-model="selectedAccountId" class="bank-input account-select">
            <option 
              v-for="acc in userAccounts" 
              :key="acc.id" 
              :value="acc.id"
              :disabled="acc.status !== 'ACTIVE'"
            >
              {{ acc.isPrimary ? '[대표]' : '[서브]' }} {{ formatAccNum(acc.accountNumber) }} (잔액: {{ acc.balance.toLocaleString() }}원 / {{ acc.status }})
            </option>
          </select>
        </div>

        <div class="bank-input-group">
          <label class="bank-label">결제 금액 (원)</label>
          <input
            v-model.number="amount"
            type="number"
            class="bank-input amount-input"
            placeholder="금액 입력"
            required
          />
        </div>

        <div class="idempotency-box">
          <Lock :size="16" />
          <span>멱등성 키: {{ idempotencyKey.slice(0, 18) }}...</span>
        </div>

        <button
          type="submit"
          class="bank-btn bank-btn-primary"
          :disabled="loading"
        >
          <span v-if="!loading">결제하기</span>
          <span v-else>결제 승인 중...</span>
        </button>
      </form>
    </div>

    <!-- Alert / Result Modal Card -->
    <div v-if="resultMessage" class="bank-card result-card" :class="resultStatus">
      <h4>{{ resultStatus === 'success' ? '결제 성공!' : '결제 실패' }}</h4>
      <p>{{ resultMessage }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ArrowLeft, Lock } from 'lucide-vue-next'
import axios from 'axios'

const orderId = ref(`ORD-${Date.now().toString().slice(-6)}`)
const buyerId = ref(1)
const amount = ref(50000)
const selectedAccountId = ref<number | null>(null)
const userAccounts = ref<any[]>([])

const idempotencyKey = ref(crypto.randomUUID())
const loading = ref(false)
const resultMessage = ref('')
const resultStatus = ref<'success' | 'failed'>('success')

const formatAccNum = (numStr: string) => {
  if (!numStr) return ''
  const str = String(numStr)
  if (str.length === 13) {
    return `${str.slice(0, 3)}-${str.slice(3, 7)}-${str.slice(7, 11)}-${str.slice(11)}`
  }
  return str
}

const fetchAccounts = async () => {
  if (!buyerId.value) return

  try {
    const res = await axios.get(`/api/v1/accounts/user/${buyerId.value}`)
    userAccounts.value = res.data
    // 대표 계좌(isPrimary = true)를 기본 선택값으로 지정
    const primary = res.data.find((a: any) => a.isPrimary)
    if (primary) {
      selectedAccountId.value = primary.id
    } else if (res.data.length > 0) {
      selectedAccountId.value = res.data[0].id
    }
  } catch (e) {
    userAccounts.value = []
  }
}

const submitPayment = async () => {
  loading.value = true
  resultMessage.value = ''

  try {
    const response = await axios.post('/api/v1/payments/approve', {
      orderId: orderId.value,
      buyerId: buyerId.value,
      amount: amount.value,
      fromAccountId: selectedAccountId.value
    }, {
      headers: {
        'Idempotency-Key': idempotencyKey.value
      }
    })

    resultStatus.value = 'success'
    resultMessage.value = `주문 [${response.data.orderId}] 승인이 완료되었습니다. (상태: ${response.data.status})`
  } catch (error: any) {
    resultStatus.value = 'failed'
    resultMessage.value = error.response?.data?.message || '결제 승인 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      buyerId.value = user.id
    } catch (e) {}
  }
  fetchAccounts()
})
</script>

<style scoped>
.header-nav {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.back-btn {
  background: none;
  border: none;
  color: var(--text-primary);
  cursor: pointer;
}

.nav-title {
  font-size: 20px;
  font-weight: 700;
}

.bank-input-group {
  margin-bottom: 16px;
}

.bank-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.account-select {
  background: #0b0e14;
  color: #ffffff;
  font-weight: 600;
}

.payment-form {
  margin-top: 24px;
}

.amount-input {
  font-size: 20px;
  font-weight: 700;
  color: var(--bank-blue);
}

.idempotency-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: var(--surface-subtle);
  padding: 10px 14px;
  border-radius: var(--border-radius-sm);
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 20px;
}

.result-card.success {
  border-left: 4px solid var(--bank-green);
  background-color: var(--bank-green-light);
}

.result-card.failed {
  border-left: 4px solid var(--bank-red);
  background-color: var(--bank-red-light);
}
</style>
