package com.bank.system.api.application

import com.bank.system.domain.PaymentStatus
import com.bank.system.infra.AccountJpaEntity
import com.bank.system.infra.AccountJpaRepository
import com.bank.system.infra.PaymentHistoryJpaRepository
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

import com.bank.system.domain.PgPort
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every

@SpringBootTest
class PaymentFacadeConcurrencyTest @Autowired constructor(
    private val paymentFacade: PaymentFacade,
    private val paymentJpaRepository: PaymentJpaRepository,
    private val accountJpaRepository: AccountJpaRepository,
    private val paymentHistoryJpaRepository: PaymentHistoryJpaRepository
) {

    @MockkBean
    private lateinit var pgPort: PgPort

    @AfterEach
    fun tearDown() {
        paymentJpaRepository.deleteAllInBatch()
        paymentJpaRepository.deleteAllInBatch()
        accountJpaRepository.deleteAllInBatch()
    }

    @Test
    @DisplayName("동시에 100번의 결제 승인 요청이 와도 단 1번만 성공하고 나머지는 튕겨내야 한다.")
    fun approvePaymentConcurrencyTest() {

        val buyerId = 1000L
        val orderId = "ORD-CONCURRENCY-001"
        val paymentAmount = 50000L

        accountJpaRepository.save(
            AccountJpaEntity(ownerId = buyerId, balance = 100000L)
        )

        paymentFacade.createPayment(orderId, buyerId = buyerId, amount = paymentAmount)

        every { pgPort.pay(any(), any()) } returns true

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

        val finalAccount = accountJpaRepository.findByOwnerId(buyerId)
        assertThat(finalAccount?.balance).isEqualTo(50000L)
    }
}