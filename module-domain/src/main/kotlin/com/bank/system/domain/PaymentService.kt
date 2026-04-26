package com.bank.system.domain

import com.bank.system.domain.exception.PaymentNotFoundException
import com.bank.system.domain.exception.PgApprovalException

class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val pgPort: PgPort
) {
    fun approvePayment(orderId: String): Payment {
        val payment = paymentRepository.findByOrderId(orderId)
            ?: throw PaymentNotFoundException(orderId)

        val isSuccess = pgPort.pay(orderId, payment.amount)
        if (!isSuccess) {
            throw PgApprovalException()
        }

        payment.approve()
        return paymentRepository.save(payment)
    }

    fun requestPayment(orderId: String, buyerId: Long, amount: Long): Payment {
        val newPayment = Payment(orderId = orderId, buyerId = buyerId, amount = amount)
        return paymentRepository.save(newPayment)
    }
}