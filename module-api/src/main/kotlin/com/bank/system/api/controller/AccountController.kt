package com.bank.system.api.controller

import com.bank.system.domain.Account
import com.bank.system.domain.AccountRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val accountRepository: AccountRepository
) {
    @PostMapping
    fun createAccount(@RequestParam ownerId: Long, @RequestParam initialBalance: Long) : Account {
        val account = Account(ownerId = ownerId, balance = initialBalance)
        return accountRepository.save(account)
    }
}