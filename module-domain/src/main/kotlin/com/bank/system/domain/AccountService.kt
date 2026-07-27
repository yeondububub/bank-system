package com.bank.system.domain

import com.bank.system.domain.exception.AccountNotFoundException

class AccountService(
    private val accountRepository: AccountRepository
) {
    fun createAccount(account: Account): Account {
        return accountRepository.save(account)
    }

    fun getByOwnerId(ownerId: Long): Account? {
        return accountRepository.findByOwnerId(ownerId)
    }

    fun getById(id: Long): Account? {
        return accountRepository.findById(id)
    }

    fun approveAccount(accountId: Long): Account {
        val account = accountRepository.findById(accountId)
            ?: throw AccountNotFoundException("id: $accountId")

        account.approve()
        return accountRepository.save(account)
    }

    fun rejectAccount(accountId: Long): Account {
        val account = accountRepository.findById(accountId)
            ?: throw AccountNotFoundException("id: $accountId")

        account.reject()
        return accountRepository.save(account)
    }

    fun getPendingAccounts(): List<Account> {
        return accountRepository.findPendingAccounts()
    }
}
