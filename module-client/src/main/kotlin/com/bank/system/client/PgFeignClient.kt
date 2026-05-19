package com.bank.system.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "pgClient", url = "\${pg.api.url:http://localhost:8081}")
interface PgFeignClient {
    @PostMapping("/v1/payments")
    fun requestPayment(@RequestBody request: PgPaymentRequest): PgPaymentResponse

    @PostMapping("/v1/payments/cancel")
    fun cancelPayment(@RequestBody request: PgCancelRequest): PgCancelResponse
}

data class PgPaymentRequest(
    val orderId: String,
    val amount: Long
)

data class PgPaymentResponse(
    val success: Boolean,
    val transactionId: String?
)

data class PgCancelRequest(
    val orderId: String,
    val amount: Long
)

data class PgCancelResponse(
    val success: Boolean,
    val message: String?
)
