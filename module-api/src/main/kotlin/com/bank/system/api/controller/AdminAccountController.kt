package com.bank.system.api.controller

import com.bank.system.api.dto.AccountResponse
import com.bank.system.domain.AccountService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/accounts")
class AdminAccountController(
    private val accountService: AccountService
) {

    @GetMapping("/pending")
    fun getPendingAccounts(): ResponseEntity<List<AccountResponse>> {
        val pendingAccounts = accountService.getPendingAccounts()
        val response = pendingAccounts.map { AccountResponse.from(it) }
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{accountId}/approve")
    fun approveAccount(@PathVariable accountId: Long): ResponseEntity<AccountResponse> {
        val approvedAccount = accountService.approveAccount(accountId)
        return ResponseEntity.ok(AccountResponse.from(approvedAccount))
    }

    @PostMapping("/{accountId}/reject")
    fun rejectAccount(@PathVariable accountId: Long): ResponseEntity<AccountResponse> {
        val rejectedAccount = accountService.rejectAccount(accountId)
        return ResponseEntity.ok(AccountResponse.from(rejectedAccount))
    }
}
