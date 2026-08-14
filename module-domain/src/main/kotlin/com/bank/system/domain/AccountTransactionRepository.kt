package com.bank.system.domain

interface AccountTransactionRepository {
    fun save(transaction: AccountTransaction): AccountTransaction
    fun findAllByAccountNumberOrderByCreatedAtDesc(accountNumber: String): List<AccountTransaction>
    fun findAllByAccountNumberInOrderByCreatedAtDesc(accountNumbers: List<String>): List<AccountTransaction>
    fun findAllOrderByCreatedAtDesc(): List<AccountTransaction>
    fun findAllPaged(page: Int, size: Int, accountNumber: String? = null): PageResult<AccountTransaction>
}

data class PageResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
)
