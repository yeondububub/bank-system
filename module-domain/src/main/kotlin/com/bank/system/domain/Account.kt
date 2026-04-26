package com.bank.system.domain

import com.bank.system.domain.exception.InsufficientBalanceException
import com.bank.system.domain.exception.InvalidRequestException

class Account(
    val id: Long? = null,
    val ownerId: Long,
    var balance: Long
) {
    fun withdraw(amount: Long) {
        if (amount <= 0) {
            throw InvalidRequestException("출금 금액은 0보다 커야 합니다.")
        }

        if (this.balance < amount) {
            throw InsufficientBalanceException("잔액이 부족합니다. (현재 잔액: $balance, 결제 요청 금액: $amount)")
        }

        this.balance -= amount
    }

    fun deposit(amount: Long) {
        if (amount <= 0) {
            throw InvalidRequestException("입금 금액은 0보다 커야 합니다.")
        }
        this.balance += amount
    }
}