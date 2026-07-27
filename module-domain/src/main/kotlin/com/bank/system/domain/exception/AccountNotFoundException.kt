package com.bank.system.domain.exception

class AccountNotFoundException(
    val identifier: String
) : RuntimeException("계좌 정보를 찾을 수 없습니다. (식별자: $identifier)")
