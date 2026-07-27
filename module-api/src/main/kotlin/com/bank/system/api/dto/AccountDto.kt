package com.bank.system.api.dto

import com.bank.system.domain.Account
import com.bank.system.domain.AccountStatus

data class AccountResponse(
    val id: Long?,
    val ownerId: Long,
    val accountNumber: String,
    val balance: Long,
    val status: AccountStatus
) {
    companion object {
        fun from(account: Account): AccountResponse {
            return AccountResponse(
                id = account.id,
                ownerId = account.ownerId,
                accountNumber = account.accountNumber,
                balance = account.balance,
                status = account.status
            )
        }
    }
}

data class CreateAccountRequest(
    val ownerId: Long,
    val initialBalance: Long = 1000000L
)
