package com.bank.system.api.dto

import com.bank.system.domain.Account
import com.bank.system.domain.AccountStatus
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

data class AccountResponse(
    @JsonSerialize(using = ToStringSerializer::class)
    val id: Long?,
    val ownerId: Long,
    val accountNumber: String,
    val balance: Long,
    val status: AccountStatus,
    val isPrimary: Boolean
) {
    companion object {
        fun from(account: Account): AccountResponse {
            return AccountResponse(
                id = account.id,
                ownerId = account.ownerId,
                accountNumber = account.accountNumber,
                balance = account.balance,
                status = account.status,
                isPrimary = account.isPrimary
            )
        }
    }
}

data class CreateAccountRequest(
    val ownerId: Long,
    val initialBalance: Long = 1000000L
)
