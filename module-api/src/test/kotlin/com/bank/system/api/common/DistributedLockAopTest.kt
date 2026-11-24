package com.bank.system.api.common

import com.bank.system.common.DistributedLock
import com.bank.system.common.exception.ErrorCode
import com.bank.system.common.exception.LockAcquisitionException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory
import java.util.concurrent.TimeUnit

class DistributedLockAopTest {

    private val redissonClient = mockk<RedissonClient>()
    private val rLock = mockk<RLock>(relaxed = true)
    private val proxyService: TestLockService

    init {
        val factory = AspectJProxyFactory(TestLockService())
        factory.addAspect(DistributedLockAop(redissonClient))
        proxyService = factory.getProxy()
    }

    @Test
    @DisplayName("prefix 미지정 시 메서드명을 네임스페이스로 사용하여 락을 획득하고 종료 시 해제한다")
    fun lockAcquireAndReleaseWithMethodNameNamespace() {
        val lockKey = "LOCK-TEST-001"
        val expectedLockName = "LOCK:executeWithLock:$lockKey"

        every { redissonClient.getLock(expectedLockName) } returns rLock
        every { rLock.tryLock(1L, 10L, TimeUnit.SECONDS) } returns true
        every { rLock.isHeldByCurrentThread } returns true

        val result = proxyService.executeWithLock(lockKey)

        assertThat(result).isEqualTo("SUCCESS")
        verify(exactly = 1) { rLock.tryLock(1L, 10L, TimeUnit.SECONDS) }
        verify(exactly = 1) { rLock.unlock() }
    }

    @Test
    @DisplayName("동일한 prefix(ORDER)를 지정한 서로 다른 메서드는 동일한 리소스 락 키(LOCK:ORDER:orderId)를 공유한다")
    fun sharedPrefixMethodsUseIdenticalLockKey() {
        val orderId = "ORD-2026-001"
        val expectedSharedLockName = "LOCK:ORDER:$orderId"

        every { redissonClient.getLock(expectedSharedLockName) } returns rLock
        every { rLock.tryLock(5L, 10L, TimeUnit.SECONDS) } returns true
        every { rLock.isHeldByCurrentThread } returns true

        proxyService.approveOrder(orderId)
        proxyService.cancelOrder(orderId)

        verify(exactly = 2) { redissonClient.getLock(expectedSharedLockName) }
        verify(exactly = 2) { rLock.unlock() }
    }

    @Test
    @DisplayName("락 획득에 실패하면 409 LOCK_ACQUISITION_FAILED 예외(LockAcquisitionException)를 던진다")
    fun lockAcquisitionFailureThrowsLockAcquisitionException() {
        val orderId = "ORD-BUSY-001"
        val expectedLockName = "LOCK:ORDER:$orderId"

        every { redissonClient.getLock(expectedLockName) } returns rLock
        every { rLock.tryLock(5L, 10L, TimeUnit.SECONDS) } returns false
        every { rLock.isHeldByCurrentThread } returns false

        val ex = assertThrows<LockAcquisitionException> {
            proxyService.approveOrder(orderId)
        }

        assertEquals(ErrorCode.LOCK_ACQUISITION_FAILED, ex.errorCode)
        verify(exactly = 0) { rLock.unlock() }
    }
}

open class TestLockService {

    @DistributedLock(key = "#id", waitTime = 1L)
    open fun executeWithLock(id: String): String {
        return "SUCCESS"
    }

    @DistributedLock(prefix = "ORDER", key = "#orderId")
    open fun approveOrder(orderId: String): String {
        return "APPROVED:$orderId"
    }

    @DistributedLock(prefix = "ORDER", key = "#orderId")
    open fun cancelOrder(orderId: String): String {
        return "CANCELED:$orderId"
    }
}
