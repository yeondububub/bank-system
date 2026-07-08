package com.bank.system.domain

enum class PgTransactionStatus {
    SUCCESS,
    FAILED,
    NOT_FOUND
}

data class PgStatusResponse(
    val status: PgTransactionStatus,
    val transactionId: String?
)
