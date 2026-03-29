package com.bank.system.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PaymentServiceTest {

    private val paymentRepository = mockk<PaymentRepository>()
    private val paymentService = PaymentService(paymentRepository)

    @Test
    fun `결제 승인이 성공적으로 수행되고 DB에 저장된다`() {
        val orderId = "ORD-123"
        val pendingPayment = Payment(id = 1L, orderId = orderId, buyerId = 100L, amount = 5000L)

        every { paymentRepository.findByOrderId(orderId) } returns pendingPayment
        every { paymentRepository.save(any()) } returnsArgument 0

        val result = paymentService.approvePayment(orderId)

        assertThat(result.status).isEqualTo(PaymentStatus.SUCCESS)
        verify(exactly = 1) { paymentRepository.save(pendingPayment) }
    }

    @Test
    fun `결제 정보를 찾을 수 없으면 예외가 발생한다`() {
        // given
        val orderId = "INVALID-ORD"
        every { paymentRepository.findByOrderId(orderId) } returns null

        // when & then
        assertThrows<IllegalArgumentException> {
            paymentService.approvePayment(orderId)
        }
    }
}