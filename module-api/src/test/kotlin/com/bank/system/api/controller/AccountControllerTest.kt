package com.bank.system.api.controller

import com.bank.system.api.config.AuthenticatedUser
import com.bank.system.api.dto.CreateAccountRequest
import com.bank.system.api.dto.TransferRequest
import com.bank.system.common.exception.BusinessException
import com.bank.system.common.exception.ErrorCode
import com.bank.system.common.util.SnowflakeIdGenerator
import com.bank.system.domain.Account
import com.bank.system.domain.AccountRepository
import com.bank.system.domain.AccountStatus
import com.bank.system.domain.AccountTransaction
import com.bank.system.domain.AccountTransactionRepository
import com.bank.system.domain.TransactionType
import com.bank.system.domain.User
import com.bank.system.domain.UserRole
import com.bank.system.domain.UserService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    private fun authUser(userId: Long, role: UserRole = UserRole.USER) = AuthenticatedUser(
        userId = userId,
        email = "user$userId@bank.com",
        role = role
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
        val response = controller.transferMoney(request, authUser(1004L))

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(70000L, response.body?.balanceAfterFrom)
        assertEquals(80000L, toAcc.balance)

        verify(exactly = 2) { accountTransactionRepository.save(any()) }
    }

    @Test
    @DisplayName("타인의 계좌를 출금 계좌로 지정하여 송금 시도 시 ACCESS_DENIED 예외가 발생한다 (BOLA 방어)")
    fun testTransferMoney_OtherUserAccount_ThrowsAccessDenied() {
        // given
        val fromAcc = Account(id = 1L, ownerId = 1004L, accountNumber = "1001111222333", balance = 100000L, status = AccountStatus.ACTIVE)
        every { accountRepository.findByAccountNumber("1001111222333") } returns fromAcc

        val request = TransferRequest(
            fromAccountNumber = "1001111222333",
            toAccountNumber = "2001111222333",
            amount = 30000L,
            memo = "무단 출금 시도"
        )

        // when & then
        val ex = assertThrows<BusinessException> {
            controller.transferMoney(request, authUser(9999L))
        }
        assertEquals(ErrorCode.ACCESS_DENIED, ex.errorCode)
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
    @DisplayName("특정 계좌의 거래 내역 조회가 본인 계좌일 때 정상 동작한다")
    fun test3() {
        // given
        val accountNumber = "1001111222333"
        val mockAccount = Account(id = 1L, ownerId = 1004L, accountNumber = accountNumber, balance = 90000L, status = AccountStatus.ACTIVE)
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

        every { accountRepository.findByAccountNumber(accountNumber) } returns mockAccount
        every { accountTransactionRepository.findAllByAccountNumberOrderByCreatedAtDesc(accountNumber) } returns mockTxs

        // when
        val response = controller.getAccountTransactions(accountNumber, authUser(1004L))

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(TransactionType.TRANSFER_OUT, response.body?.get(0)?.type)
        assertEquals(10000L, response.body?.get(0)?.amount)
    }

    @Test
    @DisplayName("특정 유저의 특정 계좌 거래 내역 조회 시 소유권이 확인되면 해당 계좌의 거래 내역만 반환된다")
    fun testUserSpecificAccountTransactions_Success() {
        // given
        val userId = 1004L
        val accountNumber = "1001111222333"
        val mockAccount = Account(id = 1L, ownerId = userId, accountNumber = accountNumber, balance = 100000L, status = AccountStatus.ACTIVE)
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

        every { accountRepository.findByAccountNumber(accountNumber) } returns mockAccount
        every { accountTransactionRepository.findAllByAccountNumberOrderByCreatedAtDesc(accountNumber) } returns mockTxs

        // when
        val response = controller.getUserTransactions(userId, accountNumber, authUser(userId))

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.size)
        assertEquals(accountNumber, response.body?.get(0)?.accountNumber)
    }

    @Test
    @DisplayName("특정 유저의 계좌가 아닌 타인의 계좌번호로 거래 내역 조회 시 ACCESS_DENIED 예외가 발생한다 (BOLA 방어)")
    fun testUserSpecificAccountTransactions_OtherUserAccount_ThrowsException() {
        // given
        val userId = 1004L
        val otherUserAccountNumber = "9998888777666"
        val otherAccount = Account(id = 99L, ownerId = 9999L, accountNumber = otherUserAccountNumber, balance = 100000L, status = AccountStatus.ACTIVE)

        every { accountRepository.findByAccountNumber(otherUserAccountNumber) } returns otherAccount

        // when & then
        val ex = assertThrows<BusinessException> {
            controller.getUserTransactions(userId, otherUserAccountNumber, authUser(userId))
        }
        assertEquals(ErrorCode.ACCESS_DENIED, ex.errorCode)
    }

    @Test
    @DisplayName("타인의 userId로 계좌 목록 조회 시도 시 ACCESS_DENIED 예외가 발생한다 (BOLA 방어)")
    fun testGetUserAccounts_OtherUser_ThrowsAccessDenied() {
        val ex = assertThrows<BusinessException> {
            controller.getUserAccounts(ownerId = 1004L, loginUser = authUser(9999L))
        }
        assertEquals(ErrorCode.ACCESS_DENIED, ex.errorCode)
    }

    @Test
    @DisplayName("accountNumber 없이 유저 거래 내역 조회 시 유저가 보유한 모든 계좌의 거래 내역이 조회된다")
    fun testUserAllTransactions_WithoutAccountNumber() {
        // given
        val userId = 1004L
        val acc1 = Account(id = 1L, ownerId = userId, accountNumber = "1001111222333", balance = 100000L, status = AccountStatus.ACTIVE)
        val acc2 = Account(id = 2L, ownerId = userId, accountNumber = "1004444555666", balance = 200000L, status = AccountStatus.ACTIVE)

        every { accountRepository.findAllByOwnerId(userId) } returns listOf(acc1, acc2)
        every { accountTransactionRepository.findAllByAccountNumberInOrderByCreatedAtDesc(listOf("1001111222333", "1004444555666")) } returns emptyList()

        // when
        val response = controller.getUserTransactions(userId, null, authUser(userId))

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(0, response.body?.size)
    }

    @Test
    @DisplayName("계좌 개설 시 클라이언트 입력과 무관하게 서버 정책 초기 잔액으로 계좌가 생성된다")
    fun testCreateAccountUsesServerDefaultBalance() {
        // given
        val ownerId = 1004L
        val customController = AccountController(
            accountRepository = accountRepository,
            accountTransactionRepository = accountTransactionRepository,
            snowflakeIdGenerator = snowflakeIdGenerator,
            userService = userService,
            defaultInitialBalance = 500_000L
        )
        val accountSlot = slot<Account>()
        every { accountRepository.findAllByOwnerId(ownerId) } returns emptyList()
        every { accountRepository.existsByAccountNumber(any()) } returns false
        every { snowflakeIdGenerator.nextId() } returns 12345L
        every { accountRepository.save(capture(accountSlot)) } answers { accountSlot.captured }

        // when
        val response = customController.createAccount(
            loginUser = authUser(ownerId),
            ownerId = ownerId,
            request = CreateAccountRequest(ownerId = ownerId)
        )

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(500_000L, response.body?.balance)
        assertEquals(true, response.body?.isPrimary)
    }
}
