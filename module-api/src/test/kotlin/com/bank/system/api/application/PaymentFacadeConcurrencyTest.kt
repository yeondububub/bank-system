package com.bank.system.api.application

import com.bank.system.domain.PaymentStatus
import com.bank.system.infra.PaymentJpaRepository
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class PaymentFacadeConcurrencyTest @Autowired constructor(
    private val paymentFacade: PaymentFacade,
    private val paymentJpaRepository: PaymentJpaRepository
) {

    @AfterEach
    fun tearDown() {
        paymentJpaRepository.deleteAllInBatch()
    }

    @Test
    @DisplayName("동시에 100번의 결제 승인 요청이 와도 단 1번만 성공하고 나머지는 튕겨내야 한다.")
    fun approvePaymentConcurrencyTest() {

        val orderId = "ORD-CONCURRENCY-001"
        paymentFacade.createPayment(orderId, buyerId = 1000L, amount = 50000L)

        val threadCount = 100
        val executorService = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(threadCount)

        val successCount = AtomicInteger()
        val failCount = AtomicInteger()

        for (i in 0 until threadCount) {
            executorService.submit {
                try {
                    paymentFacade.approvePayment(orderId)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()

        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(99)

        val finalPayment = paymentJpaRepository.findByOrderId(orderId)
        assertThat(finalPayment?.status).isEqualTo(PaymentStatus.SUCCESS)
    }
}