package com.bank.system.api.application

import com.bank.system.api.config.JwtTokenProvider
import com.bank.system.api.config.PasswordEncoder
import com.bank.system.api.dto.LoginRequest
import com.bank.system.api.dto.SignUpRequest
import com.bank.system.common.util.SnowflakeIdGenerator
import com.bank.system.domain.User
import com.bank.system.domain.UserService
import com.bank.system.domain.exception.InvalidPasswordException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AuthFacadeTest {

    private val userService = mockk<UserService>()
    private val passwordEncoder = PasswordEncoder()
    private val jwtTokenProvider = JwtTokenProvider("test_secret_key_123456789")
    private val snowflakeIdGenerator = SnowflakeIdGenerator(1, 1)

    private val authFacade = AuthFacade(userService, passwordEncoder, jwtTokenProvider, snowflakeIdGenerator)

    @Test
    fun `회원가입 성공 - 비밀번호가 암호화되어 저장된다`() {
        // given
        val request = SignUpRequest("test@bank.com", "password123", "홍길동")
        val expectedUser = User(id = 1L, email = request.email, password = passwordEncoder.encode(request.password), name = request.name)

        every { userService.signUp(any()) } returns expectedUser

        // when
        val response = authFacade.signUp(request)

        // then
        assertEquals(1L, response.id)
        assertEquals("test@bank.com", response.email)
        assertEquals("홍길동", response.name)

        verify(exactly = 1) { userService.signUp(any()) }
    }

    @Test
    fun `로그인 성공 - 올바른 이메일과 비밀번호 시 토큰이 발급된다`() {
        // given
        val rawPassword = "password123"
        val encodedPassword = passwordEncoder.encode(rawPassword)
        val user = User(id = 10L, email = "login@bank.com", password = encodedPassword, name = "김이름")

        val loginRequest = LoginRequest("login@bank.com", rawPassword)

        every { userService.getByEmail("login@bank.com") } returns user

        // when
        val response = authFacade.login(loginRequest)

        // then
        assertNotNull(response.accessToken)
        assertEquals("Bearer", response.tokenType)
        assertEquals(10L, response.user.id)
    }

    @Test
    fun `로그인 실패 - 비밀번호 불일치 시 예외가 발생한다`() {
        // given
        val user = User(id = 10L, email = "login@bank.com", password = passwordEncoder.encode("correctPass"), name = "김이름")
        val loginRequest = LoginRequest("login@bank.com", "wrongPass")

        every { userService.getByEmail("login@bank.com") } returns user

        // when & then
        assertThrows<InvalidPasswordException> {
            authFacade.login(loginRequest)
        }
    }
}
