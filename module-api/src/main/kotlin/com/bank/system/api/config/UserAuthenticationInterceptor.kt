package com.bank.system.api.config

import com.bank.system.common.exception.BusinessException
import com.bank.system.common.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class UserAuthenticationInterceptor(
    private val jwtTokenProvider: JwtTokenProvider
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (request.method.equals("OPTIONS", ignoreCase = true)) {
            return true
        }

        val authHeader = request.getHeader(AUTHORIZATION_HEADER)
        if (authHeader.isNullOrBlank() || !authHeader.startsWith(BEARER_PREFIX)) {
            throw BusinessException(ErrorCode.UNAUTHORIZED, "인증 토큰이 필요합니다.")
        }

        val token = authHeader.substring(BEARER_PREFIX.length).trim()
        if (token.isEmpty() || !jwtTokenProvider.validateToken(token)) {
            throw BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않거나 만료된 토큰입니다.")
        }

        val claims = try {
            jwtTokenProvider.parseClaims(token)
        } catch (e: IllegalArgumentException) {
            throw BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않거나 만료된 토큰입니다.")
        }

        request.setAttribute(
            AuthenticatedUser.REQUEST_ATTRIBUTE_KEY,
            AuthenticatedUser(
                userId = claims.userId,
                email = claims.email,
                role = claims.role
            )
        )

        return true
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}

