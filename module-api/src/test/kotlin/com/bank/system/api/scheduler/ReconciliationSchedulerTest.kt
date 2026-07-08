package com.bank.system.api.scheduler

import com.bank.system.api.application.PaymentFacade
import com.bank.system.domain.*
import io.mockk.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ReconciliationSchedulerTest {

    private val paymentRepository = mockk<PaymentRepository>()
    private val paymentFacade = mockk<PaymentFacade>()
    private val pgPort = mockk<PgPort>()

    private val reconciliationScheduler = ReconciliationScheduler(
        paymentRepository = paymentRepository,
        paymentFacade = paymentFacade,
        pgPort = pgPort
    )

    @Test
    @DisplayName("APPROVING 상태인 결제가 있고 updatedAt이 10초 이상 지난 경우, PG사 상태가 SUCCESS이면 completeApproval을 호출한다")
    fun testReconcileSuccess() {
        val orderId = "ORD-REC-001"
        val stuckPayment = Payment(
            id = 1L,
            orderId = orderId,
            buyerId = 1004L,
            amount = 5000L,
            status = PaymentStatus.APPROVING,
            createdAt = LocalDateTime.now().minusSeconds(15),
            updatedAt = LocalDateTime.now().minusSeconds(15)
        )

        every { paymentRepository.findByStatus(PaymentStatus.APPROVING) } returns listOf(stuckPayment)
        every { pgPort.queryStatus(orderId) } returns PgStatusResponse(PgTransactionStatus.SUCCESS, "TX-123")
        every { paymentFacade.completeApproval(orderId) } returns stuckPayment

        reconciliationScheduler.reconcilePayments()

        verify(exactly = 1) { pgPort.queryStatus(orderId) }
        verify(exactly = 1) { paymentFacade.completeApproval(orderId) }
        verify(exactly = 0) { paymentFacade.failApproval(any()) }
    }

    @Test
    @DisplayName("APPROVING 상태인 결제가 있고 updatedAt이 10초 이상 지난 경우, PG사 상태가 FAILED이면 failApproval을 호출한다")
    fun testReconcileFailure() {
        val orderId = "ORD-REC-002"
        val stuckPayment = Payment(
            id = 2L,
            orderId = orderId,
            buyerId = 1004L,
            amount = 5000L,
            status = PaymentStatus.APPROVING,
            createdAt = LocalDateTime.now().minusSeconds(15),
            updatedAt = LocalDateTime.now().minusSeconds(15)
        )

        every { paymentRepository.findByStatus(PaymentStatus.APPROVING) } returns listOf(stuckPayment)
        every { pgPort.queryStatus(orderId) } returns PgStatusResponse(PgTransactionStatus.FAILED, null)
        every { paymentFacade.failApproval(orderId) } returns stuckPayment

        reconciliationScheduler.reconcilePayments()

        verify(exactly = 1) { pgPort.queryStatus(orderId) }
        verify(exactly = 1) { paymentFacade.failApproval(orderId) }
        verify(exactly = 0) { paymentFacade.completeApproval(any()) }
    }

    @Test
    @DisplayName("APPROVING 상태이지만 updatedAt이 10초를 경과하지 않은 결제는 대사 대상에서 제외한다")
    fun testReconcileSkipsRecent() {
        val orderId = "ORD-REC-003"
        val recentPayment = Payment(
            id = 3L,
            orderId = orderId,
            buyerId = 1004L,
            amount = 5000L,
            status = PaymentStatus.APPROVING,
            createdAt = LocalDateTime.now().minusSeconds(2),
            updatedAt = LocalDateTime.now().minusSeconds(2)
        )

        every { paymentRepository.findByStatus(PaymentStatus.APPROVING) } returns listOf(recentPayment)

        reconciliationScheduler.reconcilePayments()

        verify(exactly = 0) { pgPort.queryStatus(any()) }
        verify(exactly = 0) { paymentFacade.completeApproval(any()) }
        verify(exactly = 0) { paymentFacade.failApproval(any()) }
    }
}
