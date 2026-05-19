package com.bank.system.domain.event

data class PaymentCanceledEvent(
    val paymentId: Long,
    val orderId: String,
    val buyerId: Long,
    val amount: Long
)
