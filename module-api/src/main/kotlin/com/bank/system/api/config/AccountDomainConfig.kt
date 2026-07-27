package com.bank.system.api.config

import com.bank.system.domain.AccountRepository
import com.bank.system.domain.AccountService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AccountDomainConfig {
    @Bean
    fun accountService(accountRepository: AccountRepository): AccountService {
        return AccountService(accountRepository)
    }
}
