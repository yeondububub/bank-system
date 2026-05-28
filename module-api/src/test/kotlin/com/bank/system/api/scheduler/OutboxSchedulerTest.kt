package com.bank.system.api.scheduler

import com.bank.system.api.listener.PaymentNotificationListener
import com.bank.system.domain.OutboxMessage
import com.bank.system.domain.OutboxMessageStatus
import com.bank.system.domain.OutboxRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OutboxSchedulerTest {

    private val outboxRepository = mockk<OutboxRepository>(relaxed = true)
    private val paymentNotificationListener = mockk<PaymentNotificationListener>(relaxed = true)
    private val objectMapper = ObjectMapper()

    private val outboxScheduler = OutboxScheduler(
        outboxRepository = outboxRepository,
        paymentNotificationListener = paymentNotificationListener,
        objectMapper = objectMapper
    )

    @Test
    @DisplayName("성공적으로 처리된 Outbox 메시지는 PROCESSED 상태로 마크된다")
    fun testProcessOutboxMessageSuccess() {
        // given
        val message = OutboxMessage(
            id = 1L,
            aggregateType = "PAYMENT",
            aggregateId = "10",
            eventType = "PaymentCompletedEvent",
            payload = """{"buyerId":1004,"orderId":"ORD-001","amount":5000}""",
            status = OutboxMessageStatus.PENDING,
            retryCount = 0
        )
        every { outboxRepository.findPendingMessages(any()) } returns listOf(message)

        // when
        outboxScheduler.processOutboxMessages()

        // then
        assertThat(message.status).isEqualTo(OutboxMessageStatus.PROCESSED)
        assertThat(message.processedAt).isNotNull
        verify(exactly = 1) { outboxRepository.saveAll(listOf(message)) }
        verify(exactly = 1) { paymentNotificationListener.sendPaymentCompletedNotification(1004L, "ORD-001", 5000L) }
    }

    @Test
    @DisplayName("메시지 처리 중 예외 발생 시 retryCount가 증가하고 PENDING 상태를 유지한다")
    fun testProcessOutboxMessageFailureIncrementsRetryCount() {
        // given
        val message = OutboxMessage(
            id = 1L,
            aggregateType = "PAYMENT",
            aggregateId = "10",
            eventType = "PaymentCompletedEvent",
            payload = """{"buyerId":1004,"orderId":"ORD-001","amount":5000}""",
            status = OutboxMessageStatus.PENDING,
            retryCount = 1
        )
        every { outboxRepository.findPendingMessages(any()) } returns listOf(message)
        every { 
            paymentNotificationListener.sendPaymentCompletedNotification(any(), any(), any()) 
        } throws RuntimeException("Connection timeout")

        // when
        outboxScheduler.processOutboxMessages()

        // then
        assertThat(message.status).isEqualTo(OutboxMessageStatus.PENDING)
        assertThat(message.retryCount).isEqualTo(2)
        verify(exactly = 1) { outboxRepository.saveAll(listOf(message)) }
    }

    @Test
    @DisplayName("메시지 처리 실패 횟수가 5회에 도달하면 FAILED 상태로 처리된다")
    fun testProcessOutboxMessageExceedsMaxRetriesMarkAsFailed() {
        // given
        val message = OutboxMessage(
            id = 1L,
            aggregateType = "PAYMENT",
            aggregateId = "10",
            eventType = "PaymentCompletedEvent",
            payload = """{"buyerId":1004,"orderId":"ORD-001","amount":5000}""",
            status = OutboxMessageStatus.PENDING,
            retryCount = 4 // 4 times already failed, this run makes it 5
        )
        every { outboxRepository.findPendingMessages(any()) } returns listOf(message)
        every { 
            paymentNotificationListener.sendPaymentCompletedNotification(any(), any(), any()) 
        } throws RuntimeException("Connection timeout")

        // when
        outboxScheduler.processOutboxMessages()

        // then
        assertThat(message.status).isEqualTo(OutboxMessageStatus.FAILED)
        assertThat(message.retryCount).isEqualTo(5)
        verify(exactly = 1) { outboxRepository.saveAll(listOf(message)) }
    }
}
