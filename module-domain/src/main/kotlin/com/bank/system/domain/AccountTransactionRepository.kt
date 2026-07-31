package com.bank.system.domain

interface AccountTransactionRepository {
    fun save(transaction: AccountTransaction): AccountTransaction
    fun findAllByAccountNumberOrderByCreatedAtDesc(accountNumber: String): List<AccountTransaction>
    fun findAllByAccountNumberInOrderByCreatedAtDesc(accountNumbers: List<String>): List<AccountTransaction>
}
