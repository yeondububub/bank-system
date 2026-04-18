package com.bank.system.domain

interface PaymentHistoryRepository {
    fun save(history: PaymentHistory): PaymentHistory
}