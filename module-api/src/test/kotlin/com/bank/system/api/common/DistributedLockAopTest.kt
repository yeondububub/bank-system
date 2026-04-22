package com.bank.system.api.common

import com.bakn.system.common.DistributedLock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component

@SpringBootTest
class DistributedLockAopTest @Autowired constructor(
    private val testLockService: TestLockService,
    private val redissonClient: RedissonClient
) {

    @Test
    @DisplayName("AOP가 정상적으로 락을 획득하고, 메서드 종료 시 락을 해제한다.")
    fun lockAcquireAndReleaseTest() {
        val lockKey = "LOCK-TEST-001"

        val result = testLockService.executeWithLock(lockKey)

        assertThat(result).isEqualTo("SUCCESS")

        val lockName = "LOCK:executeWithLock:$lockKey"
        val lock = redissonClient.getLock(lockName)

        assertThat(lock.isLocked).isFalse()
    }

}

@Component
class TestLockService {

    @DistributedLock(key = "#id", waitTime = 1L)
    fun executeWithLock(id: String): String {
        return "SUCCESS"
    }
}