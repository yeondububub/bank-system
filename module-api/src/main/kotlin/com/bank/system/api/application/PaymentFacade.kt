package com.bank.system.api.application

import com.bank.system.common.DistributedLock
import com.bank.system.domain.Payment
import com.bank.system.domain.PgPort
import com.bank.system.domain.exception.PgApprovalException
import org.springframework.stereotype.Service

@Service
class PaymentFacade(
    private val paymentTransactionService: PaymentTransactionService,
    private val pgPort: PgPort
) {

    @DistributedLock(key = "#orderId")
    fun approvePayment(orderId: String): Payment {
        // 1단계: 승인 준비 (출금 및 상태 변경: PENDING -> APPROVING)
        val payment = paymentTransactionService.prepareApproval(orderId)

        // 2단계: 외부 PG사 결제 승인 요청 (트랜잭션 밖에서 실행)
        val isSuccess = try {
            pgPort.pay(orderId, payment.amount)
        } catch (e: Exception) {
            // PG사 호출 에러 시 보상 트랜잭션 수행 (환불 및 FAILED 변경)
            paymentTransactionService.failApproval(orderId)
            throw PgApprovalException()
        }

        // 3단계: 최종 결과 반영 (성공 시 SUCCESS, 실패 시 보상 트랜잭션)
        return if (isSuccess) {
            paymentTransactionService.completeApproval(orderId)
        } else {
            paymentTransactionService.failApproval(orderId)
            throw PgApprovalException()
        }
    }

    fun prepareApproval(orderId: String): Payment {
        return paymentTransactionService.prepareApproval(orderId)
    }

    fun completeApproval(orderId: String): Payment {
        return paymentTransactionService.completeApproval(orderId)
    }

    fun failApproval(orderId: String): Payment {
        return paymentTransactionService.failApproval(orderId)
    }

    fun createPayment(orderId: String, buyerId: Long, amount: Long): Payment {
        return paymentTransactionService.requestPayment(orderId, buyerId, amount)
    }

    @DistributedLock(key = "#orderId")
    fun cancelPayment(orderId: String): Payment {
        return paymentTransactionService.cancelPayment(orderId)
    }
}