package com.bank.system.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountNumberGeneratorTest {

    @Test
    fun `계좌번호가 352로 시작하는 하이픈 없는 13자리 숫자 문자열로 생성된다`() {
        val accountNumber = AccountNumberGenerator.generate()

        assertTrue(accountNumber.startsWith("352"), "계좌번호는 352로 시작해야 합니다.")
        assertEquals(13, accountNumber.length, "계좌번호 길이는 하이픈 없이 총 13자리여야 합니다.")
        assertFalse(accountNumber.contains("-"), "백엔드 계좌번호에는 하이픈(-)이 없어야 합니다.")
        assertTrue(accountNumber.all { it.isDigit() }, "계좌번호는 숫자만으로 구성되어야 합니다.")
    }
}
