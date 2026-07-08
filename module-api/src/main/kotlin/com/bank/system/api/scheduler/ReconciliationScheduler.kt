package com.bank.system.api.scheduler

import com.bank.system.api.application.PaymentFacade
import com.bank.system.domain.PaymentRepository
import com.bank.system.domain.PaymentStatus
import com.bank.system.domain.PgPort
import com.bank.system.domain.PgTransactionStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ReconciliationScheduler(
    private val paymentRepository: PaymentRepository,
    private val paymentFacade: PaymentFacade,
    private val pgPort: PgPort
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 10000) // 10초마다 실행
    fun reconcilePayments() {
        val stuckPayments = paymentRepository.findByStatus(PaymentStatus.APPROVING)
        if (stuckPayments.isEmpty()) {
            return
        }

        // 최종 업데이트 후 10초 이상 경과하여 stuck된 건들 대상 선별
        val cutoffTime = LocalDateTime.now().minusSeconds(10)
        val targets = stuckPayments.filter { it.updatedAt.isBefore(cutoffTime) }

        if (targets.isEmpty()) {
            return
        }

        log.info("💳 [Reconciliation] 대기 상태에서 멈춘 결제건 {}건을 발견하여 대사를 시작합니다.", targets.size)

        for (payment in targets) {
            try {
                log.info("🔍 [Reconciliation] 결제 대사 진행 중 (orderId: {}, buyerId: {}, amount: {})",
                    payment.orderId, payment.buyerId, payment.amount)
                
                val pgStatus = pgPort.queryStatus(payment.orderId)
                when (pgStatus.status) {
                    PgTransactionStatus.SUCCESS -> {
                        log.info("✅ [Reconciliation] PG사 결제 승인 확인 완료. 결제 완료 처리합니다. (orderId: {})", payment.orderId)
                        paymentFacade.completeApproval(payment.orderId)
                    }
                    PgTransactionStatus.FAILED, PgTransactionStatus.NOT_FOUND -> {
                        log.warn("🚨 [Reconciliation] PG사 결제 실패 또는 없음 확인. 결제 실패 및 보상(환불) 처리합니다. (orderId: {})", payment.orderId)
                        paymentFacade.failApproval(payment.orderId)
                    }
                }
            } catch (e: Exception) {
                log.error("❌ [Reconciliation] 결제 대사 처리 중 오류 발생 (orderId: {})", payment.orderId, e)
            }
        }
    }
}
