package com.bank.system.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccountTransactionTest {

    @Test
    fun `AccountTransaction 생성 및 속성 검증`() {
        // given
        val tx = AccountTransaction(
            id = 100L,
            accountNumber = "3521234567890",
            type = TransactionType.TRANSFER_OUT,
            amount = 50000L,
            balanceAfter = 950000L,
            counterpartyName = "홍길동",
            counterpartyAccountNumber = "3520000000000",
            memo = "송금"
        )

        // then
        assertEquals(100L, tx.id)
        assertEquals("3521234567890", tx.accountNumber)
        assertEquals(TransactionType.TRANSFER_OUT, tx.type)
        assertEquals(50000L, tx.amount)
        assertEquals(950000L, tx.balanceAfter)
        assertEquals("홍길동", tx.counterpartyName)
        assertEquals("3520000000000", tx.counterpartyAccountNumber)
        assertEquals("송금", tx.memo)
    }
}
