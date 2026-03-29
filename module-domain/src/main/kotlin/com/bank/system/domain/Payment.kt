package com.bank.system.domain

class Payment(
    val id: Long? = null,
    val orderId: String,
    val buyerId: Long,
    val amount: Long,
    var status: PaymentStatus = PaymentStatus.PENDING
) {
    init {
        require(amount > 0) { "결재 금액은 0보다 커야 합니다. (요청 금액: $amount)" }
    }

    fun approve() {
        check(this.status == PaymentStatus.PENDING) {
            "대기(PENDING) 상태의 결제만 승인할 수 있습니다. (현재 상태: $status)"
        }
        this.status = PaymentStatus.SUCCESS
    }

    fun fail() {
        check(this.status == PaymentStatus.PENDING) {
            "대기 상태의 결제만 실패 처리할 수 있습니다."
        }
        this.status = PaymentStatus.FAILED
    }

    fun cancel() {
        check(this.status == PaymentStatus.SUCCESS) {
            "이미 완료된 결제만 취소할 수 있습니다."
        }
        this.status = PaymentStatus.CANCELLED
    }
}