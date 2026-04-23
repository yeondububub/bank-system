package com.bank.system.api.common

import com.bank.system.common.Idempotent
import com.bank.system.common.exception.IdempotencyException
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class IdempotencyAop(
    private val redissonClient: RedissonClient
) {

    @Around("@annotation(idempotent)")
    fun checkIdempotency(joinPoint: ProceedingJoinPoint, idempotent: Idempotent): Any? {
        val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        val request = requestAttributes?.request ?: return joinPoint.proceed()

        val idempotencyKey = request.getHeader(idempotent.headerName)

        if (idempotencyKey.isNullOrBlank()) {
            throw IllegalArgumentException("${idempotent.headerName} 헤더가 누락되었습니다.")
        }

        val redisKey = "IDEMPOTENCY:${request.requestURI}:$idempotencyKey"
        val bucket = redissonClient.getBucket<String>(redisKey)

        val isFirstRequest = bucket.trySet("PROCESSING", idempotent.ttlSeconds, java.util.concurrent.TimeUnit.SECONDS)

        if (!isFirstRequest) {
            val status = bucket.get()
            if (status == "PROCESSING") {
                throw IdempotencyException("요청이 이미 처리 중입니다. 잠시 후 다시 시도해주세요.")
            } else if (status == "DONE") {
                throw IdempotencyException("이미 성공적으로 처리 완료된 요청입니다.")
            } else {
                throw IdempotencyException("요청 처리 상태를 알 수 없습니다.")
            }
        }

        return try {
            val result = joinPoint.proceed()
            bucket.set("DONE", idempotent.ttlSeconds, java.util.concurrent.TimeUnit.SECONDS)
            result
        } catch (ex: Exception) {
            bucket.delete()
            throw ex
        }
    }
}
