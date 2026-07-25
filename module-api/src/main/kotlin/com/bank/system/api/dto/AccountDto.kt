package com.bank.system.api.dto

import com.bank.system.domain.Account

data class AccountResponse(
    val id: Long?,
    val ownerId: Long,
    val balance: Long
) {
    companion object {
        fun from(account: Account): AccountResponse {
            return AccountResponse(
                id = account.id,
                ownerId = account.ownerId,
                balance = account.balance
            )
        }
    }
}

data class CreateAccountRequest(
    val ownerId: Long,
    val initialBalance: Long
)
