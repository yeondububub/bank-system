package com.bank.system.domain

interface PgPort {
    fun pay(orderId: String, amount: Long): Boolean
}
