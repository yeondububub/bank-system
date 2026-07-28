package com.bank.system.api.config

import com.bank.system.common.exception.BusinessException
import com.bank.system.common.exception.ErrorCode
import com.bank.system.domain.UserRole
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AdminAuthorizationInterceptorTest {

    private val jwtTokenProvider = mockk<JwtTokenProvider>()
    private val interceptor = AdminAuthorizationInterceptor(jwtTokenProvider)

    private val request = mockk<HttpServletRequest>()
    private val response = mockk<HttpServletResponse>()

    @Test
    fun `Authorization 헤더가 없으면 401 BusinessException 예외가 발생한다`() {
        every { request.method } returns "POST"
        every { request.getHeader("Authorization") } returns null

        val ex = assertThrows<BusinessException> {
            interceptor.preHandle(request, response, Any())
        }

        assertEquals(ErrorCode.UNAUTHORIZED, ex.errorCode)
    }

    @Test
    fun `일반 유저(USER) 권한 토큰으로 요청 시 403 BusinessException 예외가 발생한다`() {
        val token = "valid-user-token"
        every { request.method } returns "POST"
        every { request.getHeader("Authorization") } returns "Bearer $token"
        every { jwtTokenProvider.validateToken(token) } returns true
        every { jwtTokenProvider.getUserRoleFromToken(token) } returns UserRole.USER

        val ex = assertThrows<BusinessException> {
            interceptor.preHandle(request, response, Any())
        }

        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `관리자(ADMIN) 권한 토큰으로 요청 시 preHandle이 true를 반환한다`() {
        val token = "valid-admin-token"
        every { request.method } returns "POST"
        every { request.getHeader("Authorization") } returns "Bearer $token"
        every { jwtTokenProvider.validateToken(token) } returns true
        every { jwtTokenProvider.getUserRoleFromToken(token) } returns UserRole.ADMIN

        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
    }
}
