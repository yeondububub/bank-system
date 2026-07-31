package com.bank.system.api.dto

import com.bank.system.domain.AccountTransaction
import com.bank.system.domain.TransactionType
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.format.DateTimeFormatter

data class AccountTransactionResponse(
    @JsonSerialize(using = ToStringSerializer::class)
    val id: Long?,
    val accountNumber: String,
    val type: TransactionType,
    val amount: Long,
    val balanceAfter: Long,
    val counterpartyName: String?,
    val counterpartyAccountNumber: String?,
    val memo: String?,
    val createdAt: String
) {
    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun from(tx: AccountTransaction): AccountTransactionResponse {
            return AccountTransactionResponse(
                id = tx.id,
                accountNumber = tx.accountNumber,
                type = tx.type,
                amount = tx.amount,
                balanceAfter = tx.balanceAfter,
                counterpartyName = tx.counterpartyName,
                counterpartyAccountNumber = tx.counterpartyAccountNumber,
                memo = tx.memo,
                createdAt = tx.createdAt.format(FORMATTER)
            )
        }
    }
}
