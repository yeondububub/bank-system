package com.bank.system.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PaymentTest {

    @Test
    fun `결제 금액이 0 이하이면 생성할 수 없다`() {
        val exception = assertThrows<IllegalArgumentException> {
            Payment(orderId = "ORD-1", buyerId = 1L, amount = 0L)
        }

        assertThat(exception.message).contains("0보다 커야 합니다")
    }

    @Test
    fun `결제 승인 준비 시 상태가 APPROVING으로 변경된다`() {
        val payment = Payment(orderId = "ORD-1", buyerId = 1L, amount = 1000L)
        payment.prepareApproval()
        assertThat(payment.status).isEqualTo(PaymentStatus.APPROVING)
    }

    @Test
    fun `결제 승인 완료 시 상태가 SUCCESS로 변경된다`() {
        val payment = Payment(orderId = "ORD-1", buyerId = 1L, amount = 1000L)
        payment.prepareApproval()
        payment.approve()
        assertThat(payment.status).isEqualTo(PaymentStatus.SUCCESS)
    }

    @Test
    fun `이미 완료된 결제는 다시 승인할 수 없다`() {
        val payment = Payment(orderId = "ORD-1", buyerId = 1L, amount = 1000L).apply {
            prepareApproval()
            approve()
        }

        assertThrows<IllegalStateException> {
            payment.approve()
        }
    }
}