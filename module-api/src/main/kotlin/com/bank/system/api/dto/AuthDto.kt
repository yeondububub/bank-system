package com.bank.system.api.dto

import com.bank.system.domain.User
import com.bank.system.domain.UserRole
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignUpRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다.")
    val password: String,

    @field:NotBlank(message = "이름은 필수입니다.")
    val name: String,

    val role: UserRole = UserRole.USER
)

data class LoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    val password: String
)

data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val user: UserResponse
)

data class UserResponse(
    @JsonSerialize(using = ToStringSerializer::class)
    val id: Long,
    val email: String,
    val name: String,
    val role: UserRole
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id!!,
                email = user.email,
                name = user.name,
                role = user.role
            )
        }
    }
}
