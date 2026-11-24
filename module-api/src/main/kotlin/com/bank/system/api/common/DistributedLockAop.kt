package com.bank.system.api.common

import com.bank.system.common.DistributedLock
import com.bank.system.common.exception.LockAcquisitionException
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
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class DistributedLockAop(
    private val redissonClient: RedissonClient
) {

    private val parser = SpelExpressionParser()

    @Around("@annotation(com.bank.system.common.DistributedLock)")
    fun lock(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val distributedLock = method.getAnnotation(DistributedLock::class.java)
            ?: throw IllegalStateException("DistributedLock annotation not found on method ${method.name}")

        val context = StandardEvaluationContext().apply {
            val parameterNames = signature.parameterNames
            val args = joinPoint.args
            for (i in parameterNames.indices) {
                setVariable(parameterNames[i], args[i])
            }
        }

        val dynamicKey = parser.parseExpression(distributedLock.key).getValue(context)?.toString()
            ?: throw IllegalArgumentException("DistributedLock key evaluation returned null on method ${method.name}")

        val namespace = distributedLock.prefix.ifBlank { method.name }
        val lockName = "LOCK:$namespace:$dynamicKey"

        val lock = redissonClient.getLock(lockName)

        return try {
            val available: Boolean = lock.tryLock(
                distributedLock.waitTime,
                distributedLock.leaseTime,
                distributedLock.timeUnit
            )

            if (!available) {
                throw LockAcquisitionException()
            }

            joinPoint.proceed()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }
}
