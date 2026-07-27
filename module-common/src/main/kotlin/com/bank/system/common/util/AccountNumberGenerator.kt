package com.bank.system.common.util

import java.security.SecureRandom
import kotlin.math.abs

class AccountNumberGenerator {
    companion object {
        private const val PREFIX = "352"
        private val random = SecureRandom()

        fun generate(): String {
            val randomNumber = abs(random.nextLong()) % 10000000000L // 10자리 숫자
            val formatted = String.format("%010d", randomNumber)
            return "$PREFIX$formatted" // 3자리 + 10자리 = 총 13자리 숫자 문자열 (하이픈 없음)
        }
    }
}
