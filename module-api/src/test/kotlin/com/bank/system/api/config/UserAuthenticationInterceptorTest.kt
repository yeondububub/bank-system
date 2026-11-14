package com.bank.system.api.config

import com.bank.system.common.exception.BusinessException
import com.bank.system.common.exception.ErrorCode
import com.bank.system.domain.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest

class UserAuthenticationInterceptorTest {

    private val jwtTokenProvider = mockk<JwtTokenProvider>()
    private val interceptor = UserAuthenticationInterceptor(jwtTokenProvider)
    private val resolver = LoginUserArgumentResolver()

    private val request = mockk<HttpServletRequest>(relaxed = true)
    private val response = mockk<HttpServletResponse>(relaxed = true)

    @Test
    @DisplayName("OPTIONS 프리플라이트 요청은 토큰 검증 없이 통과한다")
    fun optionsPreflightBypass() {
        every { request.method } returns "OPTIONS"

        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
    }

    @Test
    @DisplayName("Authorization 헤더가 누락되면 401 UNAUTHORIZED 예외가 발생한다")
    fun missingHeaderThrowsUnauthorized() {
        every { request.method } returns "GET"
        every { request.getHeader("Authorization") } returns null

        val ex = assertThrows<BusinessException> {
            interceptor.preHandle(request, response, Any())
        }

        assertEquals(ErrorCode.UNAUTHORIZED, ex.errorCode)
    }

    @Test
    @DisplayName("유효한 Bearer 토큰이면 요청 속성에 AuthenticatedUser를 저장하고 통과한다")
    fun validBearerTokenSetsAuthenticatedUser() {
        val token = "valid.jwt.token"
        val claims = JwtTokenProvider.TokenClaims(
            userId = 1004L,
            email = "user@bank.com",
            role = UserRole.USER,
            issuedAtMs = 1000L,
            expiresAtMs = 9999999L
        )
        val userSlot = slot<AuthenticatedUser>()

        every { request.method } returns "POST"
        every { request.getHeader("Authorization") } returns "Bearer $token"
        every { jwtTokenProvider.validateToken(token) } returns true
        every { jwtTokenProvider.parseClaims(token) } returns claims
        every { request.setAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE_KEY, capture(userSlot)) } returns Unit

        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
        assertEquals(1004L, userSlot.captured.userId)
        assertEquals("user@bank.com", userSlot.captured.email)
        assertEquals(UserRole.USER, userSlot.captured.role)
        verify(exactly = 1) { request.setAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE_KEY, any()) }
    }

    @Test
    @DisplayName("LoginUserArgumentResolver는 요청 속성에서 AuthenticatedUser를 추출하여 반환한다")
    fun resolverExtractsAuthenticatedUser() {
        val expectedUser = AuthenticatedUser(userId = 1004L, email = "user@bank.com", role = UserRole.USER)
        val webRequest = mockk<NativeWebRequest>()
        val parameter = mockk<MethodParameter>()

        every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns request
        every { request.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE_KEY) } returns expectedUser

        val resolved = resolver.resolveArgument(parameter, null, webRequest, null)

        assertEquals(expectedUser, resolved)
    }
}

