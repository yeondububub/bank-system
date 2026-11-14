package com.bank.system.api.config

import com.bank.system.common.exception.BusinessException
import com.bank.system.common.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class LoginUserArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(LoginUser::class.java) &&
            AuthenticatedUser::class.java.isAssignableFrom(parameter.parameterType)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): AuthenticatedUser {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED, "요청 컨텍스트를 확인할 수 없습니다.")

        return request.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE_KEY) as? AuthenticatedUser
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED, "인증 정보를 확인할 수 없습니다.")
    }
}

