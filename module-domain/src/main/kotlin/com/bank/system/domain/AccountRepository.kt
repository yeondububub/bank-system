package com.bank.system.domain

interface AccountRepository {
    fun save(account: Account): Account
    fun findById(id: Long): Account?
    fun findByIdWithLock(id: Long): Account?
    fun findByOwnerId(ownerId: Long): Account?
    fun findAllByOwnerId(ownerId: Long): List<Account>
    fun findPrimaryByOwnerId(ownerId: Long): Account?
    fun findByOwnerIdWithLock(ownerId: Long): Account?
    fun findByAccountNumber(accountNumber: String): Account?
    fun existsByAccountNumber(accountNumber: String): Boolean
    fun findPendingAccounts(): List<Account>
}