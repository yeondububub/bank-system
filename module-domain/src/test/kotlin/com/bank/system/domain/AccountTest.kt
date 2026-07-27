package com.bank.system.domain

import com.bank.system.domain.exception.UnapprovedAccountException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AccountTest {

    @Test
    fun `PENDING 승인 대기 상태의 계좌에서 출금 시도 시 UnapprovedAccountException 예외가 발생한다`() {
        val account = Account(id = 1L, ownerId = 100L, accountNumber = "3520000000001", balance = 10000L, status = AccountStatus.PENDING)

        assertThrows<UnapprovedAccountException> {
            account.withdraw(1000L)
        }
    }

    @Test
    fun `approve 메서드를 호출하면 ACTIVE 상태가 되며 입출금이 가능해진다`() {
        val account = Account(id = 1L, ownerId = 100L, accountNumber = "3520000000001", balance = 10000L, status = AccountStatus.PENDING)

        account.approve()

        assertEquals(AccountStatus.ACTIVE, account.status)
        account.withdraw(2000L)
        assertEquals(8000L, account.balance)
    }
}
