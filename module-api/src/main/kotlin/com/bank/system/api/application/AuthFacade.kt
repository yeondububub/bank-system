package com.bank.system.api.application

import com.bank.system.api.config.JwtTokenProvider
import com.bank.system.api.config.PasswordEncoder
import com.bank.system.api.dto.LoginRequest
import com.bank.system.api.dto.SignUpRequest
import com.bank.system.api.dto.TokenResponse
import com.bank.system.api.dto.UserResponse
import com.bank.system.common.util.SnowflakeIdGenerator
import com.bank.system.domain.User
import com.bank.system.domain.UserService
import com.bank.system.domain.exception.InvalidPasswordException
import com.bank.system.domain.exception.UserNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthFacade(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val snowflakeIdGenerator: SnowflakeIdGenerator
) {

    @Transactional
    fun signUp(request: SignUpRequest): UserResponse {
        val encodedPassword = passwordEncoder.encode(request.password)
        val user = User(
            id = snowflakeIdGenerator.nextId(),
            email = request.email,
            password = encodedPassword,
            name = request.name
        )
        val savedUser = userService.signUp(user)
        return UserResponse.from(savedUser)
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): TokenResponse {
        val user = userService.getByEmail(request.email)
            ?: throw UserNotFoundException(request.email)

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidPasswordException()
        }

        val token = jwtTokenProvider.createToken(user.id!!, user.email)
        return TokenResponse(
            accessToken = token,
            user = UserResponse.from(user)
        )
    }

    @Transactional(readOnly = true)
    fun getMyInfo(userId: Long): UserResponse {
        val user = userService.getById(userId)
            ?: throw UserNotFoundException("id: $userId")
        return UserResponse.from(user)
    }
}
