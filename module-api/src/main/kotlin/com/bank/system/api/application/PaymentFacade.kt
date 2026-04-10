package com.bank.system.api.application

import com.bank.system.domain.Payment
import com.bank.system.domain.PaymentRepository
import com.bank.system.domain.PaymentService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentFacade(
    private val paymentService: PaymentService,
    private val paymentRepository: PaymentRepository
) {

    @Transactional
    fun approvePayment(orderId: String): Payment {

        val payment = paymentRepository.findByOrderIdWithLock(orderId)
            ?: throw IllegalArgumentException("결제 정보를 찾을 수 없습니다. (orderId: $orderId)")

        payment.approve()

        return paymentRepository.save(payment)
    }

    @Transactional
    fun createPayment(orderId: String, buyerId: Long, amount: Long): Payment {
        return paymentService.requestPayment(orderId, buyerId, amount)
    }
}