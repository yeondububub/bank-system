package com.bank.system.domain

interface AccountRepository {
    fun save(account: Account): Account
    fun findByOwnerId(ownerId: Long): Account?
    fun findByOwnerIdWithLock(ownerId: Long): Account?
}