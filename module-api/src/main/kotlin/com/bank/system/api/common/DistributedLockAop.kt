package com.bank.system.api.common

import com.bank.system.common.DistributedLock
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RedissonClient
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component


@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class DistributedLockAop(
    private val redissonClient: RedissonClient
) {

    private val parser = SpelExpressionParser()

    @Around("@annotation(distributedLock)")
    fun lock(joinPoint: ProceedingJoinPoint, distributedLock: DistributedLock) : Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method

        val context = StandardEvaluationContext().apply {
            val parameterNames = signature.parameterNames
            val args = joinPoint.args
            for (i in parameterNames.indices) {
                setVariable(parameterNames[i], args[i])
            }
        }

        val dynamicKey = parser.parseExpression(distributedLock.key).getValue(context) as String

        val lockName = "LOCK:${method.name}:$dynamicKey"

        val lock = redissonClient.getLock(lockName)

        return try {
            // 락 획득 시도
            val available: Boolean = lock.tryLock(
                distributedLock.waitTime,
                distributedLock.leaseTime,
                distributedLock.timeUnit
            )

            if (!available) {
                throw IllegalStateException("현재 처리 중인 요청입니다. 잠시 후 다시 다시 시도해주세요.")
            }

            joinPoint.proceed()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }

}