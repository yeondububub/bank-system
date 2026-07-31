package com.bank.system.api.controller

import com.bank.system.api.dto.TransferRequest
import com.bank.system.common.util.SnowflakeIdGenerator
import com.bank.system.domain.Account
import com.bank.system.domain.AccountRepository
import com.bank.system.domain.AccountStatus
import com.bank.system.domain.AccountTransaction
import com.bank.system.domain.AccountTransactionRepository
import com.bank.system.domain.TransactionType
import com.bank.system.domain.User
import com.bank.system.domain.UserService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

class AccountControllerTest {

    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val accountTransactionRepository = mockk<AccountTransactionRepository>(relaxed = true)
    private val snowflakeIdGenerator = mockk<SnowflakeIdGenerator>()
    private val userService = mockk<UserService>()

    private val controller = AccountController(
        accountRepository = accountRepository,
        accountTransactionRepository = accountTransactionRepository,
        snowflakeIdGenerator = snowflakeIdGenerator,
        userService = userService
    )

    @Test
    @DisplayName("계좌 간 송금 성공 시 출금 및 입금이 수행되고 이력이 2건 저장된다")
    fun test1() {
        // given
        val fromAcc = Account(id = 1L, ownerId = 1004L, accountNumber = "1001111222333", balance = 100000L, status = AccountStatus.ACTIVE)
        val toAcc = Account(id = 2L, ownerId = 2004L, accountNumber = "2001111222333", balance = 50000L, status = AccountStatus.ACTIVE)

        val fromUser = User(id = 1004L, email = "user1@bank.com", password = "pw", name = "홍길동")
        val toUser = User(id = 2004L, email = "user2@bank.com", password = "pw", name = "김철수")

        every { accountRepository.findByAccountNumber("1001111222333") } returns fromAcc
        every { accountRepository.findByAccountNumber("2001111222333") } returns toAcc
        every { userService.getById(1004L) } returns fromUser
        every { userService.getById(2004L) } returns toUser
        every { snowflakeIdGenerator.nextId() } returns 99999L

        val txSlot = slot<AccountTransaction>()
        every { accountTransactionRepository.save(capture(txSlot)) } answers { txSlot.captured }

        val request = TransferRequest(
            fromAccountNumber = "1001111222333",
            toAccountNumber = "2001111222333",
            amount = 30000L,
            memo = "용돈"
        )

        // when
        val response = controller.transferMoney(request)

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(70000L, response.body?.balanceAfterFrom)
        assertEquals(80000L, toAcc.balance)

        verify(exactly = 2) { accountTransactionRepository.save(any()) }
    }

    @Test
    @DisplayName("계좌 예금주 조회 시 예금주 이름이 반환된다")
    fun test2() {
        // given
        val acc = Account(id = 1L, ownerId = 1004L, accountNumber = "1001111222333", balance = 100000L, status = AccountStatus.ACTIVE)
        val user = User(id = 1004L, email = "user1@bank.com", password = "pw", name = "홍길동")

        every { accountRepository.findByAccountNumber("1001111222333") } returns acc
        every { userService.getById(1004L) } returns user

        // when
        val response = controller.getAccountHolder("1001111222333")

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("홍길동", response.body?.ownerName)
    }

    @Test
    @DisplayName("특정 계좌의 거래 내역 조회가 정상 동작한다")
    fun test3() {
        // given
        val accountNumber = "1001111222333"
        val mockTxs = listOf(
            AccountTransaction(
                id = 101L,
                accountNumber = accountNumber,
                type = TransactionType.TRANSFER_OUT,
                amount = 10000L,
                balanceAfter = 90000L,
                counterpartyName = "김철수",
                counterpartyAccountNumber = "2001111222333",
                createdAt = LocalDateTime.now()
            )
        )

        every { accountRepository.existsByAccountNumber(accountNumber) } returns true
        every { accountTransactionRepository.findAllByAccountNumberOrderByCreatedAtDesc(accountNumber) } returns mockTxs

        // when
        val response = controller.getAccountTransactions(accountNumber)

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.size)
        assertEquals(TransactionType.TRANSFER_OUT, response.body?.get(0)?.type)
        assertEquals(10000L, response.body?.get(0)?.amount)
    }
}
