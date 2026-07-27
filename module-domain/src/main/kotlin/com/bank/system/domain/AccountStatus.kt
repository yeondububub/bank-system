package com.bank.system.domain

enum class AccountStatus {
    PENDING,    // 계좌 개설 신청 완료 (관리자 승인 대기 중)
    ACTIVE,     // 관리자 승인 완료 (정상 거래 가능)
    REJECTED,   // 관리자 승인 거절
    SUSPENDED   // 사고 신고 등으로 인한 거래 정지
}
