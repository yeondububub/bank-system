package com.bank.system.domain

import java.time.LocalDateTime

class PaymentHistory(
    val id: Long? = null,
    val paymentId: Long,
    val fromStatus: PaymentStatus?,
    val toStatus: PaymentStatus,
    val createdAt: LocalDateTime = LocalDateTime.now()
)