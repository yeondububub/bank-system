package com.bank.system.domain.exception

class UnapprovedAccountException(
    message: String = "승인 완료된 활성 계좌(ACTIVE)만 거래할 수 있습니다."
) : RuntimeException(message)
