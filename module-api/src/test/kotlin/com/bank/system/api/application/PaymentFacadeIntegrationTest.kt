package com.bank.system.api.application

import com.bank.system.domain.PaymentStatus
import com.bank.system.domain.PgPort
import com.bank.system.domain.exception.PgApprovalException
import com.bank.system.infra.AccountJpaEntity
import com.bank.system.infra.AccountJpaRepository
import com.bank.system.infra.OutboxJpaRepository
import com.bank.system.infra.PaymentHistoryJpaRepository
import com.bank.system.infra.PaymentJpaRepository
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class PaymentFacadeIntegrationTest @Autowired constructor(
    private val paymentFacade: PaymentFacade,
    private val paymentJpaRepository: PaymentJpaRepository,
    private val accountJpaRepository: AccountJpaRepository,
    private val paymentHistoryJpaRepository: PaymentHistoryJpaRepository,
    private val outboxJpaRepository: OutboxJpaRepository
) {

    @MockkBean
    private lateinit var pgPort: PgPort

    @AfterEach
    fun tearDown() {
        paymentJpaRepository.deleteAllInBatch()
        paymentHistoryJpaRepository.deleteAllInBatch()
        accountJpaRepository.deleteAllInBatch()
        outboxJpaRepository.deleteAllInBatch()
    }

    @Test
    @DisplayName("결제 생성 요청 시 PENDING 상태의 결제가 데이터베이스에 저장된다")
    fun testCreatePayment() {
        val orderId = "ORD-INT-001"
        val buyerId = 1004L
        val amount = 10000L

        val payment = paymentFacade.createPayment(orderId, buyerId, amount)

        assertThat(payment.id).isNotNull
        assertThat(payment.orderId).isEqualTo(orderId)
        assertThat(payment.status).isEqualTo(PaymentStatus.PENDING)

        val savedEntity = paymentJpaRepository.findByOrderId(orderId)
        assertThat(savedEntity).isNotNull
        assertThat(savedEntity!!.status).isEqualTo(PaymentStatus.PENDING)
    }

    @Test
    @DisplayName("결제 승인 성공 시 금액이 차감되고 SUCCESS 상태가 되며 Outbox 메시지가 저장된다")
    fun testApprovePaymentSuccess() {
        val orderId = "ORD-INT-002"
        val buyerId = 1004L
        val amount = 30000L

        accountJpaRepository.save(AccountJpaEntity(ownerId = buyerId, balance = 100000L))
        paymentFacade.createPayment(orderId, buyerId, amount)

        every { pgPort.pay(orderId, amount) } returns true

        val approvedPayment = paymentFacade.approvePayment(orderId)

        assertThat(approvedPayment.status).isEqualTo(PaymentStatus.SUCCESS)

        // DB 검증
        val updatedAccount = accountJpaRepository.findByOwnerId(buyerId)
        assertThat(updatedAccount!!.balance).isEqualTo(70000L) // 100,000 - 30,000

        val updatedPayment = paymentJpaRepository.findByOrderId(orderId)
        assertThat(updatedPayment!!.status).isEqualTo(PaymentStatus.SUCCESS)

        // Outbox 검증
        val outboxMessages = outboxJpaRepository.findAll()
        assertThat(outboxMessages).hasSize(1)
        assertThat(outboxMessages[0].aggregateId).isEqualTo(approvedPayment.id.toString())
        assertThat(outboxMessages[0].eventType).isEqualTo("PaymentCompletedEvent")
    }

    @Test
    @DisplayName("PG사 승인 실패 시 예외가 발생하고 트랜잭션이 롤백되어 금액이 차감되지 않고 Outbox 메시지도 저장되지 않는다")
    fun testApprovePaymentPgFailureRollback() {
        val orderId = "ORD-INT-003"
        val buyerId = 1004L
        val amount = 30000L

        accountJpaRepository.save(AccountJpaEntity(ownerId = buyerId, balance = 100000L))
        paymentFacade.createPayment(orderId, buyerId, amount)

        every { pgPort.pay(orderId, amount) } returns false

        assertThrows(PgApprovalException::class.java) {
            paymentFacade.approvePayment(orderId)
        }

        // DB 검증 (계좌는 환불/롤백되어 100,000 유지, 결제 상태는 FAILED로 변경)
        val updatedAccount = accountJpaRepository.findByOwnerId(buyerId)
        assertThat(updatedAccount!!.balance).isEqualTo(100000L)

        val updatedPayment = paymentJpaRepository.findByOrderId(orderId)
        assertThat(updatedPayment!!.status).isEqualTo(PaymentStatus.FAILED)

        // Outbox 검증
        val outboxMessages = outboxJpaRepository.findAll()
        assertThat(outboxMessages).isEmpty() // 실패하여 Outbox 저장되지 않음
    }

    @Test
    @DisplayName("결제 취소 성공 시 금액이 환불되고 CANCELLED 상태가 되며 Outbox 메시지가 저장된다")
    fun testCancelPaymentSuccess() {
        val orderId = "ORD-INT-004"
        val buyerId = 1004L
        val amount = 20000L

        accountJpaRepository.save(AccountJpaEntity(ownerId = buyerId, balance = 80000L))
        
        // 결제 생성 및 승인 완료 처리
        paymentFacade.createPayment(orderId, buyerId, amount)
        every { pgPort.pay(orderId, amount) } returns true
        paymentFacade.approvePayment(orderId)

        // Outbox 비우기 (승인 시 발생한 outbox 메시지 제거)
        outboxJpaRepository.deleteAllInBatch()

        // PG 취소 성공 모킹
        every { pgPort.cancel(orderId, amount) } returns true

        val cancelledPayment = paymentFacade.cancelPayment(orderId)

        assertThat(cancelledPayment.status).isEqualTo(PaymentStatus.CANCELLED)

        // DB 검증
        val updatedAccount = accountJpaRepository.findByOwnerId(buyerId)
        assertThat(updatedAccount!!.balance).isEqualTo(80000L) // 환불되어 80,000 (80,000 - 20,000 + 20,000)

        val updatedPayment = paymentJpaRepository.findByOrderId(orderId)
        assertThat(updatedPayment!!.status).isEqualTo(PaymentStatus.CANCELLED)

        // Outbox 검증
        val outboxMessages = outboxJpaRepository.findAll()
        assertThat(outboxMessages).hasSize(1)
        assertThat(outboxMessages[0].aggregateId).isEqualTo(cancelledPayment.id.toString())
        assertThat(outboxMessages[0].eventType).isEqualTo("PaymentCanceledEvent")
    }
}
