package com.bank.system.pg.controller

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/v1/payments")
class PgController {

    private val log = LoggerFactory.getLogger(javaClass)
    
    // 결제 상태 저장을 위한 인메모리 Map (orderId -> transactionId)
    private val paymentStore = ConcurrentHashMap<String, String>()

    @PostMapping
    fun pay(@RequestBody request: PgPaymentRequestDto): PgPaymentResponseDto {
        log.info("💳 [MOCK PG] 결제 승인 요청 수신 (orderId: {}, amount: {})", request.orderId, request.amount)
        
        // 결제 성공 처리 및 저장
        val transactionId = "PG-TX-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        paymentStore[request.orderId] = transactionId
        
        return PgPaymentResponseDto(
            success = true,
            transactionId = transactionId
        )
    }

    @GetMapping("/{orderId}")
    fun queryStatus(@PathVariable orderId: String): PgQueryResponseDto {
        log.info("🔍 [MOCK PG] 결제 상태 조회 요청 수신 (orderId: {})", orderId)
        val transactionId = paymentStore[orderId]
        return if (transactionId != null) {
            PgQueryResponseDto(exists = true, status = "SUCCESS", transactionId = transactionId)
        } else {
            PgQueryResponseDto(exists = false, status = "NOT_FOUND", transactionId = null)
        }
    }

    @PostMapping("/cancel")
    fun cancel(@RequestBody request: PgCancelRequestDto): PgCancelResponseDto {
        log.info("🔄 [MOCK PG] 결제 취소 요청 수신 (orderId: {}, amount: {})", request.orderId, request.amount)
        
        paymentStore.remove(request.orderId)
        
        return PgCancelResponseDto(
            success = true,
            message = "SUCCESS"
        )
    }
}

data class PgQueryResponseDto(
    val exists: Boolean,
    val status: String,
    val transactionId: String?
)

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
