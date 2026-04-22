package com.bank.system.api.application

import com.bank.system.common.DistributedLock
import com.bank.system.domain.AccountRepository
import com.bank.system.domain.Payment
import com.bank.system.domain.PaymentHistory
import com.bank.system.domain.PaymentHistoryRepository
import com.bank.system.domain.PaymentRepository
import com.bank.system.domain.PaymentService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentFacade(
    private val paymentService: PaymentService,
    private val paymentRepository: PaymentRepository,
    private val paymentHistoryRepository: PaymentHistoryRepository,
    private val accountRepository: AccountRepository
) {

    @DistributedLock(key = "#orderId")
    @Transactional
    fun approvePayment(orderId: String): Payment {

        val payment = paymentRepository.findByOrderIdWithLock(orderId)
            ?: throw IllegalArgumentException("결제 정보를 찾을 수 없습니다. (orderId: $orderId)")

        val beforeStatus = payment.status

        val account = accountRepository.findByOwnerIdWithLock(payment.buyerId)
            ?: throw IllegalArgumentException("계좌 정보를 찾을 수 없습니다. (buyerId: ${payment.buyerId})")

        account.withdraw(payment.amount)
        payment.approve()

        accountRepository.save(account)
        val savedPayment = paymentRepository.save(payment)

        paymentHistoryRepository.save(
            PaymentHistory(
                paymentId = savedPayment.id!!,
                fromStatus = beforeStatus,
                toStatus = savedPayment.status
            )
        )

        return savedPayment
    }

    @Transactional
    fun createPayment(orderId: String, buyerId: Long, amount: Long): Payment {
        return paymentService.requestPayment(orderId, buyerId, amount)
    }
}