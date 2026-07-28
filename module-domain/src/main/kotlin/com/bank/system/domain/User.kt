package com.bank.system.domain

import java.time.LocalDateTime

data class User(
    val id: Long? = null,
    val email: String,
    val password: String,
    val name: String,
    val role: UserRole = UserRole.USER,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(email.isNotBlank()) { "이메일은 필수입니다." }
        require(password.isNotBlank()) { "비밀번호는 필수입니다." }
        require(name.isNotBlank()) { "이름은 필수입니다." }
    }
}
