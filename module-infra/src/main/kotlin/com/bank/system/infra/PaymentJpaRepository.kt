package com.bank.system.infra

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PaymentJpaRepository : JpaRepository<PaymentJpaEntity, Long> {
    fun findByOrderId(orderId: String): PaymentJpaEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.orderId = :orderId")
    fun findByOrderIdWithLock(@Param("orderId") orderId: String): PaymentJpaEntity?

    fun findByStatus(status: com.bank.system.domain.PaymentStatus): List<PaymentJpaEntity>
}