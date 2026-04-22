package com.bank.system.common.exception

class IdempotencyException(
    message: String = "현재 처리 중이거나 이미 완료된 요청입니다."
) : RuntimeException(message)
