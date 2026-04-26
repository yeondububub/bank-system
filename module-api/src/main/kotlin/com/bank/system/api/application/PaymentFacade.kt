package com.bank.system.api.application

import com.bank.system.common.DistributedLock
import com.bank.system.domain.AccountRepository
import com.bank.system.domain.Payment
import com.bank.system.domain.PaymentHistory
import com.bank.system.domain.PaymentHistoryRepository
import com.bank.system.domain.PaymentRepository
import com.bank.system.domain.PaymentService
import com.bank.system.domain.PgPort
import com.bank.system.domain.event.PaymentCompletedEvent
import com.bank.system.domain.exception.PaymentNotFoundException
import com.bank.system.domain.exception.PgApprovalException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentFacade(
    private val paymentService: PaymentService,
    private val paymentRepository: PaymentRepository,
    private val paymentHistoryRepository: PaymentHistoryRepository,
    private val accountRepository: AccountRepository,
    private val pgPort: PgPort,
    private val eventPublisher: ApplicationEventPublisher
) {

    @DistributedLock(key = "#orderId")
    @Transactional
    fun approvePayment(orderId: String): Payment {

        val payment = paymentRepository.findByOrderIdWithLock(orderId)
            ?: throw PaymentNotFoundException(orderId)

        val beforeStatus = payment.status

        val account = accountRepository.findByOwnerIdWithLock(payment.buyerId)
            ?: throw IllegalArgumentException("계좌 정보를 찾을 수 없습니다. (buyerId: ${payment.buyerId})")

        // 1. 내부 DB 상태 변경 (잔액 차감 및 결제 승인 상태로 변경)
        account.withdraw(payment.amount)
        payment.approve()

        // 2. 외부 PG사 승인 요청
        val isSuccess = pgPort.pay(orderId, payment.amount)
        if (!isSuccess) {
            throw PgApprovalException()
        }

        accountRepository.save(account)
        val savedPayment = paymentRepository.save(payment)

        paymentHistoryRepository.save(
            PaymentHistory(
                paymentId = savedPayment.id!!,
                fromStatus = beforeStatus,
                toStatus = savedPayment.status
            )
        )

        // 3. 결제 완료 이벤트 발행
        eventPublisher.publishEvent(
            PaymentCompletedEvent(
                paymentId = savedPayment.id!!,
                orderId = savedPayment.orderId,
                buyerId = savedPayment.buyerId,
                amount = savedPayment.amount
            )
        )

        return savedPayment
    }

    @Transactional
    fun createPayment(orderId: String, buyerId: Long, amount: Long): Payment {
        return paymentService.requestPayment(orderId, buyerId, amount)
    }
}