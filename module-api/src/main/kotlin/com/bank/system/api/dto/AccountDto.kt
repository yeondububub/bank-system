package com.bank.system.api.dto

import com.bank.system.domain.Account
import com.bank.system.domain.AccountStatus
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

data class AccountResponse(
    @JsonSerialize(using = ToStringSerializer::class)
    val id: Long?,
    val ownerId: Long,
    val accountNumber: String,
    val balance: Long,
    val status: AccountStatus,
    @param:JsonProperty("isPrimary") @get:JsonProperty("isPrimary")
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

data class TransferRequest(
    val fromAccountNumber: String,
    val toAccountNumber: String,
    val amount: Long,
    val memo: String? = null
)

data class TransferResponse(
    val transactionId: String,
    val fromAccountId: Long,
    val fromAccountNumber: String,
    val toAccountNumber: String,
    val amount: Long,
    val balanceAfterFrom: Long,
    val timestamp: String
)
