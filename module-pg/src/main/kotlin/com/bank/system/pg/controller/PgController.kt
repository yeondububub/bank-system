package com.bank.system.pg.controller

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/payments")
class PgController {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun pay(@RequestBody request: PgPaymentRequestDto): PgPaymentResponseDto {
        log.info("💳 [MOCK PG] 결제 승인 요청 수신 (orderId: {}, amount: {})", request.orderId, request.amount)
        
        // 간단한 Mocking: 승인 성공 처리
        val transactionId = "PG-TX-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        return PgPaymentResponseDto(
            success = true,
            transactionId = transactionId
        )
    }

    @PostMapping("/cancel")
    fun cancel(@RequestBody request: PgCancelRequestDto): PgCancelResponseDto {
        log.info("🔄 [MOCK PG] 결제 취소 요청 수신 (orderId: {}, amount: {})", request.orderId, request.amount)
        
        // 간단한 Mocking: 취소 성공 처리
        return PgCancelResponseDto(
            success = true,
            message = "SUCCESS"
        )
    }
}

data class PgPaymentRequestDto(
    val orderId: String,
    val amount: Long
)

data class PgPaymentResponseDto(
    val success: Boolean,
    val transactionId: String?
)

data class PgCancelRequestDto(
    val orderId: String,
    val amount: Long
)

data class PgCancelResponseDto(
    val success: Boolean,
    val message: String?
)
