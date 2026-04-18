package com.bank.system.infra

import com.bank.system.domain.PaymentStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "payment_histories")
class PaymentHistoryJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val paymentId: Long,
    @Enumerated(EnumType.STRING)
    val fromStatus: PaymentStatus?,
    @Enumerated(EnumType.STRING)
    val toStatus: PaymentStatus,
    val createdAt: LocalDateTime
)