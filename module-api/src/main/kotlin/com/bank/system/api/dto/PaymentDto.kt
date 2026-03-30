package com.bank.system.api.dto

import com.bank.system.domain.Payment
import com.bank.system.domain.PaymentStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class PaymentRequest(
    @field:NotBlank(message = "")
    val orderId: String,

    @field:Min(value = 1, message = "구매자 ID는 1 이상이여야 한다.")
    val buyerId: Long,

    @field:Min(value = 100, message = "최소 결제 금액은 100원 입니다.")
    val amount: Long
)

data class PaymentResponse(
    val paymentId: Long,
    val orderId: String,
    val amount: Long,
    val status: PaymentStatus
) {
    companion object {
        fun from(payment: Payment): PaymentResponse {
            return PaymentResponse(
                paymentId = payment.id!!,
                orderId = payment.orderId,
                amount = payment.amount,
                status = payment.status
            )
        }
    }
}