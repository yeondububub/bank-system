package com.bank.system.api.common

import com.bank.system.common.Idempotent
import com.bank.system.common.exception.IdempotencyException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@SpringBootTest
class IdempotencyAopTest @Autowired constructor(
    private val testIdempotencyService: TestIdempotencyService,
    private val redissonClient: RedissonClient
) {

    @BeforeEach
    fun setUp() {
        val request = MockHttpServletRequest()
        request.requestURI = "/api/test"
        request.addHeader("Idempotency-Key", "test-idempotency-key-1")
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    @Test
    @DisplayName("멱등성 키가 없는 최초 요청은 정상적으로 실행되고 Redis 상태가 DONE으로 변경된다.")
    fun successOnFirstRequest() {
        val redisKey = "IDEMPOTENCY:/api/test:test-idempotency-key-1"
        redissonClient.getBucket<String>(redisKey).delete()

        val result = testIdempotencyService.executeIdempotent("DATA")
        
        assertThat(result).isEqualTo("SUCCESS:DATA")

        val status = redissonClient.getBucket<String>(redisKey).get()
        assertThat(status).isEqualTo("DONE")
    }

    @Test
    @DisplayName("이미 처리 중이거나 처리 완료된 멱등성 키로 요청하면 예외가 발생한다.")
    fun failOnDuplicateRequest() {
        val redisKey = "IDEMPOTENCY:/api/test:test-idempotency-key-1"
        redissonClient.getBucket<String>(redisKey).delete()
        
        // 1. First execution
        testIdempotencyService.executeIdempotent("DATA1")

        // 2. Second execution (Duplicate)
        assertThatThrownBy {
            testIdempotencyService.executeIdempotent("DATA2")
        }.isInstanceOf(IdempotencyException::class.java)
         .hasMessageContaining("이미 성공적으로 처리 완료된 요청입니다.")
    }

    @Test
    @DisplayName("멱등성 헤더가 없으면 예외가 발생한다.")
    fun failOnMissingHeader() {
        val request = MockHttpServletRequest()
        request.requestURI = "/api/test"
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        assertThatThrownBy {
            testIdempotencyService.executeIdempotent("DATA")
        }.isInstanceOf(IllegalArgumentException::class.java)
         .hasMessageContaining("Idempotency-Key 헤더가 누락되었습니다.")
    }
}

@Component
class TestIdempotencyService {

    @Idempotent
    fun executeIdempotent(data: String): String {
        return "SUCCESS:$data"
    }
}
