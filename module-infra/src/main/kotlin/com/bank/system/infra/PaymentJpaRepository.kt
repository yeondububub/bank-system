package com.bank.system.infra

import org.springframework.data.jpa.repository.JpaRepository

interface PaymentJpaRepository : JpaRepository<PaymentJpaEntity, Long> {
    fun findByOrderId(orderId: String): PaymentJpaEntity?
}