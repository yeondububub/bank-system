package com.bank.system.domain

import java.time.LocalDateTime

class Payment(
    val id: Long? = null,
    val orderId: String,
    val buyerId: Long,
    val amount: Long,
    var status: PaymentStatus = PaymentStatus.PENDING,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(amount > 0) { "결재 금액은 0보다 커야 합니다. (요청 금액: $amount)" }
    }

    fun prepareApproval() {
        check(this.status == PaymentStatus.PENDING) {
            "대기(PENDING) 상태의 결제만 승인 준비를 할 수 있습니다. (현재 상태: $status)"
        }
        this.status = PaymentStatus.APPROVING
        this.updatedAt = LocalDateTime.now()
    }

    fun approve() {
        check(this.status == PaymentStatus.APPROVING) {
            "승인 중(APPROVING) 상태의 결제만 승인 완료할 수 있습니다. (현재 상태: $status)"
        }
        this.status = PaymentStatus.SUCCESS
        this.updatedAt = LocalDateTime.now()
    }

    fun fail() {
        check(this.status == PaymentStatus.PENDING || this.status == PaymentStatus.APPROVING) {
            "대기(PENDING) 또는 승인 중(APPROVING) 상태의 결제만 실패 처리할 수 있습니다. (현재 상태: $status)"
        }
        this.status = PaymentStatus.FAILED
        this.updatedAt = LocalDateTime.now()
    }

    fun cancel() {
        check(this.status == PaymentStatus.SUCCESS) {
            "이미 완료된 결제만 취소할 수 있습니다. (현재 상태: $status)"
        }
        this.status = PaymentStatus.CANCELLED
        this.updatedAt = LocalDateTime.now()
    }
}