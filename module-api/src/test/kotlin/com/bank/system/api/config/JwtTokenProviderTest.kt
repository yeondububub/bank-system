package com.bank.system.api.config

import com.bank.system.domain.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class JwtTokenProviderTest {

    private val secret = "test-secret-key-for-hmac-sha256-signing-verification"
    private val provider = JwtTokenProvider(secretKey = secret, expirationMs = 60_000L)

    @Test
    @DisplayName("표준 3파트(Header.Payload.Signature) JWT를 생성하고 클레임을 정상 파싱한다")
    fun createAndParseTokenSuccess() {
        val emailWithColon = "user:special@bank.com"
        val token = provider.createToken(userId = 1004L, email = emailWithColon, role = UserRole.ADMIN)

        assertEquals(3, token.split(".").size)
        assertTrue(provider.validateToken(token))
        assertEquals(1004L, provider.getUserIdFromToken(token))
        assertEquals(emailWithColon, provider.getEmailFromToken(token))
        assertEquals(UserRole.ADMIN, provider.getUserRoleFromToken(token))
    }

    @Test
    @DisplayName("서명이 위변조된 토큰은 검증에 실패한다")
    fun rejectTamperedToken() {
        val token = provider.createToken(userId = 1004L, email = "user@bank.com", role = UserRole.USER)
        val tamperedToken = token.dropLast(2) + "XX"

        assertFalse(provider.validateToken(tamperedToken))
    }

    @Test
    @DisplayName("만료 시간이 지난 토큰은 검증에 실패한다")
    fun rejectExpiredToken() {
        val expiredProvider = JwtTokenProvider(secretKey = secret, expirationMs = -1000L)
        val expiredToken = expiredProvider.createToken(userId = 1004L, email = "user@bank.com", role = UserRole.USER)

        assertFalse(provider.validateToken(expiredToken))
    }

    @Test
    @DisplayName("BCrypt PasswordEncoder는 동일 평문에 대해 매번 다른 솔트 해시를 생성하며 검증에 성공한다")
    fun bcryptSaltAndBoundaryTest() {
        val encoder = PasswordEncoder()
        val raw = "MySecurePass!23"

        val hash1 = encoder.encode(raw)
        val hash2 = encoder.encode(raw)

        assertNotEquals(hash1, hash2)
        assertTrue(encoder.matches(raw, hash1))
        assertTrue(encoder.matches(raw, hash2))
        assertFalse(encoder.matches("WrongPass", hash1))
        assertFalse(encoder.matches("a".repeat(80), hash1)) // 72바이트 초과 입력 시 예외 없이 false 반환
    }
}

