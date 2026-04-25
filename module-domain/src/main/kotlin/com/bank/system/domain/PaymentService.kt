package com.bank.system.domain

class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val pgPort: PgPort
) {
    fun approvePayment(orderId: String): Payment {
        val payment = paymentRepository.findByOrderId(orderId)
            ?: throw IllegalArgumentException("결재 정보를 찾을 수 없습니다. (orderId: $orderId)")

        val isSuccess = pgPort.pay(orderId, payment.amount)
        if (!isSuccess) {
            throw IllegalStateException("PG사 결제 승인 요청에 실패했습니다.")
        }

        payment.approve()
        return paymentRepository.save(payment)
    }

    fun requestPayment(orderId: String, buyerId: Long, amount: Long): Payment {
        val newPayment = Payment(orderId = orderId, buyerId = buyerId, amount = amount)
        return paymentRepository.save(newPayment)
    }
}