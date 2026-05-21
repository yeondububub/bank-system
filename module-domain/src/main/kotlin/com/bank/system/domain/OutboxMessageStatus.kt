package com.bank.system.domain

enum class OutboxMessageStatus {
    PENDING,
    PROCESSED,
    FAILED
}
