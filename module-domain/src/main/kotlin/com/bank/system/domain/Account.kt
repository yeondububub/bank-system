package com.bank.system.domain

class Account(
    val id: Long? = null,
    val ownerId: Long,
    var balance: Long
) {
    fun withdraw(amount: Long) {
        require(amount > 0) { "출금 금액은 0보다 커야 합니다."}

        check(this.balance >= amount) {
            "잔액이 부족합니다. (현재 잔액: $balance, 결제 요청 금액: $amount"
        }

        this.balance -= amount
    }

    fun deposit(amount: Long) {
        require(amount > 0) { "입금 금액은 0보다 커야 합니다." }
        this.balance += amount
    }
}