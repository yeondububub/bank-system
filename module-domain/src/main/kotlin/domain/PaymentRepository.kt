package domain

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findById(id: Long): Payment?
    fun findByOrderId(orderId: String): Payment?
}