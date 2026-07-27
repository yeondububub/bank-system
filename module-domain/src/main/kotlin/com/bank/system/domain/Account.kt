package com.bank.system.domain

import com.bank.system.domain.exception.InsufficientBalanceException
import com.bank.system.domain.exception.InvalidRequestException
import com.bank.system.domain.exception.UnapprovedAccountException

class Account(
    val id: Long? = null,
    val ownerId: Long,
    val accountNumber: String,
    var balance: Long,
    var status: AccountStatus = AccountStatus.PENDING
) {
    init {
        require(accountNumber.isNotBlank()) { "계좌번호는 필수입니다." }
    }

    fun withdraw(amount: Long) {
        if (this.status != AccountStatus.ACTIVE) {
            throw UnapprovedAccountException("승인 완료된 활성 계좌(ACTIVE)만 출금할 수 있습니다. (현재 상태: ${this.status})")
        }

        if (amount <= 0) {
            throw InvalidRequestException("출금 금액은 0보다 커야 합니다.")
        }

        if (this.balance < amount) {
            throw InsufficientBalanceException("잔액이 부족합니다. (현재 잔액: $balance, 결제 요청 금액: $amount)")
        }

        this.balance -= amount
    }

    fun deposit(amount: Long) {
        if (this.status != AccountStatus.ACTIVE) {
            throw UnapprovedAccountException("승인 완료된 활성 계좌(ACTIVE)만 입금할 수 있습니다. (현재 상태: ${this.status})")
        }

        if (amount <= 0) {
            throw InvalidRequestException("입금 금액은 0보다 커야 합니다.")
        }
        this.balance += amount
    }

    fun approve() {
        if (this.status != AccountStatus.PENDING) {
            throw InvalidRequestException("승인 대기 중인 계좌(PENDING)만 승인할 수 있습니다.")
        }
        this.status = AccountStatus.ACTIVE
    }

    fun reject() {
        if (this.status != AccountStatus.PENDING) {
            throw InvalidRequestException("승인 대기 중인 계좌(PENDING)만 거절할 수 있습니다.")
        }
        this.status = AccountStatus.REJECTED
    }

    fun suspend() {
        this.status = AccountStatus.SUSPENDED
    }
}