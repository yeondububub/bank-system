<template>
  <div class="history-view">
    <div class="header-nav">
      <button class="back-btn" @click="$router.push('/')">
        <ArrowLeft :size="22" />
      </button>
      <h2 class="nav-title">결제 이력 조회</h2>
    </div>

    <!-- History Filter Cards -->
    <div class="bank-card">
      <div class="history-list">
        <div v-for="item in mockHistories" :key="item.id" class="history-item">
          <div class="item-left">
            <div class="item-icon" :class="item.status.toLowerCase()">
              <CheckCircle2 v-if="item.status === 'SUCCESS'" :size="20" />
              <Clock v-else-if="item.status === 'PENDING' || item.status === 'APPROVING'" :size="20" />
              <XCircle v-else :size="20" />
            </div>
            <div>
              <div class="item-order">{{ item.orderId }}</div>
              <div class="item-date">{{ item.createdAt }}</div>
            </div>
          </div>
          <div class="item-right">
            <div class="item-amount">{{ item.amount.toLocaleString() }} 원</div>
            <span class="bank-badge" :class="getBadgeClass(item.status)">
              {{ item.status }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowLeft, CheckCircle2, Clock, XCircle } from 'lucide-vue-next'

const mockHistories = ref([
  { id: 1, orderId: 'ORD-982141', amount: 50000, status: 'SUCCESS', createdAt: '2026-07-25 21:10:00' },
  { id: 2, orderId: 'ORD-881240', amount: 12000, status: 'APPROVING', createdAt: '2026-07-25 21:08:12' },
  { id: 3, orderId: 'ORD-712399', amount: 33000, status: 'CANCELED', createdAt: '2026-07-25 20:30:45' },
  { id: 4, orderId: 'ORD-612091', amount: 99000, status: 'FAILED', createdAt: '2026-07-25 19:15:22' }
])

const getBadgeClass = (status: string) => {
  switch (status) {
    case 'SUCCESS': return 'bank-badge-success'
    case 'PENDING':
    case 'APPROVING': return 'bank-badge-pending'
    default: return 'bank-badge-failed'
  }
}
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

.history-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.history-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.item-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-icon.success { background-color: var(--bank-green-light); color: var(--bank-green); }
.item-icon.approving, .item-icon.pending { background-color: var(--bank-blue-light); color: var(--bank-blue); }
.item-icon.canceled, .item-icon.failed { background-color: var(--bank-red-light); color: var(--bank-red); }

.item-order {
  font-size: 15px;
  font-weight: 600;
}

.item-date {
  font-size: 12px;
  color: var(--text-tertiary);
}

.item-right {
  text-align: right;
}

.item-amount {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 4px;
}
</style>
