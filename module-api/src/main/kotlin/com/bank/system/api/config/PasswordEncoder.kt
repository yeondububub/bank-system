package com.bank.system.api.config

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class PasswordEncoder {

    private val delegate = BCryptPasswordEncoder()

    fun encode(rawPassword: String): String {
        require(rawPassword.isNotBlank()) { "비밀번호는 비어 있을 수 없습니다." }
        require(rawPassword.toByteArray(Charsets.UTF_8).size <= MAX_BCRYPT_BYTES) {
            "비밀번호는 최대 ${MAX_BCRYPT_BYTES}바이트를 초과할 수 없습니다."
        }
        return delegate.encode(rawPassword)
    }

    fun matches(rawPassword: String, encodedPassword: String): Boolean {
        if (rawPassword.isBlank() || encodedPassword.isBlank()) {
            return false
        }
        if (rawPassword.toByteArray(Charsets.UTF_8).size > MAX_BCRYPT_BYTES) {
            return false
        }
        return try {
            delegate.matches(rawPassword, encodedPassword)
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    companion object {
        // BCrypt 알고리즘의 입력 길이 상한(72 bytes) 초과 시 예외 발생을 방어
        private const val MAX_BCRYPT_BYTES = 72
    }
}
