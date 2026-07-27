package com.bank.system.api.controller

import com.bank.system.domain.Account
import com.bank.system.domain.AccountService
import com.bank.system.domain.AccountStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class AdminAccountControllerTest {

    private val accountService = mockk<AccountService>()
    private val adminAccountController = AdminAccountController(accountService)

    @Test
    fun `관리자가 계좌 승인 시 ACTIVE 상태가 된 계좌 정보가 반환된다`() {
        // given
        val accountId = 12345L
        val approvedAccount = Account(
            id = accountId,
            ownerId = 1004L,
            accountNumber = "3520000001004",
            balance = 1000000L,
            status = AccountStatus.ACTIVE
        )

        every { accountService.approveAccount(accountId) } returns approvedAccount

        // when
        val response = adminAccountController.approveAccount(accountId)

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(AccountStatus.ACTIVE, response.body?.status)
        assertEquals(accountId, response.body?.id)
    }

    @Test
    fun `관리자가 계좌 거절 시 REJECTED 상태가 된 계좌 정보가 반환된다`() {
        // given
        val accountId = 12345L
        val rejectedAccount = Account(
            id = accountId,
            ownerId = 1004L,
            accountNumber = "3520000001004",
            balance = 1000000L,
            status = AccountStatus.REJECTED
        )

        every { accountService.rejectAccount(accountId) } returns rejectedAccount

        // when
        val response = adminAccountController.rejectAccount(accountId)

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(AccountStatus.REJECTED, response.body?.status)
    }
}
