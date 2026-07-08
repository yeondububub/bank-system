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

    @CircuitBreaker(name = "pgCircuitBreaker", fallbackMethod = "cancelFallback")
    override fun cancel(orderId: String, amount: Long): Boolean {
        return try {
            val response = pgFeignClient.cancelPayment(PgCancelRequest(orderId, amount))
            response.success
        } catch (e: Exception) {
            throw e
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun cancelFallback(orderId: String, amount: Long, t: Throwable): Boolean {
        // 취소 실패 또는 서킷 오픈 시 환불을 완료할 수 없으므로 false 반환
        return false
    }

    override fun queryStatus(orderId: String): com.bank.system.domain.PgStatusResponse {
        return try {
            val response = pgFeignClient.queryPayment(orderId)
            if (!response.exists) {
                com.bank.system.domain.PgStatusResponse(
                    status = com.bank.system.domain.PgTransactionStatus.NOT_FOUND,
                    transactionId = null
                )
            } else if (response.status == "SUCCESS") {
                com.bank.system.domain.PgStatusResponse(
                    status = com.bank.system.domain.PgTransactionStatus.SUCCESS,
                    transactionId = response.transactionId
                )
            } else {
                com.bank.system.domain.PgStatusResponse(
                    status = com.bank.system.domain.PgTransactionStatus.FAILED,
                    transactionId = response.transactionId
                )
            }
        } catch (e: Exception) {
            throw e
        }
    }
}
