package com.bank.system.api.dto

data class TransactionPageResponse(
    val content: List<AccountTransactionResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
)
