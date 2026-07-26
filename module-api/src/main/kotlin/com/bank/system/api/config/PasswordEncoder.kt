package com.bank.system.api.config

import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class PasswordEncoder {
    fun encode(rawPassword: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(rawPassword.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return encode(rawPassword) == encodedPassword
    }
}
