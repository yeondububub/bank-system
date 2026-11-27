package com.bank.system.api.dto

import com.bank.system.domain.Account
import com.bank.system.domain.AccountStatus
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

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
    val ownerId: Long
)

data class TransferRequest(
    @field:NotBlank(message = "출금 계좌번호는 필수입니다.")
    val fromAccountNumber: String,
    @field:NotBlank(message = "입금 계좌번호는 필수입니다.")
    val toAccountNumber: String,
    @field:Positive(message = "송금 금액은 0원보다 커야 합니다.")
    val amount: Long,
    val memo: String? = null
)

data class TransferResponse(
    val transactionId: String,
    @JsonSerialize(using = ToStringSerializer::class)
    val fromAccountId: Long,
    val fromAccountNumber: String,
    val toAccountNumber: String,
    val amount: Long,
    val balanceAfterFrom: Long,
    val timestamp: String
)

data class AccountHolderResponse(
    val accountNumber: String,
    @JsonSerialize(using = ToStringSerializer::class)
    val ownerId: Long,
    val ownerName: String
)
