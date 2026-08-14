package com.bank.system.api.controller

import com.bank.system.api.dto.AccountTransactionResponse
import com.bank.system.api.dto.TransactionPageResponse
import com.bank.system.domain.AccountTransactionRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/transactions")
class AdminTransactionController(
    private val accountTransactionRepository: AccountTransactionRepository
) {

    @GetMapping
    fun getAllTransactions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) accountNumber: String?
    ): ResponseEntity<TransactionPageResponse> {
        val result = accountTransactionRepository.findAllPaged(page, size, accountNumber)
        val response = TransactionPageResponse(
            content = result.content.map { AccountTransactionResponse.from(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            isLast = result.isLast
        )
        return ResponseEntity.ok(response)
    }
}
