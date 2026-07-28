package com.bank.system.api.config

import com.bank.system.common.exception.BusinessException
import com.bank.system.common.exception.ErrorCode
import com.bank.system.domain.UserRole
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AdminAuthorizationInterceptor(
    private val jwtTokenProvider: JwtTokenProvider
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (request.method == "OPTIONS") {
            return true
        }

        val authHeader = request.getHeader("Authorization")
        if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
            throw BusinessException(ErrorCode.UNAUTHORIZED, "관리자 권한 인증 토큰이 필요합니다.")
        }

        val token = authHeader.substring(7)
        if (!jwtTokenProvider.validateToken(token)) {
            throw BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않거나 만료된 토큰입니다.")
        }

        val role = jwtTokenProvider.getUserRoleFromToken(token)
        if (role != UserRole.ADMIN) {
            throw BusinessException(ErrorCode.FORBIDDEN, "관리자 전용 기능입니다. 접근 권한이 없습니다.")
        }

        return true
    }
}
