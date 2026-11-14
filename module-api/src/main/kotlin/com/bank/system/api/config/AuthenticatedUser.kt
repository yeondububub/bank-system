package com.bank.system.api.config

import com.bank.system.domain.UserRole

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class LoginUser

data class AuthenticatedUser(
    val userId: Long,
    val email: String,
    val role: UserRole
) {
    fun isAdmin(): Boolean = role == UserRole.ADMIN

    companion object {
        const val REQUEST_ATTRIBUTE_KEY = "AUTHENTICATED_USER"
    }
}

