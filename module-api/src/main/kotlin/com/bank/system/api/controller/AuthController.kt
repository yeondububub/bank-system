package com.bank.system.api.controller

import com.bank.system.api.application.AuthFacade
import com.bank.system.api.config.AuthenticatedUser
import com.bank.system.api.config.LoginUser
import com.bank.system.api.dto.LoginRequest
import com.bank.system.api.dto.SignUpRequest
import com.bank.system.api.dto.TokenResponse
import com.bank.system.api.dto.UserResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authFacade: AuthFacade
) {

    @PostMapping("/signup")
    fun signUp(@Valid @RequestBody request: SignUpRequest): ResponseEntity<UserResponse> {
        val userResponse = authFacade.signUp(request)
        return ResponseEntity.ok(userResponse)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        val tokenResponse = authFacade.login(request)
        return ResponseEntity.ok(tokenResponse)
    }

    @GetMapping("/me")
    fun getMyInfo(@LoginUser loginUser: AuthenticatedUser): ResponseEntity<UserResponse> {
        val userResponse = authFacade.getMyInfo(loginUser.userId)
        return ResponseEntity.ok(userResponse)
    }
}
