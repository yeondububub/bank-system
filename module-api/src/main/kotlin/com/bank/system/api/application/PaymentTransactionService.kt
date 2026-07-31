package com.bank.system.api.application

import com.bank.system.common.util.SnowflakeIdGenerator
import com.bank.system.domain.AccountRepository
import com.bank.system.domain.Payment
import com.bank.system.domain.PaymentHistory
import com.bank.system.domain.PaymentHistoryRepository
import com.bank.system.domain.PaymentRepository
import com.bank.system.domain.PaymentService
import com.bank.system.domain.PgPort
import com.bank.system.domain.event.PaymentCompletedEvent
import com.bank.system.domain.event.PaymentCanceledEvent
import com.bank.system.domain.exception.PaymentNotFoundException
import com.bank.system.domain.AccountTransaction
import com.bank.system.domain.AccountTransactionRepository
import com.bank.system.domain.TransactionType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentTransactionService(
    private val paymentService: PaymentService,
    private val paymentRepository: PaymentRepository,
    private val paymentHistoryRepository: PaymentHistoryRepository,
    private val accountRepository: AccountRepository,
    private val accountTransactionRepository: AccountTransactionRepository,
    private val pgPort: PgPort,
    private val eventPublisher: ApplicationEventPublisher,
    private val snowflakeIdGenerator: SnowflakeIdGenerator
) {

    @Transactional
    fun prepareApproval(orderId: String): Payment {
        val payment = paymentRepository.findByOrderIdWithLock(orderId)
            ?: throw PaymentNotFoundException(orderId)

        val beforeStatus = payment.status

        val account = accountRepository.findPrimaryByOwnerId(payment.buyerId)
            ?: accountRepository.findAllByOwnerId(payment.buyerId).firstOrNull()
            ?: throw IllegalArgumentException("계좌 정보를 찾을 수 없습니다. (buyerId: ${payment.buyerId})")

        // 잔액 차감 및 승인 대기 상태로 변경
        account.withdraw(payment.amount)
        payment.prepareApproval()

        accountRepository.save(account)
        val savedPayment = paymentRepository.save(payment)

        paymentHistoryRepository.save(
            PaymentHistory(
                paymentId = savedPayment.id!!,
                fromStatus = beforeStatus,
                toStatus = savedPayment.status
            )
        )

        // 계좌 거래 내역 기록 (PAYMENT)
        accountTransactionRepository.save(
            AccountTransaction(
                accountNumber = account.accountNumber,
                type = TransactionType.PAYMENT,
                amount = payment.amount,
                balanceAfter = account.balance,
                memo = "결제 승인 요청 (${payment.orderId})"
            )
        )

        return savedPayment
    }

    @Transactional
    fun completeApproval(orderId: String): Payment {
        val payment = paymentRepository.findByOrderIdWithLock(orderId)
            ?: throw PaymentNotFoundException(orderId)

        val beforeStatus = payment.status

        payment.approve()
        val savedPayment = paymentRepository.save(payment)

        paymentHistoryRepository.save(
            PaymentHistory(
                paymentId = savedPayment.id!!,
                fromStatus = beforeStatus,
                toStatus = savedPayment.status
            )
        )

        // 결제 완료 이벤트 발행
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
    fun failApproval(orderId: String): Payment {
        val payment = paymentRepository.findByOrderIdWithLock(orderId)
            ?: throw PaymentNotFoundException(orderId)

        val beforeStatus = payment.status

        val account = accountRepository.findPrimaryByOwnerId(payment.buyerId)
            ?: accountRepository.findAllByOwnerId(payment.buyerId).firstOrNull()
            ?: throw IllegalArgumentException("계좌 정보를 찾을 수 없습니다. (buyerId: ${payment.buyerId})")

        // 선출금 금액 입금(환불) 및 FAILED 처리
        account.deposit(payment.amount)
        payment.fail()

        accountRepository.save(account)
        val savedPayment = paymentRepository.save(payment)

        paymentHistoryRepository.save(
            PaymentHistory(
                paymentId = savedPayment.id!!,
                fromStatus = beforeStatus,
                toStatus = savedPayment.status
            )
        )

        // 계좌 거래 내역 기록 (REFUND)
        accountTransactionRepository.save(
            AccountTransaction(
                accountNumber = account.accountNumber,
                type = TransactionType.REFUND,
                amount = payment.amount,
                balanceAfter = account.balance,
                memo = "결제 승인 실패 환불 (${payment.orderId})"
            )
        )

        return savedPayment
    }

    @Transactional
    fun cancelPayment(orderId: String): Payment {
        val payment = paymentRepository.findByOrderIdWithLock(orderId)
            ?: throw PaymentNotFoundException(orderId)

        val beforeStatus = payment.status

        val account = accountRepository.findPrimaryByOwnerId(payment.buyerId)
            ?: accountRepository.findAllByOwnerId(payment.buyerId).firstOrNull()
            ?: throw IllegalArgumentException("계좌 정보를 찾을 수 없습니다. (buyerId: ${payment.buyerId})")

        // 1. 내부 DB 상태 변경 (결제 취소 상태로 변경)
        payment.cancel()

        // 2. 외부 PG사 취소 요청
        val isSuccess = pgPort.cancel(orderId, payment.amount)
        if (!isSuccess) {
            // PG사 취소 실패 시 예외를 던져서 트랜잭션 롤백 (도메인 상태 복구)
            throw IllegalStateException("PG사 결제 취소 요청에 실패했습니다.")
        }

        // 3. 결제 금액 환불
        account.deposit(payment.amount)

        accountRepository.save(account)
        val savedPayment = paymentRepository.save(payment)

        paymentHistoryRepository.save(
            PaymentHistory(
                paymentId = savedPayment.id!!,
                fromStatus = beforeStatus,
                toStatus = savedPayment.status
            )
        )

        // 계좌 거래 내역 기록 (REFUND)
        accountTransactionRepository.save(
            AccountTransaction(
                accountNumber = account.accountNumber,
                type = TransactionType.REFUND,
                amount = payment.amount,
                balanceAfter = account.balance,
                memo = "결제 취소 환불 (${payment.orderId})"
            )
        )

        // 4. 결제 취소 이벤트 발행
        eventPublisher.publishEvent(
            PaymentCanceledEvent(
                paymentId = savedPayment.id!!,
                orderId = savedPayment.orderId,
                buyerId = savedPayment.buyerId,
                amount = savedPayment.amount
            )
        )

        return savedPayment
    }

    @Transactional
    fun requestPayment(orderId: String, buyerId: Long, amount: Long): Payment {
        val newPayment = Payment(
            id = snowflakeIdGenerator.nextId(),
            orderId = orderId,
            buyerId = buyerId,
            amount = amount
        )
        return paymentRepository.save(newPayment)
    }
}
