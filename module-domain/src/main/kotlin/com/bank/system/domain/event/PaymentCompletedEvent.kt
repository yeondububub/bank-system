package com.bank.system.domain.event

data class PaymentCompletedEvent(
    val paymentId: Long,
    val orderId: String,
    val buyerId: Long,
    val amount: Long
)
