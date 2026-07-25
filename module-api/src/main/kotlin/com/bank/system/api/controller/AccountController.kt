package com.bank.system.api.controller

import com.bank.system.api.dto.AccountResponse
import com.bank.system.api.dto.CreateAccountRequest
import com.bank.system.domain.Account
import com.bank.system.domain.AccountRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val accountRepository: AccountRepository
) {

    @GetMapping("/{ownerId}")
    fun getAccount(@PathVariable ownerId: Long): ResponseEntity<AccountResponse> {
        val account = accountRepository.findByOwnerId(ownerId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(AccountResponse.from(account))
    }

    @PostMapping
    fun createAccount(
        @RequestParam(required = false) ownerId: Long?,
        @RequestParam(required = false) initialBalance: Long?,
        @RequestBody(required = false) request: CreateAccountRequest?
    ): ResponseEntity<AccountResponse> {
        val finalOwnerId = request?.ownerId ?: ownerId ?: throw IllegalArgumentException("ownerId는 필수입니다.")
        val finalBalance = request?.initialBalance ?: initialBalance ?: 1000000L

        val account = Account(ownerId = finalOwnerId, balance = finalBalance)
        val savedAccount = accountRepository.save(account)

        return ResponseEntity.ok(AccountResponse.from(savedAccount))
    }
}