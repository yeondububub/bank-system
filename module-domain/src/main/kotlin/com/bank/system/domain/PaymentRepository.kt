package com.bank.system.domain

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findById(id: Long): Payment?
    fun findByOrderId(orderId: String): Payment?
    fun findByOrderIdWithLock(orderId: String): Payment?
    fun findByStatus(status: PaymentStatus): List<Payment>
}