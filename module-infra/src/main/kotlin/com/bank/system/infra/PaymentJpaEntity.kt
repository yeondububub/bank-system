package com.bank.system.infra

import com.bank.system.domain.PaymentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "payments")
class PaymentJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val orderId: String,

    @Column(nullable = false)
    val buyerId: Long,

    @Column(nullable = false)
    val amount: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus,

    @Column(nullable = false)
    val createdAt: java.time.LocalDateTime,

    @Column(nullable = false)
    var updatedAt: java.time.LocalDateTime
)