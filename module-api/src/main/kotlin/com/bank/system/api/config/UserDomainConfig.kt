package com.bank.system.api.config

import com.bank.system.domain.UserRepository
import com.bank.system.domain.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UserDomainConfig {
    @Bean
    fun userService(userRepository: UserRepository): UserService {
        return UserService(userRepository)
    }
}
