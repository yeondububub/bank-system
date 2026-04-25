package com.bank.system.client

import com.bank.system.domain.PgPort
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.springframework.stereotype.Component

@Component
class PgAdapter(
    private val pgFeignClient: PgFeignClient
) : PgPort {

    @CircuitBreaker(name = "pgCircuitBreaker", fallbackMethod = "payFallback")
    override fun pay(orderId: String, amount: Long): Boolean {
        return try {
            val response = pgFeignClient.requestPayment(PgPaymentRequest(orderId, amount))
            response.success
        } catch (e: Exception) {
            throw e
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun payFallback(orderId: String, amount: Long, t: Throwable): Boolean {
        // PG사 호출 실패 또는 서킷 오픈 시 처리 로직
        // 결제를 승인할 수 없으므로 false 반환
        return false
    }
}
