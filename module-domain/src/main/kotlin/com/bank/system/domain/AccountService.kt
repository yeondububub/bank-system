package com.bank.system.domain

class AccountService(
    private val accountRepository: AccountRepository
) {
    fun createAccount(account: Account): Account {
        return accountRepository.save(account)
    }

    fun getByOwnerId(ownerId: Long): Account? {
        return accountRepository.findByOwnerId(ownerId)
    }
}
