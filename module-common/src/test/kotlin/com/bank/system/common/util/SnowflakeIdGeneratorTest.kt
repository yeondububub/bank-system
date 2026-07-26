package com.bank.system.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SnowflakeIdGeneratorTest {

    private val generator = SnowflakeIdGenerator(workerId = 1, datacenterId = 1)

    @Test
    fun `Snowflake ID가 오름차순으로 생성된다`() {
        val id1 = generator.nextId()
        val id2 = generator.nextId()

        assertTrue(id2 > id1, "새로 생성된 ID가 이전 ID보다 커야 합니다.")
    }

    @Test
    fun `동시성 환경에서 10000개 ID 발급 시 충돌(중복)이 전혀 없다`() {
        val threadCount = 10
        val idsPerThread = 1000
        val executor = Executors.newFixedThreadPool(threadCount)
        val idMap = ConcurrentHashMap<Long, Boolean>()

        for (i in 0 until threadCount) {
            executor.submit {
                for (j in 0 until idsPerThread) {
                    val id = generator.nextId()
                    idMap[id] = true
                }
            }
        }

        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        assertEquals(threadCount * idsPerThread, idMap.size, "생성된 모든 ID는 고유해야 합니다.")
    }
}
