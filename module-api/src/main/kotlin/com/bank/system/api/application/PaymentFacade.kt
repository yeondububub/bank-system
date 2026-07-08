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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy

@Service
class PaymentFacade(
    private val paymentService: PaymentService,
    private val paymentRepository: PaymentRepository,
    private val paymentHistoryRepository: PaymentHistoryRepository,
    private val accountRepository: AccountRepository,
    private val pgPort: PgPort,
    private val eventPublisher: ApplicationEventPublisher
) {

    @Autowired
    @Lazy
    private lateinit var self: PaymentFacade

    @DistributedLock(key = "#orderId")
    fun approvePayment(orderId: String): Payment {
        // 1단계: 승인 준비 (출금 및 상태 변경: PENDING -> APPROVING)
        val payment = self.prepareApproval(orderId)

        // 2단계: 외부 PG사 결제 승인 요청 (트랜잭션 밖에서 실행)
        val isSuccess = try {
            pgPort.pay(orderId, payment.amount)
        } catch (e: Exception) {
            // PG사 호출 에러 시 보상 트랜잭션 수행 (환불 및 FAILED 변경)
            self.failApproval(orderId)
            throw PgApprovalException()
        }

        // 3단계: 최종 결과 반영 (성공 시 SUCCESS, 실패 시 보상 트랜잭션)
        return if (isSuccess) {
            self.completeApproval(orderId)
        } else {
            self.failApproval(orderId)
            throw PgApprovalException()
        }
    }

    @Transactional
    fun prepareApproval(orderId: String): Payment {
        val payment = paymentRepository.findByOrderIdWithLock(orderId)
            ?: throw PaymentNotFoundException(orderId)

        val beforeStatus = payment.status

        val account = accountRepository.findByOwnerIdWithLock(payment.buyerId)
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

        val account = accountRepository.findByOwnerIdWithLock(payment.buyerId)
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

        return savedPayment
    }

    @Transactional
    fun createPayment(orderId: String, buyerId: Long, amount: Long): Payment {
        return paymentService.requestPayment(orderId, buyerId, amount)
    }

    @DistributedLock(key = "#orderId")
    @Transactional
    fun cancelPayment(orderId: String): Payment {
        val payment = paymentRepository.findByOrderIdWithLock(orderId)
            ?: throw PaymentNotFoundException(orderId)

        val beforeStatus = payment.status

        val account = accountRepository.findByOwnerIdWithLock(payment.buyerId)
            ?: throw IllegalArgumentException("계좌 정보를 찾을 수 없습니다. (buyerId: ${payment.buyerId})")

        // 1. 내부 DB 상태 변경 (결제 취소 상태로 변경)
        payment.cancel()

        // 2. 외부 PG사 취소 요청
        val isSuccess = pgPort.cancel(orderId, payment.amount)
        if (!isSuccess) {
            // PG사 취소 실패 시 예외를 던져서 트랜잭션 롤백 (도메인 상태 복구)
            // (실무에서는 재시도 로직이나 수동 처리 큐로 넘길 수 있음)
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

        // 4. 결제 취소 이벤트 발행
        eventPublisher.publishEvent(
            com.bank.system.domain.event.PaymentCanceledEvent(
                paymentId = savedPayment.id!!,
                orderId = savedPayment.orderId,
                buyerId = savedPayment.buyerId,
                amount = savedPayment.amount
            )
        )

        return savedPayment
    }
}