package com.bank.system.domain

interface PgPort {
    fun pay(orderId: String, amount: Long): Boolean
    fun cancel(orderId: String, amount: Long): Boolean
    fun queryStatus(orderId: String): PgStatusResponse
}
