package domain

class PaymentService(
    private val paymentRepository: PaymentRepository
) {
    fun approvePayment(orderId: String): Payment {
        val payment = paymentRepository.findByOrderId(orderId)
            ?: throw IllegalArgumentException("결재 정보를 찾을 수 없습니다. (orderId: $orderId)")
        payment.approve()
        return paymentRepository.save(payment)
    }

    fun requestPayment(orderId: String, buyerId: Long, amount: Long): Payment {
        val newPayment = Payment(orderId = orderId, buyerId = buyerId, amount = amount)
        return paymentRepository.save(newPayment)
    }
}