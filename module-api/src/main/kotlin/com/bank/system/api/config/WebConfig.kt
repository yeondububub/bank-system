package com.bank.system.api.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val userAuthenticationInterceptor: UserAuthenticationInterceptor,
    private val adminAuthorizationInterceptor: AdminAuthorizationInterceptor,
    private val loginUserArgumentResolver: LoginUserArgumentResolver
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(userAuthenticationInterceptor)
            .order(1)
            .addPathPatterns("/api/v1/**")
            .excludePathPatterns(
                "/api/v1/auth/signup",
                "/api/v1/auth/login"
            )

        registry.addInterceptor(adminAuthorizationInterceptor)
            .order(2)
            .addPathPatterns("/api/v1/admin/**")
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(loginUserArgumentResolver)
    }
}
