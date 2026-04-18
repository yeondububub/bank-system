package com.bank.system.infra

import org.springframework.data.jpa.repository.JpaRepository

interface PaymentHistoryJpaRepository : JpaRepository<PaymentHistoryJpaEntity, Long> {
}