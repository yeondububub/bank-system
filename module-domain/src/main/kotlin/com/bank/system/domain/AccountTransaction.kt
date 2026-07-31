package com.bank.system.domain

import java.time.LocalDateTime

enum class TransactionType {
    TRANSFER_OUT,
    TRANSFER_IN,
    PAYMENT,
    REFUND
}

class AccountTransaction(
    val id: Long? = null,
    val accountNumber: String,
    val type: TransactionType,
    val amount: Long,
    val balanceAfter: Long,
    val counterpartyName: String? = null,
    val counterpartyAccountNumber: String? = null,
    val memo: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
